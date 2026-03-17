package com.lab.inventory.controller;

import com.lab.common.result.Result;
import com.lab.inventory.dto.ConsumptionStatisticsDTO;
import com.lab.inventory.dto.StockSummaryDTO;
import com.lab.inventory.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

/**
 * 报表统计控制器
 */
@Slf4j
@Tag(name = "报表统计")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    
    private final ReportService reportService;
    
    @Operation(summary = "库存汇总报表", description = "按分类统计库存数量和金额，计算库存总价值")
    @GetMapping("/stock-summary")
    public Result<StockSummaryDTO> getStockSummary(
            @Parameter(description = "仓库ID（可选）") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "物料类型（可选）: 1-耗材, 2-试剂, 3-危化品") @RequestParam(required = false) Integer materialType) {
        
        log.info("查询库存汇总报表: warehouseId={}, materialType={}", warehouseId, materialType);
        StockSummaryDTO summary = reportService.getStockSummary(warehouseId, materialType);
        return Result.success(summary);
    }
    
    @Operation(summary = "消耗统计报表", description = "按时间范围统计药品消耗量和成本，计算各药品成本占比")
    @GetMapping("/consumption-statistics")
    public Result<ConsumptionStatisticsDTO> getConsumptionStatistics(
            @Parameter(description = "开始日期", required = true) 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期", required = true) 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "物料类型（可选）: 1-耗材, 2-试剂, 3-危化品") 
            @RequestParam(required = false) Integer materialType) {
        
        log.info("查询消耗统计报表: startDate={}, endDate={}, materialType={}", startDate, endDate, materialType);
        ConsumptionStatisticsDTO statistics = reportService.getConsumptionStatistics(startDate, endDate, materialType);
        return Result.success(statistics);
    }


    @Operation(summary = "导出库存汇总报表为Excel", description = "导出库存汇总报表为Excel文件")
    @GetMapping("/stock-summary/export")
    public void exportStockSummary(
            @Parameter(description = "仓库ID（可选）") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "物料类型（可选）: 1-耗材, 2-试剂, 3-危化品") @RequestParam(required = false) Integer materialType,
            HttpServletResponse response) throws IOException {

        log.info("导出库存汇总报表: warehouseId={}, materialType={}", warehouseId, materialType);

        byte[] excelBytes = reportService.exportStockSummaryToExcel(warehouseId, materialType);

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = "库存汇总报表_" + LocalDate.now() + ".xlsx";
        response.setHeader("Content-Disposition", "attachment; filename=" +
                java.net.URLEncoder.encode(fileName, "UTF-8"));

        // 写入响应流
        response.getOutputStream().write(excelBytes);
        response.getOutputStream().flush();
    }

    @Operation(summary = "导出消耗统计报表为Excel", description = "导出消耗统计报表为Excel文件")
    @GetMapping("/consumption-statistics/export")
    public void exportConsumptionStatistics(
            @Parameter(description = "开始日期", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "物料类型（可选）: 1-耗材, 2-试剂, 3-危化品")
            @RequestParam(required = false) Integer materialType,
            HttpServletResponse response) throws IOException {

        log.info("导出消耗统计报表: startDate={}, endDate={}, materialType={}", startDate, endDate, materialType);

        byte[] excelBytes = reportService.exportConsumptionStatisticsToExcel(startDate, endDate, materialType);

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = "消耗统计报表_" + startDate + "_" + endDate + ".xlsx";
        response.setHeader("Content-Disposition", "attachment; filename=" +
                java.net.URLEncoder.encode(fileName, "UTF-8"));

        // 写入响应流
        response.getOutputStream().write(excelBytes);
        response.getOutputStream().flush();
    }

}
