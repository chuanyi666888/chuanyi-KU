package com.smartgrain.domain.grain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartgrain.domain.grain.entity.InboundOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InboundOrderMapper extends BaseMapper<InboundOrder> {
}
