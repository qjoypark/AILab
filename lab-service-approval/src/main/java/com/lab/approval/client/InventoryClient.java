package com.lab.approval.client;

import java.math.BigDecimal;

/**
 * 库存服务客户端接口
 * TODO: 后续使用Feign实现服务间调用
 */
public interface InventoryClient {
    
    /**
     * 检查库存是否充足
     * 
     * @param materialId 药品ID
     * @param quantity 需要数量
     * @return 是否充足
     */
    boolean checkStockAvailability(Long materialId, BigDecimal quantity);
    
    /**
     * 获取药品可用库存数量
     * 
     * @param materialId 药品ID
     * @return 可用库存数量
     */
    BigDecimal getAvailableStock(Long materialId);
    
    /**
     * 创建出库单
     * 
     * @param applicationId 申请单ID
     * @return 出库单ID
     */
    Long createStockOutOrder(Long applicationId);
    
    /**
     * 危化品归还入库
     * 
     * @param materialId 药品ID
     * @param returnQuantity 归还数量
     * @param remark 备注
     * @return 是否成功
     */
    boolean returnHazardousMaterial(Long materialId, BigDecimal returnQuantity, String remark);
}
