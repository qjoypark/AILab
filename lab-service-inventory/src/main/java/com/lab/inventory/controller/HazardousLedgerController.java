package com.lab.inventory.controller;

import com.lab.common.result.Result;
import com.lab.inventory.dto.HazardousLedgerDTO;
import com.lab.inventory.dto.HazardousLedgerQueryDTO;
import com.lab.inventory.service.HazardousLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 危化品台账控制器
 */
@Tag(name = "危化品台账管理")
@RestController
@RequestMapping("/api/v1/hazardous/ledger")
@RequiredArgsConstructor
public class HazardousLedgerController {
    
    private final HazardousLedgerService hazardousLedgerService;
    
    /**
     * 查询危化品台账报表
     */
    @Operation(summary = "查询危化品台账报表")
    @GetMapping
    public Result<List<HazardousLedgerDTO>> queryLedger(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long materialId) {
        
        HazardousLedgerQueryDTO queryDTO = new HazardousLedgerQueryDTO();
        queryDTO.setStartDate(startDate);
        queryDTO.setEndDate(endDate);
        queryDTO.setMaterialId(materialId);
        
        List<HazardousLedgerDTO> ledgerList = hazardousLedgerService.queryLedger(queryDTO);
        return Result.success(ledgerList);
    }
    
    /**
     * 导出危化品台账报表为Excel
     */
    @Operation(summary = "导出危化品台账报表为Excel")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportLedger(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long materialId) {
        
        HazardousLedgerQueryDTO queryDTO = new HazardousLedgerQueryDTO();
        queryDTO.setStartDate(startDate);
        queryDTO.setEndDate(endDate);
        queryDTO.setMaterialId(materialId);
        
        byte[] excelBytes = hazardousLedgerService.exportLedgerToExcel(queryDTO);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", 
                "hazardous_ledger_" + LocalDate.now() + ".xlsx");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }
}
