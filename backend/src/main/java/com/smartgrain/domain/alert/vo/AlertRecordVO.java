package com.smartgrain.domain.alert.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AlertRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String alertCode;
    private Long sensorId;
    private String sensorCode;
    private String sensorName;
    private Long warehouseId;
    private Long depotId;
    private String warehouseName;
    private String depotName;
    private String sensorType;
    private String alertType;
    private String alertTypeName;
    private BigDecimal thresholdValue;
    private BigDecimal actualValue;
    private String unit;
    private Integer severity;
    private String severityName;
    private String status;
    private String statusName;
    private LocalDateTime triggeredAt;
    private LocalDateTime resolvedAt;
    private String remark;
    private LocalDateTime createdAt;
}
