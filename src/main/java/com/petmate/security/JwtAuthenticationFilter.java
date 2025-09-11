// com/petmate/security/JwtAuthenticationFilter.java
package com.petmate.security;

import com.petmate.security.jwt.JwtClaimAccessor;
import com.petmate.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        final String uri = request.getRequestURI();
        final String method = request.getMethod();
        final String authHeader = request.getHeader("Authorization");

        // Authorization 헤더 로그
        if (authHeader == null) {
            log.info("[JWT] {} {} - Authorization: <absent>", method, uri);
        } else {
            String shown = authHeader.length() > 24 ? authHeader.substring(0, 24) + "...(len=" + authHeader.length() + ")" : authHeader;
            log.info("[JWT] {} {} - Authorization: {}", method, uri, shown);
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.parse(token);
            boolean expired = jwtUtil.isExpired(token);
            String type = JwtClaimAccessor.type(claims);

            log.info("[JWT] parsed sub={}, type={}, exp={}, expired={}",
                    claims.getSubject(), type, claims.getExpiration(), expired);

            if (!"access".equals(type) || expired) {
                chain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("[JWT] already authenticated. skip");
                chain.doFilter(request, response);
                return;
            }

            List<GrantedAuthority> authorities = JwtClaimAccessor.roles(claims).stream()
                    .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.info("[JWT] authenticated sub={} roles={}", claims.getSubject(), authorities);

        } catch (Exception e) {
            log.warn("[JWT] verification failed: {}", e.getMessage(), e);
        }

        chain.doFilter(request, response);
    }
}
