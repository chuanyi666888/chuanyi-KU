package com.smartgrain.controller.v1;

import com.smartgrain.common.api.Result;
import com.smartgrain.common.constant.SecurityConstants;
import com.smartgrain.domain.iam.dto.ChangePasswordRequest;
import com.smartgrain.domain.iam.dto.LoginRequest;
import com.smartgrain.domain.iam.dto.LoginResponse;
import com.smartgrain.domain.iam.dto.UserInfo;
import com.smartgrain.service.iam.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证 Auth")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        return Result.success(authService.login(request, httpRequest));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.JWT_HEADER);
        String token = null;
        if (StringUtils.hasText(header) && header.startsWith(SecurityConstants.JWT_PREFIX)) {
            token = header.substring(SecurityConstants.JWT_PREFIX.length()).trim();
        }
        authService.logout(token);
        return Result.success();
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestParam String refreshToken) {
        return Result.success(authService.refresh(refreshToken));
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/userinfo")
    public Result<UserInfo> userinfo() {
        return Result.success(authService.currentUserInfo());
    }

    @Operation(summary = "修改自己的密码")
    @PostMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return Result.success();
    }
}
