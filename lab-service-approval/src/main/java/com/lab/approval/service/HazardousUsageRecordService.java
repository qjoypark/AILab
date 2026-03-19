package com.lab.approval.service;

import com.lab.approval.dto.HazardousReturnRequest;
import com.lab.approval.entity.HazardousUsageRecord;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 危化品使用记录服务接口
 */
public interface HazardousUsageRecordService {

    /**
     * 鍒嗛〉鏌ヨ鍗卞寲鍝佷娇鐢ㄨ褰?
     */
    Page<HazardousUsageRecord> listUsageRecords(
            int page,
            int size,
            Integer status,
            String keyword,
            LocalDate startDate,
            LocalDate endDate
    );
    
    /**
     * 创建危化品使用记录
     * 
     * @param record 使用记录
     * @return 创建的记录
     */
    HazardousUsageRecord createUsageRecord(HazardousUsageRecord record);
    
    /**
     * 危化品归还
     * 
     * @param recordId 使用记录ID
     * @param returnRequest 归还请求
     * @return 更新后的使用记录
     */
    HazardousUsageRecord returnHazardousMaterial(Long recordId, HazardousReturnRequest returnRequest);
    
    /**
     * 获取危化品已领用未归还数量
     * 
     * @param materialId 药品ID
     * @return 已领用未归还数量
     */
    BigDecimal getUnreturnedQuantity(Long materialId);
}
