package com.smartgrain.domain.grain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存快照（按 粮仓 + 批次 维度）
 * - 入库审核通过：current_tons += weight
 * - 出库待审：frozen_tons += weight（可用库存扣减）
 * - 出库审核通过：current_tons -= weight，frozen_tons -= weight
 * - 出库驳回：frozen_tons -= weight（可用库存恢复）
 */
@Data
@TableName("inventory")
public class Inventory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long depotId;
    private Long warehouseId;
    private Long batchId;
    private String grainType;
    private BigDecimal currentTons;
    private BigDecimal frozenTons;
    private BigDecimal availableTons;

    @Version
    private Integer version;

    private LocalDateTime updatedAt;
}
