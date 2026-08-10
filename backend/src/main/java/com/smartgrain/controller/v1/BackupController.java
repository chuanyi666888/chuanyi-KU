package com.smartgrain.controller.v1;

import com.smartgrain.common.api.Result;
import com.smartgrain.domain.backup.vo.BackupRecordVO;
import com.smartgrain.domain.backup.vo.BackupStatsVO;
import com.smartgrain.service.backup.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 备份恢复管理 API
 */
@Tag(name = "备份恢复 Backup")
@RestController
@RequestMapping("/api/v1/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @Operation(summary = "备份记录列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:backup:view')")
    public Result<List<BackupRecordVO>> list(
            @RequestParam(defaultValue = "mysql") String dbKind,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(backupService.listBackups(dbKind, category, limit));
    }

    @Operation(summary = "今日备份列表")
    @GetMapping("/today")
    @PreAuthorize("hasAuthority('system:backup:view')")
    public Result<List<BackupRecordVO>> today() {
        return Result.success(backupService.listTodayBackups());
    }

    @Operation(summary = "备份统计概览")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('system:backup:view')")
    public Result<BackupStatsVO> stats() {
        return Result.success(backupService.stats());
    }

    @Operation(summary = "校验备份完整性（MD5）")
    @GetMapping("/verify")
    @PreAuthorize("hasAuthority('system:backup:view')")
    public Result<Map<String, Object>> verify(
            @RequestParam String dbKind,
            @RequestParam String category,
            @RequestParam String backupDate) {
        return Result.success(backupService.verify(dbKind, category, backupDate));
    }

    @Operation(summary = "触发备份任务（异步执行）")
    @PostMapping("/trigger")
    @PreAuthorize("hasAuthority('system:backup:manage')")
    public Result<Map<String, Object>> trigger(
            @RequestParam(defaultValue = "mysql") String dbKind,
            @RequestParam(defaultValue = "full") String action) {
        return Result.success(backupService.trigger(dbKind, action));
    }

    @Operation(summary = "查询备份任务执行状态")
    @GetMapping("/tasks/{taskId}")
    @PreAuthorize("hasAuthority('system:backup:view')")
    public Result<Object> task(@PathVariable String taskId) {
        return Result.success(backupService.getTask(taskId));
    }
}
