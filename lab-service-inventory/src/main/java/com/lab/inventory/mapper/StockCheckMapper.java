package com.lab.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.inventory.entity.StockCheck;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存盘点Mapper
 */
@Mapper
public interface StockCheckMapper extends BaseMapper<StockCheck> {
}
