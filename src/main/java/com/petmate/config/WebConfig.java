package com.petmate.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:C:/petmate}")
    private String uploadRoot; // 로컬 파일 저장 루트

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 개발용 허용 오리진
                .allowedOrigins("http://localhost:3000", "http://localhost:3001")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
        log.info("CORS applied. origins=http://localhost:3000, http://localhost:3001");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 로컬 저장 파일 정적 제공: /files/** → C:/petmate/**
        String location = "file:" + (uploadRoot.endsWith("/") ? uploadRoot : uploadRoot + "/");
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
        log.info("Static mapping applied. /files/** -> {}", location);
    }
}


// 중복된코드 제거

//     @Bean
//     public WebMvcConfigurer corsConfigurer() {
//         return new WebMvcConfigurer() {
//             @Override
//             public void addCorsMappings(CorsRegistry registry) {
//                 registry.addMapping("/**")
//                         .allowedOrigins("http://localhost:3000")
//                         .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
// //                        .allowedHeaders("*")
// //                        .allowCredentials(true);
//                         .allowedOriginPatterns("*")
//                         .allowCredentials(false);
//             }
//         };
//     }

//     @Bean
//     public CorsConfigurationSource corsConfigurationSource() {
//         CorsConfiguration configuration = new CorsConfiguration();

//         // 허용할 origin 설정 (프론트엔드 주소)
//         configuration.setAllowedOriginPatterns(Arrays.asList("*")); // 개발환경에서는 모든 origin 허용

//         // 허용할 HTTP 메소드
//         configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

//         // 허용할 헤더
//         configuration.setAllowedHeaders(Arrays.asList("*"));

//         // 자격 증명 허용 (쿠키, 인증 헤더 등)
//         configuration.setAllowCredentials(true);

//         // preflight 요청 결과를 캐시할 시간 (초)
//         configuration.setMaxAge(3600L);

//         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//         source.registerCorsConfiguration("/**", configuration);
//         return source;
//     }

// }
