package com.petmate;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.petmate.repository.mybatis")

public class PetmateApplication {
    public static void main(String[] args) {
        SpringApplication.run(PetmateApplication.class, args);
    }
}
