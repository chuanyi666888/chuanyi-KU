package com.smartgrain.service.iam;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.domain.iam.dto.PasswordResetRequest;
import com.smartgrain.domain.iam.dto.UserCreateRequest;
import com.smartgrain.domain.iam.dto.UserUpdateRequest;
import com.smartgrain.domain.iam.vo.UserVO;

public interface UserService {

    PageResult<UserVO> page(int page, int size, String keyword, Integer status);

    UserVO getDetail(Long id);

    Long create(UserCreateRequest request);

    void update(UserUpdateRequest request);

    void delete(Long id);

    void resetPassword(PasswordResetRequest request);
}
