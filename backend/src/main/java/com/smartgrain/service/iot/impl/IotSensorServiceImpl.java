package com.smartgrain.service.iot.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.ResultCode;
import com.smartgrain.common.exception.BusinessException;
import com.smartgrain.domain.grain.entity.GrainDepot;
import com.smartgrain.domain.grain.entity.Warehouse;
import com.smartgrain.domain.grain.mapper.GrainDepotMapper;
import com.smartgrain.domain.grain.mapper.WarehouseMapper;
import com.smartgrain.domain.iot.dto.SensorCreateRequest;
import com.smartgrain.domain.iot.dto.SensorUpdateRequest;
import com.smartgrain.domain.iot.entity.IotReading;
import com.smartgrain.domain.iot.entity.IotSensor;
import com.smartgrain.domain.iot.mapper.IotReadingMapper;
import com.smartgrain.domain.iot.mapper.IotSensorMapper;
import com.smartgrain.domain.iot.vo.ReadingVO;
import com.smartgrain.domain.iot.vo.SensorVO;
import com.smartgrain.service.iot.IotSensorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IotSensorServiceImpl implements IotSensorService {

    private final IotSensorMapper sensorMapper;
    private final IotReadingMapper readingMapper;
    private final WarehouseMapper warehouseMapper;
    private final GrainDepotMapper grainDepotMapper;

    @Override
    public PageResult<SensorVO> page(int page, int size, Long depotId, Long warehouseId, String sensorType, Integer status) {
        LambdaQueryWrapper<IotSensor> wrapper = new LambdaQueryWrapper<>();
        if (depotId != null) {
            wrapper.eq(IotSensor::getDepotId, depotId);
        }
        if (warehouseId != null) {
            wrapper.eq(IotSensor::getWarehouseId, warehouseId);
        }
        if (StringUtils.hasText(sensorType)) {
            wrapper.eq(IotSensor::getSensorType, sensorType);
        }
        if (status != null) {
            wrapper.eq(IotSensor::getStatus, status);
        }
        wrapper.orderByDesc(IotSensor::getCreatedAt);

        Page<IotSensor> result = sensorMapper.selectPage(new Page<>(page, size), wrapper);
        List<SensorVO> list = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, size);
    }

    @Override
    public List<SensorVO> listByWarehouse(Long warehouseId) {
        LambdaQueryWrapper<IotSensor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IotSensor::getWarehouseId, warehouseId)
                .orderByAsc(IotSensor::getCode);
        List<IotSensor> sensors = sensorMapper.selectList(wrapper);
        return sensors.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public SensorVO getDetail(Long id) {
        IotSensor sensor = sensorMapper.selectById(id);
        if (sensor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "传感器不存在");
        }
        return toVO(sensor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SensorCreateRequest req) {
        // 校验粮仓存在
        Warehouse warehouse = warehouseMapper.selectById(req.getWarehouseId());
        if (warehouse == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "粮仓不存在");
        }
        // 校验 (code) 唯一
        Long exists = sensorMapper.selectCount(new LambdaQueryWrapper<IotSensor>()
                .eq(IotSensor::getCode, req.getCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "传感器编码已存在");
        }

        IotSensor sensor = new IotSensor();
        sensor.setCode(req.getCode());
        sensor.setName(req.getName());
        sensor.setWarehouseId(req.getWarehouseId());
        sensor.setDepotId(req.getDepotId());
        sensor.setSensorType(req.getSensorType());
        sensor.setUnit(req.getUnit() == null ? "" : req.getUnit());
        sensor.setMinValue(req.getMinValue());
        sensor.setMaxValue(req.getMaxValue());
        sensor.setStatus(1);
        sensor.setRemark(req.getRemark());
        sensorMapper.insert(sensor);

        log.info("[IOT] 新增传感器 id={} code={}", sensor.getId(), sensor.getCode());
        return sensor.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SensorUpdateRequest req) {
        IotSensor sensor = sensorMapper.selectById(req.getId());
        if (sensor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "传感器不存在");
        }

        sensor.setName(req.getName());
        sensor.setSensorType(req.getSensorType());
        sensor.setUnit(req.getUnit());
        sensor.setMinValue(req.getMinValue());
        sensor.setMaxValue(req.getMaxValue());
        if (req.getStatus() != null) {
            sensor.setStatus(req.getStatus());
        }
        sensor.setRemark(req.getRemark());
        sensorMapper.updateById(sensor);

        log.info("[IOT] 更新传感器 id={}", req.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        IotSensor sensor = sensorMapper.selectById(id);
        if (sensor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "传感器不存在");
        }
        sensorMapper.deleteById(id);
        log.info("[IOT] 删除传感器 id={} code={}", id, sensor.getCode());
    }

    @Override
    public List<ReadingVO> recentReadings(Long sensorId, int limit) {
        IotSensor sensor = sensorMapper.selectById(sensorId);
        if (sensor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "传感器不存在");
        }

        LambdaQueryWrapper<IotReading> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IotReading::getSensorId, sensorId)
                .orderByDesc(IotReading::getReadingAt)
                .last("LIMIT " + limit);
        List<IotReading> readings = readingMapper.selectList(wrapper);

        return readings.stream().map(r -> {
            ReadingVO vo = new ReadingVO();
            vo.setId(r.getId());
            vo.setSensorId(r.getSensorId());
            vo.setSensorCode(sensor.getCode());
            vo.setSensorName(sensor.getName());
            vo.setSensorType(sensor.getSensorType());
            vo.setUnit(sensor.getUnit());
            vo.setValue(r.getValue());
            vo.setReadingAt(r.getReadingAt());
            return vo;
        }).collect(Collectors.toList());
    }

    private SensorVO toVO(IotSensor sensor) {
        SensorVO vo = new SensorVO();
        vo.setId(sensor.getId());
        vo.setCode(sensor.getCode());
        vo.setName(sensor.getName());
        vo.setWarehouseId(sensor.getWarehouseId());
        vo.setDepotId(sensor.getDepotId());
        vo.setSensorType(sensor.getSensorType());
        vo.setUnit(sensor.getUnit());
        vo.setMinValue(sensor.getMinValue());
        vo.setMaxValue(sensor.getMaxValue());
        vo.setLastValue(sensor.getLastValue());
        vo.setLastAt(sensor.getLastAt());
        vo.setStatus(sensor.getStatus());
        vo.setRemark(sensor.getRemark());
        vo.setCreatedAt(sensor.getCreatedAt());

        // 填充 warehouseName
        Warehouse warehouse = warehouseMapper.selectById(sensor.getWarehouseId());
        if (warehouse != null) {
            vo.setWarehouseName(warehouse.getName());
        }
        // 填充 depotName
        GrainDepot depot = grainDepotMapper.selectById(sensor.getDepotId());
        if (depot != null) {
            vo.setDepotName(depot.getName());
        }
        return vo;
    }
}
