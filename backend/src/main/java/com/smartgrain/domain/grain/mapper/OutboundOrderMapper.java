package com.smartgrain.domain.grain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartgrain.domain.grain.entity.OutboundOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboundOrderMapper extends BaseMapper<OutboundOrder> {
}
