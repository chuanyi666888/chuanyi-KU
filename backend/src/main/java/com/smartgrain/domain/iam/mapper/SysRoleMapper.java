package com.smartgrain.domain.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartgrain.domain.iam.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /** 通过用户 ID 查询角色编码 */
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /** 通过用户 ID 查询角色对象 */
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    /** 统计绑定该角色的用户数 */
    long countUsersByRoleId(@Param("roleId") Long roleId);
}
