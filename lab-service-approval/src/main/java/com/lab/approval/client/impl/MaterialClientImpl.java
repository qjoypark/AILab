package com.lab.approval.client.impl;

import com.lab.approval.client.MaterialClient;
import com.lab.approval.dto.MaterialInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 药品服务客户端实现
 * TODO: 后续使用Feign实现服务间调用
 */
@Slf4j
@Component
public class MaterialClientImpl implements MaterialClient {
    
    @Override
    public MaterialInfo getMaterialInfo(Long materialId) {
        // TODO: 调用药品服务API获取药品信息
        // 临时实现：返回模拟数据
        log.info("获取药品信息: materialId={}", materialId);
        
        MaterialInfo info = new MaterialInfo();
        info.setId(materialId);
        info.setMaterialName("测试药品-" + materialId);
        info.setSpecification("100ml");
        info.setUnit("瓶");
        info.setMaterialType(1);
        
        return info;
    }
}
