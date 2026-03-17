package com.lab.approval.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 用户信息DTO
 */
@Data
public class UserInfo {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 用户类型: 1-管理员, 2-教师, 3-学生
     */
    private Integer userType;
    
    /**
     * 所属部门
     */
    private String department;
    
    /**
     * 安全资质状态: 0-未认证, 1-已认证
     */
    private Integer safetyCertStatus;
    
    /**
     * 安全资质到期日期
     */
    private LocalDate safetyCertExpireDate;
}
