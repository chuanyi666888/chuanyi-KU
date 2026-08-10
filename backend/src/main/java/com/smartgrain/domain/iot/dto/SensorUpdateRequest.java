package com.smartgrain.domain.iot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SensorUpdateRequest {

    @NotNull(message = "ID不能为空")
    private Long id;

    private String name;
    private String sensorType;
    private String unit;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private Integer status;
    private String remark;
}
