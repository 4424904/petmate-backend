// src/main/java/com/petmate/domain/auth/service/AuthService.java
package com.petmate.domain.auth.service;

import com.petmate.common.repository.mybatis.token.RefreshTokenMapper;
import com.petmate.domain.auth.dto.request.LoginRequestDto;
import com.petmate.domain.auth.dto.request.SignupRequestDto;
import com.petmate.domain.auth.dto.response.TokenResponseDto;
import com.petmate.domain.auth.dto.response.UserInfoResponseDto;
import com.petmate.domain.user.entity.UserEntity;
import com.petmate.domain.user.repository.jpa.UserRepository;
import com.petmate.security.jwt.JwtClaimAccessor;
import com.petmate.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @Value("${app.public-img-url}")
    private String imgBase; // 예: http://localhost:8090/img/

    // RefreshToken 관리 (추후 UserEntity 기반으로 변경 예정)
    private final RefreshTokenMapper refreshTokenMapper;

    // 메인 사용자 데이터 저장소
    private final UserRepository userRepository;

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    private String toImgUrl(String val){
        if (val == null || val.isBlank()) return null;
        if (val.startsWith("http://") || val.startsWith("https://")) return val;
        String base = imgBase.endsWith("/") ? imgBase : imgBase + "/";
        String file = val.startsWith("/") ? val.substring(1) : val;
        // val이 'img/uuid.png' 같은 상대경로여도 정상 처리
        if (file.startsWith("img/")) file = file.substring(4);
        return base + file;
    }
    /** 로그인 - 소셜 로그인으로만 처리됨 */
    public TokenResponseDto signin(LoginRequestDto request) {
        // 소셜 로그인을 통해서만 로그인이 처리되므로 이 메서드는 사용하지 않음
        log.warn("⚠️ 일반 로그인 시도 - 소셜 로그인을 이용해주세요");
        throw new RuntimeException("소셜 로그인을 이용해주세요.");
    }

    /** RefreshToken으로 AccessToken 재발급 */
    public TokenResponseDto refreshAccessToken(String refreshToken) {
        log.info("🔄 Refresh Token 처리 시작");
        if (jwtUtil.isExpired(refreshToken)) {
            log.error("❌ RefreshToken 만료됨");
            throw new RuntimeException("RefreshToken이 만료되었습니다. 다시 로그인하세요.");
        }
        Claims claims = jwtUtil.parse(refreshToken);
        String tokenType = JwtClaimAccessor.type(claims);
        log.info("🔍 토큰 타입 확인: 예상='refresh', 실제='{}'", tokenType);
        if (!"refresh".equals(tokenType)) {
            log.error("❌ 잘못된 토큰 타입: {}", tokenType);
            throw new RuntimeException("유효하지 않은 토큰 유형입니다.");
        }

        String userId = claims.getSubject();
        log.info("🔍 사용자 ID: {}", userId);

        // refresh token에서 email 가져와서 사용자 조회
        Object emailClaim = claims.get("email");
        if (emailClaim == null) {
            throw new RuntimeException("Refresh token에 email 정보가 없습니다.");
        }

        String email = String.valueOf(emailClaim);
        log.info("📧 Refresh Token Email: {}", email);

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        String newAccessToken = jwtUtil.issue(
                userId,
                jwtUtil.accessTtlMs(),
                JwtClaimAccessor.accessClaims(
                        user.getProvider(),
                        user.getEmail(),
                        user.getName(),
                        user.getNickName(),
                        toImgUrl(user.getProfileImage()),
                        user.getRole() != null ? user.getRole() : "1",
                        user.getBirthDate() != null ? user.getBirthDate().toString() : null,
                        user.getGender(),
                        user.getPhone()
                )
        );
        log.info("✅ AccessToken 재발급 완료 - 사용자: {}", email);
        return new TokenResponseDto(newAccessToken);
    }

    /** 회원가입 - 소셜 로그인으로만 처리됨 */
    public void signup(SignupRequestDto request) {
        // 소셜 로그인을 통해서만 회원가입이 처리되므로 이 메서드는 사용하지 않음
        log.warn("⚠️ 일반 회원가입 시도 - 소셜 로그인을 이용해주세요");
        throw new RuntimeException("소셜 로그인을 이용해주세요.");
    }

    /** 로그아웃 */
    public void signout(String refreshToken) {
        refreshTokenMapper.deleteByToken(refreshToken);
    }

    @Transactional(readOnly = true)
    public UserInfoResponseDto getUserInfoByEmail(String email) {
        UserEntity u = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        String birthDate = (u.getBirthDate()!=null)? u.getBirthDate().toString(): null;

        return new UserInfoResponseDto(
                String.valueOf(u.getId()),
                u.getEmail(),
                u.getName(),
                u.getNickName(),
                toImgUrl(u.getProfileImage()),  // <- 절대 URL로 변환
                u.getProvider(),
                u.getRole(),
                birthDate,
                u.getGender(),
                u.getPhone()
        );
    }

    private static String nz(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }
}
