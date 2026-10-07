package com.example.restconfai.restconf;

/**
 * Represents a RESTCONF response received from a network device.
 *
 * What: Encapsulates the HTTP response from a RESTCONF operation.
 * Why: Provides a uniform response structure for all RESTCONF operations.
 * How: Created by RestconfClient after receiving an HTTP response.
 * Example: 200 OK with JSON body containing interface configuration.
 */
public class RestconfResponse {

    private int statusCode;
    private String statusMessage;
    private String body;
    private String contentType;
    private long executionTimeMs;
    private boolean success;

    public RestconfResponse() {}

    public RestconfResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
        this.success = statusCode >= 200 && statusCode < 300;
        this.statusMessage = deriveStatusMessage(statusCode);
    }

    public RestconfResponse(int statusCode, String statusMessage, String body) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.body = body;
        this.success = statusCode >= 200 && statusCode < 300;
    }

    private String deriveStatusMessage(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 500 -> "Internal Server Error";
            default -> "HTTP " + code;
        };
    }

    // Getters and Setters
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    @Override
    public String toString() {
        return "RestconfResponse{status=" + statusCode + ", success=" + success + "}";
    }
}
