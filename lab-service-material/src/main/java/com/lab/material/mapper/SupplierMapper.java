package com.lab.material.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.material.entity.Supplier;
import org.apache.ibatis.annotations.Mapper;

/**
 * 供应商Mapper
 */
@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {
}
