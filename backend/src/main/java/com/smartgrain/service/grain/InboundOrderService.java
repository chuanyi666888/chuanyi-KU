package com.smartgrain.service.grain;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.domain.grain.dto.InboundCreateRequest;
import com.smartgrain.domain.grain.dto.OrderAuditRequest;
import com.smartgrain.domain.grain.vo.InboundOrderVO;

public interface InboundOrderService {

    PageResult<InboundOrderVO> page(int page, int size, Long depotId, Long warehouseId,
                                    String batchNo, Integer status, String keyword);

    InboundOrderVO getDetail(Long id);

    Long create(InboundCreateRequest req);

    void audit(OrderAuditRequest req);
}
