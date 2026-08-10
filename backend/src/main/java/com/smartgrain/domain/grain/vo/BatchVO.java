package com.smartgrain.domain.grain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BatchVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String batchNo;
    private String grainType;
    private String variety;
    private String grade;
    private String origin;
    private Short productionYear;
    private BigDecimal moisture;
    private BigDecimal impurity;
    private BigDecimal testWeight;
    private BigDecimal totalInTons;
    private BigDecimal totalOutTons;
    private BigDecimal stockTons;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}
