package com.lab.material.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 */
public interface FileUploadService {
    
    /**
     * 上传文件
     * @param file 文件
     * @return 文件URL
     */
    String uploadFile(MultipartFile file);
    
    /**
     * 删除文件
     * @param fileUrl 文件URL
     */
    void deleteFile(String fileUrl);
}
