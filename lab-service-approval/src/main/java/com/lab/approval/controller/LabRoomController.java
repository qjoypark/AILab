package com.lab.approval.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.approval.dto.LabRoomDTO;
import com.lab.approval.dto.LabRoomForm;
import com.lab.approval.dto.LabRoomManagerDTO;
import com.lab.approval.dto.SaveLabRoomManagersRequest;
import com.lab.approval.service.LabRoomService;
import com.lab.approval.support.RequestUserContextResolver;
import com.lab.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/lab-rooms")
@RequiredArgsConstructor
@Tag(name = "Lab Room Management", description = "Lab room and lab room manager APIs")
public class LabRoomController {

    private final LabRoomService labRoomService;
    private final RequestUserContextResolver requestUserContextResolver;

    @GetMapping
    @Operation(summary = "List lab rooms")
    public Result<Page<LabRoomDTO>> listLabRooms(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer roomType,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(request);
        return Result.success(labRoomService.listLabRooms(page, size, status, roomType, keyword, currentUser));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get lab room detail")
    public Result<LabRoomDTO> getLabRoom(@PathVariable Long id, HttpServletRequest request) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(request);
        return Result.success(labRoomService.getLabRoom(id, currentUser));
    }

    @PostMapping
    @Operation(summary = "Create lab room")
    public Result<LabRoomDTO> createLabRoom(@Valid @RequestBody LabRoomForm form, HttpServletRequest request) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(request);
        log.info("Create lab room: userId={}, roomCode={}", currentUser.userId(), form.getRoomCode());
        return Result.success(labRoomService.createLabRoom(form, currentUser));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update lab room")
    public Result<LabRoomDTO> updateLabRoom(@PathVariable Long id,
                                            @Valid @RequestBody LabRoomForm form,
                                            HttpServletRequest request) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(request);
        log.info("Update lab room: id={}, userId={}", id, currentUser.userId());
        return Result.success(labRoomService.updateLabRoom(id, form, currentUser));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete lab room")
    public Result<Void> deleteLabRoom(@PathVariable Long id, HttpServletRequest request) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(request);
        log.info("Delete lab room: id={}, userId={}", id, currentUser.userId());
        labRoomService.deleteLabRoom(id, currentUser);
        return Result.success();
    }

    @GetMapping("/{id}/managers")
    @Operation(summary = "List lab room managers")
    public Result<List<LabRoomManagerDTO>> listManagers(@PathVariable Long id, HttpServletRequest request) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(request);
        return Result.success(labRoomService.listManagers(id, currentUser));
    }

    @PutMapping("/{id}/managers")
    @Operation(summary = "Save lab room managers")
    public Result<List<LabRoomManagerDTO>> saveManagers(@PathVariable Long id,
                                                        @RequestBody SaveLabRoomManagersRequest saveRequest,
                                                        HttpServletRequest request) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(request);
        List<LabRoomManagerDTO> managers = saveRequest == null ? List.of() : saveRequest.getManagers();
        log.info("Save lab room managers: labRoomId={}, userId={}, managerCount={}",
                id, currentUser.userId(), managers == null ? 0 : managers.size());
        return Result.success(labRoomService.saveManagers(id, managers, currentUser));
    }
}
