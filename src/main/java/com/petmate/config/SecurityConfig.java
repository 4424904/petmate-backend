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
                .cors(c -> {})
                .csrf(c -> c.disable())
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"error\":\"unauthorized\"}");
                        })
                        .accessDeniedHandler((req, res, ex) -> {
                            res.setStatus(403);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"error\":\"forbidden\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 정적 리소스(읽기만 허용)
                        .requestMatchers(HttpMethod.GET,
                                "/files/**", "/static/**", "/favicon.ico", "/error", "/img/**"
                        ).permitAll()

                        // 인증 관련
                        .requestMatchers("/auth/signin", "/auth/signup", "/auth/refresh", "/auth/signout").permitAll()

                        // 공개 API
                        .requestMatchers(HttpMethod.GET, "/pet/breeds", "/pet/breeds/**").permitAll()

                        // 타임슬롯 조회 허용 추가 (이 줄을 추가하세요!)
                        .requestMatchers(HttpMethod.GET, "/api/products/*/available-slots").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/*/refresh-slots").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payment/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/payment/**").permitAll()
                        .requestMatchers("/api/payment/danal/**").permitAll()

                        // 파일 업로드(인증 필요)
                        .requestMatchers(HttpMethod.POST, "/upload/pet").authenticated()

                        // 보호 자원
                        .requestMatchers("/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/pet/apply").authenticated()

                        // 내 반려동물 조회
                        .requestMatchers(HttpMethod.GET, "/pet/my", "/pet/my/**").authenticated()

                        // 수정/삭제는 /pet/{petId}
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
}
