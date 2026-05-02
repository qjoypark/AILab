package com.lab.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.annotation.AuditLog;
import com.lab.common.exception.BusinessException;
import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.dto.HazardousReturnStockInRequest;
import com.lab.inventory.dto.StockInDTO;
import com.lab.inventory.entity.StockIn;
import com.lab.inventory.entity.StockInDetail;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.entity.Warehouse;
import com.lab.inventory.mapper.StockInDetailMapper;
import com.lab.inventory.mapper.StockInMapper;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.mapper.SysUserMapper;
import com.lab.inventory.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 入库服务实现
 */
@Service
@RequiredArgsConstructor
public class StockInServiceImpl implements com.lab.inventory.service.StockInService {
    
    private static final Long DEFAULT_SYSTEM_OPERATOR_ID = 1L;
    private static final int HAZARDOUS_WAREHOUSE_TYPE = 2;
    private static final String SYSTEM_AUTO_CREATED_MARK = "[系统自动创建-危化品归还]";
    private static final int COLUMN_IN_TYPE = 0;
    private static final int COLUMN_WAREHOUSE = 1;
    private static final int COLUMN_REMARK = 2;
    private static final int COLUMN_MATERIAL_ID = 3;
    private static final int COLUMN_QUANTITY = 4;
    private static final int COLUMN_UNIT_PRICE = 5;
    private static final int COLUMN_BATCH_NUMBER = 6;
    private static final int COLUMN_PRODUCTION_DATE = 7;
    private static final int COLUMN_EXPIRE_DATE = 8;
    private static final List<DateTimeFormatter> SUPPORTED_DATE_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd")
    );

    private final StockInMapper stockInMapper;
    private final StockInDetailMapper stockInDetailMapper;
    private final StockInventoryMapper stockInventoryMapper;
    private final WarehouseMapper warehouseMapper;
    private final SysUserMapper sysUserMapper;
    private final MaterialClient materialClient;
    
    @Override
    public Page<StockIn> listStockIn(
            int page,
            int size,
            String keyword,
            Long warehouseId,
            Integer status,
            LocalDateTime createdTimeStart,
            LocalDateTime createdTimeEnd
    ) {
        Page<StockIn> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<StockIn> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(StockIn::getInOrderNo, keyword.trim());
        }
        if (warehouseId != null) {
            wrapper.eq(StockIn::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(StockIn::getStatus, status);
        }
        if (createdTimeStart != null) {
            wrapper.ge(StockIn::getCreatedTime, createdTimeStart);
        }
        if (createdTimeEnd != null) {
            wrapper.le(StockIn::getCreatedTime, createdTimeEnd);
        }
        
        wrapper.orderByDesc(StockIn::getCreatedTime);
        Page<StockIn> resultPage = stockInMapper.selectPage(pageParam, wrapper);
        fillCreatorNames(resultPage.getRecords());
        return resultPage;
    }
    
    @Override
    public StockIn getStockInById(Long id) {
        StockIn stockIn = stockInMapper.selectById(id);
        if (stockIn == null) {
            throw new BusinessException("入库单不存在");
        }

        LambdaQueryWrapper<StockInDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(StockInDetail::getInOrderId, id);
        detailWrapper.orderByAsc(StockInDetail::getId);
        List<StockInDetail> details = stockInDetailMapper.selectList(detailWrapper);
        stockIn.setItems(details);
        fillCreatorNames(List.of(stockIn));

        return stockIn;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "CREATE", businessType = "STOCK_IN", description = "创建入库单")
    public StockIn createStockIn(StockInDTO dto) {
        // 生成入库单号
        String inOrderNo = generateInOrderNo();
        LocalDateTime now = LocalDateTime.now();
        
        // 创建入库单
        StockIn stockIn = new StockIn();
        BeanUtils.copyProperties(dto, stockIn);
        Long operatorId = dto.getOperatorId() != null ? dto.getOperatorId() : DEFAULT_SYSTEM_OPERATOR_ID;
        stockIn.setInOrderNo(inOrderNo);
        stockIn.setStatus(1); // 待入库
        stockIn.setOperatorId(operatorId);
        stockIn.setCreatedBy(operatorId);
        stockIn.setCreatedTime(now);
        stockIn.setUpdatedBy(operatorId);
        stockIn.setUpdatedTime(now);
        
        // 计算总金额
        BigDecimal totalAmount = dto.getItems().stream()
            .map(item -> {
                BigDecimal qty = item.getQuantity();
                BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                return qty.multiply(price);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        stockIn.setTotalAmount(totalAmount);
        
        stockInMapper.insert(stockIn);
        
        // 创建入库明细
        for (StockInDTO.StockInDetailDTO itemDto : dto.getItems()) {
            StockInDetail detail = new StockInDetail();
            BeanUtils.copyProperties(itemDto, detail);
            detail.setInOrderId(stockIn.getId());
            detail.setCreatedTime(now);
            
            if (itemDto.getUnitPrice() != null) {
                detail.setTotalAmount(itemDto.getQuantity().multiply(itemDto.getUnitPrice()));
            }
            
            stockInDetailMapper.insert(detail);
        }
        
        stockIn.setCreatedByName(resolveCreatorName(stockIn));
        return stockIn;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "CONFIRM", businessType = "STOCK_IN", description = "确认入库")
    public void confirmStockIn(Long id) {
        StockIn stockIn = getStockInById(id);
        
        if (stockIn.getStatus() != 1) {
            throw new BusinessException("入库单状态不允许确认");
        }
        
        // 查询入库明细
        LambdaQueryWrapper<StockInDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockInDetail::getInOrderId, id);
        List<StockInDetail> details = stockInDetailMapper.selectList(wrapper);
        
        // 更新库存
        for (StockInDetail detail : details) {
            updateInventory(stockIn, detail);
        }
        
        // 更新入库单状态
        stockIn.setStatus(2); // 已入库
        stockIn.setUpdatedBy(stockIn.getOperatorId());
        stockIn.setUpdatedTime(LocalDateTime.now());
        stockInMapper.updateById(stockIn);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "CANCEL", businessType = "STOCK_IN", description = "取消入库单")
    public void cancelStockIn(Long id) {
        StockIn stockIn = getStockInById(id);
        
        if (stockIn.getStatus() != 1) {
            throw new BusinessException("入库单状态不允许取消");
        }
        
        stockIn.setStatus(3); // 已取消
        stockIn.setUpdatedBy(stockIn.getOperatorId());
        stockIn.setUpdatedTime(LocalDateTime.now());
        stockInMapper.updateById(stockIn);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long hazardousReturnStockIn(HazardousReturnStockInRequest request) {
        if (request.getReturnQuantity() == null || request.getReturnQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("归还数量必须大于0");
        }

        Long warehouseId = selectHazardousReturnWarehouseId();
        LocalDateTime now = LocalDateTime.now();

        StockIn stockIn = new StockIn();
        stockIn.setInOrderNo(generateInOrderNo());
        stockIn.setInType(4); // 归还入库
        stockIn.setWarehouseId(warehouseId);
        stockIn.setInDate(LocalDate.now());
        stockIn.setOperatorId(DEFAULT_SYSTEM_OPERATOR_ID);
        stockIn.setStatus(2); // 自动确认入库
        stockIn.setTotalAmount(BigDecimal.ZERO);
        stockIn.setRemark(buildSystemAutoRemark(request.getRemark()));
        stockIn.setCreatedBy(DEFAULT_SYSTEM_OPERATOR_ID);
        stockIn.setCreatedTime(now);
        stockIn.setUpdatedBy(DEFAULT_SYSTEM_OPERATOR_ID);
        stockIn.setUpdatedTime(now);
        stockInMapper.insert(stockIn);

        StockInDetail detail = new StockInDetail();
        detail.setInOrderId(stockIn.getId());
        detail.setMaterialId(request.getMaterialId());
        detail.setBatchNumber("HZ-RETURN-" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        detail.setQuantity(request.getReturnQuantity());
        detail.setUnitPrice(BigDecimal.ZERO);
        detail.setTotalAmount(BigDecimal.ZERO);
        detail.setCreatedTime(now);
        stockInDetailMapper.insert(detail);

        updateInventory(stockIn, detail);
        return stockIn.getId();
    }

    @Override
    public byte[] generateStockInImportTemplate() {
        String[] headers = {
                "入库类型(必填:采购入库/退货入库/盘盈入库/归还入库或1/2/3/4)",
                "仓库(必填:仓库ID或仓库名称)",
                "备注(选填)",
                "药品(必填:药品ID或药品编码/名称关键词)",
                "数量(必填)",
                "单价(选填)",
                "批次号(选填)",
                "生产日期(选填:yyyy-MM-dd)",
                "有效期(选填:yyyy-MM-dd)"
        };

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("入库导入模板");
            CellStyle headerStyle = createTemplateHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(headers[index]);
                cell.setCellStyle(headerStyle);
            }

            Row sampleRow1 = sheet.createRow(1);
            sampleRow1.createCell(COLUMN_IN_TYPE).setCellValue("采购入库");
            sampleRow1.createCell(COLUMN_WAREHOUSE).setCellValue("2楼药品库");
            sampleRow1.createCell(COLUMN_REMARK).setCellValue("示例：实验课补货");
            sampleRow1.createCell(COLUMN_MATERIAL_ID).setCellValue("1");
            sampleRow1.createCell(COLUMN_QUANTITY).setCellValue("10");
            sampleRow1.createCell(COLUMN_UNIT_PRICE).setCellValue("25.6");
            sampleRow1.createCell(COLUMN_BATCH_NUMBER).setCellValue("BATCH-20260319");
            sampleRow1.createCell(COLUMN_PRODUCTION_DATE).setCellValue("2026-03-01");
            sampleRow1.createCell(COLUMN_EXPIRE_DATE).setCellValue("2027-03-01");

            Row sampleRow2 = sheet.createRow(2);
            sampleRow2.createCell(COLUMN_MATERIAL_ID).setCellValue("2");
            sampleRow2.createCell(COLUMN_QUANTITY).setCellValue("5");
            sampleRow2.createCell(COLUMN_UNIT_PRICE).setCellValue("18.2");
            sampleRow2.createCell(COLUMN_BATCH_NUMBER).setCellValue("BATCH-20260319-B");

            Sheet guideSheet = workbook.createSheet("填写说明");
            guideSheet.createRow(0).createCell(0).setCellValue("1) 第1行是表头，请勿修改列顺序。");
            guideSheet.createRow(1).createCell(0).setCellValue("2) 从第2行开始填写，支持一张单导入多条药品明细。");
            guideSheet.createRow(2).createCell(0).setCellValue("3) 入库类型/仓库/备注可只在首条填写，后续行留空默认沿用首条。");
            guideSheet.createRow(3).createCell(0).setCellValue("4) 药品与数量必填；药品支持ID、编码或名称关键词（若匹配多条会报错）。");
            guideSheet.createRow(4).createCell(0).setCellValue("5) 入库日期与经手人由系统自动填充，无需在Excel填写。");
            guideSheet.autoSizeColumn(0);

            for (int index = 0; index < headers.length; index++) {
                sheet.autoSizeColumn(index);
                int currentWidth = sheet.getColumnWidth(index);
                sheet.setColumnWidth(index, Math.min(currentWidth + 800, 15000));
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new BusinessException("生成导入模板失败");
        }
    }

    @Override
    public StockInDTO importStockInFromExcel(MultipartFile file, Long operatorId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传Excel文件");
        }

        List<StockInDTO.StockInDetailDTO> items = new ArrayList<>();
        Integer inType = null;
        Long warehouseId = null;
        String remark = null;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BusinessException("Excel中未找到数据页");
            }

            DataFormatter formatter = new DataFormatter();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                int displayRowNum = rowIndex + 1;
                if (isRowEmpty(row, formatter)) {
                    continue;
                }

                Integer rowInType = parseInType(getCellText(row, COLUMN_IN_TYPE, formatter), displayRowNum);
                Long rowWarehouseId = resolveWarehouseId(getCellText(row, COLUMN_WAREHOUSE, formatter), displayRowNum);
                String rowRemark = trimToNull(getCellText(row, COLUMN_REMARK, formatter));

                inType = mergeField(inType, rowInType, "入库类型", displayRowNum);
                warehouseId = mergeField(warehouseId, rowWarehouseId, "仓库", displayRowNum);
                if (!StringUtils.hasText(remark) && StringUtils.hasText(rowRemark)) {
                    remark = rowRemark;
                }

                String materialRaw = trimToNull(getCellText(row, COLUMN_MATERIAL_ID, formatter));
                if (!StringUtils.hasText(materialRaw)) {
                    throw new BusinessException("第" + displayRowNum + "行药品不能为空");
                }
                Long materialId = resolveMaterialId(materialRaw, displayRowNum);

                BigDecimal quantity = parseBigDecimal(getCellText(row, COLUMN_QUANTITY, formatter), "数量", displayRowNum, true);
                if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("第" + displayRowNum + "行数量必须大于0");
                }

                BigDecimal unitPrice = parseBigDecimal(getCellText(row, COLUMN_UNIT_PRICE, formatter), "单价", displayRowNum, false);
                String batchNumber = trimToNull(getCellText(row, COLUMN_BATCH_NUMBER, formatter));
                LocalDate productionDate = parseDateCell(row, COLUMN_PRODUCTION_DATE, formatter, "生产日期", displayRowNum);
                LocalDate expireDate = parseDateCell(row, COLUMN_EXPIRE_DATE, formatter, "有效期", displayRowNum);

                StockInDTO.StockInDetailDTO item = new StockInDTO.StockInDetailDTO();
                item.setMaterialId(materialId);
                item.setBatchNumber(batchNumber);
                item.setQuantity(quantity);
                item.setUnitPrice(unitPrice);
                item.setProductionDate(productionDate);
                item.setExpireDate(expireDate);
                items.add(item);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Excel导入失败，请使用模板并检查格式");
        }

        if (items.isEmpty()) {
            throw new BusinessException("导入失败：未读取到有效明细");
        }
        if (inType == null) {
            throw new BusinessException("导入失败：缺少入库类型");
        }
        if (warehouseId == null) {
            throw new BusinessException("导入失败：缺少仓库");
        }

        StockInDTO dto = new StockInDTO();
        dto.setInType(inType);
        dto.setWarehouseId(warehouseId);
        dto.setInDate(LocalDate.now());
        dto.setOperatorId(operatorId != null ? operatorId : DEFAULT_SYSTEM_OPERATOR_ID);
        dto.setRemark(remark);
        dto.setItems(items);
        return dto;
    }

    private void fillCreatorNames(List<StockIn> stockInList) {
        if (stockInList == null || stockInList.isEmpty()) {
            return;
        }
        for (StockIn stockIn : stockInList) {
            if (stockIn == null) {
                continue;
            }
            stockIn.setCreatedByName(resolveCreatorName(stockIn));
        }
    }

    private String resolveCreatorName(StockIn stockIn) {
        if (stockIn == null) {
            return "-";
        }
        if (isSystemAutoCreated(stockIn)) {
            return "系统自动创建";
        }

        Long creatorId = stockIn.getCreatedBy() != null ? stockIn.getCreatedBy() : stockIn.getOperatorId();
        if (creatorId == null) {
            return "-";
        }

        String realName = sysUserMapper.selectRealNameById(creatorId);
        if (StringUtils.hasText(realName)) {
            return realName.trim();
        }
        return "-";
    }

    private boolean isSystemAutoCreated(StockIn stockIn) {
        return StringUtils.hasText(stockIn.getRemark()) && stockIn.getRemark().startsWith(SYSTEM_AUTO_CREATED_MARK);
    }

    private String buildSystemAutoRemark(String requestRemark) {
        if (!StringUtils.hasText(requestRemark)) {
            return SYSTEM_AUTO_CREATED_MARK;
        }
        return SYSTEM_AUTO_CREATED_MARK + " " + requestRemark.trim();
    }

    private <T> T mergeField(T baseValue, T rowValue, String fieldName, int rowNum) {
        if (baseValue == null) {
            return rowValue;
        }
        if (rowValue == null) {
            return baseValue;
        }
        if (!baseValue.equals(rowValue)) {
            throw new BusinessException("第" + rowNum + "行" + fieldName + "与前序行不一致");
        }
        return baseValue;
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int columnIndex = COLUMN_IN_TYPE; columnIndex <= COLUMN_EXPIRE_DATE; columnIndex++) {
            if (StringUtils.hasText(getCellText(row, columnIndex, formatter))) {
                return false;
            }
        }
        return true;
    }

    private String getCellText(Row row, int columnIndex, DataFormatter formatter) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private Integer parseInType(String rawValue, int rowNum) {
        String value = trimToNull(rawValue);
        if (!StringUtils.hasText(value)) {
            return null;
        }

        if ("采购入库".equals(value)) {
            return 1;
        }
        if ("退货入库".equals(value)) {
            return 2;
        }
        if ("盘盈入库".equals(value) || "其他入库".equals(value)) {
            return 3;
        }
        if ("归还入库".equals(value)) {
            return 4;
        }

        try {
            int inType = Integer.parseInt(value);
            if (inType < 1 || inType > 4) {
                throw new BusinessException("第" + rowNum + "行入库类型仅支持1/2/3/4或中文类型名称");
            }
            return inType;
        } catch (NumberFormatException ex) {
            throw new BusinessException("第" + rowNum + "行入库类型格式不正确");
        }
    }

    private Long resolveWarehouseId(String rawValue, int rowNum) {
        String value = trimToNull(rawValue);
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            // 按仓库名称解析
        }

        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Warehouse::getWarehouseName, value);
        wrapper.eq(Warehouse::getStatus, 1);
        wrapper.last("LIMIT 1");
        Warehouse warehouse = warehouseMapper.selectOne(wrapper);
        if (warehouse == null) {
            throw new BusinessException("第" + rowNum + "行仓库不存在或未启用: " + value);
        }
        return warehouse.getId();
    }

    private Long resolveMaterialId(String rawValue, int rowNum) {
        String value = trimToNull(rawValue);
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            Long materialId = Long.parseLong(value);
            if (materialClient.getMaterialInfo(materialId) != null) {
                return materialId;
            }
            throw new BusinessException("第" + rowNum + "行药品ID不存在: " + value);
        } catch (NumberFormatException ignored) {
            // 按关键词解析
        }

        List<Long> materialIds = materialClient.searchMaterialIdsByKeyword(value);
        if (materialIds.isEmpty()) {
            throw new BusinessException("第" + rowNum + "行未找到匹配药品: " + value);
        }
        if (materialIds.size() > 1) {
            throw new BusinessException("第" + rowNum + "行药品匹配到多个结果，请填写药品ID: " + value);
        }
        return materialIds.get(0);
    }

    private BigDecimal parseBigDecimal(String rawValue, String fieldName, int rowNum, boolean required) {
        String value = trimToNull(rawValue);
        if (!StringUtils.hasText(value)) {
            if (required) {
                throw new BusinessException("第" + rowNum + "行" + fieldName + "不能为空");
            }
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException("第" + rowNum + "行" + fieldName + "格式不正确");
        }
    }

    private LocalDate parseDateCell(Row row, int columnIndex, DataFormatter formatter, String fieldName, int rowNum) {
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            try {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            } catch (Exception ignored) {
                // 回退到文本解析
            }
        }

        String value = trimToNull(formatter.formatCellValue(cell));
        if (!StringUtils.hasText(value)) {
            return null;
        }

        for (DateTimeFormatter dateFormatter : SUPPORTED_DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, dateFormatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一个日期格式
            }
        }
        throw new BusinessException("第" + rowNum + "行" + fieldName + "格式不正确，支持yyyy-MM-dd");
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private CellStyle createTemplateHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
    
    /**
     * 更新库存
     */
    private void updateInventory(StockIn stockIn, StockInDetail detail) {
        // 查询是否已存在该批次的库存
        LambdaQueryWrapper<StockInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockInventory::getMaterialId, detail.getMaterialId());
        wrapper.eq(StockInventory::getWarehouseId, stockIn.getWarehouseId());
        wrapper.eq(StockInventory::getBatchNumber, detail.getBatchNumber());
        
        StockInventory inventory = stockInventoryMapper.selectOne(wrapper);
        
        if (inventory != null) {
            // 更新现有库存
            inventory.setQuantity(inventory.getQuantity().add(detail.getQuantity()));
            inventory.setAvailableQuantity(inventory.getAvailableQuantity().add(detail.getQuantity()));
            
            if (detail.getUnitPrice() != null) {
                inventory.setUnitPrice(detail.getUnitPrice());
                inventory.setTotalAmount(inventory.getQuantity().multiply(detail.getUnitPrice()));
            }
            
            stockInventoryMapper.updateById(inventory);
        } else {
            // 创建新库存记录
            inventory = new StockInventory();
            inventory.setMaterialId(detail.getMaterialId());
            inventory.setWarehouseId(stockIn.getWarehouseId());
            inventory.setStorageLocationId(detail.getStorageLocationId());
            inventory.setBatchNumber(detail.getBatchNumber());
            inventory.setQuantity(detail.getQuantity());
            inventory.setAvailableQuantity(detail.getQuantity());
            inventory.setLockedQuantity(BigDecimal.ZERO);
            inventory.setProductionDate(detail.getProductionDate());
            inventory.setExpireDate(detail.getExpireDate());
            inventory.setUnitPrice(detail.getUnitPrice());
            
            if (detail.getUnitPrice() != null) {
                inventory.setTotalAmount(detail.getQuantity().multiply(detail.getUnitPrice()));
            }
            
            stockInventoryMapper.insert(inventory);
        }
    }

    private Long selectHazardousReturnWarehouseId() {
        LambdaQueryWrapper<Warehouse> hazardousWarehouseWrapper = new LambdaQueryWrapper<>();
        hazardousWarehouseWrapper.eq(Warehouse::getWarehouseType, HAZARDOUS_WAREHOUSE_TYPE);
        hazardousWarehouseWrapper.eq(Warehouse::getStatus, 1);
        hazardousWarehouseWrapper.orderByAsc(Warehouse::getId);
        hazardousWarehouseWrapper.last("LIMIT 1");
        Warehouse hazardousWarehouse = warehouseMapper.selectOne(hazardousWarehouseWrapper);
        if (hazardousWarehouse != null) {
            return hazardousWarehouse.getId();
        }

        LambdaQueryWrapper<Warehouse> activeWarehouseWrapper = new LambdaQueryWrapper<>();
        activeWarehouseWrapper.eq(Warehouse::getStatus, 1);
        activeWarehouseWrapper.orderByAsc(Warehouse::getId);
        activeWarehouseWrapper.last("LIMIT 1");
        Warehouse fallbackWarehouse = warehouseMapper.selectOne(activeWarehouseWrapper);
        if (fallbackWarehouse != null) {
            return fallbackWarehouse.getId();
        }

        throw new BusinessException("未找到可用仓库，无法执行危化品归还入库");
    }
    
    /**
     * 生成入库单号
     */
    private String generateInOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "IN" + date;
        
        // 查询当天最大单号
        LambdaQueryWrapper<StockIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(StockIn::getInOrderNo, prefix);
        wrapper.orderByDesc(StockIn::getInOrderNo);
        wrapper.last("LIMIT 1");
        
        StockIn lastOrder = stockInMapper.selectOne(wrapper);
        
        int sequence = 1;
        if (lastOrder != null) {
            String lastNo = lastOrder.getInOrderNo();
            sequence = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        
        return prefix + String.format("%04d", sequence);
    }
}
