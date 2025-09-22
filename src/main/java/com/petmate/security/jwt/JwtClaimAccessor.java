package com.petmate.security.jwt;

import io.jsonwebtoken.Claims;
import java.util.*;

public final class JwtClaimAccessor {
    private JwtClaimAccessor() {}

    public static final String TYP = "type";
    public static final String ROLE  = "role";      // 단일 role ("1","2","3","4","9")
    public static final String PROV = "prov";
    public static final String EMAIL = "email";
    public static final String NAME = "name";
    public static final String NICKNAME = "nickname";
    public static final String PICTURE = "picture";
    public static final String JTI = "jti";

    public static final String BIRTH_DATE = "birth_date";
    public static final String GENDER = "gender";
    public static final String PHONE = "phone";

    private static final Set<String> ALLOWED_ROLES = Set.of("1","2","3","4","9");

    // 기존 시그니처 유지
    public static Map<String,Object> accessClaims(
            String provider, String email,
            String name, String nickname, String picture) {
        return accessClaims(provider, email, name, nickname, picture, null, null, null, null);
    }

    public static Map<String,Object> accessClaims(
            String provider, String email,
            String name, String nickname, String picture, String userRole) {
        return accessClaims(provider, email, name, nickname, picture, userRole, null, null, null);
    }

    // 최종
    public static Map<String,Object> accessClaims(
            String provider, String email,
            String name, String nickname, String picture, String userRole,
            String birthDate, String gender, String phone) {

        var claims = new HashMap<String,Object>();
        claims.put(TYP, "access");

        String roleVal = sanitizeRole(userRole);
        claims.put(ROLE, roleVal);

        if (provider != null) claims.put(PROV, provider);
        if (email != null)    claims.put(EMAIL, email);
        if (name != null)     claims.put(NAME, name);
        if (nickname != null) claims.put(NICKNAME, nickname);
        if (picture != null)  claims.put(PICTURE, picture);

        if (birthDate != null) claims.put(BIRTH_DATE, birthDate);
        if (gender != null)    claims.put(GENDER, gender);
        if (phone != null)     claims.put(PHONE, phone);

        return claims;
    }

    public static Map<String,Object> refreshClaims() {
        return Map.of(TYP, "refresh", JTI, UUID.randomUUID().toString());
    }

    public static String type(Claims c){ return s(c.get(TYP)); }
    public static String provider(Claims c){ return s(c.get(PROV)); }
    public static String email(Claims c){ return s(c.get(EMAIL)); }
    public static String name(Claims c){ return s(c.get(NAME)); }
    public static String nickname(Claims c){ return s(c.get(NICKNAME)); }
    public static String picture(Claims c){ return s(c.get(PICTURE)); }

    public static String birthDate(Claims c){ return s(c.get(BIRTH_DATE)); }
    public static String gender(Claims c){ return s(c.get(GENDER)); }
    public static String phone(Claims c){ return s(c.get(PHONE)); }

    // 단일 role getter (문자열, 공백/따옴표/허용외 값 방지)
    public static String role(Claims c){
        String r = s(c.get(ROLE));
        return sanitizeRole(r);
    }

    public static boolean isPetmate(Claims c){
        String r = role(c);
        return "3".equals(r) || "4".equals(r);
    }

    private static String s(Object v){
        return v == null ? null : String.valueOf(v);
    }

    private static String sanitizeRole(String r){
        if (r == null) return "1";
        r = r.trim();
        if ((r.startsWith("\"") && r.endsWith("\"")) || (r.startsWith("'") && r.endsWith("'"))) {
            r = r.substring(1, r.length()-1).trim();
        }
        return ALLOWED_ROLES.contains(r) ? r : "1";
    }
}
