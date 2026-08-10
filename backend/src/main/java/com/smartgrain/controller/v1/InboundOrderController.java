package com.smartgrain.controller.v1;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.Result;
import com.smartgrain.domain.grain.dto.InboundCreateRequest;
import com.smartgrain.domain.grain.dto.OrderAuditRequest;
import com.smartgrain.domain.grain.vo.InboundOrderVO;
import com.smartgrain.service.grain.InboundOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "入库单 InboundOrder")
@RestController
@RequestMapping("/api/v1/inbound-orders")
@RequiredArgsConstructor
public class InboundOrderController {

    private final InboundOrderService inboundOrderService;

    @Operation(summary = "入库单分页查询")
    @GetMapping
    @PreAuthorize("hasAuthority('grain:inbound:view')")
    public Result<PageResult<InboundOrderVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long depotId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(inboundOrderService.page(page, size, depotId, warehouseId, batchNo, status, keyword));
    }

    @Operation(summary = "入库单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('grain:inbound:view')")
    public Result<InboundOrderVO> detail(@PathVariable Long id) {
        return Result.success(inboundOrderService.getDetail(id));
    }

    @Operation(summary = "新增入库单")
    @PostMapping
    @PreAuthorize("hasAuthority('grain:inbound:create')")
    public Result<Long> create(@Valid @RequestBody InboundCreateRequest request) {
        return Result.success(inboundOrderService.create(request));
    }

    @Operation(summary = "审核入库单")
    @PostMapping("/audit")
    @PreAuthorize("hasAuthority('grain:inbound:audit')")
    public Result<Void> audit(@Valid @RequestBody OrderAuditRequest request) {
        inboundOrderService.audit(request);
        return Result.success();
    }
}
