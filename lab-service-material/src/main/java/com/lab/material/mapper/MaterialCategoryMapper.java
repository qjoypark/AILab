package com.lab.material.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.material.entity.MaterialCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 药品分类Mapper
 */
@Mapper
public interface MaterialCategoryMapper extends BaseMapper<MaterialCategory> {
}
