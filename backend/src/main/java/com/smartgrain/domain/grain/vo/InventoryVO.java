package com.smartgrain.domain.grain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InventoryVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long depotId;
    private String depotName;
    private Long warehouseId;
    private String warehouseName;
    private Long batchId;
    private String batchNo;
    private String grainType;
    private BigDecimal currentTons;
    private BigDecimal frozenTons;
    private BigDecimal availableTons;
    private LocalDateTime updatedAt;
}
