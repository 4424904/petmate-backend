package com.petmate.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;
    private final long refreshExpirationMs;
    private final String issuer;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs,
            @Value("${jwt.refresh-expiration}") long refreshExpirationMs,
            @Value("${jwt.issuer:petmate}") String issuer
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.issuer = issuer;
    }

    /* -------- 발급 -------- */

    public String generateToken(String userId) {
        return issue(userId, expirationMs, Collections.singletonMap("typ", "access"));
    }

    public String generateRefreshToken(String userId) {
        return issue(userId, refreshExpirationMs, Map.of("typ", "refresh", "jti", UUID.randomUUID().toString()));
    }

    public String issueAccess(String userId, List<String> roles, String provider) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "access");
        claims.put("roles", roles);
        claims.put("prov", provider);
        return issue(userId, expirationMs, claims);
    }

    public String issueAccess(String userId,
                              List<String> roles,
                              String provider,
                              String email,
                              String nickname,
                              String picture) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "access");
        claims.put("roles", roles);
        claims.put("prov", provider);
        if (email != null)    claims.put("email", email);
        if (nickname != null) claims.put("nickname", nickname);
        if (picture != null)  claims.put("picture", picture);
        return issue(userId, expirationMs, claims);
    }

    public String issueRefresh(String userId) {
        return issue(userId, refreshExpirationMs, Map.of("typ", "refresh", "jti", UUID.randomUUID().toString()));
    }

    private String issue(String subject, long ttlMs, Map<String, Object> claims) {
        Date now = new Date();
        return Jwts.builder()
                .claims(new HashMap<>(claims))
                .subject(subject)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /* -------- 파싱/검증 -------- */

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validate(String token) {
        try { parseClaims(token); return true; }
        catch (Exception e) { return false; }
    }

    public String getUserId(String token) { return parseClaims(token).getSubject(); }

    public boolean isTokenExpired(String token) {
        try { return parseClaims(token).getExpiration().before(new Date()); }
        catch (Exception e) { return true; }
    }

    public boolean isAccessToken(String token) {
        return "access".equals(String.valueOf(parseClaims(token).get("typ")));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(String.valueOf(parseClaims(token).get("typ")));
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object v = parseClaims(token).get("roles");
        if (v instanceof Collection<?> c) {
            List<String> out = new ArrayList<>();
            for (Object o : c) out.add(String.valueOf(o));
            return out;
        }
        return Collections.emptyList();
    }

    public String getProvider(String token) {
        Object v = parseClaims(token).get("prov");
        return v == null ? null : String.valueOf(v);
    }

    public String getEmail(String token) {
        Object v = parseClaims(token).get("email");
        return v == null ? null : String.valueOf(v);
    }

    public String getNickname(String token) {
        Object v = parseClaims(token).get("nickname");
        return v == null ? null : String.valueOf(v);
    }

    public String getPicture(String token) {
        Object v = parseClaims(token).get("picture");
        return v == null ? null : String.valueOf(v);
    }
}
