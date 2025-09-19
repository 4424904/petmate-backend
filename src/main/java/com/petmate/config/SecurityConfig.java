// src/main/java/com/petmate/config/SecurityConfig.java
package com.petmate.config;

import com.petmate.security.CustomOAuth2UserService;
import com.petmate.security.JwtAuthenticationFilter;
import com.petmate.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(c -> c.disable())
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"unauthorized\",\"path\":\"" + req.getRequestURI() + "\"}");
                        })
                        .accessDeniedHandler((req, res, ex) -> {
                            res.setStatus(403);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"forbidden\",\"path\":\"" + req.getRequestURI() + "\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Static
                        .requestMatchers(HttpMethod.GET,
                                "/files/**", "/static/**", "/favicon.ico", "/error", "/img/**").permitAll()

                        // Auth endpoints
                        .requestMatchers("/auth/signin", "/auth/signup", "/auth/refresh", "/auth/signout").permitAll()

                        // ✅ Restore endpoint 공개
                        .requestMatchers(HttpMethod.POST, "/user/restore").permitAll()
                        // Public API
                        .requestMatchers(HttpMethod.GET, "/pet/breeds", "/pet/breeds/**").permitAll()

                        // Upload (need auth)
                        .requestMatchers(HttpMethod.POST, "/upload/pet").authenticated()

                        // Me endpoints - HTTP 메서드별로 명시적 설정
                        .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/user/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/user/me").authenticated()  // ✅ 추가
                        .requestMatchers(HttpMethod.DELETE, "/user/me").authenticated()

                        // Pet APIs
                        .requestMatchers(HttpMethod.GET, "/pet/my", "/pet/my/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/pet/apply").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/pet/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/pet/**").authenticated()

                        .anyRequest().authenticated()
                )
                .oauth2Login(o -> o
                        .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
