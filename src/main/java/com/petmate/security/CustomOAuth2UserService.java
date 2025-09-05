// src/main/java/com/petmate/security/CustomOAuth2UserService.java
package com.petmate.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        OAuth2User src = super.loadUser(req);
        String provider = req.getClientRegistration().getRegistrationId(); // google/kakao/naver

        // 통합 표준 필드
        String providerId;
        String email;
        String name;
        String nickname;
        String picture;

        Map<String, Object> attrs = new HashMap<>(src.getAttributes());

        switch (provider) {
            case "google" -> {
                // keys: sub, email, name, picture
                providerId = String.valueOf(attrs.get("sub"));
                email      = asString(attrs.get("email"));
                name       = asStringOr(attrs.get("name"), email);
                nickname   = name;
                picture    = asString(attrs.get("picture"));
            }
            case "kakao" -> {
                // keys: id, kakao_account{email, profile{nickname, profile_image_url}}
                providerId = String.valueOf(attrs.get("id"));
                Map<String, Object> acc = getMap(attrs, "kakao_account");
                Map<String, Object> prof = getMap(acc, "profile");
                email    = asString(acc.get("email")); // 이메일 동의가 없으면 null
                nickname = asStringOr(prof.get("nickname"), email);
                name     = nickname;
                picture  = asString(prof.get("profile_image_url"));
            }
            case "naver" -> {
                // keys: response{id, email, name, nickname, profile_image}
                Map<String, Object> resp = getMap(attrs, "response");
                providerId = asString(resp.get("id"));
                email      = asString(resp.get("email"));
                name       = asStringOr(resp.get("name"), email);
                nickname   = asStringOr(resp.get("nickname"), name);
                picture    = asString(resp.get("profile_image"));
                // 원본 보존
                attrs.put("response", resp);
            }
            default -> throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
        }

        // DB 없이 임시 userId 규칙: {provider}_{id or email or anon}
        String userId = provider + "_" + Optional.ofNullable(providerId)
                .or(() -> Optional.ofNullable(email))
                .orElse("anon");

        // 표준 필드 주입
        attrs.put("provider", provider);
        attrs.put("providerId", providerId);
        attrs.put("userId", userId);
        attrs.put("email", email);
        attrs.put("name", name);
        attrs.put("nickname", nickname);
        attrs.put("picture", picture);

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attrs,
                "userId" // principal attribute
        );
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }
    private static String asStringOr(Object v, String fallback) {
        String s = asString(v);
        return (s == null || s.isBlank()) ? fallback : s;
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Map<String, Object> src, String key) {
        Object v = src == null ? null : src.get(key);
        return (v instanceof Map) ? (Map<String, Object>) v : Collections.emptyMap();
    }
}
