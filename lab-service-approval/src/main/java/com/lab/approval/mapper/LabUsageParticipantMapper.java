package com.lab.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.approval.dto.LabUsageParticipantDTO;
import com.lab.approval.entity.LabUsageParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LabUsageParticipantMapper extends BaseMapper<LabUsageParticipant> {

    @Select({
            "<script>",
            "SELECT",
            "  u.id AS userId,",
            "  COALESCE(NULLIF(u.real_name, ''), u.username) AS realName,",
            "  u.department AS deptName",
            "FROM sys_user u",
            "WHERE u.id IN",
            "<foreach collection='userIds' item='userId' open='(' separator=',' close=')'>",
            "  #{userId}",
            "</foreach>",
            "  AND u.status = 1",
            "  AND u.deleted = 0",
            "</script>"
    })
    List<LabUsageParticipantDTO> selectActiveUsersByIds(@Param("userIds") List<Long> userIds);
}
