package com.smartgrain.service.alert;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.domain.alert.dto.AlertProcessRequest;
import com.smartgrain.domain.alert.vo.AlertProcessLogVO;
import com.smartgrain.domain.alert.vo.AlertRecordVO;
import com.smartgrain.domain.alert.vo.AlertStatsVO;

import java.util.List;

public interface AlertService {

    /** 分页查询告警 */
    PageResult<AlertRecordVO> page(int page, int size,
                                   Long depotId, Long warehouseId, Long sensorId,
                                   String sensorType, String alertType,
                                   Integer severity, String status,
                                   String startTime, String endTime);

    /** 告警详情 */
    AlertRecordVO detail(Long id);

    /** 处理告警：ACK / RESOLVE / IGNORE / REOPEN */
    Long process(AlertProcessRequest request);

    /** 删除告警（软删除） */
    void delete(Long id);

    /** 查询告警处理流水 */
    List<AlertProcessLogVO> processLogs(Long alertId);

    /** 告警统计 */
    AlertStatsVO stats();
}
