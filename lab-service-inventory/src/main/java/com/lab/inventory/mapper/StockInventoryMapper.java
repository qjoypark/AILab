package com.lab.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.inventory.entity.StockInventory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存Mapper
 */
@Mapper
public interface StockInventoryMapper extends BaseMapper<StockInventory> {
}
