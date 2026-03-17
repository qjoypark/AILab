package com.lab.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通知分页结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通知分页结果DTO")
public class NotificationPageDTO {
    
    @Schema(description = "总记录数")
    private Long total;
    
    @Schema(description = "未读消息数量")
    private Long unreadCount;
    
    @Schema(description = "通知列表")
    private List<NotificationDTO> list;
}
