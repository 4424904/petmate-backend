package com.petmate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") 							// 모든 경로에 대해
                .allowedOrigins("http://localhost:3000", "http://localhost:3001") 	// 허용할 Origin
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") 		// 허용할 HTTP 메서드
                .allowedHeaders("*") 							// 허용할 헤더
                .allowCredentials(true); 						// 자격 증명 허용 (쿠키 포함)
        
        log.info("CORS configuration has been applied: Allowed Origins - http://localhost:3000, http://localhost:3001");
        
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
