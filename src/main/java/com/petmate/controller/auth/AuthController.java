package com.petmate.controller.auth;

import com.petmate.dto.request.auth.LoginRequestDto;
import com.petmate.dto.request.auth.SignupRequestDto;
import com.petmate.dto.response.auth.TokenResponseDto;
import com.petmate.security.JwtUtil;
import com.petmate.service.auth.AuthService;
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
        var result = authService.signin(request);

        // 크로스 도메인 대응: SameSite=None
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
    public ResponseEntity<String> signout(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken != null) authService.signout(refreshToken);

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body("로그아웃 성공");
    }

    // AccessToken 재발급
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refresh(HttpServletRequest request) {
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("refreshToken".equals(c.getName())) refreshToken = c.getValue();
            }
        }
        if (refreshToken == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // 서비스가 리프레시 토큰 회전 시 새 쿠키 설정
        TokenResponseDto newToken = authService.refreshAccessToken(refreshToken);
        if (newToken.getRefreshToken() != null && !newToken.getRefreshToken().isBlank()) {
            ResponseCookie rc = ResponseCookie.from("refreshToken", newToken.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .path("/")
                    .maxAge(7 * 24 * 60 * 60)
                    .build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, rc.toString())
                    .body(new TokenResponseDto(newToken.getAccessToken()));
        }
        return ResponseEntity.ok(new TokenResponseDto(newToken.getAccessToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponseDto> me(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validate(token) || !jwtUtil.isAccessToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserInfoResponseDto dto = new UserInfoResponseDto(
                jwtUtil.getUserId(token),
                jwtUtil.getEmail(token),
                jwtUtil.getNickname(token),
                jwtUtil.getPicture(token),
                jwtUtil.getProvider(token),
                jwtUtil.getRoles(token) // List<String>
        );

        return ResponseEntity.ok(dto);
    }

}
