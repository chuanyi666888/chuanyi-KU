package com.smartgrain.service.audit;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.domain.audit.entity.AuditLog;

import java.time.LocalDateTime;

public interface AuditLogService {

    PageResult<AuditLog> page(int page, int size, String operator, String module,
                              String operation, LocalDateTime startTime, LocalDateTime endTime);

    void record(AuditLog log);
}
