package com.petmate;

import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.petmate.domain")
public class PetmateApplication {
    public static void main(String[] args) {
        // .env 파일 로딩 (여러 위치에서 시도)
        Dotenv dotenv = null;
        try {
            // 1. 현재 디렉토리에서 시도
            dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMissing()
                    .load();
        } catch (Exception e1) {
            try {
                // 2. 상위 디렉토리에서 시도 (jar 실행 시)
                dotenv = Dotenv.configure()
                        .directory("../../")
                        .ignoreIfMissing()
                        .load();
            } catch (Exception e2) {
                // 3. 환경변수만 사용
                System.out.println("No .env file found, using system environment variables only");
                dotenv = Dotenv.configure()
                        .ignoreIfMissing()
                        .systemProperties()
                        .load();
            }
        }
        
        // 환경변수를 시스템 프로퍼티로 설정
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
        
        SpringApplication.run(PetmateApplication.class, args);
    }
}
