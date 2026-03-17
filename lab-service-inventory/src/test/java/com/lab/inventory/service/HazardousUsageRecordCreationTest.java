package com.lab.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.inventory.client.ApprovalClient;
import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.dto.HazardousUsageRecordDTO;
import com.lab.inventory.dto.MaterialApplicationDTO;
import com.lab.inventory.dto.MaterialInfo;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.entity.StockOut;
import com.lab.inventory.entity.StockOutDetail;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.mapper.StockOutDetailMapper;
import com.lab.inventory.mapper.StockOutMapper;
import com.lab.inventory.service.impl.StockOutServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 危化品使用记录创建测试
 * 
 * **Validates: Requirements 6.6**
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("危化品使用记录创建测试")
class HazardousUsageRecordCreationTest {
    
    @Mock
    private StockOutMapper stockOutMapper;
    
    @Mock
    private StockOutDetailMapper stockOutDetailMapper;
    
    @Mock
    private StockInventoryMapper stockInventoryMapper;
    
    @Mock
    private ApprovalClient approvalClient;
    
    @Mock
    private MaterialClient materialClient;
    
    @InjectMocks
    private StockOutServiceImpl stockOutService;
    
    private StockOut stockOut;
    private MaterialApplicationDTO application;
    private List<StockOutDetail> details;
    
    @BeforeEach
    void setUp() {
        // 准备出库单数据
        stockOut = new StockOut();
        stockOut.setId(1L);
        stockOut.setOutOrderNo("OUT202401010001");
        stockOut.setApplicationId(100L);
        stockOut.setWarehouseId(1L);
        stockOut.setReceiverId(10L);
        stockOut.setReceiverName("张三");
        stockOut.setReceiverDept("化学实验室");
        stockOut.setOutDate(LocalDate.now());
        stockOut.setStatus(1); // 待出库
        
        // 准备申请单数据
        application = new MaterialApplicationDTO();
        application.setId(100L);
        application.setApplicationNo("APP202401010001");
        application.setApplicantId(10L);
        application.setApplicantName("张三");
        application.setApplicantDept("化学实验室");
        application.setUsageLocation("实验室A101");
        application.setUsagePurpose("化学实验");
        application.setStatus(3); // 审批通过
        
        // 准备出库明细数据
        StockOutDetail detail1 = new StockOutDetail();
        detail1.setId(1L);
        detail1.setOutOrderId(1L);
        detail1.setMaterialId(1001L);
        detail1.setQuantity(new BigDecimal("2.5"));
        
        StockOutDetail detail2 = new StockOutDetail();
        detail2.setId(2L);
        detail2.setOutOrderId(1L);
        detail2.setMaterialId(1002L);
        detail2.setQuantity(new BigDecimal("1.0"));
        
        details = Arrays.asList(detail1, detail2);
    }
    
    @Test
    @DisplayName("危化品出库时应自动创建使用记录")
    void shouldCreateUsageRecordForHazardousMaterial() {
        // Given: 准备危化品信息（materialType=3）
        MaterialInfo hazardousMaterial = new MaterialInfo();
        hazardousMaterial.setId(1001L);
        hazardousMaterial.setMaterialName("浓硫酸");
        hazardousMaterial.setMaterialType(3); // 危化品
        hazardousMaterial.setIsControlled(0);
        
        // 准备库存数据
        StockInventory inventory = new StockInventory();
        inventory.setId(1L);
        inventory.setMaterialId(1001L);
        inventory.setWarehouseId(1L);
        inventory.setQuantity(new BigDecimal("10.0"));
        inventory.setAvailableQuantity(new BigDecimal("10.0"));
        
        // Mock行为
        when(stockOutMapper.selectById(1L)).thenReturn(stockOut);
        when(stockOutDetailMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(details);
        when(stockInventoryMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(inventory));
        when(approvalClient.getApplicationDetail(100L)).thenReturn(application);
        when(materialClient.getMaterialInfo(1001L)).thenReturn(hazardousMaterial);
        
        // When: 确认出库
        stockOutService.confirmStockOut(1L);
        
        // Then: 验证创建了危化品使用记录
        ArgumentCaptor<HazardousUsageRecordDTO> recordCaptor = 
            ArgumentCaptor.forClass(HazardousUsageRecordDTO.class);
        verify(approvalClient).createHazardousUsageRecord(recordCaptor.capture());
        
        HazardousUsageRecordDTO capturedRecord = recordCaptor.getValue();
        assertThat(capturedRecord.getApplicationId()).isEqualTo(100L);
        assertThat(capturedRecord.getApplicationNo()).isEqualTo("APP202401010001");
        assertThat(capturedRecord.getMaterialId()).isEqualTo(1001L);
        assertThat(capturedRecord.getMaterialName()).isEqualTo("浓硫酸");
        assertThat(capturedRecord.getUserId()).isEqualTo(10L);
        assertThat(capturedRecord.getUserName()).isEqualTo("张三");
        assertThat(capturedRecord.getReceivedQuantity()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(capturedRecord.getUsageDate()).isEqualTo(LocalDate.now());
        assertThat(capturedRecord.getUsageLocation()).isEqualTo("实验室A101");
        assertThat(capturedRecord.getUsagePurpose()).isEqualTo("化学实验");
    }
    
    @Test
    @DisplayName("易制毒物品出库时应自动创建使用记录")
    void shouldCreateUsageRecordForControlledMaterial() {
        // Given: 准备易制毒物品信息（isControlled=1）
        MaterialInfo controlledMaterial = new MaterialInfo();
        controlledMaterial.setId(1001L);
        controlledMaterial.setMaterialName("高锰酸钾");
        controlledMaterial.setMaterialType(2); // 试剂
        controlledMaterial.setIsControlled(1); // 易制毒
        
        // 准备库存数据
        StockInventory inventory = new StockInventory();
        inventory.setId(1L);
        inventory.setMaterialId(1001L);
        inventory.setWarehouseId(1L);
        inventory.setQuantity(new BigDecimal("10.0"));
        inventory.setAvailableQuantity(new BigDecimal("10.0"));
        
        // Mock行为
        when(stockOutMapper.selectById(1L)).thenReturn(stockOut);
        when(stockOutDetailMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(details);
        when(stockInventoryMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(inventory));
        when(approvalClient.getApplicationDetail(100L)).thenReturn(application);
        when(materialClient.getMaterialInfo(1001L)).thenReturn(controlledMaterial);
        
        // When: 确认出库
        stockOutService.confirmStockOut(1L);
        
        // Then: 验证创建了危化品使用记录
        ArgumentCaptor<HazardousUsageRecordDTO> recordCaptor = 
            ArgumentCaptor.forClass(HazardousUsageRecordDTO.class);
        verify(approvalClient).createHazardousUsageRecord(recordCaptor.capture());
        
        HazardousUsageRecordDTO capturedRecord = recordCaptor.getValue();
        assertThat(capturedRecord.getMaterialId()).isEqualTo(1001L);
        assertThat(capturedRecord.getMaterialName()).isEqualTo("高锰酸钾");
    }
    
    @Test
    @DisplayName("普通耗材出库时不应创建使用记录")
    void shouldNotCreateUsageRecordForNormalMaterial() {
        // Given: 准备普通耗材信息
        MaterialInfo normalMaterial = new MaterialInfo();
        normalMaterial.setId(1001L);
        normalMaterial.setMaterialName("试管");
        normalMaterial.setMaterialType(1); // 耗材
        normalMaterial.setIsControlled(0);
        
        // 准备库存数据
        StockInventory inventory = new StockInventory();
        inventory.setId(1L);
        inventory.setMaterialId(1001L);
        inventory.setWarehouseId(1L);
        inventory.setQuantity(new BigDecimal("100.0"));
        inventory.setAvailableQuantity(new BigDecimal("100.0"));
        
        // Mock行为
        when(stockOutMapper.selectById(1L)).thenReturn(stockOut);
        when(stockOutDetailMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(details);
        when(stockInventoryMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(inventory));
        when(approvalClient.getApplicationDetail(100L)).thenReturn(application);
        when(materialClient.getMaterialInfo(1001L)).thenReturn(normalMaterial);
        
        // When: 确认出库
        stockOutService.confirmStockOut(1L);
        
        // Then: 验证没有创建危化品使用记录
        verify(approvalClient, never()).createHazardousUsageRecord(any());
    }
    
    @Test
    @DisplayName("出库单包含多个危化品时应为每个危化品创建使用记录")
    void shouldCreateMultipleUsageRecordsForMultipleHazardousMaterials() {
        // Given: 准备两个危化品信息
        MaterialInfo hazardousMaterial1 = new MaterialInfo();
        hazardousMaterial1.setId(1001L);
        hazardousMaterial1.setMaterialName("浓硫酸");
        hazardousMaterial1.setMaterialType(3);
        hazardousMaterial1.setIsControlled(0);
        
        MaterialInfo hazardousMaterial2 = new MaterialInfo();
        hazardousMaterial2.setId(1002L);
        hazardousMaterial2.setMaterialName("盐酸");
        hazardousMaterial2.setMaterialType(3);
        hazardousMaterial2.setIsControlled(0);
        
        // 准备库存数据
        StockInventory inventory1 = new StockInventory();
        inventory1.setId(1L);
        inventory1.setMaterialId(1001L);
        inventory1.setWarehouseId(1L);
        inventory1.setQuantity(new BigDecimal("10.0"));
        inventory1.setAvailableQuantity(new BigDecimal("10.0"));
        
        StockInventory inventory2 = new StockInventory();
        inventory2.setId(2L);
        inventory2.setMaterialId(1002L);
        inventory2.setWarehouseId(1L);
        inventory2.setQuantity(new BigDecimal("5.0"));
        inventory2.setAvailableQuantity(new BigDecimal("5.0"));
        
        // Mock行为
        when(stockOutMapper.selectById(1L)).thenReturn(stockOut);
        when(stockOutDetailMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(details);
        when(stockInventoryMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenAnswer(invocation -> {
                LambdaQueryWrapper<StockInventory> wrapper = invocation.getArgument(0);
                // 根据materialId返回不同的库存
                return Arrays.asList(inventory1); // 简化处理
            });
        when(approvalClient.getApplicationDetail(100L)).thenReturn(application);
        when(materialClient.getMaterialInfo(1001L)).thenReturn(hazardousMaterial1);
        when(materialClient.getMaterialInfo(1002L)).thenReturn(hazardousMaterial2);
        
        // When: 确认出库
        stockOutService.confirmStockOut(1L);
        
        // Then: 验证为两个危化品都创建了使用记录
        verify(approvalClient, times(2)).createHazardousUsageRecord(any(HazardousUsageRecordDTO.class));
    }
    
    @Test
    @DisplayName("无法获取药品信息时应跳过创建使用记录但不影响出库")
    void shouldSkipUsageRecordCreationWhenMaterialInfoNotAvailable() {
        // Given: 药品信息获取失败
        StockInventory inventory = new StockInventory();
        inventory.setId(1L);
        inventory.setMaterialId(1001L);
        inventory.setWarehouseId(1L);
        inventory.setQuantity(new BigDecimal("10.0"));
        inventory.setAvailableQuantity(new BigDecimal("10.0"));
        
        // Mock行为
        when(stockOutMapper.selectById(1L)).thenReturn(stockOut);
        when(stockOutDetailMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(details);
        when(stockInventoryMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(inventory));
        when(approvalClient.getApplicationDetail(100L)).thenReturn(application);
        when(materialClient.getMaterialInfo(1001L)).thenReturn(null); // 获取失败
        
        // When: 确认出库
        stockOutService.confirmStockOut(1L);
        
        // Then: 验证出库成功，但没有创建使用记录
        verify(stockOutMapper).updateById(any(StockOut.class));
        verify(approvalClient, never()).createHazardousUsageRecord(any());
    }
}
