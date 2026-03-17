package com.lab.approval.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建领用申请请求
 */
@Data
public class CreateApplicationRequest {
    
    /**
     * 申请类型: 1-普通领用, 2-危化品领用
     */
    private Integer applicationType;
    
    /**
     * 用途说明
     */
    private String usagePurpose;
    
    /**
     * 使用地点
     */
    private String usageLocation;
    
    /**
     * 期望领用日期
     */
    private LocalDate expectedDate;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 申请明细列表
     */
    private List<ApplicationItemRequest> items;
    
    /**
     * 申请明细
     */
    @Data
    public static class ApplicationItemRequest {
        /**
         * 药品ID
         */
        private Long materialId;
        
        /**
         * 申请数量
         */
        private java.math.BigDecimal applyQuantity;
        
        /**
         * 备注
         */
        private String remark;
    }
}
