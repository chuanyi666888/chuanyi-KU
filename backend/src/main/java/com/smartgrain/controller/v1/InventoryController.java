package com.smartgrain.controller.v1;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.Result;
import com.smartgrain.domain.grain.vo.InventoryVO;
import com.smartgrain.service.grain.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "库存 Inventory")
@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "库存分页查询")
    @GetMapping
    @PreAuthorize("hasAuthority('grain:inventory:view')")
    public Result<PageResult<InventoryVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long depotId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String grainType) {
        return Result.success(inventoryService.page(page, size, depotId, warehouseId, batchId, grainType));
    }
}
