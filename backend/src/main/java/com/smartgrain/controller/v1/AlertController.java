package com.smartgrain.controller.v1;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.Result;
import com.smartgrain.domain.alert.dto.AlertProcessRequest;
import com.smartgrain.domain.alert.vo.AlertProcessLogVO;
import com.smartgrain.domain.alert.vo.AlertRecordVO;
import com.smartgrain.domain.alert.vo.AlertStatsVO;
import com.smartgrain.service.alert.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "告警管理 Alert")
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @Operation(summary = "告警分页查询")
    @GetMapping
    @PreAuthorize("hasAuthority('alert:record:view')")
    public Result<PageResult<AlertRecordVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long depotId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long sensorId,
            @RequestParam(required = false) String sensorType,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) Integer severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(alertService.page(page, size, depotId, warehouseId, sensorId,
                sensorType, alertType, severity, status, startTime, endTime));
    }

    @Operation(summary = "告警详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('alert:record:view')")
    public Result<AlertRecordVO> detail(@PathVariable Long id) {
        return Result.success(alertService.detail(id));
    }

    @Operation(summary = "处理告警 ACK/RESOLVE/IGNORE/REOPEN")
    @PostMapping("/process")
    @PreAuthorize("hasAuthority('alert:record:process')")
    public Result<Long> process(@Valid @RequestBody AlertProcessRequest request) {
        return Result.success(alertService.process(request));
    }

    @Operation(summary = "删除告警")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('alert:record:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        alertService.delete(id);
        return Result.success();
    }

    @Operation(summary = "告警处理流水")
    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAuthority('alert:record:view')")
    public Result<List<AlertProcessLogVO>> processLogs(@PathVariable Long id) {
        return Result.success(alertService.processLogs(id));
    }

    @Operation(summary = "告警统计")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('alert:stats:view')")
    public Result<AlertStatsVO> stats() {
        return Result.success(alertService.stats());
    }
}
