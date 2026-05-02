package com.lab.approval.service.impl;

import com.lab.approval.dto.ApprovalContext;
import com.lab.approval.dto.ApproverCandidateDTO;
import com.lab.approval.mapper.ApproverUserMapper;
import com.lab.approval.service.ApproverAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 审批人自动分配服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApproverAssignmentServiceImpl implements ApproverAssignmentService {

    private static final String ROLE_CENTER_ADMIN = "CENTER_ADMIN";
    private static final String ROLE_CENTER_ADMIN_ALIAS = "003";
    private static final String ROLE_LAB_ROOM_MANAGER = "LAB_ROOM_MANAGER";
    private static final String ROLE_CENTER_DIRECTOR = "CENTER_DIRECTOR";
    private static final String ROLE_DEPUTY_DEAN = "DEPUTY_DEAN";
    private static final String ROLE_DEAN = "DEAN";

    private final ApproverUserMapper approverUserMapper;

    @Override
    public Long assignApprover(String approverRole, ApprovalContext context) {
        List<ApproverCandidateDTO> candidates = listApproverCandidates(approverRole, context);
        if (candidates.isEmpty()) {
            log.warn("未找到候选审批人: approverRole={}", approverRole);
            return null;
        }
        return candidates.get(0).getUserId();
    }

    @Override
    public List<ApproverCandidateDTO> listApproverCandidates(String approverRole, ApprovalContext context) {
        String effectiveRoleCode = resolveEffectiveRoleCode(approverRole, context);
        if (effectiveRoleCode == null || effectiveRoleCode.isBlank()) {
            return Collections.emptyList();
        }
        if (ROLE_LAB_ROOM_MANAGER.equals(effectiveRoleCode) && context != null && context.getLabRoomId() != null) {
            List<ApproverCandidateDTO> managers = approverUserMapper.selectLabRoomManagers(context.getLabRoomId());
            if (managers != null && !managers.isEmpty()) {
                return managers;
            }
        }
        List<String> roleCodes = buildRoleCodeCandidates(effectiveRoleCode);
        List<ApproverCandidateDTO> candidates = approverUserMapper.selectApproversByRoleCodes(roleCodes);
        return candidates == null ? Collections.emptyList() : candidates;
    }

    @Override
    public String resolveEffectiveRoleCode(String approverRole, ApprovalContext context) {
        if (context != null && context.getApplicationType() != null
                && (context.getApplicationType() == 1 || context.getApplicationType() == 2)) {
            return ROLE_CENTER_ADMIN;
        }
        if (approverRole == null || approverRole.isBlank()) {
            return null;
        }
        return approverRole.trim().toUpperCase();
    }

    private List<String> buildRoleCodeCandidates(String effectiveRoleCode) {
        if (effectiveRoleCode == null || effectiveRoleCode.isBlank()) {
            return Collections.emptyList();
        }
        List<String> roleCodes = new ArrayList<>();
        roleCodes.add(effectiveRoleCode);
        if (ROLE_CENTER_ADMIN.equals(effectiveRoleCode)) {
            roleCodes.add(ROLE_CENTER_ADMIN_ALIAS);
        } else if (ROLE_LAB_ROOM_MANAGER.equals(effectiveRoleCode)) {
            roleCodes.add("005");
            roleCodes.add("LAB_MANAGER");
        } else if (ROLE_CENTER_DIRECTOR.equals(effectiveRoleCode)) {
            roleCodes.add("003");
            roleCodes.add(ROLE_CENTER_ADMIN);
        } else if (ROLE_DEPUTY_DEAN.equals(effectiveRoleCode)) {
            roleCodes.add("002");
        } else if (ROLE_DEAN.equals(effectiveRoleCode)) {
            roleCodes.add("001");
        }
        return roleCodes;
    }
}
