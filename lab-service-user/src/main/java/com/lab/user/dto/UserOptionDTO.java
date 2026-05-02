package com.lab.user.dto;

import com.lab.user.entity.SysUser;
import lombok.Data;

@Data
public class UserOptionDTO {

    private Long id;

    private String username;

    private String realName;

    private Integer userType;

    private String department;

    private Integer status;

    public static UserOptionDTO fromEntity(SysUser user) {
        UserOptionDTO dto = new UserOptionDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setUserType(user.getUserType());
        dto.setDepartment(user.getDepartment());
        dto.setStatus(user.getStatus());
        return dto;
    }
}
