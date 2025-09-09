// src/main/java/com/petmate/config/MyBatisConfig.java
package com.petmate.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.petmate.common.repository.mybatis")
public class MyBatisConfig { }
