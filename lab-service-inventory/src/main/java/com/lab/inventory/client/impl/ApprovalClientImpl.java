package com.lab.inventory.client.impl;

import com.lab.inventory.client.ApprovalClient;
import com.lab.inventory.dto.HazardousUsageRecordDTO;
import com.lab.inventory.dto.MaterialApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批服务客户端实现
 * TODO: 后续使用Feign实现服务间调用
 */
@Slf4j
@Component
public class ApprovalClientImpl implements ApprovalClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${approval.service.url:http://localhost:8083}")
    private String approvalServiceUrl;
    
    public ApprovalClientImpl() {
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    public MaterialApplicationDTO getApplicationDetail(Long applicationId) {
        log.info("获取申请单详情: applicationId={}", applicationId);
        
        try {
            String url = approvalServiceUrl + "/api/v1/applications/" + applicationId;
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null) {
                    // 简化处理，直接转换为DTO
                    MaterialApplicationDTO dto = new MaterialApplicationDTO();
                    dto.setId(Long.valueOf(data.get("id").toString()));
                    dto.setApplicationNo((String) data.get("applicationNo"));
                    dto.setApplicantId(Long.valueOf(data.get("applicantId").toString()));
                    dto.setApplicantName((String) data.get("applicantName"));
                    dto.setApplicantDept((String) data.get("applicantDept"));
                    dto.setApplicationType((Integer) data.get("applicationType"));
                    dto.setUsagePurpose((String) data.get("usagePurpose"));
                    dto.setUsageLocation((String) data.get("usageLocation"));
                    dto.setStatus((Integer) data.get("status"));
                    
                    log.info("获取申请单详情成功: applicationId={}", applicationId);
                    return dto;
                }
            }
            
            log.error("获取申请单详情失败: applicationId={}, response={}", applicationId, response);
            return null;
        } catch (Exception e) {
            log.error("调用审批服务获取申请单详情失败: applicationId={}", applicationId, e);
            return null;
        }
    }
    
    @Override
    public void updateApplicationStatusToStockOut(Long applicationId) {
        log.info("更新申请单状态为已出库: applicationId={}", applicationId);
        
        try {
            String url = approvalServiceUrl + "/api/v1/applications/" + applicationId + "/status";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("status", 5); // 5-已出库
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            restTemplate.exchange(url, HttpMethod.PUT, request, Map.class);
            
            log.info("更新申请单状态为已出库成功: applicationId={}", applicationId);
        } catch (Exception e) {
            log.error("调用审批服务更新申请单状态失败: applicationId={}", applicationId, e);
        }
    }
    
    @Override
    public void createHazardousUsageRecord(HazardousUsageRecordDTO recordDTO) {
        log.info("创建危化品使用记录: applicationId={}, materialId={}, userId={}", 
            recordDTO.getApplicationId(), recordDTO.getMaterialId(), recordDTO.getUserId());
        
        try {
            String url = approvalServiceUrl + "/api/v1/hazardous/usage-records";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<HazardousUsageRecordDTO> request = new HttpEntity<>(recordDTO, headers);
            
            restTemplate.postForEntity(url, request, Map.class);
            
            log.info("创建危化品使用记录成功: applicationId={}, materialId={}", 
                recordDTO.getApplicationId(), recordDTO.getMaterialId());
        } catch (Exception e) {
            log.error("调用审批服务创建危化品使用记录失败: applicationId={}, materialId={}", 
                recordDTO.getApplicationId(), recordDTO.getMaterialId(), e);
            // 不抛出异常，避免影响出库流程
        }
    }
    
    @Override
    public BigDecimal getUnreturnedQuantity(Long materialId) {
        log.info("获取危化品已领用未归还数量: materialId={}", materialId);
        
        try {
            String url = approvalServiceUrl + "/api/v1/hazardous/usage-records/unreturned/" + materialId;
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Object data = body.get("data");
                
                if (data != null) {
                    BigDecimal quantity = new BigDecimal(data.toString());
                    log.info("获取危化品已领用未归还数量成功: materialId={}, quantity={}", materialId, quantity);
                    return quantity;
                }
            }
            
            log.warn("获取危化品已领用未归还数量失败，返回0: materialId={}", materialId);
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("调用审批服务获取已领用未归还数量失败: materialId={}", materialId, e);
            return BigDecimal.ZERO;
        }
    }
    
    @Override
    public List<MaterialApplicationDTO> getPendingApprovals(Long approverId) {
        log.info("查询用户的待审批申请列表: approverId={}", approverId);
        
        try {
            String url = approvalServiceUrl + "/api/v1/applications/pending?approverId=" + approverId;
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                null, 
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Object data = body.get("data");
                
                if (data instanceof List) {
                    List<Map<String, Object>> dataList = (List<Map<String, Object>>) data;
                    List<MaterialApplicationDTO> result = new ArrayList<>();
                    
                    for (Map<String, Object> item : dataList) {
                        MaterialApplicationDTO dto = new MaterialApplicationDTO();
                        dto.setId(Long.valueOf(item.get("id").toString()));
                        dto.setApplicationNo((String) item.get("applicationNo"));
                        dto.setApplicantId(Long.valueOf(item.get("applicantId").toString()));
                        dto.setApplicantName((String) item.get("applicantName"));
                        dto.setApplicantDept((String) item.get("applicantDept"));
                        dto.setApplicationType((Integer) item.get("applicationType"));
                        dto.setUsagePurpose((String) item.get("usagePurpose"));
                        dto.setUsageLocation((String) item.get("usageLocation"));
                        dto.setStatus((Integer) item.get("status"));
                        dto.setApprovalStatus((Integer) item.get("approvalStatus"));
                        
                        if (item.get("expectedDate") != null) {
                            // Handle date conversion if needed
                        }
                        
                        result.add(dto);
                    }
                    
                    log.info("查询用户的待审批申请列表成功: approverId={}, count={}", approverId, result.size());
                    return result;
                }
            }
            
            log.warn("查询用户的待审批申请列表失败，返回空列表: approverId={}", approverId);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("调用审批服务查询待审批申请列表失败: approverId={}", approverId, e);
            return new ArrayList<>();
        }
    }
}

