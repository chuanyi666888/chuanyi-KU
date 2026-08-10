package com.smartgrain.domain.grain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InboundOrderVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private Long batchId;
    private String batchNo;
    private Long depotId;
    private String depotName;
    private Long warehouseId;
    private String warehouseName;
    private BigDecimal weightTons;
    private String inboundType;
    private String carrier;
    private String vehicleNo;
    private String qualityReport;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime inboundAt;
    private Integer status;
    private String auditRemark;
    private Long auditedBy;
    private LocalDateTime auditedAt;
    private String remark;
    private LocalDateTime createdAt;
}
