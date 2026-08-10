package com.smartgrain.domain.alert.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AlertProcessLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long alertId;
    private String action;
    private String actionName;
    private String fromStatus;
    private String toStatus;
    private String comment;
    private LocalDateTime processAt;
    private Long processBy;
    private String processByName;
}
