package com.lab.material;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 药品管理服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.lab.material.mapper")
public class MaterialServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MaterialServiceApplication.class, args);
    }
}
