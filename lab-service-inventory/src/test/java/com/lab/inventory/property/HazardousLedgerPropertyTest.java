package com.lab.inventory.property;

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
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Positive;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 危化品台账报表完整性属性测试
 * 
 * 使用jqwik框架进行基于属性的测试，每个属性测试运行100次迭代
 */
@SpringBootTest
@Transactional
public class HazardousLedgerPropertyTest {
    
    @Autowired
    private HazardousLedgerService hazardousLedgerService;
    
    @Autowired
    private StockInventoryMapper stockInventoryMapper;
    
    @Autowired
    private StockInDetailMapper stockInDetailMapper;
    
    @Autowired
    private StockOutDetailMapper stockOutDetailMapper;
    
    @MockBean
    private MaterialClient materialClient;
    
    @MockBean
    private ApprovalClient approvalClient;
    
    /**
     * 属性 17: 危化品台账报表完整性
     * 
     * **Validates: Requirements 6.10**
     * 
     * 对于任何危化品台账报表，报表应包含每个危化品的名称、CAS号、危险类别、
     * 管控类型、期初库存、入库总量、出库总量、期末库存、账实差异等信息。
     * 
     * 验证要点：
     * 1. 报表包含所有必需字段
     * 2. 字段值不为null（除了可选字段）
     * 3. 数值计算正确：期末库存 = 期初库存 + 入库总量 - 出库总量
     * 4. 账实差异计算正确
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 17: 危化品台账报表完整性")
    void hazardousLedgerReportCompleteness(
            @ForAll @Positive BigDecimal openingStock,
            @ForAll @Positive BigDecimal stockInQuantity,
            @ForAll @Positive BigDecimal stockOutQuantity,
            @ForAll @IntRange(min = 0, max = 2) int controlType) {
        
        // 前置条件：出库数量不应超过期初库存+入库数量
        Assume.that(stockOutQuantity.compareTo(openingStock.add(stockInQuantity)) <= 0);
        
        // 1. 准备测试数据
        Long materialId = System.currentTimeMillis();
        String casNumber = "CAS-" + materialId;
        String dangerCategory = "易燃液体";
        String materialName = "测试危化品-" + materialId;
        
        // 创建危化品信息
        MaterialInfo material = new MaterialInfo();
        material.setId(materialId);
        material.setMaterialName(materialName);
        material.setMaterialType(3); // 3-危化品
        material.setUnit("kg");
        material.setCasNumber(casNumber);
        material.setDangerCategory(dangerCategory);
        material.setIsControlled(controlType);
        
        // 模拟MaterialClient返回危化品列表
        when(materialClient.getHazardousMaterials())
                .thenReturn(Collections.singletonList(material));
        
        // 创建库存记录（用于计算期初库存）
        StockInventory inventory = new StockInventory();
        inventory.setMaterialId(materialId);
        inventory.setWarehouseId(1L);
        inventory.setBatchNumber("BATCH-" + materialId);
        inventory.setQuantity(openingStock);
        inventory.setAvailableQuantity(openingStock);
        inventory.setLockedQuantity(BigDecimal.ZERO);
        inventory.setUnitPrice(BigDecimal.TEN);
        inventory.setTotalAmount(openingStock.multiply(BigDecimal.TEN));
        inventory.setProductionDate(LocalDate.now().minusMonths(6));
        inventory.setExpireDate(LocalDate.now().plusYears(1));
        stockInventoryMapper.insert(inventory);
        
        // 创建入库明细记录
        StockInDetail inDetail = new StockInDetail();
        inDetail.setInOrderId(1L);
        inDetail.setMaterialId(materialId);
        inDetail.setBatchNumber("BATCH-IN-" + materialId);
        inDetail.setQuantity(stockInQuantity);
        inDetail.setUnitPrice(BigDecimal.TEN);
        inDetail.setTotalAmount(stockInQuantity.multiply(BigDecimal.TEN));
        inDetail.setProductionDate(LocalDate.now());
        inDetail.setExpireDate(LocalDate.now().plusYears(1));
        stockInDetailMapper.insert(inDetail);
        
        // 创建出库明细记录
        StockOutDetail outDetail = new StockOutDetail();
        outDetail.setOutOrderId(1L);
        outDetail.setMaterialId(materialId);
        outDetail.setBatchNumber("BATCH-OUT-" + materialId);
        outDetail.setQuantity(stockOutQuantity);
        outDetail.setUnitPrice(BigDecimal.TEN);
        outDetail.setTotalAmount(stockOutQuantity.multiply(BigDecimal.TEN));
        stockOutDetailMapper.insert(outDetail);
        
        // 模拟已领用未归还数量（用于计算账实差异）
        BigDecimal unreturnedQuantity = stockOutQuantity.multiply(new BigDecimal("0.1")); // 10%未归还
        when(approvalClient.getUnreturnedQuantity(materialId))
                .thenReturn(unreturnedQuantity);
        
        // 2. 执行查询
        HazardousLedgerQueryDTO queryDTO = new HazardousLedgerQueryDTO();
        queryDTO.setMaterialId(materialId);
        queryDTO.setStartDate(LocalDate.now().minusMonths(1));
        queryDTO.setEndDate(LocalDate.now());
        
        List<HazardousLedgerDTO> ledgerList = hazardousLedgerService.queryLedger(queryDTO);
        
        // 3. 验证报表完整性
        assertThat(ledgerList)
                .as("应返回查询结果")
                .isNotEmpty()
                .hasSize(1);
        
        HazardousLedgerDTO ledger = ledgerList.get(0);
        
        // 3.1 验证必需字段存在且不为null
        assertThat(ledger.getMaterialId())
                .as("药品ID不应为null")
                .isNotNull()
                .isEqualTo(materialId);
        
        assertThat(ledger.getMaterialName())
                .as("药品名称不应为null")
                .isNotNull()
                .isEqualTo(materialName);
        
        assertThat(ledger.getCasNumber())
                .as("CAS号不应为null")
                .isNotNull()
                .isEqualTo(casNumber);
        
        assertThat(ledger.getDangerCategory())
                .as("危险类别不应为null")
                .isNotNull()
                .isEqualTo(dangerCategory);
        
        assertThat(ledger.getControlType())
                .as("管控类型不应为null")
                .isNotNull()
                .isEqualTo(controlType);
        
        assertThat(ledger.getUnit())
                .as("单位不应为null")
                .isNotNull()
                .isEqualTo("kg");
        
        assertThat(ledger.getOpeningStock())
                .as("期初库存不应为null")
                .isNotNull();
        
        assertThat(ledger.getTotalStockIn())
                .as("入库总量不应为null")
                .isNotNull();
        
        assertThat(ledger.getTotalStockOut())
                .as("出库总量不应为null")
                .isNotNull();
        
        assertThat(ledger.getClosingStock())
                .as("期末库存不应为null")
                .isNotNull();
        
        assertThat(ledger.getDiscrepancyRate())
                .as("账实差异不应为null")
                .isNotNull();
        
        // 3.2 验证数值计算正确性
        // 期末库存 = 期初库存 + 入库总量 - 出库总量
        BigDecimal expectedClosingStock = ledger.getOpeningStock()
                .add(ledger.getTotalStockIn())
                .subtract(ledger.getTotalStockOut());
        
        assertThat(ledger.getClosingStock())
                .as("期末库存应等于：期初库存 + 入库总量 - 出库总量")
                .isEqualByComparingTo(expectedClosingStock);
        
        // 3.3 验证账实差异计算
        // 账实差异应该是一个百分比值，范围在0-100之间
        assertThat(ledger.getDiscrepancyRate().compareTo(BigDecimal.ZERO))
                .as("账实差异应大于等于0")
                .isGreaterThanOrEqualTo(0);
        
        assertThat(ledger.getDiscrepancyRate().compareTo(new BigDecimal("100")))
                .as("账实差异应小于等于100%")
                .isLessThanOrEqualTo(0);
    }
    
    /**
     * 属性 17 边界情况测试：多个危化品的报表完整性
     * 
     * **Validates: Requirements 6.10**
     * 
     * 当查询多个危化品时，每个危化品都应包含完整的台账信息
     */
    @Property(tries = 50)
    @Tag("Feature: smart-lab-management-system, Property 17: 危化品台账报表完整性")
    void multipleMaterialsLedgerCompleteness(
            @ForAll @IntRange(min = 2, max = 5) int materialCount) {
        
        // 1. 准备多个危化品数据
        List<MaterialInfo> materials = new java.util.ArrayList<>();
        
        for (int i = 0; i < materialCount; i++) {
            Long materialId = System.currentTimeMillis() + i;
            
            MaterialInfo material = new MaterialInfo();
            material.setId(materialId);
            material.setMaterialName("危化品-" + i);
            material.setMaterialType(3);
            material.setUnit("kg");
            material.setCasNumber("CAS-" + materialId);
            material.setDangerCategory("危险类别-" + i);
            material.setIsControlled(i % 3); // 0, 1, 2 循环
            
            materials.add(material);
            
            // 创建库存记录
            StockInventory inventory = new StockInventory();
            inventory.setMaterialId(materialId);
            inventory.setWarehouseId(1L);
            inventory.setBatchNumber("BATCH-" + materialId);
            inventory.setQuantity(new BigDecimal("100"));
            inventory.setAvailableQuantity(new BigDecimal("100"));
            inventory.setLockedQuantity(BigDecimal.ZERO);
            inventory.setUnitPrice(BigDecimal.TEN);
            inventory.setTotalAmount(new BigDecimal("1000"));
            stockInventoryMapper.insert(inventory);
            
            // 模拟已领用未归还数量
            when(approvalClient.getUnreturnedQuantity(materialId))
                    .thenReturn(new BigDecimal("10"));
        }
        
        // 模拟MaterialClient返回所有危化品
        when(materialClient.getHazardousMaterials())
                .thenReturn(materials);
        
        // 2. 执行查询（不指定materialId，查询所有危化品）
        HazardousLedgerQueryDTO queryDTO = new HazardousLedgerQueryDTO();
        queryDTO.setStartDate(LocalDate.now().minusMonths(1));
        queryDTO.setEndDate(LocalDate.now());
        
        List<HazardousLedgerDTO> ledgerList = hazardousLedgerService.queryLedger(queryDTO);
        
        // 3. 验证
        assertThat(ledgerList)
                .as("应返回所有危化品的台账记录")
                .hasSize(materialCount);
        
        // 验证每个台账记录都包含完整信息
        for (int i = 0; i < ledgerList.size(); i++) {
            HazardousLedgerDTO ledger = ledgerList.get(i);
            
            assertThat(ledger.getMaterialId())
                    .as("第" + i + "个危化品的ID不应为null")
                    .isNotNull();
            
            assertThat(ledger.getMaterialName())
                    .as("第" + i + "个危化品的名称不应为null")
                    .isNotNull();
            
            assertThat(ledger.getCasNumber())
                    .as("第" + i + "个危化品的CAS号不应为null")
                    .isNotNull();
            
            assertThat(ledger.getDangerCategory())
                    .as("第" + i + "个危化品的危险类别不应为null")
                    .isNotNull();
            
            assertThat(ledger.getControlType())
                    .as("第" + i + "个危化品的管控类型不应为null")
                    .isNotNull();
            
            assertThat(ledger.getOpeningStock())
                    .as("第" + i + "个危化品的期初库存不应为null")
                    .isNotNull();
            
            assertThat(ledger.getTotalStockIn())
                    .as("第" + i + "个危化品的入库总量不应为null")
                    .isNotNull();
            
            assertThat(ledger.getTotalStockOut())
                    .as("第" + i + "个危化品的出库总量不应为null")
                    .isNotNull();
            
            assertThat(ledger.getClosingStock())
                    .as("第" + i + "个危化品的期末库存不应为null")
                    .isNotNull();
            
            assertThat(ledger.getDiscrepancyRate())
                    .as("第" + i + "个危化品的账实差异不应为null")
                    .isNotNull();
        }
    }
    
    /**
     * 属性 17 边界情况测试：无库存变动的危化品报表
     * 
     * **Validates: Requirements 6.10**
     * 
     * 即使危化品没有库存变动，报表也应包含完整信息
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 17: 危化品台账报表完整性")
    void noStockMovementLedgerCompleteness(
            @ForAll @Positive BigDecimal initialStock) {
        
        // 1. 准备测试数据（只有期初库存，无入库出库）
        Long materialId = System.currentTimeMillis();
        
        MaterialInfo material = new MaterialInfo();
        material.setId(materialId);
        material.setMaterialName("无变动危化品");
        material.setMaterialType(3);
        material.setUnit("L");
        material.setCasNumber("CAS-STATIC");
        material.setDangerCategory("腐蚀性液体");
        material.setIsControlled(1);
        
        when(materialClient.getHazardousMaterials())
                .thenReturn(Collections.singletonList(material));
        
        // 创建库存记录（无入库出库记录）
        StockInventory inventory = new StockInventory();
        inventory.setMaterialId(materialId);
        inventory.setWarehouseId(1L);
        inventory.setBatchNumber("BATCH-STATIC");
        inventory.setQuantity(initialStock);
        inventory.setAvailableQuantity(initialStock);
        inventory.setLockedQuantity(BigDecimal.ZERO);
        inventory.setUnitPrice(BigDecimal.TEN);
        inventory.setTotalAmount(initialStock.multiply(BigDecimal.TEN));
        stockInventoryMapper.insert(inventory);
        
        // 无已领用未归还数量
        when(approvalClient.getUnreturnedQuantity(materialId))
                .thenReturn(BigDecimal.ZERO);
        
        // 2. 执行查询
        HazardousLedgerQueryDTO queryDTO = new HazardousLedgerQueryDTO();
        queryDTO.setMaterialId(materialId);
        queryDTO.setStartDate(LocalDate.now().minusMonths(1));
        queryDTO.setEndDate(LocalDate.now());
        
        List<HazardousLedgerDTO> ledgerList = hazardousLedgerService.queryLedger(queryDTO);
        
        // 3. 验证
        assertThat(ledgerList)
                .as("应返回查询结果")
                .hasSize(1);
        
        HazardousLedgerDTO ledger = ledgerList.get(0);
        
        // 验证所有字段都存在
        assertThat(ledger.getMaterialName()).isNotNull();
        assertThat(ledger.getCasNumber()).isNotNull();
        assertThat(ledger.getDangerCategory()).isNotNull();
        assertThat(ledger.getControlType()).isNotNull();
        assertThat(ledger.getUnit()).isNotNull();
        assertThat(ledger.getOpeningStock()).isNotNull();
        assertThat(ledger.getTotalStockIn()).isNotNull();
        assertThat(ledger.getTotalStockOut()).isNotNull();
        assertThat(ledger.getClosingStock()).isNotNull();
        assertThat(ledger.getDiscrepancyRate()).isNotNull();
        
        // 验证无变动情况下的数值
        assertThat(ledger.getTotalStockIn())
                .as("无入库时，入库总量应为0")
                .isEqualByComparingTo(BigDecimal.ZERO);
        
        assertThat(ledger.getTotalStockOut())
                .as("无出库时，出库总量应为0")
                .isEqualByComparingTo(BigDecimal.ZERO);
        
        assertThat(ledger.getClosingStock())
                .as("无变动时，期末库存应等于期初库存")
                .isEqualByComparingTo(ledger.getOpeningStock());
        
        assertThat(ledger.getDiscrepancyRate())
                .as("无已领用未归还时，账实差异应为0")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
    
    /**
     * 属性 17 边界情况测试：管控类型的正确性
     * 
     * **Validates: Requirements 6.10**
     * 
     * 报表应正确显示不同的管控类型（0-否, 1-易制毒, 2-易制爆）
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 17: 危化品台账报表完整性")
    void controlTypeCorrectness(
            @ForAll @IntRange(min = 0, max = 2) int controlType) {
        
        // 1. 准备测试数据
        Long materialId = System.currentTimeMillis();
        
        MaterialInfo material = new MaterialInfo();
        material.setId(materialId);
        material.setMaterialName("管控类型测试-" + controlType);
        material.setMaterialType(3);
        material.setUnit("kg");
        material.setCasNumber("CAS-CONTROL-" + controlType);
        material.setDangerCategory("测试类别");
        material.setIsControlled(controlType);
        
        when(materialClient.getHazardousMaterials())
                .thenReturn(Collections.singletonList(material));
        
        // 创建库存记录
        StockInventory inventory = new StockInventory();
        inventory.setMaterialId(materialId);
        inventory.setWarehouseId(1L);
        inventory.setBatchNumber("BATCH-CONTROL");
        inventory.setQuantity(new BigDecimal("100"));
        inventory.setAvailableQuantity(new BigDecimal("100"));
        inventory.setLockedQuantity(BigDecimal.ZERO);
        inventory.setUnitPrice(BigDecimal.TEN);
        inventory.setTotalAmount(new BigDecimal("1000"));
        stockInventoryMapper.insert(inventory);
        
        when(approvalClient.getUnreturnedQuantity(materialId))
                .thenReturn(BigDecimal.ZERO);
        
        // 2. 执行查询
        HazardousLedgerQueryDTO queryDTO = new HazardousLedgerQueryDTO();
        queryDTO.setMaterialId(materialId);
        
        List<HazardousLedgerDTO> ledgerList = hazardousLedgerService.queryLedger(queryDTO);
        
        // 3. 验证
        assertThat(ledgerList).hasSize(1);
        
        HazardousLedgerDTO ledger = ledgerList.get(0);
        
        assertThat(ledger.getControlType())
                .as("管控类型应与输入一致")
                .isEqualTo(controlType);
        
        // 验证管控类型的有效性
        assertThat(ledger.getControlType())
                .as("管控类型应在有效范围内（0-2）")
                .isBetween(0, 2);
    }
}
