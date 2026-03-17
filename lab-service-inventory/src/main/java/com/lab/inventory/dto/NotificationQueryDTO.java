package com.lab.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通知查询DTO
 */
@Data
@Schema(description = "通知查询DTO")
public class NotificationQueryDTO {
    
    @Schema(description = "接收人ID")
    private Long receiverId;
    
    @Schema(description = "通知类型: 1-审批, 2-预警, 3-系统")
    private Integer notificationType;
    
    @Schema(description = "是否已读: 0-未读, 1-已读")
    private Integer isRead;
    
    @Schema(description = "页码", example = "1")
    private Integer page = 1;
    
    @Schema(description = "每页数量", example = "10")
    private Integer size = 10;
}
