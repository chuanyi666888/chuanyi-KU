package com.smartgrain.controller.v1;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.Result;
import com.smartgrain.domain.iot.dto.SensorCreateRequest;
import com.smartgrain.domain.iot.dto.SensorUpdateRequest;
import com.smartgrain.domain.iot.vo.ReadingVO;
import com.smartgrain.domain.iot.vo.SensorVO;
import com.smartgrain.service.iot.IotSensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "IoT 传感器 IotSensor")
@RestController
@RequestMapping("/api/v1/iot/sensors")
@RequiredArgsConstructor
public class IotSensorController {

    private final IotSensorService iotSensorService;

    @Operation(summary = "传感器分页查询")
    @GetMapping
    @PreAuthorize("hasAuthority('iot:sensor:view')")
    public Result<PageResult<SensorVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long depotId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String sensorType,
            @RequestParam(required = false) Integer status) {
        return Result.success(iotSensorService.page(page, size, depotId, warehouseId, sensorType, status));
    }

    @Operation(summary = "按粮仓查询传感器列表")
    @GetMapping("/by-warehouse/{warehouseId}")
    @PreAuthorize("hasAuthority('iot:sensor:view')")
    public Result<List<SensorVO>> listByWarehouse(@PathVariable Long warehouseId) {
        return Result.success(iotSensorService.listByWarehouse(warehouseId));
    }

    @Operation(summary = "传感器详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('iot:sensor:view')")
    public Result<SensorVO> detail(@PathVariable Long id) {
        return Result.success(iotSensorService.getDetail(id));
    }

    @Operation(summary = "传感器最近读数")
    @GetMapping("/{id}/readings")
    @PreAuthorize("hasAuthority('iot:reading:view')")
    public Result<List<ReadingVO>> recentReadings(
            @PathVariable Long id,
            @RequestParam(defaultValue = "60") int limit) {
        return Result.success(iotSensorService.recentReadings(id, limit));
    }

    @Operation(summary = "新增传感器")
    @PostMapping
    @PreAuthorize("hasAuthority('iot:sensor:create')")
    public Result<Long> create(@Valid @RequestBody SensorCreateRequest request) {
        return Result.success(iotSensorService.create(request));
    }

    @Operation(summary = "修改传感器")
    @PutMapping
    @PreAuthorize("hasAuthority('iot:sensor:update')")
    public Result<Void> update(@Valid @RequestBody SensorUpdateRequest request) {
        iotSensorService.update(request);
        return Result.success();
    }

    @Operation(summary = "删除传感器")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('iot:sensor:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        iotSensorService.delete(id);
        return Result.success();
    }
}
