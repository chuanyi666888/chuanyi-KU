package com.smartgrain.service.grain.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.ResultCode;
import com.smartgrain.common.exception.BusinessException;
import com.smartgrain.domain.grain.dto.DepotCreateRequest;
import com.smartgrain.domain.grain.dto.DepotUpdateRequest;
import com.smartgrain.domain.grain.entity.GrainDepot;
import com.smartgrain.domain.grain.mapper.GrainDepotMapper;
import com.smartgrain.domain.grain.vo.DepotVO;
import com.smartgrain.service.grain.GrainDepotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrainDepotServiceImpl implements GrainDepotService {

    private final GrainDepotMapper grainDepotMapper;

    @Override
    public PageResult<DepotVO> page(int page, int size, String keyword, Integer status) {
        LambdaQueryWrapper<GrainDepot> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(GrainDepot::getCode, keyword)
                    .or().like(GrainDepot::getName, keyword)
                    .or().like(GrainDepot::getAddress, keyword)
                    .or().like(GrainDepot::getManager, keyword));
        }
        if (status != null) {
            wrapper.eq(GrainDepot::getStatus, status);
        }
        wrapper.orderByDesc(GrainDepot::getCreatedAt);

        Page<GrainDepot> result = grainDepotMapper.selectPage(new Page<>(page, size), wrapper);
        List<DepotVO> list = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, size);
    }

    @Override
    public List<DepotVO> listAll() {
        LambdaQueryWrapper<GrainDepot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GrainDepot::getStatus, 1)
                .orderByAsc(GrainDepot::getCode);
        List<GrainDepot> depots = grainDepotMapper.selectList(wrapper);
        return depots.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public DepotVO getDetail(Long id) {
        GrainDepot depot = grainDepotMapper.selectById(id);
        if (depot == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "粮库不存在");
        }
        return toVO(depot);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DepotCreateRequest req) {
        // 编码唯一性校验
        Long exists = grainDepotMapper.selectCount(new LambdaQueryWrapper<GrainDepot>()
                .eq(GrainDepot::getCode, req.getCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "粮库编码已存在");
        }

        GrainDepot depot = new GrainDepot();
        depot.setCode(req.getCode());
        depot.setName(req.getName());
        depot.setAddress(req.getAddress());
        depot.setProvince(req.getProvince());
        depot.setCity(req.getCity());
        depot.setDistrict(req.getDistrict());
        depot.setLongitude(req.getLongitude());
        depot.setLatitude(req.getLatitude());
        depot.setCapacityTons(req.getCapacityTons());
        depot.setManager(req.getManager());
        depot.setPhone(req.getPhone());
        depot.setRemark(req.getRemark());
        depot.setStatus(1);
        grainDepotMapper.insert(depot);

        log.info("[DEPOT] 新增粮库 id={} code={}", depot.getId(), depot.getCode());
        return depot.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DepotUpdateRequest req) {
        GrainDepot depot = grainDepotMapper.selectById(req.getId());
        if (depot == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "粮库不存在");
        }

        depot.setName(req.getName());
        depot.setAddress(req.getAddress());
        depot.setProvince(req.getProvince());
        depot.setCity(req.getCity());
        depot.setDistrict(req.getDistrict());
        depot.setLongitude(req.getLongitude());
        depot.setLatitude(req.getLatitude());
        depot.setCapacityTons(req.getCapacityTons());
        depot.setManager(req.getManager());
        depot.setPhone(req.getPhone());
        if (req.getStatus() != null) {
            depot.setStatus(req.getStatus());
        }
        depot.setRemark(req.getRemark());
        grainDepotMapper.updateById(depot);

        log.info("[DEPOT] 更新粮库 id={}", req.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        GrainDepot depot = grainDepotMapper.selectById(id);
        if (depot == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "粮库不存在");
        }
        grainDepotMapper.deleteById(id);
        log.info("[DEPOT] 删除粮库 id={} code={}", id, depot.getCode());
    }

    private DepotVO toVO(GrainDepot depot) {
        DepotVO vo = new DepotVO();
        vo.setId(depot.getId());
        vo.setCode(depot.getCode());
        vo.setName(depot.getName());
        vo.setAddress(depot.getAddress());
        vo.setProvince(depot.getProvince());
        vo.setCity(depot.getCity());
        vo.setDistrict(depot.getDistrict());
        vo.setLongitude(depot.getLongitude());
        vo.setLatitude(depot.getLatitude());
        vo.setCapacityTons(depot.getCapacityTons());
        vo.setManager(depot.getManager());
        vo.setPhone(depot.getPhone());
        vo.setStatus(depot.getStatus());
        vo.setRemark(depot.getRemark());
        vo.setCreatedAt(depot.getCreatedAt());
        return vo;
    }
}
