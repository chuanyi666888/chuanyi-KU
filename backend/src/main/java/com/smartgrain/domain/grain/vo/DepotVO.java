package com.smartgrain.domain.grain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DepotVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
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
    private LocalDateTime createdAt;
}
