package com.smartgrain.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.smartgrain.common.util.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充
 * - 新增：created_at / updated_at / created_by / updated_by
 * - 修改：updated_at / updated_by
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = safeUserId();
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createdBy", Long.class, userId);
        this.strictInsertFill(metaObject, "updatedBy", Long.class, userId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updatedBy", Long.class, safeUserId());
    }

    private Long safeUserId() {
        try {
            return SecurityUtils.isAuthenticated() ? SecurityUtils.getCurrentUserId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
