package com.lab.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.approval.entity.MaterialApplicationItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 领用申请明细Mapper
 */
@Mapper
public interface MaterialApplicationItemMapper extends BaseMapper<MaterialApplicationItem> {
}
