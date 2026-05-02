package com.lab.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.approval.client.InventoryClient;
import com.lab.approval.client.MaterialClient;
import com.lab.approval.dto.HazardousReturnRequest;
import com.lab.approval.dto.MaterialInfo;
import com.lab.approval.entity.HazardousUsageRecord;
import com.lab.approval.entity.MaterialApplication;
import com.lab.approval.mapper.HazardousUsageRecordMapper;
import com.lab.approval.mapper.MaterialApplicationMapper;
import com.lab.approval.service.HazardousUsageRecordService;
import com.lab.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Usage record service implementation (historical class name kept).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HazardousUsageRecordServiceImpl implements HazardousUsageRecordService {

    private final HazardousUsageRecordMapper usageRecordMapper;
    private final InventoryClient inventoryClient;
    private final MaterialClient materialClient;
    private final MaterialApplicationMapper applicationMapper;

    @Override
    public Page<HazardousUsageRecord> listUsageRecords(
            int page,
            int size,
            Integer status,
            String keyword,
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Page<HazardousUsageRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<HazardousUsageRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(HazardousUsageRecord::getStatus, status);
        }
        if (userId != null) {
            wrapper.eq(HazardousUsageRecord::getUserId, userId);
        }
        if (StringUtils.hasText(keyword)) {
            String keywordValue = keyword.trim();
            List<Long> applicationIds = resolveApplicationIdsByKeyword(keywordValue);
            wrapper.and(query -> {
                query.like(HazardousUsageRecord::getUserName, keywordValue)
                        .or()
                        .like(HazardousUsageRecord::getUsagePurpose, keywordValue)
                        .or()
                        .like(HazardousUsageRecord::getUsageLocation, keywordValue);
                if (!applicationIds.isEmpty()) {
                    query.or().in(HazardousUsageRecord::getApplicationId, applicationIds);
                }
            });
        }
        if (startDate != null) {
            wrapper.ge(HazardousUsageRecord::getUsageDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(HazardousUsageRecord::getUsageDate, endDate);
        }
        wrapper.orderByDesc(HazardousUsageRecord::getCreatedTime);

        Page<HazardousUsageRecord> resultPage = usageRecordMapper.selectPage(pageParam, wrapper);
        enrichDisplayFields(resultPage.getRecords());
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HazardousUsageRecord createUsageRecord(HazardousUsageRecord record) {
        validateAndBindApplication(record);
        log.info("Create usage record: applicationId={}, materialId={}, userId={}",
                record.getApplicationId(), record.getMaterialId(), record.getUserId());
        usageRecordMapper.insert(record);
        log.info("Usage record created: recordId={}", record.getId());
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HazardousUsageRecord returnHazardousMaterial(Long recordId, HazardousReturnRequest returnRequest) {
        HazardousUsageRecord record = usageRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("Usage record does not exist.");
        }
        if (!Objects.equals(record.getStatus(), 1)) {
            throw new BusinessException("This record has already been returned.");
        }
        if (returnRequest.getReturnedQuantity() == null) {
            throw new BusinessException("Returned quantity cannot be empty.");
        }

        ReturnQuantities quantities = resolveReturnQuantities(record, returnRequest);
        log.info("Return verification: recordId={}, received={}, used={}, returned={}, waste={}",
                recordId,
                record.getReceivedQuantity(),
                quantities.actualUsedQuantity(),
                quantities.returnedQuantity(),
                quantities.wasteQuantity());

        if (quantities.returnedQuantity().compareTo(BigDecimal.ZERO) > 0) {
            boolean success = inventoryClient.returnHazardousMaterial(
                    record.getMaterialId(),
                    quantities.returnedQuantity(),
                    "Material return stock-in, usage record ID: " + recordId
            );
            if (!success) {
                throw new BusinessException("Failed to stock-in returned quantity.");
            }
        }

        record.setActualUsedQuantity(quantities.actualUsedQuantity());
        record.setReturnedQuantity(quantities.returnedQuantity());
        record.setWasteQuantity(quantities.wasteQuantity());
        record.setReturnDate(LocalDate.now());
        record.setStatus(2);

        if (StringUtils.hasText(returnRequest.getRemark())) {
            String existingRemark = record.getRemark() != null ? record.getRemark() : "";
            record.setRemark(existingRemark.isEmpty()
                    ? returnRequest.getRemark()
                    : existingRemark + "; " + returnRequest.getRemark());
        }

        usageRecordMapper.updateById(record);
        enrichDisplayFields(List.of(record));
        log.info("Return completed: recordId={}, status={}", recordId, record.getStatus());
        return record;
    }

    @Override
    public BigDecimal getUnreturnedQuantity(Long materialId) {
        LambdaQueryWrapper<HazardousUsageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HazardousUsageRecord::getMaterialId, materialId)
                .eq(HazardousUsageRecord::getStatus, 1);
        List<HazardousUsageRecord> records = usageRecordMapper.selectList(wrapper);
        return records.stream()
                .map(HazardousUsageRecord::getReceivedQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ReturnQuantities resolveReturnQuantities(HazardousUsageRecord record, HazardousReturnRequest request) {
        BigDecimal receivedQuantity = safe(record.getReceivedQuantity());
        BigDecimal returnedQuantity = safe(request.getReturnedQuantity());
        BigDecimal actualUsedQuantity = request.getActualUsedQuantity();
        BigDecimal wasteQuantity = request.getWasteQuantity();

        if (actualUsedQuantity == null && wasteQuantity == null) {
            actualUsedQuantity = receivedQuantity.subtract(returnedQuantity);
            wasteQuantity = BigDecimal.ZERO;
        } else {
            actualUsedQuantity = safe(actualUsedQuantity);
            wasteQuantity = safe(wasteQuantity);
        }

        if (actualUsedQuantity.compareTo(BigDecimal.ZERO) < 0
                || returnedQuantity.compareTo(BigDecimal.ZERO) < 0
                || wasteQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Used/returned/waste quantities must be non-negative.");
        }

        BigDecimal totalQuantity = actualUsedQuantity.add(returnedQuantity).add(wasteQuantity);
        if (totalQuantity.compareTo(receivedQuantity) != 0) {
            throw new BusinessException(String.format(
                    "Quantity mismatch: used(%.2f) + returned(%.2f) + waste(%.2f) = %.2f, expected %.2f.",
                    actualUsedQuantity, returnedQuantity, wasteQuantity, totalQuantity, receivedQuantity
            ));
        }

        return new ReturnQuantities(actualUsedQuantity, returnedQuantity, wasteQuantity);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void enrichDisplayFields(List<HazardousUsageRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<Long, String> materialNameMap = resolveMaterialNameMap(records);
        Map<Long, String> applicationNoMap = resolveApplicationNoMap(records);
        for (HazardousUsageRecord record : records) {
            if (record == null) {
                continue;
            }
            if (record.getMaterialId() != null) {
                record.setMaterialName(materialNameMap.get(record.getMaterialId()));
            }
            if (record.getApplicationId() != null) {
                record.setApplicationNo(applicationNoMap.get(record.getApplicationId()));
            }
        }
    }

    private Map<Long, String> resolveMaterialNameMap(List<HazardousUsageRecord> records) {
        List<Long> materialIds = records.stream()
                .map(HazardousUsageRecord::getMaterialId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (materialIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, String> materialNameMap = new HashMap<>();
        for (Long materialId : materialIds) {
            MaterialInfo materialInfo = materialClient.getMaterialInfo(materialId);
            if (materialInfo != null && StringUtils.hasText(materialInfo.getMaterialName())) {
                materialNameMap.put(materialId, materialInfo.getMaterialName());
            }
        }
        return materialNameMap;
    }

    private Map<Long, String> resolveApplicationNoMap(List<HazardousUsageRecord> records) {
        List<Long> applicationIds = records.stream()
                .map(HazardousUsageRecord::getApplicationId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (applicationIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, String> applicationNoMap = new HashMap<>();
        List<MaterialApplication> applications = applicationMapper.selectBatchIds(applicationIds);
        if (applications == null || applications.isEmpty()) {
            return applicationNoMap;
        }
        for (MaterialApplication application : applications) {
            if (application == null || application.getId() == null) {
                continue;
            }
            if (StringUtils.hasText(application.getApplicationNo())) {
                applicationNoMap.put(application.getId(), application.getApplicationNo());
            }
        }
        return applicationNoMap;
    }

    private List<Long> resolveApplicationIdsByKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<MaterialApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(MaterialApplication::getId);
        wrapper.like(MaterialApplication::getApplicationNo, keyword);
        List<MaterialApplication> applications = applicationMapper.selectList(wrapper);
        if (applications == null || applications.isEmpty()) {
            return Collections.emptyList();
        }
        return applications.stream()
                .map(MaterialApplication::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private void validateAndBindApplication(HazardousUsageRecord record) {
        Long applicationId = record.getApplicationId();
        String applicationNo = trimToNull(record.getApplicationNo());
        if (applicationId == null && !StringUtils.hasText(applicationNo)) {
            throw new BusinessException("Usage record must be linked to applicationId/applicationNo.");
        }

        MaterialApplication application;
        if (applicationId != null) {
            application = applicationMapper.selectById(applicationId);
            if (application == null) {
                throw new BusinessException("Application does not exist: " + applicationId);
            }
            if (StringUtils.hasText(applicationNo) && !applicationNo.equals(application.getApplicationNo())) {
                throw new BusinessException("applicationId and applicationNo do not match.");
            }
        } else {
            LambdaQueryWrapper<MaterialApplication> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MaterialApplication::getApplicationNo, applicationNo);
            wrapper.last("LIMIT 1");
            application = applicationMapper.selectOne(wrapper);
            if (application == null) {
                throw new BusinessException("Application does not exist: " + applicationNo);
            }
        }

        record.setApplicationId(application.getId());
        record.setApplicationNo(application.getApplicationNo());
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record ReturnQuantities(BigDecimal actualUsedQuantity,
                                    BigDecimal returnedQuantity,
                                    BigDecimal wasteQuantity) {
    }
}
