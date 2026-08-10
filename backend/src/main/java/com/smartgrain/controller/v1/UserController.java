package com.smartgrain.controller.v1;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.Result;
import com.smartgrain.domain.iam.dto.PasswordResetRequest;
import com.smartgrain.domain.iam.dto.UserCreateRequest;
import com.smartgrain.domain.iam.dto.UserUpdateRequest;
import com.smartgrain.domain.iam.vo.UserVO;
import com.smartgrain.service.iam.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户 User")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户分页查询")
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<PageResult<UserVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(userService.page(page, size, keyword, status));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.success(userService.getDetail(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:create')")
    public Result<Long> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.create(request));
    }

    @Operation(summary = "修改用户")
    @PutMapping
    @PreAuthorize("hasAuthority('system:user:update')")
    public Result<Void> update(@Valid @RequestBody UserUpdateRequest request) {
        userService.update(request);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PostMapping("/reset-password")
    @PreAuthorize("hasAuthority('system:user:reset')")
    public Result<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(request);
        return Result.success();
    }
}
