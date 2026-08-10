package com.smartgrain.domain.iot.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SensorVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String name;
    private Long warehouseId;
    private String warehouseName;
    private Long depotId;
    private String depotName;
    private String sensorType;
    private String unit;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private BigDecimal lastValue;
    private LocalDateTime lastAt;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}
