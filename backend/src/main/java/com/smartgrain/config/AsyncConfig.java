package com.smartgrain.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 启用 @Async（用于审计日志异步写入）
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
