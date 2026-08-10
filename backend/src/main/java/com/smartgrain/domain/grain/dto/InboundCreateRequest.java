package com.smartgrain.domain.grain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InboundCreateRequest {
    @NotBlank(message = "批次号不能为空")
    private String batchNo;
    @NotNull(message = "粮库ID不能为空")
    private Long depotId;
    @NotNull(message = "粮仓ID不能为空")
    private Long warehouseId;
    @NotNull(message = "入库重量不能为空")
    private BigDecimal weightTons;
    private String inboundType;
    private String carrier;
    private String vehicleNo;
    private String qualityReport;
    private LocalDateTime inboundAt;
    private String remark;
}
