package com.lab.approval.client.impl;

import com.lab.approval.client.InventoryClient;
import com.lab.approval.dto.StockOutOrderInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 库存服务客户端实现
 * TODO: 后续使用Feign实现服务间调用
 */
@Slf4j
@Component
public class InventoryClientImpl implements InventoryClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${inventory.service.url:http://localhost:8083}")
    private String inventoryServiceUrl;
    
    public InventoryClientImpl() {
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    public boolean checkStockAvailability(Long materialId, BigDecimal quantity) {
        log.info("检查库存: materialId={}, quantity={}", materialId, quantity);
        
        BigDecimal availableStock = getAvailableStock(materialId);
        return availableStock.compareTo(quantity) >= 0;
    }
    
    @Override
    public BigDecimal getAvailableStock(Long materialId) {
        log.info("获取可用库存: materialId={}", materialId);

        try {
            String url = inventoryServiceUrl + "/api/v1/inventory/stock/" + materialId + "/detail";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return BigDecimal.ZERO;
            }

            Object dataObject = response.getBody().get("data");
            if (!(dataObject instanceof List<?> stockList) || CollectionUtils.isEmpty(stockList)) {
                return BigDecimal.ZERO;
            }

            BigDecimal availableStock = BigDecimal.ZERO;
            for (Object stockObject : stockList) {
                if (!(stockObject instanceof Map<?, ?> stockMap)) {
                    continue;
                }
                Object availableObj = stockMap.get("availableQuantity");
                if (availableObj != null) {
                    availableStock = availableStock.add(new BigDecimal(availableObj.toString()));
                }
            }
            return availableStock;
        } catch (Exception ex) {
            log.error("调用库存服务获取可用库存失败: materialId={}", materialId, ex);
            return BigDecimal.ZERO;
        }
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
    public List<StockOutOrderInfoDTO> getStockOutOrdersByApplicationId(Long applicationId) {
        if (applicationId == null) {
            return new ArrayList<>();
        }
        try {
            String url = inventoryServiceUrl + "/api/v1/inventory/stock-out/application/" + applicationId + "/orders";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return new ArrayList<>();
            }
            Object data = response.getBody().get("data");
            if (!(data instanceof List<?> list) || CollectionUtils.isEmpty(list)) {
                return new ArrayList<>();
            }
            List<StockOutOrderInfoDTO> result = new ArrayList<>(list.size());
            for (Object element : list) {
                if (!(element instanceof Map<?, ?> item)) {
                    continue;
                }
                StockOutOrderInfoDTO dto = new StockOutOrderInfoDTO();
                Object id = item.get("id");
                if (id != null) {
                    dto.setId(Long.valueOf(id.toString()));
                }
                Object outOrderNo = item.get("outOrderNo");
                if (outOrderNo != null) {
                    dto.setOutOrderNo(outOrderNo.toString());
                }
                Object warehouseId = item.get("warehouseId");
                if (warehouseId != null) {
                    dto.setWarehouseId(Long.valueOf(warehouseId.toString()));
                }
                Object warehouseName = item.get("warehouseName");
                if (warehouseName != null) {
                    dto.setWarehouseName(warehouseName.toString());
                }
                Object status = item.get("status");
                if (status != null) {
                    dto.setStatus(Integer.valueOf(status.toString()));
                }
                Object statusName = item.get("statusName");
                if (statusName != null) {
                    dto.setStatusName(statusName.toString());
                }
                Object createdTime = item.get("createdTime");
                if (createdTime != null) {
                    dto.setCreatedTime(parseDateTime(createdTime.toString()));
                }
                result.add(dto);
            }
            return result;
        } catch (Exception e) {
            log.error("查询申请关联出库单失败: applicationId={}", applicationId, e);
            return new ArrayList<>();
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

    private java.time.LocalDateTime parseDateTime(String value) {
        try {
            return java.time.LocalDateTime.parse(value);
        } catch (Exception ignore) {
            try {
                return java.time.OffsetDateTime.parse(value).toLocalDateTime();
            } catch (Exception e) {
                return null;
            }
        }
    }
}
