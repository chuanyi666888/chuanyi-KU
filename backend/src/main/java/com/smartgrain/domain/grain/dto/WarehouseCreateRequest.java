package com.smartgrain.domain.grain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WarehouseCreateRequest {
    @NotNull(message = "粮库ID不能为空")
    private Long depotId;
    @NotBlank(message = "粮仓编码不能为空")
    private String code;
    @NotBlank(message = "粮仓名称不能为空")
    private String name;
    private String type;
    private BigDecimal capacityTons;
    private String remark;
}
