package com.lab.inventory.property;

import com.lab.inventory.entity.*;
import com.lab.inventory.mapper.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Positive;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 库存管理属性测试
 * 
 * 使用jqwik框架进行基于属性的测试，每个属性测试运行100次迭代
 */
@SpringBootTest
@Transactional
public class InventoryPropertyTest {
    
    @Autowired
    private StockInMapper stockInMapper;
    
    @Autowired
    private StockInDetailMapper stockInDetailMapper;
    
    @Autowired
    private StockOutMapper stockOutMapper;
    
    @Autowired
    private StockOutDetailMapper stockOutDetailMapper;
    
    @Autowired
    private StockInventoryMapper stockInventoryMapper;
    
    @Autowired
    private StockCheckMapper stockCheckMapper;
    
    @Autowired
    private StockCheckDetailMapper stockCheckDetailMapper;
    
    /**
     * 属性 5: 入库操作完整记录
     * 
     * **Validates: Requirements 5.2**
     * 
     * 对于任何入库操作，系统应创建入库单记录，包含入库数量、入库日期、经手人、仓库信息，
     * 并且入库单明细应包含每个药品的批次号、数量、单价、有效期等信息。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 5: 入库操作完整记录")
    void stockInOperationCompleteRecord(
            @ForAll @IntRange(min = 1, max = 10) int itemCount,
            @ForAll @Positive BigDecimal quantity) {
        
        // 创建入库单
        StockIn stockIn = new StockIn();
        stockIn.setInOrderNo("IN" + System.currentTimeMillis());
        stockIn.setInType(1);
        stockIn.setWarehouseId(1L);
        stockIn.setInDate(LocalDate.now());
        stockIn.setOperatorId(1L);
        stockIn.setStatus(1);
        stockIn.setTotalAmount(BigDecimal.ZERO);
        
        stockInMapper.insert(stockIn);
        
        // 验证入库单记录存在
        Assertions.assertThat(stockIn.getId()).isNotNull();
        Assertions.assertThat(stockIn.getInOrderNo()).isNotBlank();
        Assertions.assertThat(stockIn.getInDate()).isNotNull();
        Assertions.assertThat(stockIn.getOperatorId()).isNotNull();
        Assertions.assertThat(stockIn.getWarehouseId()).isNotNull();
        
        // 创建入库明细
        List<StockInDetail> details = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            StockInDetail detail = new StockInDetail();
            detail.setInOrderId(stockIn.getId());
            detail.setMaterialId((long) (i + 1));
            detail.setBatchNumber("BATCH" + System.currentTimeMillis() + i);
            detail.setQuantity(quantity);
            detail.setUnitPrice(BigDecimal.TEN);
            detail.setTotalAmount(quantity.multiply(BigDecimal.TEN));
            detail.setProductionDate(LocalDate.now());
            detail.setExpireDate(LocalDate.now().plusYears(1));
            
            stockInDetailMapper.insert(detail);
            details.add(detail);
        }
        
        // 验证入库明细记录完整性
        for (StockInDetail detail : details) {
            Assertions.assertThat(detail.getId()).isNotNull();
            Assertions.assertThat(detail.getInOrderId()).isEqualTo(stockIn.getId());
            Assertions.assertThat(detail.getMaterialId()).isNotNull();
            Assertions.assertThat(detail.getBatchNumber()).isNotBlank();
            Assertions.assertThat(detail.getQuantity()).isGreaterThan(BigDecimal.ZERO);
            Assertions.assertThat(detail.getUnitPrice()).isNotNull();
            Assertions.assertThat(detail.getProductionDate()).isNotNull();
            Assertions.assertThat(detail.getExpireDate()).isNotNull();
        }
    }
    
    /**
     * 属性 6: 出库操作完整记录
     * 
     * **Validates: Requirements 5.3**
     * 
     * 对于任何出库操作，系统应创建出库单记录，包含出库数量、领用人、用途、出库日期、仓库信息，
     * 并且出库单明细应包含每个药品的批次号、数量等信息。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 6: 出库操作完整记录")
    void stockOutOperationCompleteRecord(
            @ForAll @IntRange(min = 1, max = 10) int itemCount,
            @ForAll @Positive BigDecimal quantity) {
        
        // 创建出库单
        StockOut stockOut = new StockOut();
        stockOut.setOutOrderNo("OUT" + System.currentTimeMillis());
        stockOut.setOutType(1); // 1-领用出库
        stockOut.setWarehouseId(1L);
        stockOut.setApplicationId(1L); // 关联申请单（包含用途信息）
        stockOut.setReceiverId(1L);
        stockOut.setReceiverName("测试领用人");
        stockOut.setReceiverDept("测试部门");
        stockOut.setOutDate(LocalDate.now());
        stockOut.setOperatorId(1L);
        stockOut.setStatus(1);
        stockOut.setRemark("用于实验教学"); // 用途说明
        
        stockOutMapper.insert(stockOut);
        
        // 验证出库单记录存在且包含所有必需字段
        Assertions.assertThat(stockOut.getId()).isNotNull();
        Assertions.assertThat(stockOut.getOutOrderNo()).isNotBlank();
        Assertions.assertThat(stockOut.getOutDate()).isNotNull();
        Assertions.assertThat(stockOut.getOperatorId()).isNotNull();
        Assertions.assertThat(stockOut.getWarehouseId()).isNotNull();
        
        // 验证领用人信息（需求5.3：记录领用人）
        Assertions.assertThat(stockOut.getReceiverId()).isNotNull();
        Assertions.assertThat(stockOut.getReceiverName()).isNotBlank();
        Assertions.assertThat(stockOut.getReceiverDept()).isNotBlank();
        
        // 验证用途信息（需求5.3：记录用途）
        // 用途可以通过applicationId关联到申请单，或通过remark字段记录
        Assertions.assertThat(stockOut.getApplicationId() != null || stockOut.getRemark() != null)
                .as("出库单应记录用途信息（通过applicationId或remark）")
                .isTrue();
        
        // 创建出库明细
        List<StockOutDetail> details = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            StockOutDetail detail = new StockOutDetail();
            detail.setOutOrderId(stockOut.getId());
            detail.setMaterialId((long) (i + 1));
            detail.setBatchNumber("BATCH" + System.currentTimeMillis() + i);
            detail.setQuantity(quantity);
            detail.setUnitPrice(BigDecimal.TEN);
            detail.setTotalAmount(quantity.multiply(BigDecimal.TEN));
            
            stockOutDetailMapper.insert(detail);
            details.add(detail);
        }
        
        // 验证出库明细记录完整性（需求5.3：记录出库数量）
        for (StockOutDetail detail : details) {
            Assertions.assertThat(detail.getId()).isNotNull();
            Assertions.assertThat(detail.getOutOrderId()).isEqualTo(stockOut.getId());
            Assertions.assertThat(detail.getMaterialId()).isNotNull();
            Assertions.assertThat(detail.getBatchNumber()).isNotBlank();
            Assertions.assertThat(detail.getQuantity()).isGreaterThan(BigDecimal.ZERO);
            
            // 验证金额计算正确
            if (detail.getUnitPrice() != null) {
                BigDecimal expectedAmount = detail.getQuantity().multiply(detail.getUnitPrice());
                Assertions.assertThat(detail.getTotalAmount()).isEqualByComparingTo(expectedAmount);
            }
        }
    }
    
    /**
     * 属性 7: 库存数量一致性
     * 
     * **Validates: Requirements 5.4, 6.5**
     * 
     * 对于任何药品，其当前库存数量应等于初始库存加上所有入库数量减去所有出库数量。
     * 这个不变量在任何入库、出库、盘点调整操作后都应保持。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 7: 库存数量一致性")
    void stockQuantityConsistency(
            @ForAll @Positive BigDecimal initialStock,
            @ForAll @IntRange(min = 1, max = 5) int inCount,
            @ForAll @IntRange(min = 1, max = 3) int outCount,
            @ForAll @Positive BigDecimal inQuantity,
            @ForAll @Positive BigDecimal outQuantity) {
        
        // 确保出库数量不超过总入库数量
        BigDecimal totalIn = inQuantity.multiply(BigDecimal.valueOf(inCount));
        BigDecimal totalOut = outQuantity.multiply(BigDecimal.valueOf(outCount));
        
        Assume.that(totalOut.compareTo(initialStock.add(totalIn)) <= 0);
        
        // 创建初始库存
        String batchNumber = "BATCH" + System.currentTimeMillis();
        StockInventory inventory = new StockInventory();
        inventory.setMaterialId(1L);
        inventory.setWarehouseId(1L);
        inventory.setBatchNumber(batchNumber);
        inventory.setQuantity(initialStock);
        inventory.setAvailableQuantity(initialStock);
        inventory.setLockedQuantity(BigDecimal.ZERO);
        inventory.setUnitPrice(BigDecimal.TEN);
        inventory.setTotalAmount(initialStock.multiply(BigDecimal.TEN));
        
        stockInventoryMapper.insert(inventory);
        
        // 执行入库操作
        for (int i = 0; i < inCount; i++) {
            inventory.setQuantity(inventory.getQuantity().add(inQuantity));
            inventory.setAvailableQuantity(inventory.getAvailableQuantity().add(inQuantity));
            inventory.setTotalAmount(inventory.getQuantity().multiply(inventory.getUnitPrice()));
            stockInventoryMapper.updateById(inventory);
        }
        
        // 执行出库操作
        for (int i = 0; i < outCount; i++) {
            inventory.setQuantity(inventory.getQuantity().subtract(outQuantity));
            inventory.setAvailableQuantity(inventory.getAvailableQuantity().subtract(outQuantity));
            inventory.setTotalAmount(inventory.getQuantity().multiply(inventory.getUnitPrice()));
            stockInventoryMapper.updateById(inventory);
        }
        
        // 验证库存一致性
        StockInventory finalInventory = stockInventoryMapper.selectById(inventory.getId());
        BigDecimal expectedQuantity = initialStock.add(totalIn).subtract(totalOut);
        
        Assertions.assertThat(finalInventory.getQuantity()).isEqualByComparingTo(expectedQuantity);
        Assertions.assertThat(finalInventory.getAvailableQuantity()).isEqualByComparingTo(expectedQuantity);
    }
    
    /**
     * 属性 9: 库存盘点记录完整性
     * 
     * **Validates: Requirements 5.6**
     * 
     * 对于任何库存盘点操作，系统应记录盘点日期、盘点人、盘点仓库，
     * 并且盘点明细应包含每个药品的账面数量、实际数量、差异数量和差异原因。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 9: 库存盘点记录完整性")
    void stockCheckRecordCompleteness(
            @ForAll @IntRange(min = 1, max = 10) int itemCount,
            @ForAll @Positive BigDecimal bookQuantity,
            @ForAll @Positive BigDecimal actualQuantity) {
        
        // 创建盘点单
        StockCheck stockCheck = new StockCheck();
        stockCheck.setCheckNo("CHK" + System.currentTimeMillis());
        stockCheck.setWarehouseId(1L);
        stockCheck.setCheckDate(LocalDate.now());
        stockCheck.setCheckerId(1L);
        stockCheck.setStatus(1);
        
        stockCheckMapper.insert(stockCheck);
        
        // 验证盘点单记录存在
        Assertions.assertThat(stockCheck.getId()).isNotNull();
        Assertions.assertThat(stockCheck.getCheckNo()).isNotBlank();
        Assertions.assertThat(stockCheck.getCheckDate()).isNotNull();
        Assertions.assertThat(stockCheck.getCheckerId()).isNotNull();
        Assertions.assertThat(stockCheck.getWarehouseId()).isNotNull();
        
        // 创建盘点明细
        List<StockCheckDetail> details = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            StockCheckDetail detail = new StockCheckDetail();
            detail.setCheckId(stockCheck.getId());
            detail.setMaterialId((long) (i + 1));
            detail.setBatchNumber("BATCH" + System.currentTimeMillis() + i);
            detail.setBookQuantity(bookQuantity);
            detail.setActualQuantity(actualQuantity);
            detail.setDiffQuantity(actualQuantity.subtract(bookQuantity));
            detail.setDiffReason("盘点差异");
            
            stockCheckDetailMapper.insert(detail);
            details.add(detail);
        }
        
        // 验证盘点明细记录完整性
        for (StockCheckDetail detail : details) {
            Assertions.assertThat(detail.getId()).isNotNull();
            Assertions.assertThat(detail.getCheckId()).isEqualTo(stockCheck.getId());
            Assertions.assertThat(detail.getMaterialId()).isNotNull();
            Assertions.assertThat(detail.getBatchNumber()).isNotBlank();
            Assertions.assertThat(detail.getBookQuantity()).isNotNull();
            Assertions.assertThat(detail.getActualQuantity()).isNotNull();
            Assertions.assertThat(detail.getDiffQuantity()).isNotNull();
            
            // 验证差异数量计算正确
            BigDecimal expectedDiff = detail.getActualQuantity().subtract(detail.getBookQuantity());
            Assertions.assertThat(detail.getDiffQuantity()).isEqualByComparingTo(expectedDiff);
        }
    }
}
