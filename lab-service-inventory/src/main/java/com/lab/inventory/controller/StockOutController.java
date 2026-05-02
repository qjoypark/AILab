package com.lab.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.result.Result;
import com.lab.inventory.dto.StockOutDTO;
import com.lab.inventory.dto.StockOutOrderSummaryDTO;
import com.lab.inventory.entity.StockOut;
import com.lab.inventory.service.StockOutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "出库管理")
@RestController
@RequestMapping("/api/v1/inventory/stock-out")
@RequiredArgsConstructor
public class StockOutController {

    private final StockOutService stockOutService;

    @Operation(summary = "分页查询出库单列表")
    @GetMapping
    public Result<Page<StockOut>> listStockOut(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdTimeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdTimeEnd) {
        Page<StockOut> result = stockOutService.listStockOut(
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

    @Operation(summary = "查询出库单详情")
    @GetMapping("/{id}")
    public Result<StockOut> getStockOut(@PathVariable Long id) {
        return Result.success(stockOutService.getStockOutById(id));
    }

    @Operation(summary = "生成电子出库单PDF")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportStockOutPdf(@PathVariable Long id) {
        StockOut stockOut = stockOutService.getStockOutById(id);
        byte[] pdfBytes = stockOutService.generateStockOutPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData(
                "attachment",
                "stock_out_" + (stockOut.getOutOrderNo() != null ? stockOut.getOutOrderNo() : id) + ".pdf"
        );

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @Operation(summary = "按申请单查询已生成出库单")
    @GetMapping("/application/{applicationId}/orders")
    public Result<List<StockOutOrderSummaryDTO>> listByApplication(@PathVariable Long applicationId) {
        return Result.success(stockOutService.listStockOutByApplicationId(applicationId));
    }

    @Operation(summary = "创建出库单")
    @PostMapping
    public Result<StockOut> createStockOut(@Validated @RequestBody StockOutDTO dto) {
        return Result.success(stockOutService.createStockOut(dto));
    }

    @Operation(summary = "根据申请单创建出库单")
    @PostMapping("/from-application")
    public Result<StockOut> createStockOutFromApplication(@RequestBody Map<String, Long> request) {
        Long applicationId = request.get("applicationId");
        if (applicationId == null) {
            return Result.error(400, "applicationId cannot be null");
        }
        return Result.success(stockOutService.createStockOutFromApplication(applicationId));
    }

    @Operation(summary = "确认出库")
    @PostMapping("/{id}/confirm")
    public Result<Void> confirmStockOut(@PathVariable Long id) {
        stockOutService.confirmStockOut(id);
        return Result.success();
    }

    @Operation(summary = "取消出库单")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelStockOut(@PathVariable Long id) {
        stockOutService.cancelStockOut(id);
        return Result.success();
    }
}
