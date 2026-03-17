package com.lab.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.inventory.entity.StockIn;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入库单Mapper
 */
@Mapper
public interface StockInMapper extends BaseMapper<StockIn> {
}
