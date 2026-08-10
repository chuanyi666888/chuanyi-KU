package com.smartgrain.service.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartgrain.common.api.PageResult;
import com.smartgrain.domain.audit.entity.AuditLog;
import com.smartgrain.domain.audit.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public PageResult<AuditLog> page(int page, int size, String operator, String module,
                                     String operation, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(operator)) {
            wrapper.eq(AuditLog::getOperator, operator);
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(AuditLog::getModule, module);
        }
        if (StringUtils.hasText(operation)) {
            wrapper.eq(AuditLog::getOperation, operation);
        }
        if (startTime != null) {
            wrapper.ge(AuditLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(AuditLog::getCreatedAt, endTime);
        }
        wrapper.orderByDesc(AuditLog::getCreatedAt);

        Page<AuditLog> result = auditLogMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Async
    @Override
    public void record(AuditLog log) {
        try {
            auditLogMapper.insert(log);
        } catch (Exception e) {
            AuditLogServiceImpl.log.warn("审计日志写入失败: {}", e.getMessage());
        }
    }
}
