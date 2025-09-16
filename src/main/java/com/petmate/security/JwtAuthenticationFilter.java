// com/petmate/security/JwtAuthenticationFilter.java
package com.petmate.security;

import com.petmate.security.jwt.JwtClaimAccessor;
import com.petmate.security.jwt.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
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

        // CORS preflight는 통과
        if (HttpMethod.OPTIONS.matches(method)) {
            chain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            // 1) 만료 먼저 체크(여기서 만료 예외가 나면 잡아서 조용히 통과)
            try {
                if (jwtUtil.isExpired(token)) {
                    log.info("[JWT] {} {} - expired access token", method, uri);
                    chain.doFilter(request, response); // 인증 없이 진행 → 최종 401은 EntryPoint가 처리
                    return;
                }
            } catch (ExpiredJwtException ex) {
                log.info("[JWT] {} {} - expired access token (parser)", method, uri);
                chain.doFilter(request, response);
                return;
            }

            // 2) 유효 토큰만 파싱
            Claims claims = jwtUtil.parse(token);

            // 3) 이미 인증 있으면 스킵
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                chain.doFilter(request, response);
                return;
            }

            // 4) 타입 확인
            String type = JwtClaimAccessor.type(claims);
            if (!"access".equals(type)) {
                log.info("[JWT] {} {} - non-access token", method, uri);
                chain.doFilter(request, response);
                return;
            }

            // 5) 권한 매핑
            String roleCode = JwtClaimAccessor.role(claims);
            List<GrantedAuthority> authorities = new ArrayList<>();
            switch (roleCode) {
                case "2" -> authorities.add(new SimpleGrantedAuthority("ROLE_PETOWNER"));
                case "3" -> authorities.add(new SimpleGrantedAuthority("ROLE_PETMATE"));
                case "4" -> {
                    authorities.add(new SimpleGrantedAuthority("ROLE_PETOWNER"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_PETMATE"));
                }
                case "9" -> authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                default -> authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (JwtException ex) { // 서명 오류/변조 등
            log.info("[JWT] {} {} - invalid token: {}", method, uri, ex.getClass().getSimpleName());
            // 인증 없이 통과 → 최종 401은 EntryPoint/권한체크가 응답
        } catch (Exception ex) { // 기타 예외도 조용히
            log.info("[JWT] {} {} - token handling error", method, uri);
        }

        chain.doFilter(request, response);
    }
}
