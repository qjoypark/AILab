package com.lab.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.inventory.entity.StockOutDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出库单明细Mapper
 */
@Mapper
public interface StockOutDetailMapper extends BaseMapper<StockOutDetail> {
}
