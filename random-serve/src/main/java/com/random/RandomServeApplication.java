package com.random;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 朔州市随机抽取系统服务端启动类。
 *
 * <p>作为 Spring Boot 应用入口，负责启动整个服务端。</p>
 */
@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
public class RandomServeApplication {

    /**
     * 应用程序主入口方法。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(RandomServeApplication.class, args);
    }
}
