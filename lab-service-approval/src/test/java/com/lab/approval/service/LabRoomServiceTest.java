package com.lab.approval.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.approval.dto.LabRoomDTO;
import com.lab.approval.entity.LabRoom;
import com.lab.approval.mapper.LabRoomManagerMapper;
import com.lab.approval.mapper.LabRoomMapper;
import com.lab.approval.service.impl.LabRoomServiceImpl;
import com.lab.approval.support.RequestUserContextResolver;
import com.lab.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabRoomServiceTest {

    @Mock
    private LabRoomMapper labRoomMapper;

    @Mock
    private LabRoomManagerMapper labRoomManagerMapper;

    private LabRoomServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LabRoomServiceImpl(labRoomMapper, labRoomManagerMapper);
    }

    @Test
    void listLabRooms_rejectsUserWithoutLabRoomListPermission() {
        assertThrows(BusinessException.class, () ->
                service.listLabRooms(1, 10, null, null, null, userWithoutLabRoomPermission()));

        verify(labRoomMapper, never()).selectCount(any());
        verify(labRoomMapper, never()).selectList(any());
    }

    @Test
    void listLabRooms_allowsTeacherWithLabRoomListPermission() {
        when(labRoomMapper.selectCount(any())).thenReturn(1L);
        when(labRoomMapper.selectList(any())).thenReturn(List.of(enabledRoom()));
        when(labRoomManagerMapper.selectList(any())).thenReturn(Collections.emptyList());

        Page<LabRoomDTO> result = service.listLabRooms(1, 10, null, null, null, teacher());

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("A101", result.getRecords().get(0).getRoomCode());
    }

    @Test
    void getLabRoom_rejectsUserWithoutLabRoomListPermission() {
        assertThrows(BusinessException.class, () ->
                service.getLabRoom(1L, userWithoutLabRoomPermission()));

        verify(labRoomMapper, never()).selectById(any());
    }

    @Test
    void listManagers_rejectsUserWithoutLabRoomListPermission() {
        assertThrows(BusinessException.class, () ->
                service.listManagers(1L, userWithoutLabRoomPermission()));

        verify(labRoomMapper, never()).selectById(any());
        verify(labRoomManagerMapper, never()).selectList(any());
    }

    private LabRoom enabledRoom() {
        LabRoom room = new LabRoom();
        room.setId(1L);
        room.setRoomCode("A101");
        room.setRoomName("Shared Lab");
        room.setStatus(1);
        room.setRoomType(1);
        room.setSafetyLevel(1);
        room.setCreatedTime(LocalDateTime.of(2026, 5, 2, 8, 0));
        return room;
    }

    private RequestUserContextResolver.CurrentUser teacher() {
        return new RequestUserContextResolver.CurrentUser(
                7L,
                "teacher7",
                "Teacher Seven",
                "Biology",
                List.of("TEACHER"),
                List.of("lab-room:list", "lab-usage:list", "lab-usage:create")
        );
    }

    private RequestUserContextResolver.CurrentUser userWithoutLabRoomPermission() {
        return new RequestUserContextResolver.CurrentUser(
                88L,
                "student88",
                "Student Eighty Eight",
                "Biology",
                List.of("STUDENT"),
                List.of("lab-usage:schedule:view")
        );
    }
}
