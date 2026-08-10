package com.smartgrain.domain.iot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SensorCreateRequest {

    @NotBlank(message = "传感器编码不能为空")
    private String code;

    @NotBlank(message = "传感器名称不能为空")
    private String name;

    @NotNull(message = "所属粮仓不能为空")
    private Long warehouseId;

    @NotNull(message = "所属粮库不能为空")
    private Long depotId;

    @NotBlank(message = "传感器类型不能为空")
    private String sensorType;

    private String unit;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String remark;
}
