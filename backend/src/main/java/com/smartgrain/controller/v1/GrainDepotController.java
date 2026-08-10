package com.smartgrain.controller.v1;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.Result;
import com.smartgrain.domain.grain.dto.DepotCreateRequest;
import com.smartgrain.domain.grain.dto.DepotUpdateRequest;
import com.smartgrain.domain.grain.vo.DepotVO;
import com.smartgrain.service.grain.GrainDepotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "粮库 GrainDepot")
@RestController
@RequestMapping("/api/v1/depots")
@RequiredArgsConstructor
public class GrainDepotController {

    private final GrainDepotService grainDepotService;

    @Operation(summary = "粮库分页查询")
    @GetMapping
    @PreAuthorize("hasAuthority('grain:depot:view')")
    public Result<PageResult<DepotVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(grainDepotService.page(page, size, keyword, status));
    }

    @Operation(summary = "粮库全量列表")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('grain:depot:view')")
    public Result<List<DepotVO>> listAll() {
        return Result.success(grainDepotService.listAll());
    }

    @Operation(summary = "粮库详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('grain:depot:view')")
    public Result<DepotVO> detail(@PathVariable Long id) {
        return Result.success(grainDepotService.getDetail(id));
    }

    @Operation(summary = "新增粮库")
    @PostMapping
    @PreAuthorize("hasAuthority('grain:depot:create')")
    public Result<Long> create(@Valid @RequestBody DepotCreateRequest request) {
        return Result.success(grainDepotService.create(request));
    }

    @Operation(summary = "修改粮库")
    @PutMapping
    @PreAuthorize("hasAuthority('grain:depot:update')")
    public Result<Void> update(@Valid @RequestBody DepotUpdateRequest request) {
        grainDepotService.update(request);
        return Result.success();
    }

    @Operation(summary = "删除粮库")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('grain:depot:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        grainDepotService.delete(id);
        return Result.success();
    }
}
