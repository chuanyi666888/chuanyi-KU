package com.smartgrain.domain.grain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WarehouseUpdateRequest {
    @NotNull(message = "ID不能为空")
    private Long id;
    private String name;
    private String type;
    private BigDecimal capacityTons;
    private Integer status;
    private String remark;
}
