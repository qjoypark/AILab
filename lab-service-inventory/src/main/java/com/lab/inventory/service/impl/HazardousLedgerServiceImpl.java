package com.lab.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.inventory.client.ApprovalClient;
import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.dto.HazardousLedgerDTO;
import com.lab.inventory.dto.HazardousLedgerQueryDTO;
import com.lab.inventory.dto.MaterialInfo;
import com.lab.inventory.entity.StockInDetail;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.entity.StockOutDetail;
import com.lab.inventory.mapper.StockInDetailMapper;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.mapper.StockOutDetailMapper;
import com.lab.inventory.service.HazardousLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 危化品台账服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HazardousLedgerServiceImpl implements HazardousLedgerService {
    
    private final MaterialClient materialClient;
    private final ApprovalClient approvalClient;
    private final StockInventoryMapper stockInventoryMapper;
    private final StockInDetailMapper stockInDetailMapper;
    private final StockOutDetailMapper stockOutDetailMapper;
    
    @Override
    public List<HazardousLedgerDTO> queryLedger(HazardousLedgerQueryDTO queryDTO) {
        log.info("查询危化品台账报表，参数: {}", queryDTO);
        
        // 1. 获取危化品列表
        List<MaterialInfo> hazardousMaterials = materialClient.getHazardousMaterials();
        
        // 如果指定了materialId，则只查询该药品
        if (queryDTO.getMaterialId() != null) {
            hazardousMaterials = hazardousMaterials.stream()
                    .filter(m -> m.getId().equals(queryDTO.getMaterialId()))
                    .collect(Collectors.toList());
        }
        
        if (hazardousMaterials.isEmpty()) {
            log.info("没有找到符合条件的危化品");
            return new ArrayList<>();
        }
        
        // 2. 构建材料ID到材料信息的映射
        Map<Long, MaterialInfo> materialMap = hazardousMaterials.stream()
                .collect(Collectors.toMap(MaterialInfo::getId, m -> m));
        
        List<HazardousLedgerDTO> ledgerList = new ArrayList<>();
        
        // 3. 遍历每个危化品，计算台账数据
        for (MaterialInfo material : hazardousMaterials) {
            HazardousLedgerDTO ledger = new HazardousLedgerDTO();
            ledger.setMaterialId(material.getId());
            ledger.setMaterialName(material.getMaterialName());
            ledger.setCasNumber(getCasNumber(material));
            ledger.setDangerCategory(getDangerCategory(material));
            ledger.setControlType(material.getIsControlled());
            ledger.setUnit(material.getUnit());
            
            // 3.1 计算期初库存（开始日期之前的库存）
            BigDecimal openingStock = calculateOpeningStock(material.getId(), queryDTO.getStartDate());
            ledger.setOpeningStock(openingStock);
            
            // 3.2 计算期间入库总量
            BigDecimal totalStockIn = calculateTotalStockIn(material.getId(), 
                    queryDTO.getStartDate(), queryDTO.getEndDate());
            ledger.setTotalStockIn(totalStockIn);
            
            // 3.3 计算期间出库总量
            BigDecimal totalStockOut = calculateTotalStockOut(material.getId(), 
                    queryDTO.getStartDate(), queryDTO.getEndDate());
            ledger.setTotalStockOut(totalStockOut);
            
            // 3.4 计算期末库存 = 期初库存 + 入库总量 - 出库总量
            BigDecimal closingStock = openingStock.add(totalStockIn).subtract(totalStockOut);
            ledger.setClosingStock(closingStock);
            
            // 3.5 计算账实差异
            BigDecimal discrepancyRate = calculateDiscrepancyRate(material.getId(), closingStock);
            ledger.setDiscrepancyRate(discrepancyRate);
            
            ledgerList.add(ledger);
        }
        
        log.info("查询到危化品台账记录数: {}", ledgerList.size());
        return ledgerList;
    }
    
    @Override
    public byte[] exportLedgerToExcel(HazardousLedgerQueryDTO queryDTO) {
        log.info("导出危化品台账报表为Excel，参数: {}", queryDTO);
        
        // 1. 查询台账数据
        List<HazardousLedgerDTO> ledgerList = queryLedger(queryDTO);
        
        // 2. 创建Excel工作簿
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("危化品台账");
            
            // 3. 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"药品名称", "CAS号", "危险类别", "管控类型", "单位", 
                    "期初库存", "入库总量", "出库总量", "期末库存", "账实差异(%)"};
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 4. 填充数据行
            CellStyle dataStyle = createDataStyle(workbook);
            int rowNum = 1;
            for (HazardousLedgerDTO ledger : ledgerList) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(ledger.getMaterialName());
                row.createCell(1).setCellValue(ledger.getCasNumber() != null ? ledger.getCasNumber() : "");
                row.createCell(2).setCellValue(ledger.getDangerCategory() != null ? ledger.getDangerCategory() : "");
                row.createCell(3).setCellValue(getControlTypeText(ledger.getControlType()));
                row.createCell(4).setCellValue(ledger.getUnit());
                row.createCell(5).setCellValue(ledger.getOpeningStock().doubleValue());
                row.createCell(6).setCellValue(ledger.getTotalStockIn().doubleValue());
                row.createCell(7).setCellValue(ledger.getTotalStockOut().doubleValue());
                row.createCell(8).setCellValue(ledger.getClosingStock().doubleValue());
                row.createCell(9).setCellValue(ledger.getDiscrepancyRate().doubleValue());
                
                // 应用数据样式
                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }
            
            // 5. 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // 增加一些额外宽度以确保内容完全显示
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }
            
            // 6. 写入输出流
            workbook.write(outputStream);
            byte[] excelBytes = outputStream.toByteArray();
            
            log.info("成功导出危化品台账报表，记录数: {}", ledgerList.size());
            return excelBytes;
            
        } catch (Exception e) {
            log.error("导出危化品台账报表失败", e);
            throw new RuntimeException("导出Excel失败", e);
        }
    }
    
    /**
     * 计算期初库存
     */
    private BigDecimal calculateOpeningStock(Long materialId, LocalDate startDate) {
        if (startDate == null) {
            // 如果没有指定开始日期，期初库存为0
            return BigDecimal.ZERO;
        }
        
        // 查询开始日期之前的所有入库记录
        LambdaQueryWrapper<StockInDetail> inWrapper = new LambdaQueryWrapper<>();
        inWrapper.eq(StockInDetail::getMaterialId, materialId);
        // TODO: 需要关联stock_in表来过滤日期，这里简化处理
        List<StockInDetail> inDetails = stockInDetailMapper.selectList(inWrapper);
        BigDecimal totalIn = inDetails.stream()
                .map(StockInDetail::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 查询开始日期之前的所有出库记录
        LambdaQueryWrapper<StockOutDetail> outWrapper = new LambdaQueryWrapper<>();
        outWrapper.eq(StockOutDetail::getMaterialId, materialId);
        List<StockOutDetail> outDetails = stockOutDetailMapper.selectList(outWrapper);
        BigDecimal totalOut = outDetails.stream()
                .map(StockOutDetail::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalIn.subtract(totalOut);
    }
    
    /**
     * 计算期间入库总量
     */
    private BigDecimal calculateTotalStockIn(Long materialId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<StockInDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockInDetail::getMaterialId, materialId);
        // TODO: 需要关联stock_in表来过滤日期
        
        List<StockInDetail> details = stockInDetailMapper.selectList(wrapper);
        return details.stream()
                .map(StockInDetail::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * 计算期间出库总量
     */
    private BigDecimal calculateTotalStockOut(Long materialId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<StockOutDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockOutDetail::getMaterialId, materialId);
        // TODO: 需要关联stock_out表来过滤日期
        
        List<StockOutDetail> details = stockOutDetailMapper.selectList(wrapper);
        return details.stream()
                .map(StockOutDetail::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * 计算账实差异率
     */
    private BigDecimal calculateDiscrepancyRate(Long materialId, BigDecimal bookStock) {
        if (bookStock.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        // 获取已领用未归还数量
        BigDecimal unreturnedQuantity = approvalClient.getUnreturnedQuantity(materialId);
        
        // 实际库存 = 账面库存 - 已领用未归还数量
        BigDecimal actualStock = bookStock.subtract(unreturnedQuantity);
        
        // 账实差异 = (账面库存 - 实际库存) / 账面库存 × 100%
        BigDecimal discrepancy = bookStock.subtract(actualStock)
                .divide(bookStock, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        
        return discrepancy.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 获取CAS号
     */
    private String getCasNumber(MaterialInfo material) {
        return material.getCasNumber();
    }
    
    /**
     * 获取危险类别
     */
    private String getDangerCategory(MaterialInfo material) {
        return material.getDangerCategory();
    }
    
    /**
     * 获取管控类型文本
     */
    private String getControlTypeText(Integer controlType) {
        if (controlType == null) {
            return "否";
        }
        switch (controlType) {
            case 1:
                return "易制毒";
            case 2:
                return "易制爆";
            default:
                return "否";
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
}
