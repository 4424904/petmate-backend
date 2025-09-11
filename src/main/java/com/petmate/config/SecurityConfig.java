package com.petmate.config;

import org.springframework.http.HttpMethod;
import com.petmate.security.JwtAuthenticationFilter;
import com.petmate.security.CustomOAuth2UserService;
import com.petmate.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    private final OAuth2SuccessHandler oAuth2SuccessHandler; // 커스텀 핸들러 주입

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
                // JWT만 쓸 거면 STATELESS, OAuth2 세션 필요하면 IF_REQUIRED 유지
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 정적 리소스(업로드 노출)
                        .requestMatchers("/files/**", "/static/**", "/favicon.ico", "/error").permitAll()
                        // 공개 API
                        .requestMatchers(
                                "/auth/**","/oauth2/**","/login/**","/login/oauth2/**",
                                "/api/payment/**","/api/**"
                        ).permitAll()
                        // 펫메이트 신청은 반드시 인증
                        .requestMatchers(HttpMethod.POST, "/petmate/apply").authenticated()
                        // 나머지는 인증
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
