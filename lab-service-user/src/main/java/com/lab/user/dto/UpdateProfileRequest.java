package com.lab.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 个人账号更新请求
 * 仅允许修改用户名与密码；真实姓名等基础信息由管理员维护
 */
@Data
public class UpdateProfileRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 当前密码（修改用户名或密码时用于校验）
     */
    private String currentPassword;

    /**
     * 新密码（可选）
     */
    private String newPassword;
}

