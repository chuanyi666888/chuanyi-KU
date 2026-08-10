package com.smartgrain.iot;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgrain.alert.AlertRuleEngine;
import com.smartgrain.domain.alert.entity.AlertRecord;
import com.smartgrain.domain.iot.entity.IotReading;
import com.smartgrain.domain.iot.entity.IotSensor;
import com.smartgrain.domain.iot.mapper.IotReadingMapper;
import com.smartgrain.domain.iot.mapper.IotSensorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IotSimulator {

    private final IotSensorMapper sensorMapper;
    private final IotReadingMapper readingMapper;
    private final IotWebSocketHandler wsHandler;
    private final ObjectMapper objectMapper;
    private final AlertRuleEngine alertRuleEngine;

    @Scheduled(fixedRate = 10000) // 每 10 秒
    public void generateAndPush() {
        List<IotSensor> sensors = sensorMapper.selectList(new LambdaQueryWrapper<IotSensor>().eq(IotSensor::getStatus, 1));
        for (IotSensor sensor : sensors) {
            BigDecimal value = randomValue(sensor);
            // 保存读数历史
            IotReading reading = new IotReading();
            reading.setSensorId(sensor.getId());
            reading.setValue(value);
            reading.setReadingAt(LocalDateTime.now());
            readingMapper.insert(reading);
            // 更新传感器最新值
            sensor.setLastValue(value);
            sensor.setLastAt(LocalDateTime.now());
            sensorMapper.updateById(sensor);

            // 告警规则评估
            boolean alarm = false;
            AlertRecord newAlert = alertRuleEngine.evaluate(sensor, value);
            if (newAlert != null) {
                alarm = true;
                pushAlertNotification(newAlert);
            } else {
                alarm = isAlarm(sensor, value);
            }

            // 推送 WebSocket
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("sensorId", sensor.getId());
                payload.put("sensorCode", sensor.getCode());
                payload.put("sensorName", sensor.getName());
                payload.put("sensorType", sensor.getSensorType());
                payload.put("unit", sensor.getUnit());
                payload.put("value", value);
                payload.put("readingAt", LocalDateTime.now().toString());
                payload.put("alarm", alarm);
                wsHandler.broadcast(objectMapper.writeValueAsString(payload));
            } catch (Exception e) {
                log.warn("推送失败: {}", e.getMessage());
            }
        }
        log.debug("IoT 模拟器推送 {} 条数据，在线客户端: {}", sensors.size(), wsHandler.onlineCount());
    }

    /** 告警事件单独推送，前端可弹通知 */
    private void pushAlertNotification(AlertRecord alert) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "ALERT");
            payload.put("alertId", alert.getId());
            payload.put("alertCode", alert.getAlertCode());
            payload.put("sensorId", alert.getSensorId());
            payload.put("sensorCode", alert.getSensorCode());
            payload.put("sensorName", alert.getSensorName());
            payload.put("sensorType", alert.getSensorType());
            payload.put("alertType", alert.getAlertType());
            payload.put("severity", alert.getSeverity());
            payload.put("actualValue", alert.getActualValue());
            payload.put("thresholdValue", alert.getThresholdValue());
            payload.put("unit", alert.getUnit());
            payload.put("triggeredAt", alert.getTriggeredAt().toString());
            wsHandler.broadcast(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("告警推送失败: {}", e.getMessage());
        }
    }

    private BigDecimal randomValue(IotSensor s) {
        // 基于上次值小幅波动，偶尔故意触发越界以演示告警
        BigDecimal base = s.getLastValue() != null ? s.getLastValue() : new BigDecimal("20");
        double delta = (Math.random() - 0.5) * 2; // -1 ~ +1
        BigDecimal newVal = base.add(BigDecimal.valueOf(delta)).setScale(2, RoundingMode.HALF_UP);

        // 5% 概率故意越界，触发告警引擎
        if (Math.random() < 0.05) {
            if (s.getMaxValue() != null && Math.random() < 0.5) {
                newVal = s.getMaxValue().add(BigDecimal.valueOf(1 + Math.random() * 2)).setScale(2, RoundingMode.HALF_UP);
            } else if (s.getMinValue() != null) {
                newVal = s.getMinValue().subtract(BigDecimal.valueOf(1 + Math.random() * 2)).setScale(2, RoundingMode.HALF_UP);
            }
        }
        return newVal;
    }

    private boolean isAlarm(IotSensor s, BigDecimal value) {
        if (s.getMinValue() != null && value.compareTo(s.getMinValue()) < 0) return true;
        if (s.getMaxValue() != null && value.compareTo(s.getMaxValue()) > 0) return true;
        return false;
    }
}
