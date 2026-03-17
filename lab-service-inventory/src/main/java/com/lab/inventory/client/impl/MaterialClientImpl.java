package com.lab.inventory.client.impl;

import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.dto.MaterialCategoryInfo;
import com.lab.inventory.dto.MaterialInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 药品服务客户端实现
 */
@Slf4j
@Component
public class MaterialClientImpl implements MaterialClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${material.service.url:http://localhost:8081}")
    private String materialServiceUrl;
    
    public MaterialClientImpl() {
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    public MaterialInfo getMaterialInfo(Long materialId) {
        log.info("获取药品信息: materialId={}", materialId);
        
        try {
            String url = materialServiceUrl + "/api/v1/materials/" + materialId;
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null) {
                    MaterialInfo info = new MaterialInfo();
                    info.setId(Long.valueOf(data.get("id").toString()));
                    info.setMaterialName((String) data.get("materialName"));
                    info.setSpecification((String) data.get("specification"));
                    info.setUnit((String) data.get("unit"));
                    info.setMaterialType((Integer) data.get("materialType"));
                    if (data.get("categoryId") != null) {
                        info.setCategoryId(Long.valueOf(data.get("categoryId").toString()));
                    }
                    if (data.get("isControlled") != null) {
                        info.setIsControlled((Integer) data.get("isControlled"));
                    }
                    
                    log.info("获取药品信息成功: materialId={}", materialId);
                    return info;
                }
            }
            
            log.error("获取药品信息失败: materialId={}, response={}", materialId, response);
            return null;
        } catch (Exception e) {
            log.error("调用药品服务获取药品信息失败: materialId={}", materialId, e);
            return null;
        }
    }
    
    @Override
    public List<MaterialInfo> getHazardousMaterials() {
        log.info("获取所有危化品列表");
        
        try {
            // 查询药品类型为3(危化品)或管控类型>0的药品
            String url = materialServiceUrl + "/api/v1/materials/hazardous";
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Object data = body.get("data");
                
                if (data instanceof List) {
                    List<Map<String, Object>> materials = (List<Map<String, Object>>) data;
                    List<MaterialInfo> result = new ArrayList<>();
                    
                    for (Map<String, Object> material : materials) {
                        MaterialInfo info = new MaterialInfo();
                        info.setId(Long.valueOf(material.get("id").toString()));
                        info.setMaterialName((String) material.get("materialName"));
                        info.setSpecification((String) material.get("specification"));
                        info.setUnit((String) material.get("unit"));
                        info.setMaterialType((Integer) material.get("materialType"));
                        if (material.get("categoryId") != null) {
                            info.setCategoryId(Long.valueOf(material.get("categoryId").toString()));
                        }
                        if (material.get("isControlled") != null) {
                            info.setIsControlled((Integer) material.get("isControlled"));
                        }
                        result.add(info);
                    }
                    
                    log.info("获取危化品列表成功，数量: {}", result.size());
                    return result;
                }
            }
            
            log.warn("获取危化品列表失败，返回空列表");
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("调用药品服务获取危化品列表失败", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public MaterialCategoryInfo getCategoryInfo(Long categoryId) {
        log.info("获取分类信息: categoryId={}", categoryId);
        
        try {
            String url = materialServiceUrl + "/api/v1/categories/" + categoryId;
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null) {
                    MaterialCategoryInfo info = new MaterialCategoryInfo();
                    info.setId(Long.valueOf(data.get("id").toString()));
                    info.setCategoryCode((String) data.get("categoryCode"));
                    info.setCategoryName((String) data.get("categoryName"));
                    if (data.get("parentId") != null) {
                        info.setParentId(Long.valueOf(data.get("parentId").toString()));
                    }
                    if (data.get("categoryLevel") != null) {
                        info.setCategoryLevel((Integer) data.get("categoryLevel"));
                    }
                    
                    log.info("获取分类信息成功: categoryId={}", categoryId);
                    return info;
                }
            }
            
            log.error("获取分类信息失败: categoryId={}, response={}", categoryId, response);
            return null;
        } catch (Exception e) {
            log.error("调用药品服务获取分类信息失败: categoryId={}", categoryId, e);
            return null;
        }
    }
}
