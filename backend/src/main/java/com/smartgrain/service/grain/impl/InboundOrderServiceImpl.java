package com.smartgrain.service.grain.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.ResultCode;
import com.smartgrain.common.exception.BusinessException;
import com.smartgrain.common.util.SecurityUtils;
import com.smartgrain.domain.grain.dto.InboundCreateRequest;
import com.smartgrain.domain.grain.dto.OrderAuditRequest;
import com.smartgrain.domain.grain.entity.GrainBatch;
import com.smartgrain.domain.grain.entity.GrainDepot;
import com.smartgrain.domain.grain.entity.InboundOrder;
import com.smartgrain.domain.grain.entity.Inventory;
import com.smartgrain.domain.grain.entity.Warehouse;
import com.smartgrain.domain.grain.mapper.GrainBatchMapper;
import com.smartgrain.domain.grain.mapper.GrainDepotMapper;
import com.smartgrain.domain.grain.mapper.InboundOrderMapper;
import com.smartgrain.domain.grain.mapper.InventoryMapper;
import com.smartgrain.domain.grain.mapper.WarehouseMapper;
import com.smartgrain.domain.grain.vo.InboundOrderVO;
import com.smartgrain.service.grain.InboundOrderService;
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
public class InboundOrderServiceImpl implements InboundOrderService {

    private final InboundOrderMapper inboundOrderMapper;
    private final GrainBatchMapper grainBatchMapper;
    private final GrainDepotMapper grainDepotMapper;
    private final WarehouseMapper warehouseMapper;
    private final InventoryMapper inventoryMapper;

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Random RANDOM = new Random();

    @Override
    public PageResult<InboundOrderVO> page(int page, int size, Long depotId, Long warehouseId,
                                           String batchNo, Integer status, String keyword) {
        LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<>();
        if (depotId != null) {
            wrapper.eq(InboundOrder::getDepotId, depotId);
        }
        if (warehouseId != null) {
            wrapper.eq(InboundOrder::getWarehouseId, warehouseId);
        }
        if (StringUtils.hasText(batchNo)) {
            wrapper.eq(InboundOrder::getBatchNo, batchNo);
        }
        if (status != null) {
            wrapper.eq(InboundOrder::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(InboundOrder::getOrderNo, keyword)
                    .or().like(InboundOrder::getCarrier, keyword)
                    .or().like(InboundOrder::getVehicleNo, keyword));
        }
        wrapper.orderByDesc(InboundOrder::getCreatedAt);

        Page<InboundOrder> result = inboundOrderMapper.selectPage(new Page<>(page, size), wrapper);
        List<InboundOrderVO> list = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, size);
    }

    @Override
    public InboundOrderVO getDetail(Long id) {
        InboundOrder order = inboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "入库单不存在");
        }
        return toVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(InboundCreateRequest req) {
        // 校验批次存在且状态为在储
        GrainBatch batch = grainBatchMapper.selectOne(new LambdaQueryWrapper<GrainBatch>()
                .eq(GrainBatch::getBatchNo, req.getBatchNo()));
        if (batch == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "批次不存在");
        }
        if (batch.getStatus() == null || batch.getStatus() != 1) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "批次状态不允许入库");
        }

        InboundOrder order = new InboundOrder();
        order.setOrderNo(generateOrderNo());
        order.setBatchId(batch.getId());
        order.setBatchNo(batch.getBatchNo());
        order.setDepotId(req.getDepotId());
        order.setWarehouseId(req.getWarehouseId());
        order.setWeightTons(req.getWeightTons());
        order.setInboundType(req.getInboundType() == null ? "PURCHASE" : req.getInboundType());
        order.setCarrier(req.getCarrier());
        order.setVehicleNo(req.getVehicleNo());
        order.setQualityReport(req.getQualityReport());
        order.setOperatorId(SecurityUtils.getCurrentUserId());
        order.setInboundAt(req.getInboundAt() == null ? LocalDateTime.now() : req.getInboundAt());
        order.setStatus(0);
        order.setRemark(req.getRemark());
        inboundOrderMapper.insert(order);

        log.info("[INBOUND] 新增入库单 id={} orderNo={}", order.getId(), order.getOrderNo());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(OrderAuditRequest req) {
        InboundOrder order = inboundOrderMapper.selectById(req.getId());
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "入库单不存在");
        }
        if (order.getStatus() != null && order.getStatus() != 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "入库单已审核，不可重复审核");
        }

        Integer auditResult = req.getAuditResult();
        Long currentUserId = SecurityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        if (auditResult != null && auditResult == 1) {
            // 审核通过：增加库存
            GrainBatch batch = grainBatchMapper.selectById(order.getBatchId());
            if (batch == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "批次不存在");
            }
            BigDecimal weight = order.getWeightTons() == null ? BigDecimal.ZERO : order.getWeightTons();

            // 查找库存记录
            Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                    .eq(Inventory::getWarehouseId, order.getWarehouseId())
                    .eq(Inventory::getBatchId, order.getBatchId()));
            if (inventory == null) {
                // 不存在则新建
                inventory = new Inventory();
                inventory.setDepotId(order.getDepotId());
                inventory.setWarehouseId(order.getWarehouseId());
                inventory.setBatchId(order.getBatchId());
                inventory.setGrainType(batch.getGrainType());
                inventory.setCurrentTons(weight);
                inventory.setFrozenTons(BigDecimal.ZERO);
                inventory.setAvailableTons(weight);
                inventoryMapper.insert(inventory);
            } else {
                inventory.setCurrentTons(addSafe(inventory.getCurrentTons(), weight));
                inventory.setAvailableTons(addSafe(inventory.getAvailableTons(), weight));
                inventoryMapper.updateById(inventory);
            }

            // 更新批次累计入库
            batch.setTotalInTons(addSafe(batch.getTotalInTons(), weight));
            grainBatchMapper.updateById(batch);

            // 更新粮仓当前吨数与状态
            Warehouse warehouse = warehouseMapper.selectById(order.getWarehouseId());
            if (warehouse != null) {
                warehouse.setCurrentTons(addSafe(warehouse.getCurrentTons(), weight));
                warehouse.setStatus(calcWarehouseStatus(warehouse.getCurrentTons(), warehouse.getCapacityTons()));
                warehouseMapper.updateById(warehouse);
            }

            order.setStatus(1);
        } else if (auditResult != null && auditResult == 2) {
            // 驳回：仅更新状态
            order.setStatus(2);
        } else {
            throw new BusinessException(ResultCode.PARAM_INVALID, "审核结果取值无效");
        }

        order.setAuditRemark(req.getAuditRemark());
        order.setAuditedBy(currentUserId);
        order.setAuditedAt(now);
        inboundOrderMapper.updateById(order);

        log.info("[INBOUND] 审核入库单 id={} result={}", req.getId(), auditResult);
    }

    private String generateOrderNo() {
        return "IN" + LocalDateTime.now().format(ORDER_NO_FORMAT)
                + String.format("%04d", RANDOM.nextInt(10000));
    }

    private BigDecimal addSafe(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return a.add(b);
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

    private InboundOrderVO toVO(InboundOrder order) {
        InboundOrderVO vo = new InboundOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBatchId(order.getBatchId());
        vo.setBatchNo(order.getBatchNo());
        vo.setDepotId(order.getDepotId());
        vo.setWarehouseId(order.getWarehouseId());
        vo.setWeightTons(order.getWeightTons());
        vo.setInboundType(order.getInboundType());
        vo.setCarrier(order.getCarrier());
        vo.setVehicleNo(order.getVehicleNo());
        vo.setQualityReport(order.getQualityReport());
        vo.setOperatorId(order.getOperatorId());
        vo.setOperatorName(order.getOperatorId() == null ? null : order.getOperatorId().toString());
        vo.setInboundAt(order.getInboundAt());
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
