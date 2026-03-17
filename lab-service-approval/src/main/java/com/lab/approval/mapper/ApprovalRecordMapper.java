package com.lab.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.approval.entity.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批记录Mapper
 */
@Mapper
public interface ApprovalRecordMapper extends BaseMapper<ApprovalRecord> {
}
