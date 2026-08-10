package com.smartgrain.service.alert.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.ResultCode;
import com.smartgrain.common.exception.BusinessException;
import com.smartgrain.common.util.SecurityUtils;
import com.smartgrain.domain.alert.dto.AlertProcessRequest;
import com.smartgrain.domain.alert.entity.AlertProcessLog;
import com.smartgrain.domain.alert.entity.AlertRecord;
import com.smartgrain.domain.alert.mapper.AlertProcessLogMapper;
import com.smartgrain.domain.alert.mapper.AlertRecordMapper;
import com.smartgrain.domain.alert.vo.AlertProcessLogVO;
import com.smartgrain.domain.alert.vo.AlertRecordVO;
import com.smartgrain.domain.alert.vo.AlertStatsVO;
import com.smartgrain.domain.grain.entity.GrainDepot;
import com.smartgrain.domain.grain.entity.Warehouse;
import com.smartgrain.domain.grain.mapper.GrainDepotMapper;
import com.smartgrain.domain.grain.mapper.WarehouseMapper;
import com.smartgrain.domain.iam.entity.SysUser;
import com.smartgrain.domain.iam.mapper.SysUserMapper;
import com.smartgrain.service.alert.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertRecordMapper alertMapper;
    private final AlertProcessLogMapper logMapper;
    private final WarehouseMapper warehouseMapper;
    private final GrainDepotMapper depotMapper;
    private final SysUserMapper userMapper;

    @Override
    public PageResult<AlertRecordVO> page(int page, int size,
                                          Long depotId, Long warehouseId, Long sensorId,
                                          String sensorType, String alertType,
                                          Integer severity, String status,
                                          String startTime, String endTime) {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        if (depotId != null) wrapper.eq(AlertRecord::getDepotId, depotId);
        if (warehouseId != null) wrapper.eq(AlertRecord::getWarehouseId, warehouseId);
        if (sensorId != null) wrapper.eq(AlertRecord::getSensorId, sensorId);
        if (StringUtils.hasText(sensorType)) wrapper.eq(AlertRecord::getSensorType, sensorType);
        if (StringUtils.hasText(alertType)) wrapper.eq(AlertRecord::getAlertType, alertType);
        if (severity != null) wrapper.eq(AlertRecord::getSeverity, severity);
        if (StringUtils.hasText(status)) wrapper.eq(AlertRecord::getStatus, status);
        if (StringUtils.hasText(startTime)) wrapper.ge(AlertRecord::getTriggeredAt, startTime);
        if (StringUtils.hasText(endTime)) wrapper.le(AlertRecord::getTriggeredAt, endTime);
        wrapper.orderByDesc(AlertRecord::getTriggeredAt);

        Page<AlertRecord> result = alertMapper.selectPage(new Page<>(page, size), wrapper);
        List<AlertRecordVO> list = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, size);
    }

    @Override
    public AlertRecordVO detail(Long id) {
        AlertRecord alert = alertMapper.selectById(id);
        if (alert == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "告警不存在");
        }
        return toVO(alert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long process(AlertProcessRequest req) {
        AlertRecord alert = alertMapper.selectById(req.getAlertId());
        if (alert == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "告警不存在");
        }

        String action = req.getAction();
        String fromStatus = alert.getStatus();
        String toStatus;
        switch (action) {
            case "ACK":     toStatus = "PROCESSING"; break;
            case "RESOLVE": toStatus = "RESOLVED";   break;
            case "IGNORE":  toStatus = "IGNORED";    break;
            case "REOPEN":  toStatus = "PENDING";    break;
            default:
                throw new BusinessException(ResultCode.PARAM_INVALID, "未知动作: " + action);
        }

        alert.setStatus(toStatus);
        if ("RESOLVED".equals(toStatus)) {
            alert.setResolvedAt(LocalDateTime.now());
        } else if ("PENDING".equals(toStatus)) {
            alert.setResolvedAt(null);
        }
        if (StringUtils.hasText(req.getComment())) {
            alert.setRemark(req.getComment());
        }
        alertMapper.updateById(alert);

        AlertProcessLog processLog = new AlertProcessLog();
        processLog.setAlertId(alert.getId());
        processLog.setAction(action);
        processLog.setFromStatus(fromStatus);
        processLog.setToStatus(toStatus);
        processLog.setComment(req.getComment());
        processLog.setProcessAt(LocalDateTime.now());
        processLog.setProcessBy(SecurityUtils.getCurrentUserId());
        logMapper.insert(processLog);

        log.info("[ALERT] 告警 {} 处理动作 {} -> {}", alert.getAlertCode(), action, toStatus);
        return processLog.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AlertRecord alert = alertMapper.selectById(id);
        if (alert == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "告警不存在");
        }
        alertMapper.deleteById(id);
        log.info("[ALERT] 删除告警 {}", alert.getAlertCode());
    }

    @Override
    public List<AlertProcessLogVO> processLogs(Long alertId) {
        LambdaQueryWrapper<AlertProcessLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertProcessLog::getAlertId, alertId)
                .orderByAsc(AlertProcessLog::getProcessAt);
        List<AlertProcessLog> logs = logMapper.selectList(wrapper);

        Map<Long, String> userNames = new HashMap<>();
        for (AlertProcessLog log : logs) {
            if (log.getProcessBy() != null && !userNames.containsKey(log.getProcessBy())) {
                SysUser u = userMapper.selectById(log.getProcessBy());
                userNames.put(log.getProcessBy(), u == null ? "未知" : u.getNickname());
            }
        }

        return logs.stream().map(log -> {
            AlertProcessLogVO vo = new AlertProcessLogVO();
            vo.setId(log.getId());
            vo.setAlertId(log.getAlertId());
            vo.setAction(log.getAction());
            vo.setActionName(actionName(log.getAction()));
            vo.setFromStatus(log.getFromStatus());
            vo.setToStatus(log.getToStatus());
            vo.setComment(log.getComment());
            vo.setProcessAt(log.getProcessAt());
            vo.setProcessBy(log.getProcessBy());
            vo.setProcessByName(userNames.get(log.getProcessBy()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public AlertStatsVO stats() {
        AlertStatsVO vo = new AlertStatsVO();
        vo.setTotal(alertMapper.selectCount(Wrappers.emptyWrapper()));
        vo.setPending(alertMapper.selectCount(new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getStatus, "PENDING")));
        vo.setProcessing(alertMapper.selectCount(new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getStatus, "PROCESSING")));
        vo.setResolved(alertMapper.selectCount(new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getStatus, "RESOLVED")));
        vo.setIgnored(alertMapper.selectCount(new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getStatus, "IGNORED")));

        // 按严重等级
        Map<Integer, Long> bySeverity = new HashMap<>();
        for (int s = 1; s <= 3; s++) {
            bySeverity.put(s, alertMapper.selectCount(new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getSeverity, s)));
        }
        vo.setBySeverity(bySeverity);

        // 按传感器类型
        Map<String, Long> bySensorType = new HashMap<>();
        for (String t : List.of("TEMP", "HUMIDITY", "GRAIN_TEMP", "PEST", "GAS")) {
            bySensorType.put(t, alertMapper.selectCount(new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getSensorType, t)));
        }
        vo.setBySensorType(bySensorType);

        // 按粮库
        Map<Long, Long> byDepot = new HashMap<>();
        List<GrainDepot> depots = depotMapper.selectList(null);
        for (GrainDepot d : depots) {
            byDepot.put(d.getId(), alertMapper.selectCount(new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getDepotId, d.getId())));
        }
        vo.setByDepot(byDepot);

        // 近 7 日趋势
        Map<String, Long> dailyTrend = new HashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();
            Long count = alertMapper.selectCount(new LambdaQueryWrapper<AlertRecord>()
                    .ge(AlertRecord::getTriggeredAt, start)
                    .lt(AlertRecord::getTriggeredAt, end));
            dailyTrend.put(day.format(fmt), count);
        }
        vo.setDailyTrend(dailyTrend);

        return vo;
    }

    private AlertRecordVO toVO(AlertRecord alert) {
        AlertRecordVO vo = new AlertRecordVO();
        vo.setId(alert.getId());
        vo.setAlertCode(alert.getAlertCode());
        vo.setSensorId(alert.getSensorId());
        vo.setSensorCode(alert.getSensorCode());
        vo.setSensorName(alert.getSensorName());
        vo.setWarehouseId(alert.getWarehouseId());
        vo.setDepotId(alert.getDepotId());
        vo.setSensorType(alert.getSensorType());
        vo.setAlertType(alert.getAlertType());
        vo.setAlertTypeName(alertTypeName(alert.getAlertType()));
        vo.setThresholdValue(alert.getThresholdValue());
        vo.setActualValue(alert.getActualValue());
        vo.setUnit(alert.getUnit());
        vo.setSeverity(alert.getSeverity());
        vo.setSeverityName(severityName(alert.getSeverity()));
        vo.setStatus(alert.getStatus());
        vo.setStatusName(statusName(alert.getStatus()));
        vo.setTriggeredAt(alert.getTriggeredAt());
        vo.setResolvedAt(alert.getResolvedAt());
        vo.setRemark(alert.getRemark());
        vo.setCreatedAt(alert.getCreatedAt());

        if (alert.getWarehouseId() != null) {
            Warehouse wh = warehouseMapper.selectById(alert.getWarehouseId());
            if (wh != null) vo.setWarehouseName(wh.getName());
        }
        if (alert.getDepotId() != null) {
            GrainDepot depot = depotMapper.selectById(alert.getDepotId());
            if (depot != null) vo.setDepotName(depot.getName());
        }
        return vo;
    }

    private String alertTypeName(String t) {
        return switch (t) {
            case "HIGH" -> "超上限";
            case "LOW" -> "低于下限";
            case "OFFLINE" -> "离线";
            default -> t;
        };
    }

    private String severityName(Integer s) {
        if (s == null) return "";
        return switch (s) {
            case 1 -> "低";
            case 2 -> "中";
            case 3 -> "高";
            default -> s.toString();
        };
    }

    private String statusName(String s) {
        return switch (s) {
            case "PENDING" -> "待处理";
            case "PROCESSING" -> "处理中";
            case "RESOLVED" -> "已解决";
            case "IGNORED" -> "已忽略";
            default -> s;
        };
    }

    private String actionName(String a) {
        return switch (a) {
            case "ACK" -> "确认";
            case "RESOLVE" -> "解决";
            case "IGNORE" -> "忽略";
            case "REOPEN" -> "重开";
            default -> a;
        };
    }
}
