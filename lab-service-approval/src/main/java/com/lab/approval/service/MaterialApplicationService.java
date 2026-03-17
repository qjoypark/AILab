package com.lab.approval.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.approval.dto.CreateApplicationRequest;
import com.lab.approval.dto.MaterialApplicationDTO;
import com.lab.approval.entity.MaterialApplication;

import java.time.LocalDate;

/**
 * 领用申请服务接口
 */
public interface MaterialApplicationService {
    
    /**
     * 创建领用申请
     * 
     * @param request 申请请求
     * @param applicantId 申请人ID
     * @param applicantName 申请人姓名
     * @param applicantDept 申请部门
     * @return 申请单ID
     */
    Long createApplication(CreateApplicationRequest request, Long applicantId, String applicantName, String applicantDept);
    
    /**
     * 分页查询申请列表
     * 
     * @param page 页码
     * @param size 每页数量
     * @param status 状态
     * @param applicationType 申请类型
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 申请列表
     */
    Page<MaterialApplication> listApplications(int page, int size, Integer status, Integer applicationType, 
                                                LocalDate startDate, LocalDate endDate);
    
    /**
     * 查询申请详情
     * 
     * @param id 申请单ID
     * @return 申请详情
     */
    MaterialApplicationDTO getApplicationDetail(Long id);
    
    /**
     * 取消申请
     * 
     * @param id 申请单ID
     * @param userId 用户ID
     */
    void cancelApplication(Long id, Long userId);
    
    /**
     * 更新申请状态
     * 
     * @param id 申请单ID
     * @param status 状态
     */
    void updateApplicationStatus(Long id, Integer status);
    
    /**
     * 更新审批状态
     * 
     * @param id 申请单ID
     * @param approvalStatus 审批状态
     */
    void updateApprovalStatus(Long id, Integer approvalStatus);
    
    /**
     * 更新当前审批人
     * 
     * @param id 申请单ID
     * @param approverId 审批人ID
     */
    void updateCurrentApprover(Long id, Long approverId);
    
    /**
     * 处理审批
     * 
     * @param id 申请单ID
     * @param request 审批请求
     * @param approverId 审批人ID
     * @param approverName 审批人姓名
     */
    void processApproval(Long id, com.lab.approval.dto.ApprovalProcessRequest request, 
                        Long approverId, String approverName);
}
