package com.lab.common.result;

import lombok.Getter;

/**
 * 响应状态码
 */
@Getter
public enum ResultCode {
    // 成功
    SUCCESS(200, "操作成功"),

    // 客户端错误 4xx
    PARAM_ERROR(400001, "参数错误"),
    PARAM_MISSING(400002, "参数缺失"),
    UNAUTHORIZED(401001, "未登录"),
    TOKEN_EXPIRED(401002, "令牌已过期"),
    FORBIDDEN(403001, "无权限访问"),
    NOT_FOUND(404001, "资源不存在"),

    // 服务器错误 5xx
    SYSTEM_ERROR(500001, "系统错误"),
    DATABASE_ERROR(500002, "数据库错误"),

    // 用户模块 10xxxx
    USERNAME_EXISTS(100001, "用户名已存在"),
    USER_NOT_FOUND(100002, "用户不存在"),
    PASSWORD_ERROR(100003, "密码错误"),
    SAFETY_CERT_INVALID(100004, "安全资质未通过或已过期"),

    // 药品模块 20xxxx
    MATERIAL_CODE_EXISTS(200001, "药品编码已存在"),
    MATERIAL_NOT_FOUND(200002, "药品不存在"),

    // 库存模块 30xxxx
    STOCK_INSUFFICIENT(300001, "库存不足"),
    BATCH_NOT_FOUND(300002, "批次号不存在"),

    // 申请审批模块 40xxxx
    APPLICATION_NOT_FOUND(400001, "申请单不存在"),
    APPLICATION_STATUS_ERROR(400002, "申请单状态不允许操作"),
    SAFETY_CERT_REQUIRED(400003, "安全资质未通过");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
