package com.smartgrain.domain.iot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartgrain.domain.iot.entity.IotReading;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IotReadingMapper extends BaseMapper<IotReading> {
}
