package com.smartgrain.service.iot;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.domain.iot.dto.SensorCreateRequest;
import com.smartgrain.domain.iot.dto.SensorUpdateRequest;
import com.smartgrain.domain.iot.vo.ReadingVO;
import com.smartgrain.domain.iot.vo.SensorVO;

import java.util.List;

public interface IotSensorService {

    PageResult<SensorVO> page(int page, int size, Long depotId, Long warehouseId, String sensorType, Integer status);

    List<SensorVO> listByWarehouse(Long warehouseId);

    SensorVO getDetail(Long id);

    Long create(SensorCreateRequest req);

    void update(SensorUpdateRequest req);

    void delete(Long id);

    /** 最近 N 条读数 */
    List<ReadingVO> recentReadings(Long sensorId, int limit);
}
