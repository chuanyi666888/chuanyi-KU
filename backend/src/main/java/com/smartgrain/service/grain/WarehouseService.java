package com.smartgrain.service.grain;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.domain.grain.dto.WarehouseCreateRequest;
import com.smartgrain.domain.grain.dto.WarehouseUpdateRequest;
import com.smartgrain.domain.grain.vo.WarehouseVO;

import java.util.List;

public interface WarehouseService {

    PageResult<WarehouseVO> page(int page, int size, Long depotId, String keyword, Integer status);

    List<WarehouseVO> listByDepot(Long depotId);

    WarehouseVO getDetail(Long id);

    Long create(WarehouseCreateRequest req);

    void update(WarehouseUpdateRequest req);

    void delete(Long id);
}
