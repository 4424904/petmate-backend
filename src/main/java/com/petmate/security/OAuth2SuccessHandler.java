package com.petmate.security;

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
import java.net.URLEncoder;
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
        Map<String,Object> a = p.getAttributes();

        String userId   = String.valueOf(a.get("userId"));
        String provider = String.valueOf(a.getOrDefault("provider","oauth2")).toUpperCase(Locale.ROOT);
        String email    = a.get("email")    == null ? null : String.valueOf(a.get("email"));
        String nickname = a.get("nickname") == null ? null : String.valueOf(a.get("nickname"));
        String picture  = a.get("picture")  == null ? null : String.valueOf(a.get("picture"));

        String access  = jwtUtil.issueAccess(userId, List.of("ROLE_USER"), provider, email, nickname, picture);
        String refresh = jwtUtil.issueRefresh(userId);

        ResponseCookie rc = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true).secure(true).sameSite("None").path("/").maxAge(7*24*60*60).build();
        res.addHeader("Set-Cookie", rc.toString());

        String redirect = "http://localhost:3000/oauth2/redirect";
        String tokenParam = URLEncoder.encode(access, StandardCharsets.UTF_8);
        res.sendRedirect(redirect + "#accessToken=" + tokenParam);
    }
}
