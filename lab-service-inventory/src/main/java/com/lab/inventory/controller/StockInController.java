package com.lab.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.result.Result;
import com.lab.inventory.dto.HazardousReturnStockInRequest;
import com.lab.inventory.dto.StockInDTO;
import com.lab.inventory.entity.StockIn;
import com.lab.inventory.service.StockInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 入库管理控制器
 */
@Tag(name = "入库管理")
@RestController
@RequestMapping("/api/v1/inventory/stock-in")
@RequiredArgsConstructor
public class StockInController {
    
    private final StockInService stockInService;
    
    @Operation(summary = "查询入库单列表")
    @GetMapping
    public Result<Page<StockIn>> listStockIn(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime createdTimeStart,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime createdTimeEnd) {
        Page<StockIn> result = stockInService.listStockIn(
                page,
                size,
                keyword,
                warehouseId,
                status,
                createdTimeStart,
                createdTimeEnd
        );
        return Result.success(result);
    }
    
    @Operation(summary = "查询入库单详情")
    @GetMapping("/{id}")
    public Result<StockIn> getStockIn(@PathVariable Long id) {
        StockIn stockIn = stockInService.getStockInById(id);
        return Result.success(stockIn);
    }
    
    @Operation(summary = "创建入库单")
    @PostMapping
    public Result<StockIn> createStockIn(@Validated @RequestBody StockInDTO dto) {
        StockIn stockIn = stockInService.createStockIn(dto);
        return Result.success(stockIn);
    }
    
    @Operation(summary = "确认入库")
    @PostMapping("/{id}/confirm")
    public Result<Void> confirmStockIn(@PathVariable Long id) {
        stockInService.confirmStockIn(id);
        return Result.success();
    }
    
    @Operation(summary = "取消入库单")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelStockIn(@PathVariable Long id) {
        stockInService.cancelStockIn(id);
        return Result.success();
    }

    @Operation(summary = "危化品归还自动入库")
    @PostMapping("/hazardous-return")
    public Result<Long> hazardousReturnStockIn(@Validated @RequestBody HazardousReturnStockInRequest request) {
        Long stockInId = stockInService.hazardousReturnStockIn(request);
        return Result.success(stockInId);
    }

    @Operation(summary = "下载入库导入模板")
    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        byte[] excelBytes = stockInService.generateStockInImportTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment",
                "stock_in_import_template_" + LocalDate.now() + ".xlsx");
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }

    @Operation(summary = "Excel导入预览入库单")
    @PostMapping("/import")
    public Result<StockInDTO> importStockIn(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long operatorId,
            @RequestHeader(value = "X-UserId", required = false) Long headerUserId
    ) {
        Long finalOperatorId = operatorId != null ? operatorId : headerUserId;
        StockInDTO preview = stockInService.importStockInFromExcel(file, finalOperatorId);
        return Result.success(preview);
    }
}
