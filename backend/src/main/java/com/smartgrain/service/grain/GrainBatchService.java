package com.smartgrain.service.grain;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.domain.grain.dto.BatchCreateRequest;
import com.smartgrain.domain.grain.dto.BatchUpdateRequest;
import com.smartgrain.domain.grain.vo.BatchVO;

public interface GrainBatchService {

    PageResult<BatchVO> page(int page, int size, String keyword, String grainType, Integer status);

    BatchVO getDetail(Long id);

    Long create(BatchCreateRequest req);

    void update(BatchUpdateRequest req);

    void delete(Long id);
}
