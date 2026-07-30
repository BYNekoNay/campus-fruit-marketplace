package com.campusfruit.observability;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一 API 错误响应��型。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private String errorCode;
    private String message;
    private Instant timestamp;
    private String traceId;
    private List<String> details;

    public ApiError() {
        this.timestamp = Instant.now();
    }

    public ApiError(String errorCode, String message) {
        this();
        this.errorCode = errorCode;
        this.message = message;
    }

    public ApiError(String errorCode, String message, String traceId) {
        this(errorCode, message);
        this.traceId = traceId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }

    public ApiError addDetail(String detail) {
        if (this.details == null) {
            this.details = new ArrayList<>();
        }
        this.details.add(detail);
        return this;
    }

    public static ApiError of(String errorCode, String message) {
        return new ApiError(errorCode, message);
    }

    public static ApiError of(String errorCode, String message, String traceId) {
        return new ApiError(errorCode, message, traceId);
    }
}
