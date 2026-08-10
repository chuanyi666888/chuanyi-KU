package com.smartgrain.service.grain;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.domain.grain.dto.OrderAuditRequest;
import com.smartgrain.domain.grain.dto.OutboundCreateRequest;
import com.smartgrain.domain.grain.vo.OutboundOrderVO;

public interface OutboundOrderService {

    PageResult<OutboundOrderVO> page(int page, int size, Long depotId, Long warehouseId,
                                     String batchNo, Integer status, String keyword);

    OutboundOrderVO getDetail(Long id);

    Long create(OutboundCreateRequest req);

    void audit(OrderAuditRequest req);
}
