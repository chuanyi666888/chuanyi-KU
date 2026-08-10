package com.smartgrain.domain.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartgrain.domain.iam.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /** 通过用户 ID 查询权限码 */
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    /** 通过角色 ID 列表查询权限 */
    List<SysPermission> selectPermissionsByRoleIds(@Param("roleIds") List<Long> roleIds);

    /** 通过角色 ID 查询已绑定权限 ID */
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);
}
