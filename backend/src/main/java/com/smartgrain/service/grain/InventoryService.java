package com.smartgrain.service.grain;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.domain.grain.vo.InventoryVO;

public interface InventoryService {

    PageResult<InventoryVO> page(int page, int size, Long depotId, Long warehouseId,
                                 Long batchId, String grainType);
}
