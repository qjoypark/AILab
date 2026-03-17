package com.lab.approval.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 审批处理请求DTO
 */
@Data
public class ApprovalProcessRequest {
    
    /**
     * 审批结果: 1-通过, 2-拒绝
     */
    private Integer approvalResult;
    
    /**
     * 审批意见
     */
    private String approvalOpinion;
    
    /**
     * 批准数量列表（可修改批准数量）
     */
    private List<ApprovedQuantityItem> approvedQuantities;
    
    /**
     * 批准数量项
     */
    @Data
    public static class ApprovedQuantityItem {
        /**
         * 申请明细ID
         */
        private Long itemId;
        
        /**
         * 批准数量
         */
        private BigDecimal approvedQuantity;
    }
}
