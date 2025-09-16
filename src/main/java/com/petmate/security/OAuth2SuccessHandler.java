// com/petmate/security/OAuth2SuccessHandler.java
package com.petmate.security;

import com.petmate.domain.user.entity.UserEntity;
import com.petmate.domain.user.repository.jpa.UserRepository;
import com.petmate.domain.user.service.UserService;
import com.petmate.security.jwt.JwtClaimAccessor;
import com.petmate.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.front-base-url:http://localhost:3000}") // 예: https://minjung.kr
    private String frontBaseUrl;

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res,
                                        Authentication authentication) throws IOException {
        OAuth2User p = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> a = p.getAttributes();

        String provider = str(a.get("provider"), "oauth2").toUpperCase(Locale.ROOT);
        String rawId    = str(a.get("userId"), str(a.get("id"), str(a.get("sub"), "0")));
        String email    = str(a.get("email"), provider.toLowerCase(Locale.ROOT) + "_" + rawId + "@oauth.local");
        String name     = str(a.get("name"), null);
        String nickname = str(a.get("nickname"), null);
        String picture  = str(a.get("picture"), null);

        Integer userId = userService.applyBasicUser(
                email, provider, name, nickname, null, null, null, picture
        );

        UserEntity ue = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("가입 직후 사용자 조회 실패: " + email));

        String access = jwtUtil.issue(
                String.valueOf(ue.getId()),
                jwtUtil.accessTtlMs(),
                JwtClaimAccessor.accessClaims(
                        provider,
                        email,
                        name,
                        nickname,
                        userService.findProfileImageByEmail(email),
                        ue.getRole() != null ? ue.getRole() : "1",
                        ue.getBirthDate() != null ? ue.getBirthDate().toString() : null,
                        ue.getGender(),
                        ue.getPhone()
                )
        );

        String next = req.getParameter("next");
        String path = (next != null && next.startsWith("/")) ? next : "/home";

        String url = frontBaseUrl
                + "/oauth2/redirect"
                + "?accessToken=" + URLEncoder.encode(access, StandardCharsets.UTF_8)
                + "&next="        + URLEncoder.encode(path,   StandardCharsets.UTF_8);

        res.sendRedirect(url);
    }

    private static String str(Object v, String def) {
        if (v == null) return def;
        String s = String.valueOf(v);
        return (s.isBlank() || "null".equalsIgnoreCase(s)) ? def : s;
    }
}