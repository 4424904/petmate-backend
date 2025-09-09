package com.petmate.security.jwt;

import io.jsonwebtoken.Claims;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Map;

public final class JwtClaimAccessor {
    private JwtClaimAccessor() {}

    // 표준/도메인 클레임 키 상수
    public static final String TYP = "typ";
    public static final String ROLES = "roles";
    public static final String PROV = "prov";
    public static final String EMAIL = "email";
    public static final String NICKNAME = "nickname";
    public static final String PICTURE = "picture";
    public static final String JTI = "jti";

    // 발급 시 사용할 미니 팩토리
    public static Map<String,Object> accessClaims(List<String> roles, String provider,
                                                  String email, String nickname, String picture) {
        var claims = new java.util.HashMap<String,Object>();
        claims.put(TYP, "access");
        if (roles != null)    claims.put(ROLES, roles);
        if (provider != null) claims.put(PROV, provider);
        if (email != null)    claims.put(EMAIL, email);
        if (nickname != null) claims.put(NICKNAME, nickname);
        if (picture != null)  claims.put(PICTURE, picture);
        return claims;
    }

    public static Map<String,Object> refreshClaims() {
        return Map.of(TYP, "refresh", JTI, UUID.randomUUID().toString());
    }

    // 파싱된 Claims에서 안전하게 꺼내기
    public static String type(Claims c) { return string(c.get(TYP)); }
    public static String provider(Claims c) { return string(c.get(PROV)); }
    public static String email(Claims c) { return string(c.get(EMAIL)); }
    public static String nickname(Claims c) { return string(c.get(NICKNAME)); }
    public static String picture(Claims c) { return string(c.get(PICTURE)); }

    @SuppressWarnings("unchecked")
    public static List<String> roles(Claims c) {
        Object v = c.get(ROLES);
        if (v instanceof Collection<?> col) {
            List<String> out = new ArrayList<>(col.size());
            for (Object o : col) out.add(String.valueOf(o));
            return out;
        }
        return List.of();
    }

    private static String string(Object v) { return v == null ? null : String.valueOf(v); }
}
