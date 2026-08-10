package com.smartgrain.domain.grain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepotCreateRequest {
    @NotBlank(message = "粮库编码不能为空")
    private String code;
    @NotBlank(message = "粮库名称不能为空")
    private String name;
    private String address;
    private String province;
    private String city;
    private String district;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private BigDecimal capacityTons;
    private String manager;
    private String phone;
    private String remark;
}
