package com.smartgrain.domain.grain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderAuditRequest {
    @NotNull(message = "单据ID不能为空")
    private Long id;
    /** 1=通过 2=驳回 */
    @NotNull(message = "审核结果不能为空")
    private Integer auditResult;
    private String auditRemark;
}
