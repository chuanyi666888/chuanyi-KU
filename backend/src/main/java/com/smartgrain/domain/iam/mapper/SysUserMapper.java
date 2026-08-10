package com.smartgrain.domain.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartgrain.domain.iam.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 查询用户已绑定的角色 ID */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /** 查询用户角色编码（带 ROLE_ 前缀） */
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
