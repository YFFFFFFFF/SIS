package com.sis.iids.common.api;

public class ApiResponse<T> {

    private String code;
    private String message;
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("SUCCESS", "成功", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>("SUCCESS", "成功", null);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
