// src/main/java/com/petmate/security/JwtAuthenticationFilter.java
package com.petmate.security;

import com.petmate.security.jwt.JwtClaimAccessor;
import com.petmate.security.jwt.JwtUtil;
import com.petmate.domain.auth.service.SessionManagementService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
// src/main/java/com/petmate/security/JwtAuthenticationFilter.java
// ... import 동일
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SessionManagementService sessionManagementService;

    private boolean isPublicPath(String uri) {
        // 딱 필요한 공개 경로만 허용
        return uri.equals("/auth/signin")
                || uri.equals("/auth/signup")
                || uri.equals("/auth/refresh")
                || uri.equals("/auth/signout")
                || uri.startsWith("/oauth2/")
                || uri.startsWith("/login/")
                || uri.startsWith("/files/")
                || uri.startsWith("/static/")
                || uri.startsWith("/img/")
                || uri.equals("/favicon.ico")
                || uri.equals("/error")
                // 타임슬롯 API 허용 추가 (이 두 줄을 추가하세요!)
                || uri.matches("/api/products/\\d+/available-slots")
                || uri.matches("/api/products/\\d+/refresh-slots");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        final String uri = request.getRequestURI();
        final String method = request.getMethod();

        if (HttpMethod.OPTIONS.matches(method)) {
            System.out.println("[JWT-DBG] OPTIONS " + uri + " -> pass");
            chain.doFilter(request, response);
            return;
        }

        if (isPublicPath(uri)) {
            System.out.println("[JWT-DBG] public path " + uri + " -> pass");
            chain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        System.out.println("[JWT-DBG] " + method + " " + uri + " Authorization=" + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("[JWT-DBG] no Bearer header -> pass");
            chain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);
        System.out.println("[JWT-DBG] token(first20)=" + token.substring(0, Math.min(20, token.length())) + "...");

        try {
            try {
                if (jwtUtil.isExpired(token)) {
                    System.out.println("[JWT-DBG] expired token -> pass");
                    chain.doFilter(request, response);
                    return;
                }
            } catch (ExpiredJwtException ex) {
                System.out.println("[JWT-DBG] expired (parser) -> pass");
                chain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                System.out.println("[JWT-DBG] already authenticated -> pass");
                chain.doFilter(request, response);
                return;
            }

            Claims claims = jwtUtil.parse(token);
            System.out.println("[JWT-DBG] claims=" + claims);

            String type = JwtClaimAccessor.type(claims);
            System.out.println("[JWT-DBG] token type=" + type);
            if (!"access".equals(type)) {
                System.out.println("[JWT-DBG] non-access token -> pass");
                chain.doFilter(request, response);
                return;
            }

            String email = JwtClaimAccessor.email(claims);
            String principalValue = claims.getSubject();
            // if (principalValue == null || principalValue.isBlank()) {
            //     principalValue = (email != null && !email.isBlank()) ? email : null;
            // }
            String roleCode = JwtClaimAccessor.role(claims);
            System.out.println("[JWT-DBG] principal=" + principalValue + " role=" + roleCode);

            List<GrantedAuthority> authorities = new ArrayList<>();
            switch (roleCode) {
                case "2" -> authorities.add(new SimpleGrantedAuthority("ROLE_PETOWNER"));
                case "3" -> authorities.add(new SimpleGrantedAuthority("ROLE_PETMATE"));
                case "4" -> { authorities.add(new SimpleGrantedAuthority("ROLE_PETOWNER"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_PETMATE")); }
                case "9" -> authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                default -> authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }

            var auth = new UsernamePasswordAuthenticationToken(principalValue, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
            System.out.println("[JWT-DBG] authenticated principal set");

            // 세션 활성화 (30분 타이머 리셋)
            try {
                Long userId = Long.parseLong(claims.getSubject());
                sessionManagementService.updateSessionActivityByUserId(userId);
                System.out.println("[JWT-DBG] session activity updated for userId: " + userId);
            } catch (Exception ex) {
                System.out.println("[JWT-DBG] session activity update failed: " + ex.getMessage());
            }

        } catch (JwtException ex) {
            System.out.println("[JWT-DBG] invalid token ex=" + ex.getClass().getSimpleName());
        } catch (Exception ex) {
            System.out.println("[JWT-DBG] token handling error=" + ex.getMessage());
        }

        chain.doFilter(request, response);
    }
}
