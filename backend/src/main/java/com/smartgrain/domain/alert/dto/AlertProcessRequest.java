package com.smartgrain.domain.alert.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlertProcessRequest {

    @NotNull(message = "告警ID不能为空")
    private Long alertId;

    /** ACK / RESOLVE / IGNORE / REOPEN */
    @NotNull(message = "处理动作不能为空")
    private String action;

    /** 处理意见 */
    private String comment;
}
