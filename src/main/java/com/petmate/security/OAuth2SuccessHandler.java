// com/petmate/security/OAuth2SuccessHandler.java
package com.petmate.security;

import com.petmate.domain.auth.entity.RefreshTokenEntity;
import com.petmate.domain.auth.repository.RefreshTokenRepository;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.front-base-url:http://localhost:3000}")
    private String frontBaseUrl;

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private static final String STATUS_WITHDRAWN = "0"; // UserService.withdraw에서 사용

    private boolean isLocal(HttpServletRequest req) {
        String h = req.getServerName();
        return "localhost".equalsIgnoreCase(h) || "127.0.0.1".equals(h) || "petmate.p-e.kr".equals(h);
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
    private static boolean isWithdrawnStatus(String status) {
        if (status == null) return false;
        String s = status.trim();
        return STATUS_WITHDRAWN.equals(s)
                || "withdrawn".equalsIgnoreCase(s)
                || "deleted".equalsIgnoreCase(s)
                || "inactive".equalsIgnoreCase(s);
    }
    private static String enc(String v){ return URLEncoder.encode(v==null?"":v, StandardCharsets.UTF_8); }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication authentication)
            throws IOException {

        OAuth2User p = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> a = p.getAttributes();

        String provider = str(a.get("provider"), "oauth2").toUpperCase(Locale.ROOT);
        String rawId    = str(a.get("userId"), str(a.get("id"), str(a.get("sub"), "0")));
        String email    = str(a.get("email"), provider.toLowerCase(Locale.ROOT) + "_" + rawId + "@oauth.local");
        String name     = str(a.get("name"), null);
        String nickname = str(a.get("nickname"), null);
        String picture  = str(a.get("picture"), null);

        userService.applyBasicUser(email, provider, name, nickname, null, null, null, picture);
        UserEntity ue = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("가입 직후 사용자 조회 실패: " + email));

        log.info("🔍 OAuth2 로그인 사용자 정보: id={}, email={}, role={}, status={}",
                ue.getId(), ue.getEmail(), ue.getRole(), ue.getStatus());

        // ✅ 탈퇴/비활성: 복구 페이지로 리다이렉트 (토큰/쿠키 발급 금지)
        if (isWithdrawnStatus(ue.getStatus())) {
            String next = req.getParameter("next");
            String path = (next != null && next.startsWith("/")) ? next : "/home";
            String url = frontBaseUrl
                    + "/oauth2/restore"
                    + "?email="    + enc(email)
                    + "&provider=" + enc(provider)
                    + "&next="     + enc(path);
            log.warn("▶ 탈퇴 계정 복구 유도: email={}, status={}, redirect={}", email, ue.getStatus(), url);
            res.sendRedirect(url);
            return;
        }

        // 정상 로그인: access/refresh 발급
        String access = jwtUtil.issue(
                String.valueOf(ue.getId()),
                jwtUtil.accessTtlMs(),
                JwtClaimAccessor.accessClaims(
                        provider, email, name, nickname,
                        userService.findProfileImageByEmail(email),
                        ue.getRole() != null ? ue.getRole() : "1",
                        ue.getBirthDate() != null ? ue.getBirthDate().toString() : null,
                        ue.getGender(), ue.getPhone()
                )
        );
        String refresh = jwtUtil.issue(
                String.valueOf(ue.getId()),
                jwtUtil.refreshTtlMs(),
                JwtClaimAccessor.refreshClaims()
        );
        saveRefreshToken(ue, refresh);

        boolean local = isLocal(req);
        ResponseCookie cookie = buildRefreshCookie(refresh, local);
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String next = req.getParameter("next");
        String path = (next != null && next.startsWith("/")) ? next : "/home";
        String url = frontBaseUrl
                + "/oauth2/redirect"
                + "?accessToken=" + enc(access)
                + "&next="        + enc(path);
        res.sendRedirect(url);
    }

    private void saveRefreshToken(UserEntity user, String token) {
        long tokenCount = refreshTokenRepository.countByUser(user);
        if (tokenCount >= 5) {
            var oldTokens = refreshTokenRepository.findByUserOrderByCreatedAtDesc(user);
            if (!oldTokens.isEmpty()) {
                refreshTokenRepository.delete(oldTokens.get(oldTokens.size() - 1));
            }
        }
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtUtil.refreshTtlMs() / 1000);
        RefreshTokenEntity tokenEntity = RefreshTokenEntity.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();
        refreshTokenRepository.save(tokenEntity);
    }

    private static String str(Object v, String def) {
        if (v == null) return def;
        String s = String.valueOf(v).trim();
        return (s.isBlank() || "null".equalsIgnoreCase(s)) ? def : s;
    }
}
