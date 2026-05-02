package com.lab.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.approval.dto.LabRoomManagerDTO;
import com.lab.approval.entity.LabRoomManager;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LabRoomManagerMapper extends BaseMapper<LabRoomManager> {

    @Select({
            "<script>",
            "SELECT",
            "  u.id AS managerId,",
            "  COALESCE(NULLIF(u.real_name, ''), u.username) AS managerName,",
            "  u.status AS status",
            "FROM sys_user u",
            "WHERE u.id IN",
            "<foreach collection='userIds' item='userId' open='(' separator=',' close=')'>",
            "  #{userId}",
            "</foreach>",
            "  AND u.status = 1",
            "  AND u.deleted = 0",
            "</script>"
    })
    List<LabRoomManagerDTO> selectActiveUsersByIds(@Param("userIds") List<Long> userIds);
}
