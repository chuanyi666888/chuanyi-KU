package com.smartgrain.alert;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartgrain.domain.alert.entity.AlertRecord;
import com.smartgrain.domain.alert.mapper.AlertRecordMapper;
import com.smartgrain.domain.iot.entity.IotSensor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 告警规则引擎
 *
 * 规则：
 *  - 实际值 > maxValue  → HIGH 告警
 *  - 实际值 < minValue  → LOW 告警
 *
 * 去重：同一传感器 + 同一告警类型 + 状态非 RESOLVED/IGNORED 时不重复生成
 *
 * 严重等级：
 *  - 粮温(GRAIN_TEMP) / 气体(GAS) 超阈值 → 高 (3)
 *  - 虫害(PEST) 超阈值 → 高 (3)
 *  - 温度(TEMP) / 湿度(HUMIDITY) 超阈值 → 中 (2)
 *  - 其它 → 中 (2)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertRuleEngine {

    private final AlertRecordMapper alertMapper;

    private static final DateTimeFormatter CODE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 评估传感器读数，超阈值时生成告警
     *
     * @return 生成的告警，未生成则返回 null
     */
    public AlertRecord evaluate(IotSensor sensor, BigDecimal value) {
        if (sensor == null || value == null) return null;

        String alertType = null;
        BigDecimal threshold = null;
        if (sensor.getMaxValue() != null && value.compareTo(sensor.getMaxValue()) > 0) {
            alertType = "HIGH";
            threshold = sensor.getMaxValue();
        } else if (sensor.getMinValue() != null && value.compareTo(sensor.getMinValue()) < 0) {
            alertType = "LOW";
            threshold = sensor.getMinValue();
        }
        if (alertType == null) return null;

        // 去重：同传感器同类型未关闭的告警
        Long exists = alertMapper.selectCount(new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getSensorId, sensor.getId())
                .eq(AlertRecord::getAlertType, alertType)
                .in(AlertRecord::getStatus, "PENDING", "PROCESSING"));
        if (exists != null && exists > 0) {
            log.debug("[ALERT] 传感器 {} 已有 {} 未处理告警，跳过", sensor.getCode(), alertType);
            return null;
        }

        AlertRecord alert = new AlertRecord();
        alert.setAlertCode("AL" + LocalDateTime.now().format(CODE_FMT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
        alert.setSensorId(sensor.getId());
        alert.setSensorCode(sensor.getCode());
        alert.setSensorName(sensor.getName());
        alert.setWarehouseId(sensor.getWarehouseId());
        alert.setDepotId(sensor.getDepotId());
        alert.setSensorType(sensor.getSensorType());
        alert.setAlertType(alertType);
        alert.setThresholdValue(threshold);
        alert.setActualValue(value);
        alert.setUnit(sensor.getUnit() == null ? "" : sensor.getUnit());
        alert.setSeverity(decideSeverity(sensor.getSensorType()));
        alert.setStatus("PENDING");
        alert.setTriggeredAt(LocalDateTime.now());

        alertMapper.insert(alert);
        log.warn("[ALERT] 新告警 {} 传感器={} 类型={} 实际值={}{} 阈值={}{}",
                alert.getAlertCode(), sensor.getCode(), alertType,
                value, sensor.getUnit(), threshold, sensor.getUnit());
        return alert;
    }

    private Integer decideSeverity(String sensorType) {
        if (sensorType == null) return 2;
        return switch (sensorType) {
            case "GRAIN_TEMP", "GAS", "PEST" -> 3;
            case "TEMP", "HUMIDITY" -> 2;
            default -> 2;
        };
    }
}
