package com.lab.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.inventory.dto.HazardousReturnStockInRequest;
import com.lab.inventory.dto.StockInDTO;
import com.lab.inventory.entity.StockIn;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 入库服务接口
 */
public interface StockInService {
    
    /**
     * 分页查询入库单列表
     */
    Page<StockIn> listStockIn(
            int page,
            int size,
            String keyword,
            Long warehouseId,
            Integer status,
            LocalDateTime createdTimeStart,
            LocalDateTime createdTimeEnd
    );
    
    /**
     * 根据ID查询入库单详情
     */
    StockIn getStockInById(Long id);
    
    /**
     * 创建入库单
     */
    StockIn createStockIn(StockInDTO dto);
    
    /**
     * 确认入库
     */
    void confirmStockIn(Long id);
    
    /**
     * 取消入库单
     */
    void cancelStockIn(Long id);

    /**
     * 危化品归还自动入库
     *
     * @return 自动生成的入库单ID
     */
    Long hazardousReturnStockIn(HazardousReturnStockInRequest request);

    /**
     * 下载入库导入模板
     *
     * @return Excel模板字节数组
     */
    byte[] generateStockInImportTemplate();

    /**
     * 从Excel解析入库单预览数据（不落库）
     *
     * @param file Excel文件
     * @param operatorId 经手人ID（可为空）
     * @return 入库单预览DTO
     */
    StockInDTO importStockInFromExcel(MultipartFile file, Long operatorId);
}
