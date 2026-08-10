package com.smartgrain.service.grain.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.ResultCode;
import com.smartgrain.common.exception.BusinessException;
import com.smartgrain.domain.grain.dto.WarehouseCreateRequest;
import com.smartgrain.domain.grain.dto.WarehouseUpdateRequest;
import com.smartgrain.domain.grain.entity.GrainDepot;
import com.smartgrain.domain.grain.entity.Warehouse;
import com.smartgrain.domain.grain.mapper.GrainDepotMapper;
import com.smartgrain.domain.grain.mapper.WarehouseMapper;
import com.smartgrain.domain.grain.vo.WarehouseVO;
import com.smartgrain.service.grain.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseMapper warehouseMapper;
    private final GrainDepotMapper grainDepotMapper;

    @Override
    public PageResult<WarehouseVO> page(int page, int size, Long depotId, String keyword, Integer status) {
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        if (depotId != null) {
            wrapper.eq(Warehouse::getDepotId, depotId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Warehouse::getCode, keyword)
                    .or().like(Warehouse::getName, keyword));
        }
        if (status != null) {
            wrapper.eq(Warehouse::getStatus, status);
        }
        wrapper.orderByDesc(Warehouse::getCreatedAt);

        Page<Warehouse> result = warehouseMapper.selectPage(new Page<>(page, size), wrapper);
        List<WarehouseVO> list = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, size);
    }

    @Override
    public List<WarehouseVO> listByDepot(Long depotId) {
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Warehouse::getDepotId, depotId)
                .orderByAsc(Warehouse::getCode);
        List<Warehouse> warehouses = warehouseMapper.selectList(wrapper);
        return warehouses.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public WarehouseVO getDetail(Long id) {
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "粮仓不存在");
        }
        return toVO(warehouse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WarehouseCreateRequest req) {
        // 校验粮库存在
        GrainDepot depot = grainDepotMapper.selectById(req.getDepotId());
        if (depot == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "粮库不存在");
        }
        // 校验 (depotId, code) 唯一
        Long exists = warehouseMapper.selectCount(new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getDepotId, req.getDepotId())
                .eq(Warehouse::getCode, req.getCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "粮仓编码已存在");
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setDepotId(req.getDepotId());
        warehouse.setCode(req.getCode());
        warehouse.setName(req.getName());
        warehouse.setType(req.getType() == null ? "FLAT" : req.getType());
        warehouse.setCapacityTons(req.getCapacityTons());
        warehouse.setCurrentTons(BigDecimal.ZERO);
        warehouse.setStatus(1);
        warehouse.setRemark(req.getRemark());
        warehouseMapper.insert(warehouse);

        log.info("[WAREHOUSE] 新增粮仓 id={} depotId={} code={}",
                warehouse.getId(), warehouse.getDepotId(), warehouse.getCode());
        return warehouse.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(WarehouseUpdateRequest req) {
        Warehouse warehouse = warehouseMapper.selectById(req.getId());
        if (warehouse == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "粮仓不存在");
        }

        warehouse.setName(req.getName());
        warehouse.setType(req.getType());
        warehouse.setCapacityTons(req.getCapacityTons());
        if (req.getStatus() != null) {
            warehouse.setStatus(req.getStatus());
        }
        warehouse.setRemark(req.getRemark());
        // currentTons 不允许直接修改，由入出库维护
        warehouseMapper.updateById(warehouse);

        log.info("[WAREHOUSE] 更新粮仓 id={}", req.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "粮仓不存在");
        }
        warehouseMapper.deleteById(id);
        log.info("[WAREHOUSE] 删除粮仓 id={} code={}", id, warehouse.getCode());
    }

    private WarehouseVO toVO(Warehouse warehouse) {
        WarehouseVO vo = new WarehouseVO();
        vo.setId(warehouse.getId());
        vo.setDepotId(warehouse.getDepotId());
        vo.setCode(warehouse.getCode());
        vo.setName(warehouse.getName());
        vo.setType(warehouse.getType());
        vo.setCapacityTons(warehouse.getCapacityTons());
        vo.setCurrentTons(warehouse.getCurrentTons());
        vo.setUsageRate(calcUsageRate(warehouse.getCurrentTons(), warehouse.getCapacityTons()));
        vo.setStatus(warehouse.getStatus());
        vo.setRemark(warehouse.getRemark());
        vo.setCreatedAt(warehouse.getCreatedAt());

        // 填充 depotName
        GrainDepot depot = grainDepotMapper.selectById(warehouse.getDepotId());
        if (depot != null) {
            vo.setDepotName(depot.getName());
        }
        return vo;
    }

    private BigDecimal calcUsageRate(BigDecimal currentTons, BigDecimal capacityTons) {
        if (capacityTons == null || capacityTons.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (currentTons == null) {
            return BigDecimal.ZERO;
        }
        return currentTons.multiply(new BigDecimal("100"))
                .divide(capacityTons, 2, RoundingMode.HALF_UP);
    }
}
