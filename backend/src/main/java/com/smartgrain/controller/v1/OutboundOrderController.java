package com.smartgrain.controller.v1;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.Result;
import com.smartgrain.domain.grain.dto.OrderAuditRequest;
import com.smartgrain.domain.grain.dto.OutboundCreateRequest;
import com.smartgrain.domain.grain.vo.OutboundOrderVO;
import com.smartgrain.service.grain.OutboundOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "出库单 OutboundOrder")
@RestController
@RequestMapping("/api/v1/outbound-orders")
@RequiredArgsConstructor
public class OutboundOrderController {

    private final OutboundOrderService outboundOrderService;

    @Operation(summary = "出库单分页查询")
    @GetMapping
    @PreAuthorize("hasAuthority('grain:outbound:view')")
    public Result<PageResult<OutboundOrderVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long depotId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(outboundOrderService.page(page, size, depotId, warehouseId, batchNo, status, keyword));
    }

    @Operation(summary = "出库单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('grain:outbound:view')")
    public Result<OutboundOrderVO> detail(@PathVariable Long id) {
        return Result.success(outboundOrderService.getDetail(id));
    }

    @Operation(summary = "新增出库单")
    @PostMapping
    @PreAuthorize("hasAuthority('grain:outbound:create')")
    public Result<Long> create(@Valid @RequestBody OutboundCreateRequest request) {
        return Result.success(outboundOrderService.create(request));
    }

    @Operation(summary = "审核出库单")
    @PostMapping("/audit")
    @PreAuthorize("hasAuthority('grain:outbound:audit')")
    public Result<Void> audit(@Valid @RequestBody OrderAuditRequest request) {
        outboundOrderService.audit(request);
        return Result.success();
    }
}
