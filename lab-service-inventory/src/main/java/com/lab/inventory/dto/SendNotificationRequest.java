package com.lab.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "发送站内消息请求")
public class SendNotificationRequest {

    @Schema(description = "接收人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long receiverId;

    @Schema(description = "通知类型: 1-审批, 2-预警, 3-系统")
    private Integer notificationType;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务ID")
    private Long businessId;

    @Schema(description = "推送渠道: 1-站内, 2-微信, 3-短信, 4-邮件")
    private Integer pushChannel;
}
