package com.smartgrain.controller.v1;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartgrain.common.api.Result;
import com.smartgrain.domain.alert.entity.AlertRecord;
import com.smartgrain.domain.alert.mapper.AlertRecordMapper;
import com.smartgrain.domain.grain.entity.GrainBatch;
import com.smartgrain.domain.grain.entity.GrainDepot;
import com.smartgrain.domain.grain.entity.Inventory;
import com.smartgrain.domain.grain.entity.Warehouse;
import com.smartgrain.domain.grain.mapper.GrainBatchMapper;
import com.smartgrain.domain.grain.mapper.GrainDepotMapper;
import com.smartgrain.domain.grain.mapper.InventoryMapper;
import com.smartgrain.domain.grain.mapper.WarehouseMapper;
import com.smartgrain.domain.iam.entity.SysUser;
import com.smartgrain.domain.iam.mapper.SysUserMapper;
import com.smartgrain.domain.iot.entity.IotSensor;
import com.smartgrain.domain.iot.mapper.IotSensorMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 可视化大屏聚合接口
 */
@Tag(name = "可视化大屏 Dashboard")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final GrainDepotMapper depotMapper;
    private final WarehouseMapper warehouseMapper;
    private final GrainBatchMapper batchMapper;
    private final InventoryMapper inventoryMapper;
    private final IotSensorMapper sensorMapper;
    private final AlertRecordMapper alertMapper;
    private final SysUserMapper userMapper;

    @Operation(summary = "KPI 总览")
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('iot:reading:view') or hasAuthority('alert:record:view') or hasAuthority('grain:depot:view')")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 粮库/粮仓/批次/用户/传感器
        data.put("depotCount", depotMapper.selectCount(null));
        data.put("warehouseCount", warehouseMapper.selectCount(null));
        data.put("batchCount", batchMapper.selectCount(null));
        data.put("userCount", userMapper.selectCount(null));
        data.put("sensorCount", sensorMapper.selectCount(new LambdaQueryWrapper<IotSensor>().eq(IotSensor::getStatus, 1)));
        data.put("sensorTotal", sensorMapper.selectCount(null));

        // 库存总量
        List<Inventory> inventories = inventoryMapper.selectList(null);
        double totalTons = inventories.stream()
                .mapToDouble(i -> i.getCurrentTons() == null ? 0 : i.getCurrentTons().doubleValue())
                .sum();
        data.put("totalStockTons", Math.round(totalTons * 100) / 100.0);

        // 在线粮仓数（有库存）
        long activeWarehouses = inventories.stream()
                .filter(i -> i.getCurrentTons() != null && i.getCurrentTons().doubleValue() > 0)
                .map(Inventory::getWarehouseId)
                .distinct()
                .count();
        data.put("activeWarehouses", activeWarehouses);

        // 告警统计
        data.put("alertPending", alertMapper.selectCount(new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getStatus, "PENDING")));
        data.put("alertProcessing", alertMapper.selectCount(new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getStatus, "PROCESSING")));
        data.put("alertResolved", alertMapper.selectCount(new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getStatus, "RESOLVED")));
        data.put("alertTotal", alertMapper.selectCount(null));

        return Result.success(data);
    }

    @Operation(summary = "粮库分布与库存")
    @GetMapping("/depots")
    @PreAuthorize("hasAuthority('grain:depot:view')")
    public Result<List<Map<String, Object>>> depots() {
        List<GrainDepot> depots = depotMapper.selectList(null);
        List<Inventory> inventories = inventoryMapper.selectList(null);

        Map<Long, Double> stockByDepot = new HashMap<>();
        for (Inventory inv : inventories) {
            stockByDepot.merge(inv.getDepotId(),
                    inv.getCurrentTons() == null ? 0 : inv.getCurrentTons().doubleValue(),
                    Double::sum);
        }

        return Result.success(depots.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("name", d.getName());
            m.put("province", d.getProvince());
            m.put("city", d.getCity());
            m.put("longitude", d.getLongitude());
            m.put("latitude", d.getLatitude());
            m.put("capacityTons", d.getCapacityTons());
            m.put("stockTons", Math.round(stockByDepot.getOrDefault(d.getId(), 0.0) * 100) / 100.0);
            m.put("manager", d.getManager());
            m.put("phone", d.getPhone());
            return m;
        }).toList());
    }

    @Operation(summary = "粮仓使用率")
    @GetMapping("/warehouses")
    @PreAuthorize("hasAuthority('grain:warehouse:view')")
    public Result<List<Map<String, Object>>> warehouses() {
        List<Warehouse> warehouses = warehouseMapper.selectList(null);
        return Result.success(warehouses.stream().map(w -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", w.getId());
            m.put("name", w.getName());
            m.put("depotId", w.getDepotId());
            m.put("capacityTons", w.getCapacityTons());
            m.put("currentTons", w.getCurrentTons());
            double rate = 0;
            if (w.getCapacityTons() != null && w.getCapacityTons().doubleValue() > 0
                    && w.getCurrentTons() != null) {
                rate = w.getCurrentTons().doubleValue() / w.getCapacityTons().doubleValue() * 100;
            }
            m.put("usageRate", Math.round(rate * 10) / 10.0);
            m.put("status", w.getStatus());
            return m;
        }).toList());
    }

    @Operation(summary = "粮食品种分布")
    @GetMapping("/grain-types")
    @PreAuthorize("hasAuthority('grain:batch:view')")
    public Result<List<Map<String, Object>>> grainTypes() {
        // 从 inventory 表按 grainType 聚合当前库存
        List<Inventory> inventories = inventoryMapper.selectList(null);
        Map<String, Double> byType = new LinkedHashMap<>();
        for (Inventory inv : inventories) {
            byType.merge(inv.getGrainType(),
                    inv.getCurrentTons() == null ? 0 : inv.getCurrentTons().doubleValue(),
                    Double::sum);
        }
        final Map<String, String> nameMap = Map.of(
                "WHEAT", "小麦",
                "RICE", "稻谷",
                "CORN", "玉米",
                "SOYBEAN", "大豆",
                "OIL", "食用油"
        );
        return Result.success(byType.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("type", e.getKey());
                    m.put("name", nameMap.getOrDefault(e.getKey(), e.getKey()));
                    m.put("tons", Math.round(e.getValue() * 100) / 100.0);
                    return m;
                })
                .toList());
    }

    @Operation(summary = "传感器类型分布")
    @GetMapping("/sensor-types")
    @PreAuthorize("hasAuthority('iot:sensor:view')")
    public Result<List<Map<String, Object>>> sensorTypes() {
        List<IotSensor> sensors = sensorMapper.selectList(null);
        Map<String, Long> byType = new LinkedHashMap<>();
        Map<String, Long> onlineByType = new LinkedHashMap<>();
        for (IotSensor s : sensors) {
            byType.merge(s.getSensorType(), 1L, Long::sum);
            if (s.getStatus() != null && s.getStatus() == 1) {
                onlineByType.merge(s.getSensorType(), 1L, Long::sum);
            }
        }
        final Map<String, String> nameMap = Map.of(
                "TEMP", "温度",
                "HUMIDITY", "湿度",
                "GRAIN_TEMP", "粮温",
                "PEST", "虫害",
                "GAS", "气体"
        );
        return Result.success(byType.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("type", e.getKey());
                    m.put("name", nameMap.getOrDefault(e.getKey(), e.getKey()));
                    m.put("total", e.getValue());
                    m.put("online", onlineByType.getOrDefault(e.getKey(), 0L));
                    return m;
                })
                .toList());
    }

    @Operation(summary = "近 7 日告警趋势")
    @GetMapping("/alert-trend")
    @PreAuthorize("hasAuthority('alert:stats:view')")
    public Result<Map<String, Object>> alertTrend() {
        Map<String, Object> data = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, Long> daily = new LinkedHashMap<>();
        Map<String, Long> dailyHigh = new LinkedHashMap<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();
            Long count = alertMapper.selectCount(new LambdaQueryWrapper<AlertRecord>()
                    .ge(AlertRecord::getTriggeredAt, start)
                    .lt(AlertRecord::getTriggeredAt, end));
            Long highCount = alertMapper.selectCount(new LambdaQueryWrapper<AlertRecord>()
                    .ge(AlertRecord::getTriggeredAt, start)
                    .lt(AlertRecord::getTriggeredAt, end)
                    .eq(AlertRecord::getSeverity, 3));
            daily.put(day.format(fmt), count);
            dailyHigh.put(day.format(fmt), highCount);
        }
        data.put("dates", daily.keySet());
        data.put("total", daily.values());
        data.put("high", dailyHigh.values());
        return Result.success(data);
    }
}
