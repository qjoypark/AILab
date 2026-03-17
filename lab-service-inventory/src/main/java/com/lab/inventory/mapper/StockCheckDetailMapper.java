package com.lab.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.inventory.entity.StockCheckDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存盘点明细Mapper
 */
@Mapper
public interface StockCheckDetailMapper extends BaseMapper<StockCheckDetail> {
}
