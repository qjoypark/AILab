package com.lab.inventory.service;

import com.lab.inventory.dto.ConsumptionStatisticsDTO;
import com.lab.inventory.dto.StockSummaryDTO;

import java.time.LocalDate;

/**
 * 报表服务接口
 */
public interface ReportService {
    
    /**
     * 获取库存汇总报表
     * 
     * @param warehouseId 仓库ID（可选）
     * @param materialType 物料类型（可选）: 1-耗材, 2-试剂, 3-危化品
     * @return 库存汇总报表
     */
    StockSummaryDTO getStockSummary(Long warehouseId, Integer materialType);
    
    /**
     * 获取消耗统计报表
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param materialType 物料类型（可选）: 1-耗材, 2-试剂, 3-危化品
     * @return 消耗统计报表
     */
    ConsumptionStatisticsDTO getConsumptionStatistics(LocalDate startDate, LocalDate endDate, Integer materialType);
    
    /**
     * 导出库存汇总报表为Excel
     * 
     * @param warehouseId 仓库ID（可选）
     * @param materialType 物料类型（可选）: 1-耗材, 2-试剂, 3-危化品
     * @return Excel文件字节数组
     */
    byte[] exportStockSummaryToExcel(Long warehouseId, Integer materialType);
    
    /**
     * 导出消耗统计报表为Excel
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param materialType 物料类型（可选）: 1-耗材, 2-试剂, 3-危化品
     * @return Excel文件字节数组
     */
    byte[] exportConsumptionStatisticsToExcel(LocalDate startDate, LocalDate endDate, Integer materialType);
}
