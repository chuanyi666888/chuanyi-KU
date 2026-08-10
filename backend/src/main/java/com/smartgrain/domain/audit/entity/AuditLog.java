package com.smartgrain.domain.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("audit_log")
public class AuditLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long operatorId;
    private String operator;
    private String operation;
    private String module;
    private String target;
    private String method;
    private String requestUrl;
    private String requestIp;
    private String requestBody;
    private Integer result;
    private String errorMsg;
    private Integer costMs;
    private LocalDateTime createdAt;
}
