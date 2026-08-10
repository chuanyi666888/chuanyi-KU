package com.smartgrain.controller.v1;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.Result;
import com.smartgrain.domain.iam.dto.AssignPermissionsRequest;
import com.smartgrain.domain.iam.dto.RoleCreateRequest;
import com.smartgrain.domain.iam.dto.RoleUpdateRequest;
import com.smartgrain.domain.iam.vo.RoleVO;
import com.smartgrain.service.iam.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "角色 Role")
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "角色分页查询")
    @GetMapping
    @PreAuthorize("hasAuthority('system:role:view')")
    public Result<PageResult<RoleVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(roleService.page(page, size, keyword));
    }

    @Operation(summary = "全部启用角色（下拉用）")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('system:role:view')")
    public Result<List<RoleVO>> all() {
        return Result.success(roleService.listAll());
    }

    @Operation(summary = "角色详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:view')")
    public Result<RoleVO> detail(@PathVariable Long id) {
        return Result.success(roleService.getDetail(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @PreAuthorize("hasAuthority('system:role:create')")
    public Result<Long> create(@Valid @RequestBody RoleCreateRequest request) {
        return Result.success(roleService.create(request));
    }

    @Operation(summary = "修改角色")
    @PutMapping
    @PreAuthorize("hasAuthority('system:role:update')")
    public Result<Void> update(@Valid @RequestBody RoleUpdateRequest request) {
        roleService.update(request);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }

    @Operation(summary = "为角色分配权限（全量覆盖）")
    @PostMapping("/permissions")
    @PreAuthorize("hasAuthority('system:role:assign')")
    public Result<Void> assignPermissions(@Valid @RequestBody AssignPermissionsRequest request) {
        roleService.assignPermissions(request);
        return Result.success();
    }

    @Operation(summary = "查询角色已分配权限 ID")
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:view')")
    public Result<List<Long>> permissionIds(@PathVariable Long id) {
        return Result.success(roleService.getPermissionIds(id));
    }
}
