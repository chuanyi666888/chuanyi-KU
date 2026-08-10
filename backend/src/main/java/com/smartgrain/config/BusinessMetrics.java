package com.smartgrain.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartgrain.domain.alert.entity.AlertRecord;
import com.smartgrain.domain.alert.mapper.AlertRecordMapper;
import com.smartgrain.domain.grain.entity.Inventory;
import com.smartgrain.domain.grain.mapper.InventoryMapper;
import com.smartgrain.domain.iot.entity.IotSensor;
import com.smartgrain.domain.iot.mapper.IotSensorMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务指标采集
 *
 * 暴露给 Prometheus 的业务指标：
 *  - smartgrain_stock_tons_total            库存总量（吨）
 *  - smartgrain_alerts_total{status="..."}  各状态告警数
 *  - smartgrain_sensors_total{status="..."} 在线/离线传感器数
 *
 * Prometheus 抓取后，可通过 Grafana 渲染为业务监控大屏。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessMetrics {

    private final MeterRegistry registry;
    private final InventoryMapper inventoryMapper;
    private final AlertRecordMapper alertMapper;
    private final IotSensorMapper sensorMapper;

    @PostConstruct
    public void init() {
        // 库存总量
        Gauge.builder("smartgrain.stock.tons.total", this::totalStockTons)
                .description("库存总量（吨）")
                .register(registry);

        // 各状态告警
        for (String status : List.of("PENDING", "PROCESSING", "RESOLVED", "IGNORED")) {
            Gauge.builder("smartgrain.alerts.total", () -> countAlerts(status))
                    .tags(Tags.of("status", status))
                    .description("各状态告警数")
                    .register(registry);
        }

        // 在线/离线传感器
        Gauge.builder("smartgrain.sensors.total", () -> countSensors(1))
                .tags(Tags.of("status", "online"))
                .description("在线传感器数")
                .register(registry);
        Gauge.builder("smartgrain.sensors.total", () -> countSensors(0))
                .tags(Tags.of("status", "offline"))
                .description("离线传感器数")
                .register(registry);

        log.info("[METRICS] 业务指标已注册: smartgrain.stock/alerts/sensors");
    }

    private double totalStockTons() {
        try {
            List<Inventory> list = inventoryMapper.selectList(null);
            return list.stream()
                    .mapToDouble(i -> i.getCurrentTons() == null ? 0 : i.getCurrentTons().doubleValue())
                    .sum();
        } catch (Exception e) {
            return 0;
        }
    }

    private int countAlerts(String status) {
        try {
            Long c = alertMapper.selectCount(new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getStatus, status));
            return c == null ? 0 : c.intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private int countSensors(int status) {
        try {
            Long c = sensorMapper.selectCount(new LambdaQueryWrapper<IotSensor>().eq(IotSensor::getStatus, status));
            return c == null ? 0 : c.intValue();
        } catch (Exception e) {
            return 0;
        }
    }
}
