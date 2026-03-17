package com.lab.material.dto;

import lombok.Data;

/**
 * 供应商DTO
 */
@Data
public class SupplierDTO {
    
    private Long id;
    
    private String supplierCode;
    
    private String supplierName;
    
    private String contactPerson;
    
    private String contactPhone;
    
    private String contactEmail;
    
    private String address;
    
    private String qualificationFile;
    
    private Integer status;
}
