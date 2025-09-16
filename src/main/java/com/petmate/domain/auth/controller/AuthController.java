// src/main/java/com/petmate/domain/auth/controller/AuthController.java
package com.petmate.domain.auth.controller;

import com.petmate.domain.auth.dto.request.LoginRequestDto;
import com.petmate.domain.auth.dto.request.SignupRequestDto;
import com.petmate.domain.auth.dto.response.TokenResponseDto;
import com.petmate.domain.auth.dto.response.UserInfoResponseDto;
import com.petmate.domain.auth.service.AuthService;
import com.petmate.security.jwt.JwtClaimAccessor;
import com.petmate.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    /** 로그인 */
    @PostMapping("/signin")
    public ResponseEntity<TokenResponseDto> signin(
            @RequestBody LoginRequestDto request,
            HttpServletRequest req
    ) {
        TokenResponseDto result = authService.signin(request);

        boolean isLocal = "localhost".equals(req.getServerName()) || "127.0.0.1".equals(req.getServerName());
        String sameSite = isLocal ? "Lax" : "None";
        boolean secure  = !isLocal;

        // refreshToken만 HttpOnly 쿠키로 저장
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", result.getRefreshToken())
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new TokenResponseDto(result.getAccessToken())); // accessToken은 바디로만
    }

    /** 회원가입 */
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequestDto request) {
        authService.signup(request);
        return ResponseEntity.ok("회원가입 성공");
    }

    /** 로그아웃 */
    @PostMapping("/signout")
    public ResponseEntity<String> signout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest req
    ) {
        if (refreshToken != null) authService.signout(refreshToken);

        boolean isLocal = "localhost".equals(req.getServerName()) || "127.0.0.1".equals(req.getServerName());
        String sameSite = isLocal ? "Lax" : "None";
        boolean secure  = !isLocal;

        // accessToken은 프론트 보관 가정. 서버는 refreshToken만 정리.
        ResponseCookie clearRefresh = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(secure).sameSite(sameSite)
                .path("/").maxAge(0).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefresh.toString())
                .body("로그아웃 성공");
    }

    /** AccessToken 재발급 */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refresh(HttpServletRequest request) {
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("refreshToken".equals(c.getName())) {
                    refreshToken = c.getValue();
                    break;
                }
            }
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TokenResponseDto newToken = authService.refreshAccessToken(refreshToken);

        boolean isLocal = "localhost".equals(request.getServerName()) || "127.0.0.1".equals(request.getServerName());
        String sameSite = isLocal ? "Lax" : "None";
        boolean secure  = !isLocal;

        if (newToken.getRefreshToken() != null && !newToken.getRefreshToken().isBlank()) {
            // 로테이션된 새 refreshToken 쿠키 갱신
            ResponseCookie rc = ResponseCookie.from("refreshToken", newToken.getRefreshToken())
                    .httpOnly(true).secure(secure).sameSite(sameSite)
                    .path("/").maxAge(7 * 24 * 60 * 60).build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, rc.toString())
                    .body(new TokenResponseDto(newToken.getAccessToken()));
        }
        return ResponseEntity.ok(new TokenResponseDto(newToken.getAccessToken()));
    }

    /** 내 정보 조회: 만료 시 자동 재발급하지 않음. 무조건 401 */
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponseDto> me(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            HttpServletRequest req
    ) {
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        if (token == null && req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if ("accessToken".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // 만료/위조 등은 JwtUtil에서 예외로 던지고, ApiExceptionHandler가 401 JSON으로 정리
        if (jwtUtil.isExpired(token)) {
            // 명시적 401. 프론트는 /auth/refresh 호출 후 재시도.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Claims claims = jwtUtil.parse(token);
        if (!"access".equals(JwtClaimAccessor.type(claims))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserInfoResponseDto dto = new UserInfoResponseDto(
                claims.getSubject(),
                JwtClaimAccessor.email(claims),
                JwtClaimAccessor.name(claims),
                JwtClaimAccessor.nickname(claims),
                JwtClaimAccessor.picture(claims),
                JwtClaimAccessor.provider(claims),
                JwtClaimAccessor.role(claims),
                JwtClaimAccessor.birthDate(claims),
                JwtClaimAccessor.gender(claims),
                JwtClaimAccessor.phone(claims)
        );
        return ResponseEntity.ok(dto);
    }
}
