package com.lab.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.dto.ConsumptionStatisticsDTO;
import com.lab.inventory.dto.MaterialCategoryInfo;
import com.lab.inventory.dto.MaterialInfo;
import com.lab.inventory.dto.StockSummaryDTO;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.entity.StockOut;
import com.lab.inventory.entity.StockOutDetail;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.mapper.StockOutDetailMapper;
import com.lab.inventory.mapper.StockOutMapper;
import com.lab.inventory.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 报表服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    
    private final StockInventoryMapper stockInventoryMapper;
    private final StockOutMapper stockOutMapper;
    private final StockOutDetailMapper stockOutDetailMapper;
    private final MaterialClient materialClient;
    
    @Override
    public StockSummaryDTO getStockSummary(Long warehouseId, Integer materialType) {
        log.info("获取库存汇总报表: warehouseId={}, materialType={}", warehouseId, materialType);
        
        // 1. 查询库存数据
        LambdaQueryWrapper<StockInventory> wrapper = new LambdaQueryWrapper<>();
        if (warehouseId != null) {
            wrapper.eq(StockInventory::getWarehouseId, warehouseId);
        }
        wrapper.gt(StockInventory::getQuantity, BigDecimal.ZERO);
        
        List<StockInventory> inventories = stockInventoryMapper.selectList(wrapper);
        log.info("查询到库存记录数: {}", inventories.size());
        
        if (inventories.isEmpty()) {
            return createEmptyReport();
        }
        
        // 2. 获取物料信息并按分类分组
        Map<Long, List<StockInventory>> categoryStockMap = new HashMap<>();
        Map<Long, MaterialInfo> materialInfoMap = new HashMap<>();
        Map<Long, MaterialCategoryInfo> categoryInfoMap = new HashMap<>();
        
        for (StockInventory inventory : inventories) {
            Long materialId = inventory.getMaterialId();
            
            // 获取物料信息（带缓存）
            MaterialInfo materialInfo = materialInfoMap.computeIfAbsent(materialId, 
                id -> materialClient.getMaterialInfo(id));
            
            if (materialInfo == null) {
                log.warn("物料信息不存在: materialId={}", materialId);
                continue;
            }
            
            // 过滤物料类型
            if (materialType != null && !materialType.equals(materialInfo.getMaterialType())) {
                continue;
            }
            
            Long categoryId = materialInfo.getCategoryId();
            if (categoryId == null) {
                log.warn("物料未设置分类: materialId={}", materialId);
                continue;
            }
            
            // 按分类分组
            categoryStockMap.computeIfAbsent(categoryId, k -> new ArrayList<>()).add(inventory);
            
            // 获取分类信息（带缓存）
            if (!categoryInfoMap.containsKey(categoryId)) {
                MaterialCategoryInfo categoryInfo = materialClient.getCategoryInfo(categoryId);
                if (categoryInfo != null) {
                    categoryInfoMap.put(categoryId, categoryInfo);
                }
            }
        }
        
        // 3. 计算每个分类的汇总数据
        List<StockSummaryDTO.CategorySummary> categories = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        
        for (Map.Entry<Long, List<StockInventory>> entry : categoryStockMap.entrySet()) {
            Long categoryId = entry.getKey();
            List<StockInventory> stocks = entry.getValue();
            
            MaterialCategoryInfo categoryInfo = categoryInfoMap.get(categoryId);
            if (categoryInfo == null) {
                continue;
            }
            
            StockSummaryDTO.CategorySummary summary = new StockSummaryDTO.CategorySummary();
            summary.setCategoryId(categoryId);
            summary.setCategoryName(categoryInfo.getCategoryName());
            
            // 统计物品数量（去重materialId）
            Set<Long> uniqueMaterials = stocks.stream()
                .map(StockInventory::getMaterialId)
                .collect(Collectors.toSet());
            summary.setItemCount(uniqueMaterials.size());
            
            // 计算总库存数量和总价值
            BigDecimal categoryQuantity = BigDecimal.ZERO;
            BigDecimal categoryValue = BigDecimal.ZERO;
            
            for (StockInventory stock : stocks) {
                categoryQuantity = categoryQuantity.add(stock.getQuantity());
                
                if (stock.getUnitPrice() != null && stock.getQuantity() != null) {
                    BigDecimal value = stock.getUnitPrice().multiply(stock.getQuantity());
                    categoryValue = categoryValue.add(value);
                }
            }
            
            summary.setTotalQuantity(categoryQuantity);
            summary.setTotalValue(categoryValue);
            
            totalValue = totalValue.add(categoryValue);
            categories.add(summary);
        }
        
        // 4. 计算价值占比
        final BigDecimal finalTotalValue = totalValue;
        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            categories.forEach(summary -> {
                BigDecimal percentage = summary.getTotalValue()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(finalTotalValue, 2, RoundingMode.HALF_UP);
                summary.setValuePercentage(percentage);
            });
        } else {
            categories.forEach(summary -> summary.setValuePercentage(BigDecimal.ZERO));
        }
        
        // 5. 按价值降序排序
        categories.sort((a, b) -> b.getTotalValue().compareTo(a.getTotalValue()));
        
        // 6. 构建返回结果
        StockSummaryDTO result = new StockSummaryDTO();
        result.setTotalValue(totalValue);
        result.setCategories(categories);
        
        log.info("库存汇总报表生成成功: 总价值={}, 分类数={}", totalValue, categories.size());
        return result;
    }
    
    /**
     * 创建空报表
     */
    private StockSummaryDTO createEmptyReport() {
        StockSummaryDTO result = new StockSummaryDTO();
        result.setTotalValue(BigDecimal.ZERO);
        result.setCategories(new ArrayList<>());
        return result;
    }
    
    @Override
    public ConsumptionStatisticsDTO getConsumptionStatistics(LocalDate startDate, LocalDate endDate, Integer materialType) {
        log.info("获取消耗统计报表: startDate={}, endDate={}, materialType={}", startDate, endDate, materialType);
        
        // 1. 查询时间范围内已完成的出库单
        LambdaQueryWrapper<StockOut> outWrapper = new LambdaQueryWrapper<>();
        outWrapper.eq(StockOut::getStatus, 2); // 2-已出库
        if (startDate != null) {
            outWrapper.ge(StockOut::getOutDate, startDate);
        }
        if (endDate != null) {
            outWrapper.le(StockOut::getOutDate, endDate);
        }
        
        List<StockOut> stockOuts = stockOutMapper.selectList(outWrapper);
        log.info("查询到出库单数量: {}", stockOuts.size());
        
        if (stockOuts.isEmpty()) {
            return createEmptyConsumptionReport();
        }
        
        // 2. 获取所有出库单ID
        List<Long> outOrderIds = stockOuts.stream()
            .map(StockOut::getId)
            .collect(Collectors.toList());
        
        // 3. 查询出库明细
        LambdaQueryWrapper<StockOutDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(StockOutDetail::getOutOrderId, outOrderIds);
        
        List<StockOutDetail> details = stockOutDetailMapper.selectList(detailWrapper);
        log.info("查询到出库明细数量: {}", details.size());
        
        if (details.isEmpty()) {
            return createEmptyConsumptionReport();
        }
        
        // 4. 按物料ID分组统计消耗量和成本
        Map<Long, ConsumptionStatisticsDTO.MaterialConsumption> consumptionMap = new HashMap<>();
        Map<Long, MaterialInfo> materialInfoCache = new HashMap<>();
        
        for (StockOutDetail detail : details) {
            Long materialId = detail.getMaterialId();
            
            // 获取物料信息（带缓存）
            MaterialInfo materialInfo = materialInfoCache.computeIfAbsent(materialId,
                id -> materialClient.getMaterialInfo(id));
            
            if (materialInfo == null) {
                log.warn("物料信息不存在: materialId={}", materialId);
                continue;
            }
            
            // 过滤物料类型
            if (materialType != null && !materialType.equals(materialInfo.getMaterialType())) {
                continue;
            }
            
            // 累加消耗量和成本
            ConsumptionStatisticsDTO.MaterialConsumption consumption = consumptionMap.computeIfAbsent(materialId, id -> {
                ConsumptionStatisticsDTO.MaterialConsumption c = new ConsumptionStatisticsDTO.MaterialConsumption();
                c.setMaterialId(materialId);
                c.setMaterialName(materialInfo.getMaterialName());
                c.setMaterialCode(materialInfo.getMaterialCode());
                c.setSpecification(materialInfo.getSpecification());
                c.setUnit(materialInfo.getUnit());
                c.setConsumptionQuantity(BigDecimal.ZERO);
                c.setConsumptionCost(BigDecimal.ZERO);
                return c;
            });
            
            // 累加数量
            if (detail.getQuantity() != null) {
                consumption.setConsumptionQuantity(
                    consumption.getConsumptionQuantity().add(detail.getQuantity())
                );
            }
            
            // 累加成本
            if (detail.getTotalAmount() != null) {
                consumption.setConsumptionCost(
                    consumption.getConsumptionCost().add(detail.getTotalAmount())
                );
            }
        }
        
        // 5. 计算总消耗量和总成本
        BigDecimal totalConsumption = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        
        for (ConsumptionStatisticsDTO.MaterialConsumption consumption : consumptionMap.values()) {
            totalConsumption = totalConsumption.add(consumption.getConsumptionQuantity());
            totalCost = totalCost.add(consumption.getConsumptionCost());
        }
        
        // 6. 计算成本占比
        final BigDecimal finalTotalCost = totalCost;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            consumptionMap.values().forEach(consumption -> {
                BigDecimal costRate = consumption.getConsumptionCost()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(finalTotalCost, 2, RoundingMode.HALF_UP);
                consumption.setCostRate(costRate);
            });
        } else {
            consumptionMap.values().forEach(consumption -> consumption.setCostRate(BigDecimal.ZERO));
        }
        
        // 7. 按成本降序排序
        List<ConsumptionStatisticsDTO.MaterialConsumption> materials = new ArrayList<>(consumptionMap.values());
        materials.sort((a, b) -> b.getConsumptionCost().compareTo(a.getConsumptionCost()));
        
        // 8. 构建返回结果
        ConsumptionStatisticsDTO result = new ConsumptionStatisticsDTO();
        result.setTotalConsumption(totalConsumption);
        result.setTotalCost(totalCost);
        result.setMaterials(materials);
        
        log.info("消耗统计报表生成成功: 总消耗量={}, 总成本={}, 物料数={}", 
            totalConsumption, totalCost, materials.size());
        return result;
    }
    
    /**
     * 创建空消耗统计报表
     */
    private ConsumptionStatisticsDTO createEmptyConsumptionReport() {
        ConsumptionStatisticsDTO result = new ConsumptionStatisticsDTO();
        result.setTotalConsumption(BigDecimal.ZERO);
        result.setTotalCost(BigDecimal.ZERO);
        result.setMaterials(new ArrayList<>());
        return result;
    }


    @Override
    public byte[] exportStockSummaryToExcel(Long warehouseId, Integer materialType) {
        log.info("导出库存汇总报表为Excel: warehouseId={}, materialType={}", warehouseId, materialType);

        // 1. 查询报表数据
        StockSummaryDTO summary = getStockSummary(warehouseId, materialType);

        // 2. 创建Excel工作簿
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("库存汇总报表");

            // 3. 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"分类名称", "物品数量", "总库存数量", "总价值(元)", "价值占比(%)"};

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 4. 填充数据行
            CellStyle dataStyle = createDataStyle(workbook);
            int rowNum = 1;
            for (StockSummaryDTO.CategorySummary category : summary.getCategories()) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(category.getCategoryName());
                row.createCell(1).setCellValue(category.getItemCount());
                row.createCell(2).setCellValue(category.getTotalQuantity().doubleValue());
                row.createCell(3).setCellValue(category.getTotalValue().doubleValue());
                row.createCell(4).setCellValue(category.getValuePercentage().doubleValue());

                // 应用数据样式
                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // 5. 添加汇总行
            Row summaryRow = sheet.createRow(rowNum);
            CellStyle summaryStyle = createSummaryStyle(workbook);

            Cell summaryLabelCell = summaryRow.createCell(0);
            summaryLabelCell.setCellValue("总计");
            summaryLabelCell.setCellStyle(summaryStyle);

            summaryRow.createCell(1).setCellValue("");
            summaryRow.createCell(2).setCellValue("");

            Cell totalValueCell = summaryRow.createCell(3);
            totalValueCell.setCellValue(summary.getTotalValue().doubleValue());
            totalValueCell.setCellStyle(summaryStyle);

            Cell totalPercentageCell = summaryRow.createCell(4);
            totalPercentageCell.setCellValue(100.0);
            totalPercentageCell.setCellStyle(summaryStyle);

            // 6. 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }

            // 7. 写入输出流
            workbook.write(outputStream);
            byte[] excelBytes = outputStream.toByteArray();

            log.info("成功导出库存汇总报表，分类数: {}", summary.getCategories().size());
            return excelBytes;

        } catch (Exception e) {
            log.error("导出库存汇总报表失败", e);
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    @Override
    public byte[] exportConsumptionStatisticsToExcel(LocalDate startDate, LocalDate endDate, Integer materialType) {
        log.info("导出消耗统计报表为Excel: startDate={}, endDate={}, materialType={}", startDate, endDate, materialType);

        // 1. 查询报表数据
        ConsumptionStatisticsDTO statistics = getConsumptionStatistics(startDate, endDate, materialType);

        // 2. 创建Excel工作簿
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("消耗统计报表");

            // 3. 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"药品编码", "药品名称", "规格", "单位", "消耗数量", "消耗成本(元)", "成本占比(%)"};

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 4. 填充数据行
            CellStyle dataStyle = createDataStyle(workbook);
            int rowNum = 1;
            for (ConsumptionStatisticsDTO.MaterialConsumption material : statistics.getMaterials()) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(material.getMaterialCode());
                row.createCell(1).setCellValue(material.getMaterialName());
                row.createCell(2).setCellValue(material.getSpecification() != null ? material.getSpecification() : "");
                row.createCell(3).setCellValue(material.getUnit());
                row.createCell(4).setCellValue(material.getConsumptionQuantity().doubleValue());
                row.createCell(5).setCellValue(material.getConsumptionCost().doubleValue());
                row.createCell(6).setCellValue(material.getCostRate().doubleValue());

                // 应用数据样式
                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // 5. 添加汇总行
            Row summaryRow = sheet.createRow(rowNum);
            CellStyle summaryStyle = createSummaryStyle(workbook);

            Cell summaryLabelCell = summaryRow.createCell(0);
            summaryLabelCell.setCellValue("总计");
            summaryLabelCell.setCellStyle(summaryStyle);

            summaryRow.createCell(1).setCellValue("");
            summaryRow.createCell(2).setCellValue("");
            summaryRow.createCell(3).setCellValue("");

            Cell totalQuantityCell = summaryRow.createCell(4);
            totalQuantityCell.setCellValue(statistics.getTotalConsumption().doubleValue());
            totalQuantityCell.setCellStyle(summaryStyle);

            Cell totalCostCell = summaryRow.createCell(5);
            totalCostCell.setCellValue(statistics.getTotalCost().doubleValue());
            totalCostCell.setCellStyle(summaryStyle);

            Cell totalPercentageCell = summaryRow.createCell(6);
            totalPercentageCell.setCellValue(100.0);
            totalPercentageCell.setCellStyle(summaryStyle);

            // 6. 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }

            // 7. 写入输出流
            workbook.write(outputStream);
            byte[] excelBytes = outputStream.toByteArray();

            log.info("成功导出消耗统计报表，物料数: {}", statistics.getMaterials().size());
            return excelBytes;

        } catch (Exception e) {
            log.error("导出消耗统计报表失败", e);
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        return style;
    }

    /**
     * 创建数据样式
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    /**
     * 创建汇总行样式
     */
    private CellStyle createSummaryStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        return style;
    }

}
