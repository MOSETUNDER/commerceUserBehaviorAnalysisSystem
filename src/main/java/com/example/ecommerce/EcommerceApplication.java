package com.example.ecommerce;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 跨境电商用户消费行为分析系统启动类
 * 
 * @author system
 */
@SpringBootApplication
@MapperScan("com.example.ecommerce.mapper")
public class EcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }
}

