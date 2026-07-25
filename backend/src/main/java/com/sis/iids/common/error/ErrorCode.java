package com.sis.iids.common.error;

public enum ErrorCode {
    SUCCESS("SUCCESS", "ok"),
    BAD_REQUEST("BAD_REQUEST", "请求参数错误"),
    UNAUTHORIZED("UNAUTHORIZED", "未认证或登录已失效"),
    FORBIDDEN("FORBIDDEN", "权限不足"),
    NOT_FOUND("NOT_FOUND", "资源不存在"),
    CONFLICT("CONFLICT", "资源冲突"),
    BUSINESS_ERROR("BUSINESS_ERROR", "业务处理失败"),
    ENGINE_ERROR("ENGINE_ERROR", "测算引擎错误"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
