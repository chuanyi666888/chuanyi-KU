package com.smartgrain.service.iam;

import com.smartgrain.domain.iam.dto.ChangePasswordRequest;
import com.smartgrain.domain.iam.dto.LoginRequest;
import com.smartgrain.domain.iam.dto.LoginResponse;
import com.smartgrain.domain.iam.dto.UserInfo;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest);

    void logout(String accessToken);

    LoginResponse refresh(String refreshToken);

    UserInfo currentUserInfo();

    void changePassword(ChangePasswordRequest request);
}
