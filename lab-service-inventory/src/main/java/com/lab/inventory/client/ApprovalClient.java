package com.lab.inventory.client;

import com.lab.inventory.dto.HazardousUsageRecordDTO;
import com.lab.inventory.dto.LabUsageApplicationDTO;
import com.lab.inventory.dto.MaterialApplicationDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 审批服务客户端接口
 * TODO: 后续使用Feign实现服务间调用
 */
public interface ApprovalClient {
    
    /**
     * 获取申请单详情
     * 
     * @param applicationId 申请单ID
     * @return 申请单详情
     */
    MaterialApplicationDTO getApplicationDetail(Long applicationId);
    
    /**
     * 更新申请单状态为已出库
     * 
     * @param applicationId 申请单ID
     */
    void updateApplicationStatusToStockOut(Long applicationId);
    
    /**
     * 创建危化品使用记录
     * 
     * @param recordDTO 使用记录DTO
     */
    void createHazardousUsageRecord(HazardousUsageRecordDTO recordDTO);
    
    /**
     * 获取危化品已领用未归还数量
     * 
     * @param materialId 药品ID
     * @return 已领用未归还数量
     */
    BigDecimal getUnreturnedQuantity(Long materialId);
    
    /**
     * 查询用户的待审批申请列表
     * 
     * @param approverId 审批人ID
     * @return 待审批申请列表
     */
    List<MaterialApplicationDTO> getPendingApprovals(Long approverId);

    /**
     * 查询用户的待审批实验室使用申请列表
     *
     * @param approverId 审批人ID
     * @return 待审批实验室使用申请列表
     */
    List<LabUsageApplicationDTO> getPendingLabUsageApprovals(Long approverId);
}
