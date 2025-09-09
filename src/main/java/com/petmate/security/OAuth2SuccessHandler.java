package com.petmate.security;

import com.petmate.security.jwt.JwtClaimAccessor;
import com.petmate.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User p = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> a = p.getAttributes();

        String userId   = String.valueOf(a.get("userId"));
        String provider = String.valueOf(a.getOrDefault("provider", "oauth2")).toUpperCase(Locale.ROOT);
        String email    = a.get("email")    == null ? null : String.valueOf(a.get("email"));
        String nickname = a.get("nickname") == null ? null : String.valueOf(a.get("nickname"));
        String picture  = a.get("picture")  == null ? null : String.valueOf(a.get("picture"));

        // 액세스/리프레시 발급 (JwtUtil + JwtClaimAccessor 조합)
        String access = jwtUtil.issue(
                userId,
                jwtUtil.accessTtlMs(),
                JwtClaimAccessor.accessClaims(List.of("USER"), provider, email, nickname, picture)
        );
        String refresh = jwtUtil.issue(
                userId,
                jwtUtil.refreshTtlMs(),
                JwtClaimAccessor.refreshClaims()
        );

        boolean isLocal = "localhost".equals(req.getServerName()) || "127.0.0.1".equals(req.getServerName());

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(!isLocal)
                .sameSite(isLocal ? "Lax" : "None")
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();
        res.addHeader("Set-Cookie", refreshCookie.toString());

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", access)
                .httpOnly(true)
                .secure(!isLocal)
                .sameSite(isLocal ? "Lax" : "None")
                .path("/")
                .maxAge(5 * 60)
                .build();
        res.addHeader("Set-Cookie", accessCookie.toString());

        String next = req.getParameter("next");
        String path = (next != null && next.startsWith("/")) ? next : "/home";
        res.sendRedirect("http://localhost:3000" + path);
    }
}
