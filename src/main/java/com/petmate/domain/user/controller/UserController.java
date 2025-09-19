// src/main/java/com/petmate/domain/user/controller/UserController.java
package com.petmate.domain.user.controller;

import com.petmate.domain.user.dto.request.PetmateApplyRequest;
import com.petmate.domain.user.dto.request.UserUpdateRequest;
import com.petmate.domain.user.service.UserService;
import com.petmate.domain.user.entity.UserEntity;
import com.petmate.domain.user.repository.jpa.UserRepository;
import com.petmate.domain.auth.entity.RefreshTokenEntity;
import com.petmate.domain.auth.repository.RefreshTokenRepository;
import com.petmate.security.jwt.JwtClaimAccessor;
import com.petmate.security.jwt.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;          // ✅ jakarta로 교체
import jakarta.servlet.http.HttpServletResponse;         // ✅ jakarta로 교체
import jakarta.validation.constraints.Email;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ▼ 복구(API) 의존성
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.front-base-url:http://localhost:3000}")
    private String frontBaseUrl;

    // 상태코드 규약: 0=탈퇴, 1=기본, 2=펫메이트
    private static final String STATUS_WITHDRAWN = "0";
    private static final String STATUS_ACTIVE    = "1";

    /** 펫메이트 등록 (프로필/자격증 파일 포함) */
    @PostMapping(value = "/profile/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> applyProfile(
            @RequestParam("email") @Email String email,
            @RequestParam("targetRole") String targetRole,
            @ModelAttribute PetmateApplyRequest req
    ) {
        Long userId = userService.applyProfile(email, targetRole, req);
        return ResponseEntity.ok("프로필 등록/수정 완료! 사용자 ID: " + userId + ", 역할: " + targetRole);
    }

    @PostMapping(value = "/petmate/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> applyPetMate(
            @RequestParam("email") @Email String email,
            @ModelAttribute PetmateApplyRequest req
    ) {
        Long userId = userService.apply(email, req);
        return ResponseEntity.ok("펫메이트 신청 완료! 사용자 ID: " + userId);
    }

    @PostMapping(value = "/petowner/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> applyPetOwner(
            @RequestParam("email") @Email String email,
            @ModelAttribute PetmateApplyRequest req
    ) {
        Long userId = userService.applyPetOwner(email, req);
        return ResponseEntity.ok("반려인 신청 완료! 사용자 ID: " + userId);
    }

    /** 기본 유저 등록 */
    @PostMapping(value = "/apply", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> applyUser(
            @RequestParam("email") @Email String email,
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "nickName", required = false) String nickName,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "profileImageUrl", required = false) String profileImageUrl
    ) {
        Long userId = userService.applyBasicUser(email, provider, name, nickName, phone, gender, /* age 제거 */ null, profileImageUrl);
        return ResponseEntity.ok("유저 등록 완료! 사용자 ID: " + userId);
    }

    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMyProfile(
            @AuthenticationPrincipal String email,
            @RequestPart("dto") UserUpdateRequest dto,
            @RequestPart(value = "pictureFile", required = false) MultipartFile pictureFile
    ) {
        log.info("PUT /user/me (multipart) email={}, dto={}", email, dto);
        try {
            userService.updateMyInfo(email, dto, pictureFile);
            Map<String, Object> updatedUser = userService.findByEmail(email);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            log.error("프로필 수정 실패: email={}, error={}", email, e.getMessage(), e);
            return ResponseEntity.badRequest().body("프로필 수정 실패: " + e.getMessage());
        }
    }

    /** 회원 탈퇴 */
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyAccount(@AuthenticationPrincipal String email) {
        userService.withdraw(email);
        return ResponseEntity.ok("회원 탈퇴 처리되었습니다.");
    }

    // ========================= 신규: 탈퇴 계정 복구 =========================
    @PostMapping(value = "/restore", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> restore(@RequestBody RestoreRequest body,
                                     HttpServletRequest req,            // ✅ jakarta
                                     HttpServletResponse res) {         // ✅ jakarta
        if (body == null || body.getEmail() == null || body.getEmail().isBlank()
                || body.getProvider() == null || body.getProvider().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "email과 provider는 필수입니다."
            ));
        }

        UserEntity user = userRepository.findByEmail(body.getEmail()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "해당 이메일의 사용자가 없습니다."
            ));
        }

        // 탈퇴가 아닌 경우: 토큰만 재발급하여 로그인 완료
        if (!isWithdrawn(user.getStatus())) {
            String access = issueAccess(user);
            String refresh = issueRefresh(user);
            addRefreshCookie(res, refresh, isLocal(req));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "이미 활성 계정입니다.",
                    "accessToken", access
            ));
        }

        // 복구
        user.setStatus(STATUS_ACTIVE);
        userRepository.saveAndFlush(user);
        log.info("계정 복구 완료 email={}, status={}", user.getEmail(), user.getStatus());

        // 토큰 발급
        String access = issueAccess(user);
        String refresh = issueRefresh(user);
        addRefreshCookie(res, refresh, isLocal(req));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "계정이 복구되었습니다.",
                "accessToken", access
        ));
    }

    // ========================= 내부 유틸 =========================

    private boolean isWithdrawn(String status) {
        if (status == null) return false;
        String s = status.trim();
        return STATUS_WITHDRAWN.equals(s) ||
                "withdrawn".equalsIgnoreCase(s) ||
                "deleted".equalsIgnoreCase(s) ||
                "inactive".equalsIgnoreCase(s);
    }

    private String issueAccess(UserEntity u) {
        return jwtUtil.issue(
                String.valueOf(u.getId()),
                jwtUtil.accessTtlMs(),
                JwtClaimAccessor.accessClaims(
                        u.getProvider(),
                        u.getEmail(),
                        u.getName(),
                        u.getNickName(),
                        userService.findProfileImageByEmail(u.getEmail()),
                        u.getRole() != null ? u.getRole() : "1",
                        u.getBirthDate() != null ? u.getBirthDate().toString() : null,
                        u.getGender(),
                        u.getPhone()
                )
        );
    }

    private String issueRefresh(UserEntity u) {
        String refresh = jwtUtil.issue(
                String.valueOf(u.getId()),
                jwtUtil.refreshTtlMs(),
                JwtClaimAccessor.refreshClaims()
        );
        // 최대 5개 유지
        long cnt = refreshTokenRepository.countByUser(u);
        if (cnt >= 5) {
            var list = refreshTokenRepository.findByUserOrderByCreatedAtDesc(u);
            if (!list.isEmpty()) {
                refreshTokenRepository.delete(list.get(list.size() - 1));
            }
        }
        RefreshTokenEntity e = RefreshTokenEntity.builder()
                .user(u)
                .token(refresh)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtUtil.refreshTtlMs() / 1000))
                .build();
        refreshTokenRepository.save(e);
        return refresh;
    }

    private void addRefreshCookie(HttpServletResponse res, String value, boolean local) {
        String sameSite = local ? "Lax" : "None";
        boolean secure  = !local;
        ResponseCookie cookie = ResponseCookie.from("refreshToken", value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());  // ✅ addHeader 정상
    }

    private boolean isLocal(HttpServletRequest req) {
        String h = req.getServerName();                             // ✅ getServerName 정상
        return "localhost".equalsIgnoreCase(h) || "127.0.0.1".equals(h);
    }

    @Data
    public static class RestoreRequest {
        @Email
        private String email;
        private String provider;
    }
}
