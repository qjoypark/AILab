package com.lab.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.inventory.entity.StorageLocation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 存储位置Mapper
 */
@Mapper
public interface StorageLocationMapper extends BaseMapper<StorageLocation> {
}
