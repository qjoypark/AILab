package com.lab.approval.service.impl;

import com.lab.approval.dto.ApprovalContext;
import com.lab.approval.service.ApproverAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 审批人自动分配服务实现
 * 
 * 根据申请人部门、药品类型等信息自动分配审批人
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApproverAssignmentServiceImpl implements ApproverAssignmentService {
    
    @Override
    public Long assignApprover(String approverRole, ApprovalContext context) {
        log.info("自动分配审批人: approverRole={}, context={}", approverRole, context);
        
        // 根据角色类型分配审批人
        switch (approverRole) {
            case "LAB_MANAGER":
                return assignLabManager(context);
            case "CENTER_ADMIN":
                return assignCenterAdmin(context);
            case "ADMIN":
            case "SAFETY_ADMIN":
                return assignSafetyAdmin(context);
            default:
                log.warn("未知的审批人角色: {}", approverRole);
                return null;
        }
    }
    
    /**
     * 分配实验室负责人
     * 根据申请人部门分配对应实验室的负责人
     */
    private Long assignLabManager(ApprovalContext context) {
        // TODO: 实现根据部门查询实验室负责人的逻辑
        // 这里需要调用用户服务或查询用户表
        // 简化实现：返回固定的实验室负责人ID
        log.info("分配实验室负责人: dept={}", context.getApplicantDept());
        
        // 根据部门分配不同的实验室负责人
        if (context.getApplicantDept() != null) {
            // 实际应该查询sys_user表，找到该部门的LAB_MANAGER角色用户
            // 这里简化处理，返回一个默认值
            return 2L; // 假设ID为2的用户是实验室负责人
        }
        
        return 2L;
    }
    
    /**
     * 分配中心管理员
     */
    private Long assignCenterAdmin(ApprovalContext context) {
        // TODO: 实现查询中心管理员的逻辑
        // 可以根据药品类型分配不同的中心管理员
        log.info("分配中心管理员: materialType={}", context.getMaterialType());
        
        // 实际应该查询sys_user表，找到CENTER_ADMIN角色的用户
        // 如果有多个中心管理员，可以根据负载均衡策略分配
        return 3L; // 假设ID为3的用户是中心管理员
    }
    
    /**
     * 分配安全管理员
     * 危化品审批的最后一级
     */
    private Long assignSafetyAdmin(ApprovalContext context) {
        // TODO: 实现查询安全管理员的逻辑
        log.info("分配安全管理员: hasControlledMaterial={}", context.getHasControlledMaterial());
        
        // 实际应该查询sys_user表，找到ADMIN或SAFETY_ADMIN角色的用户
        // 对于管控物品，可能需要特定的安全管理员审批
        return 1L; // 假设ID为1的用户是安全管理员
    }
}
