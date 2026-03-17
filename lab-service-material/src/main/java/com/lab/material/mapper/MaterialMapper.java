package com.lab.material.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.material.entity.Material;
import org.apache.ibatis.annotations.Mapper;

/**
 * 药品信息Mapper
 */
@Mapper
public interface MaterialMapper extends BaseMapper<Material> {
}
