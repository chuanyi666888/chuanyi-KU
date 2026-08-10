package com.smartgrain.domain.grain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepotUpdateRequest {
    @NotNull(message = "ID不能为空")
    private Long id;
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
    private Integer status;
    private String remark;
}
