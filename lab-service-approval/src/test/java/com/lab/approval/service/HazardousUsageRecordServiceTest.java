package com.lab.approval.service;

import com.lab.approval.entity.HazardousUsageRecord;
import com.lab.approval.mapper.HazardousUsageRecordMapper;
import com.lab.approval.service.impl.HazardousUsageRecordServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 危化品使用记录服务测试
 * 
 * **Validates: Requirements 6.6**
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("危化品使用记录服务测试")
class HazardousUsageRecordServiceTest {
    
    @Mock
    private HazardousUsageRecordMapper usageRecordMapper;
    
    @InjectMocks
    private HazardousUsageRecordServiceImpl usageRecordService;
    
    @Test
    @DisplayName("应成功创建危化品使用记录")
    void shouldCreateUsageRecordSuccessfully() {
        // Given: 准备使用记录数据
        HazardousUsageRecord record = new HazardousUsageRecord();
        record.setApplicationId(100L);
        record.setMaterialId(1001L);
        record.setUserId(10L);
        record.setUserName("张三");
        record.setReceivedQuantity(new BigDecimal("2.5"));
        record.setUsageDate(LocalDate.now());
        record.setUsageLocation("实验室A101");
        record.setUsagePurpose("化学实验");
        record.setStatus(1); // 使用中
        
        when(usageRecordMapper.insert(any(HazardousUsageRecord.class)))
            .thenAnswer(invocation -> {
                HazardousUsageRecord arg = invocation.getArgument(0);
                arg.setId(1L);
                return 1;
            });
        
        // When: 创建使用记录
        HazardousUsageRecord created = usageRecordService.createUsageRecord(record);
        
        // Then: 验证记录创建成功
        ArgumentCaptor<HazardousUsageRecord> recordCaptor = 
            ArgumentCaptor.forClass(HazardousUsageRecord.class);
        verify(usageRecordMapper).insert(recordCaptor.capture());
        
        HazardousUsageRecord capturedRecord = recordCaptor.getValue();
        assertThat(capturedRecord.getApplicationId()).isEqualTo(100L);
        assertThat(capturedRecord.getMaterialId()).isEqualTo(1001L);
        assertThat(capturedRecord.getUserId()).isEqualTo(10L);
        assertThat(capturedRecord.getUserName()).isEqualTo("张三");
        assertThat(capturedRecord.getReceivedQuantity()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(capturedRecord.getUsageDate()).isEqualTo(LocalDate.now());
        assertThat(capturedRecord.getUsageLocation()).isEqualTo("实验室A101");
        assertThat(capturedRecord.getUsagePurpose()).isEqualTo("化学实验");
        assertThat(capturedRecord.getStatus()).isEqualTo(1);
        
        assertThat(created.getId()).isEqualTo(1L);
    }
}
