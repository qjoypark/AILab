package com.lab.material.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 供应商实体
 */
@Data
@TableName("supplier")
public class Supplier {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String supplierCode;
    
    private String supplierName;
    
    private String contactPerson;
    
    private String contactPhone;
    
    private String contactEmail;
    
    private String address;
    
    private String qualificationFile;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
    
    @TableLogic
    private Integer deleted;
}
