package com.lab.approval.client.impl;

import com.lab.approval.client.InventoryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 库存服务客户端实现
 * TODO: 后续使用Feign实现服务间调用
 */
@Slf4j
@Component
public class InventoryClientImpl implements InventoryClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${inventory.service.url:http://localhost:8082}")
    private String inventoryServiceUrl;
    
    public InventoryClientImpl() {
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    public boolean checkStockAvailability(Long materialId, BigDecimal quantity) {
        // TODO: 调用库存服务API检查库存
        // 临时实现：假设库存充足
        log.info("检查库存: materialId={}, quantity={}", materialId, quantity);
        
        BigDecimal availableStock = getAvailableStock(materialId);
        return availableStock.compareTo(quantity) >= 0;
    }
    
    @Override
    public BigDecimal getAvailableStock(Long materialId) {
        // TODO: 调用库存服务API获取可用库存
        // 临时实现：返回模拟数据
        log.info("获取可用库存: materialId={}", materialId);
        return BigDecimal.valueOf(100);
    }
    
    @Override
    public Long createStockOutOrder(Long applicationId) {
        log.info("创建出库单: applicationId={}", applicationId);
        
        try {
            // 构建请求
            String url = inventoryServiceUrl + "/api/v1/inventory/stock-out/from-application";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("applicationId", applicationId);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 调用库存服务创建出库单
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null && data.get("id") != null) {
                    Long stockOutId = Long.valueOf(data.get("id").toString());
                    log.info("出库单创建成功: applicationId={}, stockOutId={}", applicationId, stockOutId);
                    return stockOutId;
                }
            }
            
            log.error("创建出库单失败: applicationId={}, response={}", applicationId, response);
            return null;
        } catch (Exception e) {
            log.error("调用库存服务创建出库单失败: applicationId={}", applicationId, e);
            return null;
        }
    }
    
    @Override
    public boolean returnHazardousMaterial(Long materialId, BigDecimal returnQuantity, String remark) {
        log.info("危化品归还入库: materialId={}, returnQuantity={}", materialId, returnQuantity);
        
        try {
            // 构建请求
            String url = inventoryServiceUrl + "/api/v1/inventory/stock-in/hazardous-return";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("materialId", materialId);
            requestBody.put("returnQuantity", returnQuantity);
            requestBody.put("remark", remark);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 调用库存服务进行归还入库
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("危化品归还入库成功: materialId={}, returnQuantity={}", materialId, returnQuantity);
                return true;
            }
            
            log.error("危化品归还入库失败: materialId={}, response={}", materialId, response);
            return false;
        } catch (Exception e) {
            log.error("调用库存服务归还入库失败: materialId={}", materialId, e);
            return false;
        }
    }
}
