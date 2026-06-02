package com.scmcloud.auth;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 认证服务
 *
 * @author Deng
 * createData 2025/10/11 10:16
 * @version 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@MapperScan({"com.scmcloud.common.log.mapper", "com.scmcloud.auth.mapper"})
@EnableTransactionManagement
@EnableCaching
@EnableScheduling
@ComponentScan(basePackages = {"com.scmcloud", "com.scmcloud.common", "com.scmcloud.auth"})
@EnableDubbo(scanBasePackages = "com.scmcloud")
public class AuthApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
