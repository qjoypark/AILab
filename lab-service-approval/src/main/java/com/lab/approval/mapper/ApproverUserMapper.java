package com.lab.approval.mapper;

import com.lab.approval.dto.ApproverCandidateDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 审批候选用户查询Mapper
 */
@Mapper
public interface ApproverUserMapper {

    @Select("""
            SELECT
                u.id AS userId,
                u.username AS username,
                u.real_name AS realName,
                r.role_code AS roleCode
            FROM sys_user u
            INNER JOIN sys_user_role ur ON u.id = ur.user_id
            INNER JOIN sys_role r ON ur.role_id = r.id
            WHERE r.role_code = #{roleCode}
              AND r.status = 1
              AND r.deleted = 0
              AND u.status = 1
              AND u.deleted = 0
            ORDER BY u.id ASC
            """)
    List<ApproverCandidateDTO> selectApproversByRoleCode(@Param("roleCode") String roleCode);

    @Select({
            "<script>",
            "SELECT",
            "  u.id AS userId,",
            "  u.username AS username,",
            "  u.real_name AS realName,",
            "  r.role_code AS roleCode",
            "FROM sys_user u",
            "INNER JOIN sys_user_role ur ON u.id = ur.user_id",
            "INNER JOIN sys_role r ON ur.role_id = r.id",
            "WHERE r.role_code IN",
            "<foreach collection='roleCodes' item='roleCode' open='(' separator=',' close=')'>",
            "  #{roleCode}",
            "</foreach>",
            "  AND r.status = 1",
            "  AND r.deleted = 0",
            "  AND u.status = 1",
            "  AND u.deleted = 0",
            "ORDER BY u.id ASC",
            "</script>"
    })
    List<ApproverCandidateDTO> selectApproversByRoleCodes(@Param("roleCodes") List<String> roleCodes);
}
