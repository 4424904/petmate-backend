package com.petmate.domain.auth.controller;

import com.petmate.domain.auth.dto.response.UserInfoResponseDto;
import com.petmate.domain.auth.dto.request.LoginRequestDto;
import com.petmate.domain.auth.dto.request.SignupRequestDto;
import com.petmate.domain.auth.dto.response.TokenResponseDto;
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

    // 로그인
    @PostMapping("/signin")
    public ResponseEntity<TokenResponseDto> signin(@RequestBody LoginRequestDto request) {
        TokenResponseDto result = authService.signin(request);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", result.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new TokenResponseDto(result.getAccessToken()));
    }

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequestDto request) {
        authService.signup(request);
        return ResponseEntity.ok("회원가입 성공");
    }

    // 로그아웃
    @PostMapping("/signout")
    public ResponseEntity<String> signout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest req) {

        if (refreshToken != null) authService.signout(refreshToken);

        boolean isLocal = "localhost".equals(req.getServerName()) || "127.0.0.1".equals(req.getServerName());
        String sameSite = isLocal ? "Lax" : "None";
        boolean secure  = !isLocal;

        ResponseCookie clearAccess = ResponseCookie.from("accessToken", "")
                .httpOnly(true).secure(secure).sameSite(sameSite)
                .path("/").maxAge(0).build();

        ResponseCookie clearRefresh = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(secure).sameSite(sameSite)
                .path("/").maxAge(0).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccess.toString())
                .header(HttpHeaders.SET_COOKIE, clearRefresh.toString())
                .body("로그아웃 성공");
    }

    // AccessToken 재발급
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
        if (refreshToken == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        TokenResponseDto newToken = authService.refreshAccessToken(refreshToken);

        if (newToken.getRefreshToken() != null && !newToken.getRefreshToken().isBlank()) {
            ResponseCookie rc = ResponseCookie.from("refreshToken", newToken.getRefreshToken())
                    .httpOnly(true).secure(true).sameSite("None")
                    .path("/").maxAge(7 * 24 * 60 * 60).build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, rc.toString())
                    .body(new TokenResponseDto(newToken.getAccessToken()));
        }
        return ResponseEntity.ok(new TokenResponseDto(newToken.getAccessToken()));
    }

    // 내 정보
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponseDto> me(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            HttpServletRequest req) {

        String token = null;

        // 1) Authorization: Bearer ...
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        // 2) 없으면 HttpOnly 쿠키 accessToken 사용
        if (token == null && req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if ("accessToken".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            if (jwtUtil.isExpired(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            Claims claims = jwtUtil.parse(token);
            if (!"access".equals(JwtClaimAccessor.type(claims))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            UserInfoResponseDto dto = new UserInfoResponseDto(
                    claims.getSubject(),
                    JwtClaimAccessor.email(claims),
                    JwtClaimAccessor.name(claims),        // name 필드 반영
                    JwtClaimAccessor.nickname(claims),
                    JwtClaimAccessor.picture(claims),
                    JwtClaimAccessor.provider(claims),
                    JwtClaimAccessor.roles(claims)
            );
            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
