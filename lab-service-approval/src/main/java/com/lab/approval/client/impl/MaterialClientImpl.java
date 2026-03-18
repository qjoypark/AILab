package com.lab.approval.client.impl;

import com.lab.approval.client.MaterialClient;
import com.lab.approval.dto.MaterialInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 药品服务客户端实现
 */
@Slf4j
@Component
public class MaterialClientImpl implements MaterialClient {

    private final RestTemplate restTemplate;

    @Value("${material.service.url:http://localhost:8082}")
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
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }

            Object dataObject = response.getBody().get("data");
            if (!(dataObject instanceof Map<?, ?> dataMap)) {
                return null;
            }

            MaterialInfo info = new MaterialInfo();
            info.setId(parseLong(dataMap.get("id")));
            info.setMaterialId(info.getId());
            info.setMaterialName((String) dataMap.get("materialName"));
            info.setSpecification((String) dataMap.get("specification"));
            info.setUnit((String) dataMap.get("unit"));
            info.setMaterialType(parseInteger(dataMap.get("materialType")));
            info.setIsControlled(parseInteger(dataMap.get("isControlled")));
            return info;
        } catch (Exception ex) {
            log.error("调用药品服务获取药品信息失败: materialId={}", materialId, ex);
            return null;
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
