package com.petmate.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private final String secret;
    private final long accessTtlMs;
    private final long refreshTtlMs;
    private final String issuer;

    private SecretKey key;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long accessTtlMs,
            @Value("${jwt.refresh-expiration}") long refreshTtlMs,
            @Value("${jwt.issuer:petmate}") String issuer
    ) {
        this.secret = secret;
        this.accessTtlMs = accessTtlMs;
        this.refreshTtlMs = refreshTtlMs;
        this.issuer = issuer;
    }

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 범용 발급 */
    public String issue(String subject, long ttlMs, Map<String,Object> claims) {
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** 파싱/검증 */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

    public boolean validate(String token) { try { parse(token); return true; } catch (Exception e) { return false; } }
    public boolean isExpired(String token) { try { return parse(token).getExpiration().before(new Date()); } catch (Exception e) { return true; } }
    public String subject(String token) { return parse(token).getSubject(); }

    /** 선택적 헬퍼 */
    public Object claim(String token, String name) { return parse(token).get(name); }

    public long accessTtlMs() { return accessTtlMs; }
    public long refreshTtlMs() { return refreshTtlMs; }
}
