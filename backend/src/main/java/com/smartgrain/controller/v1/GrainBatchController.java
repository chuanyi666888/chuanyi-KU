package com.smartgrain.controller.v1;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.Result;
import com.smartgrain.domain.grain.dto.BatchCreateRequest;
import com.smartgrain.domain.grain.dto.BatchUpdateRequest;
import com.smartgrain.domain.grain.vo.BatchVO;
import com.smartgrain.service.grain.GrainBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "粮油批次 GrainBatch")
@RestController
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
public class GrainBatchController {

    private final GrainBatchService grainBatchService;

    @Operation(summary = "批次分页查询")
    @GetMapping
    @PreAuthorize("hasAuthority('grain:batch:view')")
    public Result<PageResult<BatchVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String grainType,
            @RequestParam(required = false) Integer status) {
        return Result.success(grainBatchService.page(page, size, keyword, grainType, status));
    }

    @Operation(summary = "批次详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('grain:batch:view')")
    public Result<BatchVO> detail(@PathVariable Long id) {
        return Result.success(grainBatchService.getDetail(id));
    }

    @Operation(summary = "新增批次")
    @PostMapping
    @PreAuthorize("hasAuthority('grain:batch:create')")
    public Result<Long> create(@Valid @RequestBody BatchCreateRequest request) {
        return Result.success(grainBatchService.create(request));
    }

    @Operation(summary = "修改批次")
    @PutMapping
    @PreAuthorize("hasAuthority('grain:batch:update')")
    public Result<Void> update(@Valid @RequestBody BatchUpdateRequest request) {
        grainBatchService.update(request);
        return Result.success();
    }

    @Operation(summary = "删除批次")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('grain:batch:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        grainBatchService.delete(id);
        return Result.success();
    }
}
