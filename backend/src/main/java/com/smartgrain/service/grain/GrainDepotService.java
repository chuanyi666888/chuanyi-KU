package com.smartgrain.service.grain;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.domain.grain.dto.DepotCreateRequest;
import com.smartgrain.domain.grain.dto.DepotUpdateRequest;
import com.smartgrain.domain.grain.vo.DepotVO;

import java.util.List;

public interface GrainDepotService {

    PageResult<DepotVO> page(int page, int size, String keyword, Integer status);

    List<DepotVO> listAll();

    DepotVO getDetail(Long id);

    Long create(DepotCreateRequest req);

    void update(DepotUpdateRequest req);

    void delete(Long id);
}
