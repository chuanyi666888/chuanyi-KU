package com.smartgrain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 业务配置属性绑定
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "smartgrain")
public class SmartGrainProperties {

    private Jwt jwt = new Jwt();
    private Login login = new Login();
    private Cors cors = new Cors();
    private Backup backup = new Backup();

    @Data
    public static class Jwt {
        private String secret = "";
        private long accessTokenTtl = 7200;
        private long refreshTokenTtl = 604800;
        private String header = "Authorization";
        private String prefix = "Bearer ";
    }

    @Data
    public static class Login {
        private int maxFailCount = 5;
        private int lockMinutes = 15;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = Arrays.asList("http://localhost:5173");
        private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
        private boolean allowCredentials = true;
    }

    /**
     * 备份恢复相关配置（Phase 8）
     */
    @Data
    public static class Backup {
        /** MySQL 备份根目录 */
        private String mysqlRoot = "/data/backups/mysql";
        /** KingbaseES 备份根目录 */
        private String kingbaseRoot = "/data/backups/kingbase";
        /** 备份脚本目录 */
        private String scriptDir = "/opt/smartgrain/scripts/backup";
        /** 是否允许通过 API 触发脚本执行 */
        private boolean enableTrigger = true;
        /** 脚本执行超时（秒） */
        private long scriptTimeoutSec = 1800;
    }
}
