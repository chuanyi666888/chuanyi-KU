package com.smartgrain.service.grain.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartgrain.common.api.PageResult;
import com.smartgrain.domain.grain.entity.GrainBatch;
import com.smartgrain.domain.grain.entity.GrainDepot;
import com.smartgrain.domain.grain.entity.Inventory;
import com.smartgrain.domain.grain.entity.Warehouse;
import com.smartgrain.domain.grain.mapper.GrainBatchMapper;
import com.smartgrain.domain.grain.mapper.GrainDepotMapper;
import com.smartgrain.domain.grain.mapper.InventoryMapper;
import com.smartgrain.domain.grain.mapper.WarehouseMapper;
import com.smartgrain.domain.grain.vo.InventoryVO;
import com.smartgrain.service.grain.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;
    private final GrainDepotMapper grainDepotMapper;
    private final WarehouseMapper warehouseMapper;
    private final GrainBatchMapper grainBatchMapper;

    @Override
    public PageResult<InventoryVO> page(int page, int size, Long depotId, Long warehouseId,
                                        Long batchId, String grainType) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        if (depotId != null) {
            wrapper.eq(Inventory::getDepotId, depotId);
        }
        if (warehouseId != null) {
            wrapper.eq(Inventory::getWarehouseId, warehouseId);
        }
        if (batchId != null) {
            wrapper.eq(Inventory::getBatchId, batchId);
        }
        if (StringUtils.hasText(grainType)) {
            wrapper.eq(Inventory::getGrainType, grainType);
        }
        wrapper.orderByDesc(Inventory::getUpdatedAt);

        Page<Inventory> result = inventoryMapper.selectPage(new Page<>(page, size), wrapper);
        List<InventoryVO> list = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, size);
    }

    private InventoryVO toVO(Inventory inventory) {
        InventoryVO vo = new InventoryVO();
        vo.setId(inventory.getId());
        vo.setDepotId(inventory.getDepotId());
        vo.setWarehouseId(inventory.getWarehouseId());
        vo.setBatchId(inventory.getBatchId());
        vo.setGrainType(inventory.getGrainType());
        vo.setCurrentTons(inventory.getCurrentTons());
        vo.setFrozenTons(inventory.getFrozenTons());
        vo.setAvailableTons(inventory.getAvailableTons());
        vo.setUpdatedAt(inventory.getUpdatedAt());

        // 填充 depotName
        if (inventory.getDepotId() != null) {
            GrainDepot depot = grainDepotMapper.selectById(inventory.getDepotId());
            if (depot != null) {
                vo.setDepotName(depot.getName());
            }
        }
        // 填充 warehouseName
        if (inventory.getWarehouseId() != null) {
            Warehouse warehouse = warehouseMapper.selectById(inventory.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getName());
            }
        }
        // 填充 batchNo
        if (inventory.getBatchId() != null) {
            GrainBatch batch = grainBatchMapper.selectById(inventory.getBatchId());
            if (batch != null) {
                vo.setBatchNo(batch.getBatchNo());
            }
        }
        return vo;
    }
}
