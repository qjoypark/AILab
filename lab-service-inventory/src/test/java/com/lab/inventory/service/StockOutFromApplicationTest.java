package com.lab.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.common.exception.BusinessException;
import com.lab.inventory.client.ApprovalClient;
import com.lab.inventory.dto.MaterialApplicationDTO;
import com.lab.inventory.dto.MaterialApplicationItemDTO;
import com.lab.inventory.entity.StockOut;
import com.lab.inventory.entity.StockOutDetail;
import com.lab.inventory.mapper.StockOutDetailMapper;
import com.lab.inventory.mapper.StockOutMapper;
import com.lab.inventory.service.impl.StockOutServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 根据申请单创建出库单测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("根据申请单创建出库单测试")
class StockOutFromApplicationTest {
    
    @Mock
    private StockOutMapper stockOutMapper;
    
    @Mock
    private StockOutDetailMapper stockOutDetailMapper;
    
    @Mock
    private ApprovalClient approvalClient;
    
    @InjectMocks
    private StockOutServiceImpl stockOutService;
    
    private MaterialApplicationDTO mockApplication;
    
    @BeforeEach
    void setUp() {
        // 准备模拟申请单数据
        mockApplication = new MaterialApplicationDTO();
        mockApplication.setId(1L);
        mockApplication.setApplicationNo("APP20240101000001");
        mockApplication.setApplicantId(100L);
        mockApplication.setApplicantName("张三");
        mockApplication.setApplicantDept("实验室A");
        mockApplication.setApplicationType(1); // 普通领用
        mockApplication.setUsagePurpose("实验使用");
        mockApplication.setUsageLocation("实验室101");
        mockApplication.setStatus(3); // 审批通过
        
        // 准备申请明细
        List<MaterialApplicationItemDTO> items = new ArrayList<>();
        
        MaterialApplicationItemDTO item1 = new MaterialApplicationItemDTO();
        item1.setId(1L);
        item1.setMaterialId(1001L);
        item1.setMaterialName("试剂A");
        item1.setSpecification("500ml");
        item1.setUnit("瓶");
        item1.setApplyQuantity(new BigDecimal("5"));
        item1.setApprovedQuantity(new BigDecimal("5"));
        items.add(item1);
        
        MaterialApplicationItemDTO item2 = new MaterialApplicationItemDTO();
        item2.setId(2L);
        item2.setMaterialId(1002L);
        item2.setMaterialName("试剂B");
        item2.setSpecification("1L");
        item2.setUnit("瓶");
        item2.setApplyQuantity(new BigDecimal("3"));
        item2.setApprovedQuantity(new BigDecimal("2")); // 批准数量小于申请数量
        items.add(item2);
        
        mockApplication.setItems(items);
    }
    
    @Test
    @DisplayName("审批通过后应成功创建出库单")
    void shouldCreateStockOutWhenApplicationApproved() {
        // Given
        when(approvalClient.getApplicationDetail(1L)).thenReturn(mockApplication);
        when(stockOutMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(stockOutMapper.insert(any(StockOut.class))).thenAnswer(invocation -> {
            StockOut stockOut = invocation.getArgument(0);
            stockOut.setId(1001L);
            return 1;
        });
        when(stockOutDetailMapper.insert(any(StockOutDetail.class))).thenReturn(1);
        
        // When
        StockOut result = stockOutService.createStockOutFromApplication(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1001L);
        assertThat(result.getApplicationId()).isEqualTo(1L);
        assertThat(result.getReceiverId()).isEqualTo(100L);
        assertThat(result.getReceiverName()).isEqualTo("张三");
        assertThat(result.getReceiverDept()).isEqualTo("实验室A");
        assertThat(result.getOutType()).isEqualTo(1); // 领用出库
        assertThat(result.getStatus()).isEqualTo(1); // 待出库
        
        // 验证插入了出库单
        verify(stockOutMapper, times(1)).insert(any(StockOut.class));
        
        // 验证插入了2条出库明细
        verify(stockOutDetailMapper, times(2)).insert(any(StockOutDetail.class));
    }
    
    @Test
    @DisplayName("出库明细应使用批准数量而非申请数量")
    void shouldUseApprovedQuantityInStockOutDetail() {
        // Given
        when(approvalClient.getApplicationDetail(1L)).thenReturn(mockApplication);
        when(stockOutMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(stockOutMapper.insert(any(StockOut.class))).thenAnswer(invocation -> {
            StockOut stockOut = invocation.getArgument(0);
            stockOut.setId(1001L);
            return 1;
        });
        
        List<StockOutDetail> capturedDetails = new ArrayList<>();
        when(stockOutDetailMapper.insert(any(StockOutDetail.class))).thenAnswer(invocation -> {
            capturedDetails.add(invocation.getArgument(0));
            return 1;
        });
        
        // When
        stockOutService.createStockOutFromApplication(1L);
        
        // Then
        assertThat(capturedDetails).hasSize(2);
        
        // 第一个明细：批准数量等于申请数量
        StockOutDetail detail1 = capturedDetails.get(0);
        assertThat(detail1.getMaterialId()).isEqualTo(1001L);
        assertThat(detail1.getQuantity()).isEqualByComparingTo(new BigDecimal("5"));
        
        // 第二个明细：批准数量小于申请数量，应使用批准数量
        StockOutDetail detail2 = capturedDetails.get(1);
        assertThat(detail2.getMaterialId()).isEqualTo(1002L);
        assertThat(detail2.getQuantity()).isEqualByComparingTo(new BigDecimal("2"));
    }
    
    @Test
    @DisplayName("申请单不存在时应抛出异常")
    void shouldThrowExceptionWhenApplicationNotFound() {
        // Given
        when(approvalClient.getApplicationDetail(999L)).thenReturn(null);
        
        // When & Then
        assertThatThrownBy(() -> stockOutService.createStockOutFromApplication(999L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("申请单不存在");
        
        verify(stockOutMapper, never()).insert(any(StockOut.class));
    }
    
    @Test
    @DisplayName("申请单状态不是审批通过时应抛出异常")
    void shouldThrowExceptionWhenApplicationNotApproved() {
        // Given
        mockApplication.setStatus(2); // 审批中
        when(approvalClient.getApplicationDetail(1L)).thenReturn(mockApplication);
        
        // When & Then
        assertThatThrownBy(() -> stockOutService.createStockOutFromApplication(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("申请单状态不是审批通过，无法创建出库单");
        
        verify(stockOutMapper, never()).insert(any(StockOut.class));
    }
    
    @Test
    @DisplayName("申请单已创建出库单时应抛出异常")
    void shouldThrowExceptionWhenStockOutAlreadyExists() {
        // Given
        when(approvalClient.getApplicationDetail(1L)).thenReturn(mockApplication);
        when(stockOutMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        
        // When & Then
        assertThatThrownBy(() -> stockOutService.createStockOutFromApplication(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("该申请单已创建出库单");
        
        verify(stockOutMapper, never()).insert(any(StockOut.class));
    }
    
    @Test
    @DisplayName("出库单号应包含日期和序号")
    void shouldGenerateOutOrderNoWithDateAndSequence() {
        // Given
        when(approvalClient.getApplicationDetail(1L)).thenReturn(mockApplication);
        when(stockOutMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(stockOutMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        
        StockOut[] capturedStockOut = new StockOut[1];
        when(stockOutMapper.insert(any(StockOut.class))).thenAnswer(invocation -> {
            capturedStockOut[0] = invocation.getArgument(0);
            capturedStockOut[0].setId(1001L);
            return 1;
        });
        when(stockOutDetailMapper.insert(any(StockOutDetail.class))).thenReturn(1);
        
        // When
        stockOutService.createStockOutFromApplication(1L);
        
        // Then
        String outOrderNo = capturedStockOut[0].getOutOrderNo();
        assertThat(outOrderNo).isNotNull();
        assertThat(outOrderNo).startsWith("OUT");
        assertThat(outOrderNo).hasSize(15); // OUT + 8位日期 + 4位序号
        
        // 验证日期部分
        String today = LocalDate.now().toString().replace("-", "");
        assertThat(outOrderNo).contains(today);
    }
    
    @Test
    @DisplayName("出库单备注应包含申请单号")
    void shouldIncludeApplicationNoInRemark() {
        // Given
        when(approvalClient.getApplicationDetail(1L)).thenReturn(mockApplication);
        when(stockOutMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        
        StockOut[] capturedStockOut = new StockOut[1];
        when(stockOutMapper.insert(any(StockOut.class))).thenAnswer(invocation -> {
            capturedStockOut[0] = invocation.getArgument(0);
            capturedStockOut[0].setId(1001L);
            return 1;
        });
        when(stockOutDetailMapper.insert(any(StockOutDetail.class))).thenReturn(1);
        
        // When
        stockOutService.createStockOutFromApplication(1L);
        
        // Then
        String remark = capturedStockOut[0].getRemark();
        assertThat(remark).contains("APP20240101000001");
        assertThat(remark).contains("自动创建");
    }
}
