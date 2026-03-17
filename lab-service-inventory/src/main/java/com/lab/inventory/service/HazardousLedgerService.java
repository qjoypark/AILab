package com.lab.inventory.service;

import com.lab.inventory.dto.HazardousLedgerDTO;
import com.lab.inventory.dto.HazardousLedgerQueryDTO;

import java.util.List;

/**
 * 危化品台账服务接口
 */
public interface HazardousLedgerService {
    
    /**
     * 查询危化品台账报表
     * 
     * @param queryDTO 查询参数
     * @return 危化品台账列表
     */
    List<HazardousLedgerDTO> queryLedger(HazardousLedgerQueryDTO queryDTO);
    
    /**
     * 导出危化品台账报表为Excel
     * 
     * @param queryDTO 查询参数
     * @return Excel文件字节数组
     */
    byte[] exportLedgerToExcel(HazardousLedgerQueryDTO queryDTO);
}
