package com.petmate.security;

import com.petmate.domain.user.service.UserService;
import com.petmate.security.jwt.JwtClaimAccessor;
import com.petmate.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res,
                                        Authentication authentication) throws IOException {

        log.info("=== OAuth2 인증 성공 처리 시작 ===");

        OAuth2User p = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> a = p.getAttributes();

        log.info("OAuth2 사용자 정보: {}", a);

        String provider = str(a.get("provider"), "oauth2").toUpperCase(Locale.ROOT);
        String rawId    = str(a.get("userId"), str(a.get("id"), str(a.get("sub"), "0")));
        String email    = str(a.get("email"), provider.toLowerCase(Locale.ROOT) + "_" + rawId + "@oauth.local");
        String name     = str(a.get("name"), null);
        String nickname = str(a.get("nickname"), null);
        String pictureFromOAuth = str(a.get("picture"), null);

        log.info("파싱된 사용자 정보 - provider: {}, email: {}, name: {}, nickname: {}, picture: {}",
                provider, email, name, nickname, pictureFromOAuth);

        // 🔧 유저 등록/업데이트
        log.info("사용자 등록/업데이트 시작");
        Integer userId = userService.applyBasicUser(
                email, provider, name, nickname, null, null, null, pictureFromOAuth
        );
        log.info("사용자 등록/업데이트 완료 - userId: {}", userId);

        // ✅ DB에 저장된 uuid (예: "8d4f5b2f.png")
        log.info("프로필 이미지 URL 조회 시작");
        String profileUuid = userService.findProfileImageByEmail(email);
        log.info("조회된 프로필 UUID: {}", profileUuid);

        String profileImageUrl = null;
        if (profileUuid != null) {
            if (profileUuid.startsWith("http://") || profileUuid.startsWith("https://")) {
                profileImageUrl = profileUuid; // 이미 절대 URL이면 그대로 사용
                log.info("절대 URL 사용: {}", profileImageUrl);
            } else {
                profileImageUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + "/img/" + profileUuid;
                log.info("상대 경로를 절대 URL로 변환: {}", profileImageUrl);
            }
        }

        // JWT 발급
        log.info("JWT 토큰 발급 시작");
        String access = jwtUtil.issue(
                String.valueOf(userId),
                jwtUtil.accessTtlMs(),
                JwtClaimAccessor.accessClaims(
                        List.of("USER"),
                        provider,
                        email,
                        name,
                        nickname,
                        profileImageUrl
                )
        );
        log.info("JWT 토큰 발급 완료");

        String next = req.getParameter("next");
        String path = (next != null && next.startsWith("/")) ? next : "/home";

        String url = "http://localhost:3000/oauth2/redirect"
                + "?accessToken=" + URLEncoder.encode(access, StandardCharsets.UTF_8)
                + "&next="        + URLEncoder.encode(path,   StandardCharsets.UTF_8);

        log.info("리다이렉트 URL: {}", url);
        log.info("=== OAuth2 인증 성공 처리 완료 ===");

        res.sendRedirect(url);
    }

    private static String str(Object v, String def) {
        if (v == null) return def;
        String s = String.valueOf(v);
        return (s.isBlank() || "null".equalsIgnoreCase(s)) ? def : s;
    }
}