package com.lab.inventory.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper {

    @Select("""
            SELECT real_name
            FROM sys_user
            WHERE id = #{userId}
              AND deleted = 0
            LIMIT 1
            """)
    String selectRealNameById(@Param("userId") Long userId);
}
