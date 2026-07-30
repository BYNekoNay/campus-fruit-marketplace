package com.campusfruit.merchant.integration.baidu.model;

import com.fasterxml.jackson.databind.JsonNode;

public class BaiduApiResponse {
    private int status;
    private String message;
    private JsonNode result;

    public BaiduApiResponse() {
    }

    public boolean isSuccess() {
        return status == 0;
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public JsonNode getResult() { return result; }
    public void setResult(JsonNode result) { this.result = result; }
}
