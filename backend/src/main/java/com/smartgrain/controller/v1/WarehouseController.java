package com.smartgrain.controller.v1;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.Result;
import com.smartgrain.domain.grain.dto.WarehouseCreateRequest;
import com.smartgrain.domain.grain.dto.WarehouseUpdateRequest;
import com.smartgrain.domain.grain.vo.WarehouseVO;
import com.smartgrain.service.grain.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "粮仓 Warehouse")
@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "粮仓分页查询")
    @GetMapping
    @PreAuthorize("hasAuthority('grain:warehouse:view')")
    public Result<PageResult<WarehouseVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long depotId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(warehouseService.page(page, size, depotId, keyword, status));
    }

    @Operation(summary = "按粮库查询粮仓列表")
    @GetMapping("/by-depot/{depotId}")
    @PreAuthorize("hasAuthority('grain:warehouse:view')")
    public Result<List<WarehouseVO>> listByDepot(@PathVariable Long depotId) {
        return Result.success(warehouseService.listByDepot(depotId));
    }

    @Operation(summary = "粮仓详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('grain:warehouse:view')")
    public Result<WarehouseVO> detail(@PathVariable Long id) {
        return Result.success(warehouseService.getDetail(id));
    }

    @Operation(summary = "新增粮仓")
    @PostMapping
    @PreAuthorize("hasAuthority('grain:warehouse:create')")
    public Result<Long> create(@Valid @RequestBody WarehouseCreateRequest request) {
        return Result.success(warehouseService.create(request));
    }

    @Operation(summary = "修改粮仓")
    @PutMapping
    @PreAuthorize("hasAuthority('grain:warehouse:update')")
    public Result<Void> update(@Valid @RequestBody WarehouseUpdateRequest request) {
        warehouseService.update(request);
        return Result.success();
    }

    @Operation(summary = "删除粮仓")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('grain:warehouse:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return Result.success();
    }
}
