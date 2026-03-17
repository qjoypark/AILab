package com.lab.approval.property;

import com.lab.approval.client.InventoryClient;
import com.lab.approval.dto.HazardousReturnRequest;
import com.lab.approval.entity.HazardousUsageRecord;
import com.lab.approval.mapper.HazardousUsageRecordMapper;
import com.lab.approval.service.impl.HazardousUsageRecordServiceImpl;
import com.lab.common.exception.BusinessException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 危化品使用记录完整性属性测试
 * 
 * 使用jqwik框架进行基于属性的测试，每个属性测试运行100次迭代
 */
@ExtendWith(MockitoExtension.class)
public class HazardousUsageRecordPropertyTest {
    
    @Mock
    private HazardousUsageRecordMapper usageRecordMapper;
    
    @Mock
    private InventoryClient inventoryClient;
    
    @InjectMocks
    private HazardousUsageRecordServiceImpl usageRecordService;
    
    /**
     * 属性 14: 危化品使用记录完整性 - 创建记录时包含所有必需字段
     * 
     * **Validates: Requirements 6.6**
     * 
     * 对于任何危化品出库操作，系统应创建危化品使用记录，记录领用数量、使用人、
     * 使用日期、使用地点、使用目的。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 14: 危化品使用记录完整性")
    void hazardousUsageRecordShouldContainAllRequiredFields(
            @ForAll @IntRange(min = 1, max = 10000) long applicationId,
            @ForAll @IntRange(min = 1, max = 10000) long materialId,
            @ForAll @IntRange(min = 1, max = 10000) long userId,
            @ForAll @StringLength(min = 1, max = 50) String userName,
            @ForAll @BigRange(min = "0.01", max = "1000.00") String receivedQuantityStr,
            @ForAll @StringLength(min = 1, max = 200) String usageLocation,
            @ForAll @StringLength(min = 1, max = 500) String usagePurpose) {
        
        BigDecimal receivedQuantity = new BigDecimal(receivedQuantityStr);
        LocalDate usageDate = LocalDate.now();
        
        // 创建危化品使用记录
        HazardousUsageRecord record = new HazardousUsageRecord();
        record.setApplicationId(applicationId);
        record.setMaterialId(materialId);
        record.setUserId(userId);
        record.setUserName(userName);
        record.setReceivedQuantity(receivedQuantity);
        record.setUsageDate(usageDate);
        record.setUsageLocation(usageLocation);
        record.setUsagePurpose(usagePurpose);
        record.setStatus(1); // 使用中
        
        // Mock insert操作
        doAnswer(invocation -> {
            HazardousUsageRecord arg = invocation.getArgument(0);
            arg.setId(1L);
            return 1;
        }).when(usageRecordMapper).insert(any(HazardousUsageRecord.class));
        
        // 执行创建
        HazardousUsageRecord created = usageRecordService.createUsageRecord(record);
        
        // 验证记录包含所有必需字段
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getApplicationId()).isEqualTo(applicationId);
        assertThat(created.getMaterialId()).isEqualTo(materialId);
        assertThat(created.getUserId()).isEqualTo(userId);
        assertThat(created.getUserName()).isEqualTo(userName);
        assertThat(created.getReceivedQuantity()).isEqualByComparingTo(receivedQuantity);
        assertThat(created.getUsageDate()).isEqualTo(usageDate);
        assertThat(created.getUsageLocation()).isEqualTo(usageLocation);
        assertThat(created.getUsagePurpose()).isEqualTo(usagePurpose);
        assertThat(created.getStatus()).isEqualTo(1); // 使用中
        
        // 验证调用了insert方法
        verify(usageRecordMapper, times(1)).insert(any(HazardousUsageRecord.class));
        
        // 重置mock以便下次迭代
        reset(usageRecordMapper);
    }
    
    /**
     * 属性 14: 危化品使用记录完整性 - 归还时记录实际使用量和剩余量
     * 
     * **Validates: Requirements 6.6**
     * 
     * 当危化品使用完毕后，系统应要求用户记录实际使用量和剩余量（归还量+废弃量）。
     * 验证：实际使用量 + 归还量 + 废弃量 = 领用数量
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 14: 危化品使用记录完整性")
    void hazardousReturnShouldRecordActualUsageAndRemaining(
            @ForAll @BigRange(min = "1.00", max = "100.00") String receivedQuantityStr,
            @ForAll @BigRange(min = "0.00", max = "1.00") String usageRatio,
            @ForAll @BigRange(min = "0.00", max = "1.00") String returnRatio) {
        
        BigDecimal receivedQuantity = new BigDecimal(receivedQuantityStr);
        BigDecimal usageRatioDecimal = new BigDecimal(usageRatio);
        BigDecimal returnRatioDecimal = new BigDecimal(returnRatio);
        
        // 计算实际使用量、归还量、废弃量，确保总和等于领用数量
        BigDecimal actualUsed = receivedQuantity.multiply(usageRatioDecimal).setScale(2, BigDecimal.ROUND_DOWN);
        BigDecimal remaining = receivedQuantity.subtract(actualUsed);
        BigDecimal returned = remaining.multiply(returnRatioDecimal).setScale(2, BigDecimal.ROUND_DOWN);
        BigDecimal waste = remaining.subtract(returned);
        
        // 调整以确保总和精确等于领用数量
        BigDecimal total = actualUsed.add(returned).add(waste);
        if (total.compareTo(receivedQuantity) != 0) {
            BigDecimal diff = receivedQuantity.subtract(total);
            actualUsed = actualUsed.add(diff);
        }
        
        // 创建使用记录
        HazardousUsageRecord record = new HazardousUsageRecord();
        record.setId(1L);
        record.setApplicationId(100L);
        record.setMaterialId(200L);
        record.setUserId(1L);
        record.setUserName("测试用户");
        record.setReceivedQuantity(receivedQuantity);
        record.setUsageDate(LocalDate.now());
        record.setUsageLocation("实验室A");
        record.setUsagePurpose("化学实验");
        record.setStatus(1); // 使用中
        
        // Mock查询记录
        when(usageRecordMapper.selectById(1L)).thenReturn(record);
        
        // Mock库存归还
        when(inventoryClient.returnHazardousMaterial(anyLong(), any(BigDecimal.class), anyString()))
            .thenReturn(true);
        
        // Mock更新操作
        when(usageRecordMapper.updateById(any(HazardousUsageRecord.class))).thenReturn(1);
        
        // 创建归还请求
        HazardousReturnRequest returnRequest = new HazardousReturnRequest();
        returnRequest.setActualUsedQuantity(actualUsed);
        returnRequest.setReturnedQuantity(returned);
        returnRequest.setWasteQuantity(waste);
        returnRequest.setRemark("实验完成");
        
        // 执行归还
        HazardousUsageRecord returnedRecord = usageRecordService.returnHazardousMaterial(1L, returnRequest);
        
        // 验证归还记录包含实际使用量和剩余量
        assertThat(returnedRecord).isNotNull();
        assertThat(returnedRecord.getActualUsedQuantity()).isEqualByComparingTo(actualUsed);
        assertThat(returnedRecord.getReturnedQuantity()).isEqualByComparingTo(returned);
        assertThat(returnedRecord.getWasteQuantity()).isEqualByComparingTo(waste);
        assertThat(returnedRecord.getReturnDate()).isNotNull();
        assertThat(returnedRecord.getStatus()).isEqualTo(2); // 已归还
        
        // 验证数量关系：实际使用量 + 归还量 + 废弃量 = 领用数量
        BigDecimal sum = returnedRecord.getActualUsedQuantity()
            .add(returnedRecord.getReturnedQuantity())
            .add(returnedRecord.getWasteQuantity());
        assertThat(sum).isEqualByComparingTo(receivedQuantity);
        
        // 验证调用了更新方法
        verify(usageRecordMapper, times(1)).updateById(any(HazardousUsageRecord.class));
        
        // 如果有归还量，验证调用了库存归还
        if (returned.compareTo(BigDecimal.ZERO) > 0) {
            verify(inventoryClient, times(1))
                .returnHazardousMaterial(anyLong(), any(BigDecimal.class), anyString());
        }
        
        // 重置mock以便下次迭代
        reset(usageRecordMapper, inventoryClient);
    }
    
    /**
     * 属性 14: 危化品使用记录完整性 - 归还数量不匹配时应拒绝
     * 
     * **Validates: Requirements 6.6**
     * 
     * 当归还时，如果实际使用量 + 归还量 + 废弃量 ≠ 领用数量，系统应拒绝归还操作。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 14: 危化品使用记录完整性")
    void hazardousReturnShouldRejectWhenQuantityMismatch(
            @ForAll @BigRange(min = "10.00", max = "100.00") String receivedQuantityStr,
            @ForAll @BigRange(min = "0.01", max = "5.00") String mismatchAmountStr) {
        
        BigDecimal receivedQuantity = new BigDecimal(receivedQuantityStr);
        BigDecimal mismatchAmount = new BigDecimal(mismatchAmountStr);
        
        // 创建不匹配的数量：总和不等于领用数量
        BigDecimal actualUsed = receivedQuantity.multiply(new BigDecimal("0.5")).setScale(2, BigDecimal.ROUND_DOWN);
        BigDecimal returned = receivedQuantity.multiply(new BigDecimal("0.3")).setScale(2, BigDecimal.ROUND_DOWN);
        BigDecimal waste = receivedQuantity.multiply(new BigDecimal("0.2")).setScale(2, BigDecimal.ROUND_DOWN);
        
        // 故意添加不匹配量
        actualUsed = actualUsed.add(mismatchAmount);
        
        // 创建使用记录
        HazardousUsageRecord record = new HazardousUsageRecord();
        record.setId(1L);
        record.setApplicationId(100L);
        record.setMaterialId(200L);
        record.setUserId(1L);
        record.setUserName("测试用户");
        record.setReceivedQuantity(receivedQuantity);
        record.setUsageDate(LocalDate.now());
        record.setUsageLocation("实验室A");
        record.setUsagePurpose("化学实验");
        record.setStatus(1); // 使用中
        
        // Mock查询记录
        when(usageRecordMapper.selectById(1L)).thenReturn(record);
        
        // 创建归还请求（数量不匹配）
        HazardousReturnRequest returnRequest = new HazardousReturnRequest();
        returnRequest.setActualUsedQuantity(actualUsed);
        returnRequest.setReturnedQuantity(returned);
        returnRequest.setWasteQuantity(waste);
        
        // 执行并验证异常
        assertThatThrownBy(() -> {
            usageRecordService.returnHazardousMaterial(1L, returnRequest);
        })
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("数量不匹配");
        
        // 验证没有调用更新方法
        verify(usageRecordMapper, never()).updateById(any(HazardousUsageRecord.class));
        
        // 验证没有调用库存归还
        verify(inventoryClient, never())
            .returnHazardousMaterial(anyLong(), any(BigDecimal.class), anyString());
        
        // 重置mock以便下次迭代
        reset(usageRecordMapper, inventoryClient);
    }
    
    /**
     * 属性 14: 危化品使用记录完整性 - 重复归还应被拒绝
     * 
     * **Validates: Requirements 6.6**
     * 
     * 当使用记录已经归还（status=2）时，不应允许重复归还。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 14: 危化品使用记录完整性")
    void hazardousReturnShouldRejectDuplicateReturn(
            @ForAll @BigRange(min = "1.00", max = "100.00") String receivedQuantityStr) {
        
        BigDecimal receivedQuantity = new BigDecimal(receivedQuantityStr);
        
        // 创建已归还的使用记录
        HazardousUsageRecord record = new HazardousUsageRecord();
        record.setId(1L);
        record.setApplicationId(100L);
        record.setMaterialId(200L);
        record.setUserId(1L);
        record.setUserName("测试用户");
        record.setReceivedQuantity(receivedQuantity);
        record.setActualUsedQuantity(receivedQuantity.multiply(new BigDecimal("0.6")));
        record.setReturnedQuantity(receivedQuantity.multiply(new BigDecimal("0.3")));
        record.setWasteQuantity(receivedQuantity.multiply(new BigDecimal("0.1")));
        record.setUsageDate(LocalDate.now().minusDays(5));
        record.setReturnDate(LocalDate.now().minusDays(1));
        record.setUsageLocation("实验室A");
        record.setUsagePurpose("化学实验");
        record.setStatus(2); // 已归还
        
        // Mock查询记录
        when(usageRecordMapper.selectById(1L)).thenReturn(record);
        
        // 创建归还请求
        HazardousReturnRequest returnRequest = new HazardousReturnRequest();
        returnRequest.setActualUsedQuantity(receivedQuantity.multiply(new BigDecimal("0.6")));
        returnRequest.setReturnedQuantity(receivedQuantity.multiply(new BigDecimal("0.3")));
        returnRequest.setWasteQuantity(receivedQuantity.multiply(new BigDecimal("0.1")));
        
        // 执行并验证异常
        assertThatThrownBy(() -> {
            usageRecordService.returnHazardousMaterial(1L, returnRequest);
        })
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已归还");
        
        // 验证没有调用更新方法
        verify(usageRecordMapper, never()).updateById(any(HazardousUsageRecord.class));
        
        // 验证没有调用库存归还
        verify(inventoryClient, never())
            .returnHazardousMaterial(anyLong(), any(BigDecimal.class), anyString());
        
        // 重置mock以便下次迭代
        reset(usageRecordMapper, inventoryClient);
    }
    
    /**
     * 属性 14: 危化品使用记录完整性 - 全部使用完毕的情况
     * 
     * **Validates: Requirements 6.6**
     * 
     * 当危化品全部使用完毕（无归还、无废弃）时，系统应正确记录。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 14: 危化品使用记录完整性")
    void hazardousReturnShouldHandleFullyUsedCase(
            @ForAll @BigRange(min = "1.00", max = "100.00") String receivedQuantityStr) {
        
        BigDecimal receivedQuantity = new BigDecimal(receivedQuantityStr);
        
        // 创建使用记录
        HazardousUsageRecord record = new HazardousUsageRecord();
        record.setId(1L);
        record.setApplicationId(100L);
        record.setMaterialId(200L);
        record.setUserId(1L);
        record.setUserName("测试用户");
        record.setReceivedQuantity(receivedQuantity);
        record.setUsageDate(LocalDate.now());
        record.setUsageLocation("实验室A");
        record.setUsagePurpose("化学实验");
        record.setStatus(1); // 使用中
        
        // Mock查询记录
        when(usageRecordMapper.selectById(1L)).thenReturn(record);
        
        // Mock更新操作
        when(usageRecordMapper.updateById(any(HazardousUsageRecord.class))).thenReturn(1);
        
        // 创建归还请求（全部使用完毕）
        HazardousReturnRequest returnRequest = new HazardousReturnRequest();
        returnRequest.setActualUsedQuantity(receivedQuantity);
        returnRequest.setReturnedQuantity(BigDecimal.ZERO);
        returnRequest.setWasteQuantity(BigDecimal.ZERO);
        returnRequest.setRemark("全部使用完毕");
        
        // 执行归还
        HazardousUsageRecord returnedRecord = usageRecordService.returnHazardousMaterial(1L, returnRequest);
        
        // 验证记录正确
        assertThat(returnedRecord).isNotNull();
        assertThat(returnedRecord.getActualUsedQuantity()).isEqualByComparingTo(receivedQuantity);
        assertThat(returnedRecord.getReturnedQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(returnedRecord.getWasteQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(returnedRecord.getStatus()).isEqualTo(2); // 已归还
        
        // 验证没有调用库存归还（因为归还量为0）
        verify(inventoryClient, never())
            .returnHazardousMaterial(anyLong(), any(BigDecimal.class), anyString());
        
        // 重置mock以便下次迭代
        reset(usageRecordMapper, inventoryClient);
    }
    
    /**
     * 属性 14: 危化品使用记录完整性 - 全部废弃的情况
     * 
     * **Validates: Requirements 6.6**
     * 
     * 当危化品全部废弃（无使用、无归还）时，系统应正确记录。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 14: 危化品使用记录完整性")
    void hazardousReturnShouldHandleFullyWastedCase(
            @ForAll @BigRange(min = "1.00", max = "100.00") String receivedQuantityStr) {
        
        BigDecimal receivedQuantity = new BigDecimal(receivedQuantityStr);
        
        // 创建使用记录
        HazardousUsageRecord record = new HazardousUsageRecord();
        record.setId(1L);
        record.setApplicationId(100L);
        record.setMaterialId(200L);
        record.setUserId(1L);
        record.setUserName("测试用户");
        record.setReceivedQuantity(receivedQuantity);
        record.setUsageDate(LocalDate.now());
        record.setUsageLocation("实验室A");
        record.setUsagePurpose("化学实验");
        record.setStatus(1); // 使用中
        
        // Mock查询记录
        when(usageRecordMapper.selectById(1L)).thenReturn(record);
        
        // Mock更新操作
        when(usageRecordMapper.updateById(any(HazardousUsageRecord.class))).thenReturn(1);
        
        // 创建归还请求（全部废弃）
        HazardousReturnRequest returnRequest = new HazardousReturnRequest();
        returnRequest.setActualUsedQuantity(BigDecimal.ZERO);
        returnRequest.setReturnedQuantity(BigDecimal.ZERO);
        returnRequest.setWasteQuantity(receivedQuantity);
        returnRequest.setRemark("实验失败，全部废弃");
        
        // 执行归还
        HazardousUsageRecord returnedRecord = usageRecordService.returnHazardousMaterial(1L, returnRequest);
        
        // 验证记录正确
        assertThat(returnedRecord).isNotNull();
        assertThat(returnedRecord.getActualUsedQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(returnedRecord.getReturnedQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(returnedRecord.getWasteQuantity()).isEqualByComparingTo(receivedQuantity);
        assertThat(returnedRecord.getStatus()).isEqualTo(2); // 已归还
        
        // 验证没有调用库存归还（因为归还量为0）
        verify(inventoryClient, never())
            .returnHazardousMaterial(anyLong(), any(BigDecimal.class), anyString());
        
        // 重置mock以便下次迭代
        reset(usageRecordMapper, inventoryClient);
    }
}
