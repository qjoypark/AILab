package com.lab.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.lab.approval.entity.ApprovalRecord;
import com.lab.approval.mapper.ApprovalFlowConfigMapper;
import com.lab.approval.mapper.ApprovalRecordMapper;
import com.lab.approval.service.impl.ApprovalWorkflowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalWorkflowServiceImplTest {

    @Mock
    private ApprovalFlowEngine flowEngine;

    @Mock
    private ApprovalFlowConfigMapper flowConfigMapper;

    @Mock
    private ApprovalRecordMapper recordMapper;

    private ApprovalWorkflowServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ApprovalRecord.class);
        service = new ApprovalWorkflowServiceImpl(flowEngine, flowConfigMapper, recordMapper);
    }

    @Test
    void getApprovalHistory_filtersLabUsageByBusinessTypeAndApplicationId() {
        when(recordMapper.selectList(any())).thenReturn(Collections.emptyList());

        service.getApprovalHistory(3, 100L);

        ArgumentCaptor<LambdaQueryWrapper<ApprovalRecord>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(recordMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getCustomSqlSegment().toLowerCase();
        assertTrue(sqlSegment.contains("application_id"));
        assertTrue(sqlSegment.contains("business_type"));
        assertTrue(sqlSegment.contains("order by"));
        assertFalse(sqlSegment.contains("business_type is null"));
    }

    @Test
    void getApprovalHistory_keepsLegacyMaterialRecordsCompatible() {
        when(recordMapper.selectList(any())).thenReturn(Collections.emptyList());

        service.getApprovalHistory(1, 100L);

        ArgumentCaptor<LambdaQueryWrapper<ApprovalRecord>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(recordMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getCustomSqlSegment().toLowerCase();
        assertTrue(sqlSegment.contains("application_id"));
        assertTrue(sqlSegment.contains("business_type"));
        assertTrue(sqlSegment.contains("business_type is null"));
    }
}
