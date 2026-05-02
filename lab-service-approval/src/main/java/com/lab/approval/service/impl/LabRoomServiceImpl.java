package com.lab.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.approval.dto.LabRoomDTO;
import com.lab.approval.dto.LabRoomForm;
import com.lab.approval.dto.LabRoomManagerDTO;
import com.lab.approval.entity.LabRoom;
import com.lab.approval.entity.LabRoomManager;
import com.lab.approval.mapper.LabRoomManagerMapper;
import com.lab.approval.mapper.LabRoomMapper;
import com.lab.approval.service.LabRoomService;
import com.lab.approval.support.RequestUserContextResolver;
import com.lab.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabRoomServiceImpl implements LabRoomService {

    private static final int STATUS_ENABLED = 1;
    private static final String ROLE_ADMIN = "ADMIN";

    private final LabRoomMapper labRoomMapper;
    private final LabRoomManagerMapper labRoomManagerMapper;

    @Override
    public Page<LabRoomDTO> listLabRooms(int page,
                                         int size,
                                         Integer status,
                                         Integer roomType,
                                         String keyword,
                                         RequestUserContextResolver.CurrentUser currentUser) {
        requirePermission(currentUser, "lab-room:list");
        LambdaQueryWrapper<LabRoom> wrapper = new LambdaQueryWrapper<>();
        if (!canManageAllLabRooms(currentUser) && isLabRoomManager(currentUser)) {
            List<Long> roomIds = selectManagedLabRoomIds(currentUser.userId());
            if (roomIds.isEmpty()) {
                return emptyPage(page, size);
            }
            wrapper.in(LabRoom::getId, roomIds);
        } else if (!canManageAllLabRooms(currentUser)) {
            wrapper.eq(LabRoom::getStatus, STATUS_ENABLED);
        }
        if (status != null && canManageAllLabRooms(currentUser)) {
            wrapper.eq(LabRoom::getStatus, status);
        }
        if (roomType != null) {
            wrapper.eq(LabRoom::getRoomType, roomType);
        }
        if (StringUtils.isNotBlank(keyword)) {
            String trimmed = keyword.trim();
            wrapper.and(condition -> condition
                    .like(LabRoom::getRoomCode, trimmed)
                    .or()
                    .like(LabRoom::getRoomName, trimmed)
                    .or()
                    .like(LabRoom::getBuilding, trimmed)
            );
        }
        int currentPage = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        long total = labRoomMapper.selectCount(wrapper);
        wrapper.orderByDesc(LabRoom::getCreatedTime)
                .last("LIMIT " + ((long) (currentPage - 1) * pageSize) + ", " + pageSize);

        Page<LabRoomDTO> dtoPage = new Page<>(currentPage, pageSize, total);
        dtoPage.setRecords(labRoomMapper.selectList(wrapper).stream()
                .map(this::toDtoWithManagers)
                .collect(Collectors.toList()));
        return dtoPage;
    }

    @Override
    public LabRoomDTO getLabRoom(Long id, RequestUserContextResolver.CurrentUser currentUser) {
        requirePermission(currentUser, "lab-room:list");
        LabRoom room = requireLabRoom(id);
        if (!canViewLabRoom(room, currentUser)) {
            throw new BusinessException(403001, "No permission to view this lab room.");
        }
        return toDtoWithManagers(room);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LabRoomDTO createLabRoom(LabRoomForm form, RequestUserContextResolver.CurrentUser currentUser) {
        requireAdmin(currentUser);
        validateLabRoomForm(form);
        ensureRoomCodeUnique(form.getRoomCode(), null);

        LabRoom room = new LabRoom();
        applyForm(room, form);
        room.setStatus(form.getStatus() == null ? STATUS_ENABLED : form.getStatus());
        room.setCreatedBy(currentUser.userId());
        room.setCreatedTime(LocalDateTime.now());
        room.setUpdatedBy(currentUser.userId());
        room.setUpdatedTime(LocalDateTime.now());
        room.setDeleted(0);
        labRoomMapper.insert(room);
        return toDtoWithManagers(room);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LabRoomDTO updateLabRoom(Long id, LabRoomForm form, RequestUserContextResolver.CurrentUser currentUser) {
        requireAdmin(currentUser);
        validateLabRoomForm(form);
        LabRoom room = requireLabRoom(id);
        ensureRoomCodeUnique(form.getRoomCode(), id);

        applyForm(room, form);
        room.setUpdatedBy(currentUser.userId());
        room.setUpdatedTime(LocalDateTime.now());
        labRoomMapper.updateById(room);
        return toDtoWithManagers(requireLabRoom(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLabRoom(Long id, RequestUserContextResolver.CurrentUser currentUser) {
        requireAdmin(currentUser);
        requireLabRoom(id);
        labRoomMapper.deleteById(id);
    }

    @Override
    public List<LabRoomManagerDTO> listManagers(Long labRoomId, RequestUserContextResolver.CurrentUser currentUser) {
        requirePermission(currentUser, "lab-room:list");
        LabRoom room = requireLabRoom(labRoomId);
        if (!canViewLabRoom(room, currentUser)) {
            throw new BusinessException(403001, "No permission to view lab room managers.");
        }
        return selectManagers(labRoomId).stream()
                .map(LabRoomManagerDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<LabRoomManagerDTO> saveManagers(Long labRoomId,
                                                List<LabRoomManagerDTO> managers,
                                                RequestUserContextResolver.CurrentUser currentUser) {
        requireAdmin(currentUser);
        requireLabRoom(labRoomId);

        LambdaQueryWrapper<LabRoomManager> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(LabRoomManager::getLabRoomId, labRoomId);
        labRoomManagerMapper.delete(deleteWrapper);

        List<LabRoomManagerDTO> safeManagers = managers == null ? Collections.emptyList() : managers;
        Set<Long> seenManagerIds = new HashSet<>();
        List<Long> requestedManagerIds = safeManagers.stream()
                .filter(dto -> dto != null && dto.getManagerId() != null)
                .map(LabRoomManagerDTO::getManagerId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, LabRoomManagerDTO> activeUserMap = requestedManagerIds.isEmpty()
                ? Collections.emptyMap()
                : labRoomManagerMapper.selectActiveUsersByIds(requestedManagerIds).stream()
                        .collect(Collectors.toMap(LabRoomManagerDTO::getManagerId, Function.identity()));

        boolean primaryAssigned = false;
        for (LabRoomManagerDTO dto : safeManagers) {
            if (dto == null || dto.getManagerId() == null || !seenManagerIds.add(dto.getManagerId())) {
                continue;
            }
            LabRoomManagerDTO activeUser = activeUserMap.get(dto.getManagerId());
            if (activeUser == null) {
                throw new BusinessException("Selected lab room manager does not exist or is disabled: " + dto.getManagerId());
            }
            LabRoomManager manager = new LabRoomManager();
            manager.setLabRoomId(labRoomId);
            manager.setManagerId(dto.getManagerId());
            manager.setManagerName(activeUser.getManagerName());
            int isPrimary = !primaryAssigned && Integer.valueOf(1).equals(dto.getIsPrimary()) ? 1 : 0;
            manager.setIsPrimary(isPrimary);
            if (isPrimary == 1) {
                primaryAssigned = true;
            }
            manager.setStatus(dto.getStatus() == null ? STATUS_ENABLED : dto.getStatus());
            manager.setCreatedBy(currentUser.userId());
            manager.setCreatedTime(LocalDateTime.now());
            labRoomManagerMapper.insert(manager);
        }
        return listManagers(labRoomId, currentUser);
    }

    private LabRoom requireLabRoom(Long id) {
        if (id == null) {
            throw new BusinessException("Lab room id is required.");
        }
        LabRoom room = labRoomMapper.selectById(id);
        if (room == null) {
            throw new BusinessException("Lab room does not exist.");
        }
        return room;
    }

    private void validateLabRoomForm(LabRoomForm form) {
        if (form == null) {
            throw new BusinessException("Lab room form is required.");
        }
        if (StringUtils.isBlank(form.getRoomCode()) || StringUtils.isBlank(form.getRoomName())) {
            throw new BusinessException("Room code and room name are required.");
        }
        if (form.getCapacity() != null && form.getCapacity() < 0) {
            throw new BusinessException("Capacity cannot be negative.");
        }
    }

    private void ensureRoomCodeUnique(String roomCode, Long excludeId) {
        LambdaQueryWrapper<LabRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabRoom::getRoomCode, roomCode.trim());
        if (excludeId != null) {
            wrapper.ne(LabRoom::getId, excludeId);
        }
        if (labRoomMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("Lab room code already exists.");
        }
    }

    private void applyForm(LabRoom room, LabRoomForm form) {
        room.setRoomCode(form.getRoomCode().trim());
        room.setRoomName(form.getRoomName().trim());
        room.setBuilding(trimToNull(form.getBuilding()));
        room.setFloor(trimToNull(form.getFloor()));
        room.setRoomNo(trimToNull(form.getRoomNo()));
        room.setCapacity(form.getCapacity());
        room.setRoomType(form.getRoomType() == null ? 1 : form.getRoomType());
        room.setSafetyLevel(form.getSafetyLevel() == null ? 1 : form.getSafetyLevel());
        room.setEquipmentSummary(trimToNull(form.getEquipmentSummary()));
        room.setNotice(trimToNull(form.getNotice()));
        room.setStatus(form.getStatus() == null ? STATUS_ENABLED : form.getStatus());
    }

    private LabRoomDTO toDtoWithManagers(LabRoom room) {
        LabRoomDTO dto = LabRoomDTO.fromEntity(room);
        dto.setManagers(selectManagers(room.getId()).stream()
                .map(LabRoomManagerDTO::fromEntity)
                .collect(Collectors.toList()));
        return dto;
    }

    private List<LabRoomManager> selectManagers(Long labRoomId) {
        LambdaQueryWrapper<LabRoomManager> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabRoomManager::getLabRoomId, labRoomId)
                .eq(LabRoomManager::getStatus, STATUS_ENABLED)
                .orderByDesc(LabRoomManager::getIsPrimary)
                .orderByAsc(LabRoomManager::getManagerId);
        return labRoomManagerMapper.selectList(wrapper);
    }

    private List<Long> selectManagedLabRoomIds(Long userId) {
        LambdaQueryWrapper<LabRoomManager> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabRoomManager::getManagerId, userId)
                .eq(LabRoomManager::getStatus, STATUS_ENABLED);
        return labRoomManagerMapper.selectList(wrapper).stream()
                .map(LabRoomManager::getLabRoomId)
                .collect(Collectors.toList());
    }

    private boolean canViewLabRoom(LabRoom room, RequestUserContextResolver.CurrentUser currentUser) {
        if (canManageAllLabRooms(currentUser)) {
            return true;
        }
        if (room != null && Integer.valueOf(STATUS_ENABLED).equals(room.getStatus())) {
            return true;
        }
        return isManagerOfRoom(currentUser.userId(), room == null ? null : room.getId());
    }

    private boolean isManagerOfRoom(Long userId, Long labRoomId) {
        if (userId == null || labRoomId == null) {
            return false;
        }
        LambdaQueryWrapper<LabRoomManager> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabRoomManager::getLabRoomId, labRoomId)
                .eq(LabRoomManager::getManagerId, userId)
                .eq(LabRoomManager::getStatus, STATUS_ENABLED);
        return labRoomManagerMapper.selectCount(wrapper) > 0;
    }

    private boolean canManageAllLabRooms(RequestUserContextResolver.CurrentUser currentUser) {
        return currentUser.hasAnyRole(ROLE_ADMIN, "CENTER_ADMIN", "003", "CENTER_DIRECTOR", "DEPUTY_DEAN", "002", "DEAN", "001");
    }

    private boolean isLabRoomManager(RequestUserContextResolver.CurrentUser currentUser) {
        return currentUser.hasAnyRole("LAB_ROOM_MANAGER", "005", "LAB_MANAGER");
    }

    private void requireAdmin(RequestUserContextResolver.CurrentUser currentUser) {
        if (!currentUser.hasRole(ROLE_ADMIN)) {
            throw new BusinessException(403001, "Only system administrators can perform this operation.");
        }
    }

    private void requirePermission(RequestUserContextResolver.CurrentUser currentUser, String permissionCode) {
        if (currentUser.hasRole(ROLE_ADMIN)) {
            return;
        }
        if (currentUser.permissions() != null && currentUser.permissions().contains(permissionCode)) {
            return;
        }
        throw new BusinessException(403001, "No permission: " + permissionCode);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Page<LabRoomDTO> emptyPage(int page, int size) {
        Page<LabRoomDTO> empty = new Page<>(page, size, 0);
        empty.setRecords(Collections.emptyList());
        return empty;
    }
}
