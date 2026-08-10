package com.smartgrain.controller.v1;

import com.smartgrain.common.api.Result;
import com.smartgrain.domain.iam.vo.PermissionVO;
import com.smartgrain.service.iam.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "权限 Permission")
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "全量权限树")
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:permission:view')")
    public Result<List<PermissionVO>> tree() {
        return Result.success(permissionService.tree());
    }

    @Operation(summary = "当前用户菜单树")
    @GetMapping("/menu-tree")
    public Result<List<PermissionVO>> menuTree() {
        return Result.success(permissionService.currentUserMenuTree());
    }
}
