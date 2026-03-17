package com.lab.approval.client;

import com.lab.approval.dto.MaterialInfo;

/**
 * 药品服务客户端接口
 * TODO: 后续使用Feign实现服务间调用
 */
public interface MaterialClient {
    
    /**
     * 获取药品信息
     * 
     * @param materialId 药品ID
     * @return 药品信息
     */
    MaterialInfo getMaterialInfo(Long materialId);
}
