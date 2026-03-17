package com.lab.material.service.impl;

import com.lab.common.exception.BusinessException;
import com.lab.material.config.MinioConfig;
import com.lab.material.service.FileUploadService;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {
    
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "xls", "xlsx"
    );
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    @Override
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        
        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过10MB");
        }
        
        // 检查文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException("文件名不能为空");
        }
        
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException("不支持的文件类型");
        }
        
        try {
            // 确保bucket存在
            ensureBucketExists();
            
            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + "." + extension;
            
            // 上传文件
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            
            // 返回文件URL
            return minioConfig.getEndpoint() + "/" + minioConfig.getBucketName() + "/" + fileName;
            
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }
    
    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        
        try {
            // 从URL中提取文件名
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("文件删除失败", e);
            throw new BusinessException("文件删除失败: " + e.getMessage());
        }
    }
    
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(minioConfig.getBucketName())
                .build()
        );
        
        if (!exists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .build()
            );
            
            // 设置bucket为公开读
            String policy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": {"AWS": "*"},
                            "Action": ["s3:GetObject"],
                            "Resource": ["arn:aws:s3:::%s/*"]
                        }
                    ]
                }
                """.formatted(minioConfig.getBucketName());
            
            minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .config(policy)
                    .build()
            );
        }
    }
    
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
