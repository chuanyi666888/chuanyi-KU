package com.smartgrain.service.grain.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.ResultCode;
import com.smartgrain.common.exception.BusinessException;
import com.smartgrain.common.util.SecurityUtils;
import com.smartgrain.domain.grain.dto.OrderAuditRequest;
import com.smartgrain.domain.grain.dto.OutboundCreateRequest;
import com.smartgrain.domain.grain.entity.GrainBatch;
import com.smartgrain.domain.grain.entity.GrainDepot;
import com.smartgrain.domain.grain.entity.Inventory;
import com.smartgrain.domain.grain.entity.OutboundOrder;
import com.smartgrain.domain.grain.entity.Warehouse;
import com.smartgrain.domain.grain.mapper.GrainBatchMapper;
import com.smartgrain.domain.grain.mapper.GrainDepotMapper;
import com.smartgrain.domain.grain.mapper.InventoryMapper;
import com.smartgrain.domain.grain.mapper.OutboundOrderMapper;
import com.smartgrain.domain.grain.mapper.WarehouseMapper;
import com.smartgrain.domain.grain.vo.OutboundOrderVO;
import com.smartgrain.service.grain.OutboundOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundOrderServiceImpl implements OutboundOrderService {

    private final OutboundOrderMapper outboundOrderMapper;
    private final GrainBatchMapper grainBatchMapper;
    private final GrainDepotMapper grainDepotMapper;
    private final WarehouseMapper warehouseMapper;
    private final InventoryMapper inventoryMapper;

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Random RANDOM = new Random();

    @Override
    public PageResult<OutboundOrderVO> page(int page, int size, Long depotId, Long warehouseId,
                                            String batchNo, Integer status, String keyword) {
        LambdaQueryWrapper<OutboundOrder> wrapper = new LambdaQueryWrapper<>();
        if (depotId != null) {
            wrapper.eq(OutboundOrder::getDepotId, depotId);
        }
        if (warehouseId != null) {
            wrapper.eq(OutboundOrder::getWarehouseId, warehouseId);
        }
        if (StringUtils.hasText(batchNo)) {
            wrapper.eq(OutboundOrder::getBatchNo, batchNo);
        }
        if (status != null) {
            wrapper.eq(OutboundOrder::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(OutboundOrder::getOrderNo, keyword)
                    .or().like(OutboundOrder::getCustomer, keyword)
                    .or().like(OutboundOrder::getVehicleNo, keyword));
        }
        wrapper.orderByDesc(OutboundOrder::getCreatedAt);

        Page<OutboundOrder> result = outboundOrderMapper.selectPage(new Page<>(page, size), wrapper);
        List<OutboundOrderVO> list = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, size);
    }

    @Override
    public OutboundOrderVO getDetail(Long id) {
        OutboundOrder order = outboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "出库单不存在");
        }
        return toVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(OutboundCreateRequest req) {
        // 校验批次存在且状态为在储
        GrainBatch batch = grainBatchMapper.selectOne(new LambdaQueryWrapper<GrainBatch>()
                .eq(GrainBatch::getBatchNo, req.getBatchNo()));
        if (batch == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "批次不存在");
        }
        if (batch.getStatus() == null || batch.getStatus() != 1) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "批次状态不允许出库");
        }

        // 校验库存存在且可用库存足够
        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getWarehouseId, req.getWarehouseId())
                .eq(Inventory::getBatchId, batch.getId()));
        if (inventory == null) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT, "库存不存在");
        }
        BigDecimal available = inventory.getAvailableTons() == null ? BigDecimal.ZERO : inventory.getAvailableTons();
        BigDecimal weight = req.getWeightTons() == null ? BigDecimal.ZERO : req.getWeightTons();
        if (available.compareTo(weight) < 0) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT, "可用库存不足");
        }

        // 立即冻结：frozenTons += weight, availableTons -= weight（乐观锁）
        inventory.setFrozenTons(addSafe(inventory.getFrozenTons(), weight));
        inventory.setAvailableTons(subSafe(inventory.getAvailableTons(), weight));
        int updated = inventoryMapper.updateById(inventory);
        if (updated == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "库存已被其他操作修改，请重试");
        }

        OutboundOrder order = new OutboundOrder();
        order.setOrderNo(generateOrderNo());
        order.setBatchId(batch.getId());
        order.setBatchNo(batch.getBatchNo());
        order.setDepotId(req.getDepotId());
        order.setWarehouseId(req.getWarehouseId());
        order.setWeightTons(req.getWeightTons());
        order.setOutboundType(req.getOutboundType() == null ? "SALE" : req.getOutboundType());
        order.setCustomer(req.getCustomer());
        order.setVehicleNo(req.getVehicleNo());
        order.setOperatorId(SecurityUtils.getCurrentUserId());
        order.setOutboundAt(req.getOutboundAt() == null ? LocalDateTime.now() : req.getOutboundAt());
        order.setStatus(0);
        order.setRemark(req.getRemark());
        outboundOrderMapper.insert(order);

        log.info("[OUTBOUND] 新增出库单 id={} orderNo={}", order.getId(), order.getOrderNo());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(OrderAuditRequest req) {
        OutboundOrder order = outboundOrderMapper.selectById(req.getId());
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "出库单不存在");
        }
        if (order.getStatus() != null && order.getStatus() != 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "出库单已审核，不可重复审核");
        }

        Integer auditResult = req.getAuditResult();
        Long currentUserId = SecurityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal weight = order.getWeightTons() == null ? BigDecimal.ZERO : order.getWeightTons();

        // 查询库存记录
        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getWarehouseId, order.getWarehouseId())
                .eq(Inventory::getBatchId, order.getBatchId()));
        if (inventory == null) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT, "库存不存在");
        }

        if (auditResult != null && auditResult == 1) {
            // 审核通过：currentTons -= weight, frozenTons -= weight
            inventory.setCurrentTons(subSafe(inventory.getCurrentTons(), weight));
            inventory.setFrozenTons(subSafe(inventory.getFrozenTons(), weight));
            int updated = inventoryMapper.updateById(inventory);
            if (updated == 0) {
                throw new BusinessException(ResultCode.CONFLICT, "库存已被其他操作修改，请重试");
            }

            // 更新批次累计出库
            GrainBatch batch = grainBatchMapper.selectById(order.getBatchId());
            if (batch != null) {
                batch.setTotalOutTons(addSafe(batch.getTotalOutTons(), weight));
                // 如果已出清则更新状态
                BigDecimal totalIn = batch.getTotalInTons() == null ? BigDecimal.ZERO : batch.getTotalInTons();
                if (batch.getTotalOutTons().compareTo(totalIn) >= 0) {
                    batch.setStatus(2);
                }
                grainBatchMapper.updateById(batch);
            }

            // 更新粮仓当前吨数与状态
            Warehouse warehouse = warehouseMapper.selectById(order.getWarehouseId());
            if (warehouse != null) {
                warehouse.setCurrentTons(subSafe(warehouse.getCurrentTons(), weight));
                warehouse.setStatus(calcWarehouseStatus(warehouse.getCurrentTons(), warehouse.getCapacityTons()));
                warehouseMapper.updateById(warehouse);
            }

            order.setStatus(1);
        } else if (auditResult != null && auditResult == 2) {
            // 驳回：解冻 frozenTons -= weight, availableTons += weight
            inventory.setFrozenTons(subSafe(inventory.getFrozenTons(), weight));
            inventory.setAvailableTons(addSafe(inventory.getAvailableTons(), weight));
            int updated = inventoryMapper.updateById(inventory);
            if (updated == 0) {
                throw new BusinessException(ResultCode.CONFLICT, "库存已被其他操作修改，请重试");
            }
            order.setStatus(2);
        } else {
            throw new BusinessException(ResultCode.PARAM_INVALID, "审核结果取值无效");
        }

        order.setAuditRemark(req.getAuditRemark());
        order.setAuditedBy(currentUserId);
        order.setAuditedAt(now);
        outboundOrderMapper.updateById(order);

        log.info("[OUTBOUND] 审核出库单 id={} result={}", req.getId(), auditResult);
    }

    private String generateOrderNo() {
        return "OUT" + LocalDateTime.now().format(ORDER_NO_FORMAT)
                + String.format("%04d", RANDOM.nextInt(10000));
    }

    private BigDecimal addSafe(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return a.add(b);
    }

    private BigDecimal subSafe(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return a.subtract(b);
    }

    private Integer calcWarehouseStatus(BigDecimal currentTons, BigDecimal capacityTons) {
        if (currentTons == null || currentTons.compareTo(BigDecimal.ZERO) == 0) {
            return 1; // 空仓
        }
        if (capacityTons != null && currentTons.compareTo(capacityTons) >= 0) {
            return 3; // 满仓
        }
        return 2; // 在储
    }

    private OutboundOrderVO toVO(OutboundOrder order) {
        OutboundOrderVO vo = new OutboundOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBatchId(order.getBatchId());
        vo.setBatchNo(order.getBatchNo());
        vo.setDepotId(order.getDepotId());
        vo.setWarehouseId(order.getWarehouseId());
        vo.setWeightTons(order.getWeightTons());
        vo.setOutboundType(order.getOutboundType());
        vo.setCustomer(order.getCustomer());
        vo.setVehicleNo(order.getVehicleNo());
        vo.setOperatorId(order.getOperatorId());
        vo.setOperatorName(order.getOperatorId() == null ? null : order.getOperatorId().toString());
        vo.setOutboundAt(order.getOutboundAt());
        vo.setStatus(order.getStatus());
        vo.setAuditRemark(order.getAuditRemark());
        vo.setAuditedBy(order.getAuditedBy());
        vo.setAuditedAt(order.getAuditedAt());
        vo.setRemark(order.getRemark());
        vo.setCreatedAt(order.getCreatedAt());

        // 填充 depotName
        if (order.getDepotId() != null) {
            GrainDepot depot = grainDepotMapper.selectById(order.getDepotId());
            if (depot != null) {
                vo.setDepotName(depot.getName());
            }
        }
        // 填充 warehouseName
        if (order.getWarehouseId() != null) {
            Warehouse warehouse = warehouseMapper.selectById(order.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getName());
            }
        }
        return vo;
    }
}
