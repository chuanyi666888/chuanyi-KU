package com.smartgrain.service.grain.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.ResultCode;
import com.smartgrain.common.exception.BusinessException;
import com.smartgrain.domain.grain.dto.BatchCreateRequest;
import com.smartgrain.domain.grain.dto.BatchUpdateRequest;
import com.smartgrain.domain.grain.entity.GrainBatch;
import com.smartgrain.domain.grain.mapper.GrainBatchMapper;
import com.smartgrain.domain.grain.vo.BatchVO;
import com.smartgrain.service.grain.GrainBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrainBatchServiceImpl implements GrainBatchService {

    private final GrainBatchMapper grainBatchMapper;

    @Override
    public PageResult<BatchVO> page(int page, int size, String keyword, String grainType, Integer status) {
        LambdaQueryWrapper<GrainBatch> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(GrainBatch::getBatchNo, keyword)
                    .or().like(GrainBatch::getVariety, keyword)
                    .or().like(GrainBatch::getOrigin, keyword));
        }
        if (StringUtils.hasText(grainType)) {
            wrapper.eq(GrainBatch::getGrainType, grainType);
        }
        if (status != null) {
            wrapper.eq(GrainBatch::getStatus, status);
        }
        wrapper.orderByDesc(GrainBatch::getCreatedAt);

        Page<GrainBatch> result = grainBatchMapper.selectPage(new Page<>(page, size), wrapper);
        List<BatchVO> list = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, size);
    }

    @Override
    public BatchVO getDetail(Long id) {
        GrainBatch batch = grainBatchMapper.selectById(id);
        if (batch == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "批次不存在");
        }
        return toVO(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(BatchCreateRequest req) {
        // 批次号唯一性校验
        Long exists = grainBatchMapper.selectCount(new LambdaQueryWrapper<GrainBatch>()
                .eq(GrainBatch::getBatchNo, req.getBatchNo()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "批次号已存在");
        }

        GrainBatch batch = new GrainBatch();
        batch.setBatchNo(req.getBatchNo());
        batch.setGrainType(req.getGrainType());
        batch.setVariety(req.getVariety());
        batch.setGrade(req.getGrade());
        batch.setOrigin(req.getOrigin());
        batch.setProductionYear(req.getProductionYear());
        batch.setMoisture(req.getMoisture());
        batch.setImpurity(req.getImpurity());
        batch.setTestWeight(req.getTestWeight());
        batch.setTotalInTons(BigDecimal.ZERO);
        batch.setTotalOutTons(BigDecimal.ZERO);
        batch.setStatus(1);
        batch.setRemark(req.getRemark());
        grainBatchMapper.insert(batch);

        log.info("[BATCH] 新增批次 id={} batchNo={}", batch.getId(), batch.getBatchNo());
        return batch.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BatchUpdateRequest req) {
        GrainBatch batch = grainBatchMapper.selectById(req.getId());
        if (batch == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "批次不存在");
        }

        batch.setVariety(req.getVariety());
        batch.setGrade(req.getGrade());
        batch.setOrigin(req.getOrigin());
        batch.setProductionYear(req.getProductionYear());
        batch.setMoisture(req.getMoisture());
        batch.setImpurity(req.getImpurity());
        batch.setTestWeight(req.getTestWeight());
        if (req.getStatus() != null) {
            batch.setStatus(req.getStatus());
        }
        batch.setRemark(req.getRemark());
        grainBatchMapper.updateById(batch);

        log.info("[BATCH] 更新批次 id={}", req.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        GrainBatch batch = grainBatchMapper.selectById(id);
        if (batch == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "批次不存在");
        }
        if (batch.getStatus() != null && batch.getStatus() == 1) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "在储批次不可删除");
        }
        grainBatchMapper.deleteById(id);
        log.info("[BATCH] 删除批次 id={} batchNo={}", id, batch.getBatchNo());
    }

    private BatchVO toVO(GrainBatch batch) {
        BatchVO vo = new BatchVO();
        vo.setId(batch.getId());
        vo.setBatchNo(batch.getBatchNo());
        vo.setGrainType(batch.getGrainType());
        vo.setVariety(batch.getVariety());
        vo.setGrade(batch.getGrade());
        vo.setOrigin(batch.getOrigin());
        vo.setProductionYear(batch.getProductionYear());
        vo.setMoisture(batch.getMoisture());
        vo.setImpurity(batch.getImpurity());
        vo.setTestWeight(batch.getTestWeight());
        vo.setTotalInTons(batch.getTotalInTons());
        vo.setTotalOutTons(batch.getTotalOutTons());
        // 库存 = 入库总量 - 出库总量
        BigDecimal totalIn = batch.getTotalInTons() == null ? BigDecimal.ZERO : batch.getTotalInTons();
        BigDecimal totalOut = batch.getTotalOutTons() == null ? BigDecimal.ZERO : batch.getTotalOutTons();
        vo.setStockTons(totalIn.subtract(totalOut));
        vo.setStatus(batch.getStatus());
        vo.setRemark(batch.getRemark());
        vo.setCreatedAt(batch.getCreatedAt());
        return vo;
    }
}
