package com.smartgrain.domain.grain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BatchUpdateRequest {
    @NotNull(message = "ID不能为空")
    private Long id;
    private String variety;
    private String grade;
    private String origin;
    private Short productionYear;
    private BigDecimal moisture;
    private BigDecimal impurity;
    private BigDecimal testWeight;
    private Integer status;
    private String remark;
}
