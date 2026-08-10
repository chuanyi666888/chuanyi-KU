package com.smartgrain.domain.grain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BatchCreateRequest {
    @NotBlank(message = "批次号不能为空")
    private String batchNo;
    @NotBlank(message = "粮食品种不能为空")
    private String grainType;
    private String variety;
    private String grade;
    private String origin;
    private Short productionYear;
    private BigDecimal moisture;
    private BigDecimal impurity;
    private BigDecimal testWeight;
    private String remark;
}
