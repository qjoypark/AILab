package com.lab.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.approval.entity.HazardousUsageRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 危化品使用记录Mapper
 */
@Mapper
public interface HazardousUsageRecordMapper extends BaseMapper<HazardousUsageRecord> {
}
