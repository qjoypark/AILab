package com.lab.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    private Long id;
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    private String password;
    
    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    
    private String phone;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotNull(message = "用户类型不能为空")
    private Integer userType;
    
    private String department;
    
    private Integer status;
    
    private Integer safetyCertStatus;
    
    private LocalDate safetyCertExpireDate;
    
    private List<Long> roleIds;
}
