package com.smartgrain;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智慧粮库国产化综合运维与业务管理平台 - 启动类
 *
 * 目标运行环境：银河麒麟 V11 (x86_64 / aarch64)
 * 默认数据库：MySQL 8.x（Phase 7 适配 KingbaseES V8）
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.smartgrain.domain")
public class SmartGrainApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartGrainApplication.class, args);
    }
}
