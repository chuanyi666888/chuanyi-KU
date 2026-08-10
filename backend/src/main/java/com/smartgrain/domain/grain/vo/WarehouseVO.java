package com.smartgrain.domain.grain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WarehouseVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long depotId;
    private String depotName;
    private String code;
    private String name;
    private String type;
    private BigDecimal capacityTons;
    private BigDecimal currentTons;
    /** 利用率(%) */
    private BigDecimal usageRate;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}
