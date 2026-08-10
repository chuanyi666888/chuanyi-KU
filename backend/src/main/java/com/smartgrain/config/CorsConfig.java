package com.smartgrain.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 配置
 *
 * 生产环境由 Nginx 处理跨域，这里仅用于开发联调
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private final SmartGrainProperties properties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        SmartGrainProperties.Cors cors = properties.getCors();
        String[] origins = cors.getAllowedOrigins().toArray(new String[0]);
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods(cors.getAllowedMethods().split(","))
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Disposition")
                .allowCredentials(cors.isAllowCredentials())
                .maxAge(3600);
    }
}
