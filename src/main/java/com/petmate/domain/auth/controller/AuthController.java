// src/main/java/com/petmate/domain/auth/controller/AuthController.java
package com.petmate.domain.auth.controller;

import com.petmate.domain.auth.dto.response.TokenResponseDto;
import com.petmate.domain.auth.dto.response.UserInfoResponseDto;
import com.petmate.domain.auth.service.AuthService;
import com.petmate.security.jwt.JwtClaimAccessor;
import com.petmate.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    private boolean isLocal(HttpServletRequest req) {
        String host = req.getServerName();
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
    }

    private ResponseCookie buildRefreshCookie(String value, boolean local) {
        String sameSite = local ? "Lax" : "None";
        boolean secure  = !local;
        return ResponseCookie.from("refreshToken", value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();
    }

    /** 로그아웃: refresh 쿠키 제거 (권한 없어도 호출 가능) */
    @PostMapping("/signout")
    public ResponseEntity<String> signout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest req
    ) {
        if (refreshToken != null) authService.signout(refreshToken);

        boolean local = isLocal(req);
        ResponseCookie clearRefresh = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(!local)
                .sameSite(local ? "Lax" : "None")
                .path("/")
                .maxAge(0)
                .build();

        log.info("[AUTH] signout ok - clearCookie={}", clearRefresh);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefresh.toString())
                .body("로그아웃 성공");
    }

    /** AccessToken 재발급 (refresh 쿠키 필요) */
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
            log.warn("[AUTH] refresh 401 - no refreshToken cookie. serverName={}", request.getServerName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TokenResponseDto newToken = authService.refreshAccessToken(refreshToken);

        boolean local = isLocal(request);
        if (newToken.getRefreshToken() != null && !newToken.getRefreshToken().isBlank()) {
            ResponseCookie rc = buildRefreshCookie(newToken.getRefreshToken(), local);
            log.info("[AUTH] refresh ok - new refresh cookie set. local={}", local);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, rc.toString())
                    .body(new TokenResponseDto(newToken.getAccessToken()));
        }
        log.info("[AUTH] refresh ok - access only");
        return ResponseEntity.ok(new TokenResponseDto(newToken.getAccessToken()));
    }

    /** 내 정보 조회 (access 토큰만 검증) */
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponseDto> me(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            HttpServletRequest req
    ) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) token = authHeader.substring(7);
        if (token == null && req.getCookies() != null) {
            for (Cookie c : req.getCookies()) if ("accessToken".equals(c.getName())) { token = c.getValue(); break; }
        }
        if (token == null || jwtUtil.isExpired(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Claims claims = jwtUtil.parse(token);
        if (!"access".equals(JwtClaimAccessor.type(claims))) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String email = JwtClaimAccessor.email(claims);
        if (email == null || email.isBlank()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        UserInfoResponseDto dto = authService.getUserInfoByEmail(email);
        if (dto == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(dto);
    }
}
