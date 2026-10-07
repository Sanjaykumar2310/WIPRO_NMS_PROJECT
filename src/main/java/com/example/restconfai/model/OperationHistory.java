package com.example.restconfai.model;

import java.time.LocalDateTime;

/**
 * Records each RESTCONF operation performed through the application.
 *
 * What: Audit trail entry for every CRUD operation.
 * Why: Provides operation history for the dashboard and accountability.
 * How: Created by CrudService after each RESTCONF operation completes.
 * Example: GET /restconf/data/interfaces at 10:30 AM → 200 OK, 45ms.
 */
public class OperationHistory {

    private String id;
    private String deviceId;
    private String deviceName;
    private String operation;
    private String method;
    private String resourcePath;
    private int statusCode;
    private String statusMessage;
    private String requestBody;
    private String responseBody;
    private long executionTimeMs;
    private LocalDateTime timestamp;
    private boolean success;

    public OperationHistory() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getResourcePath() { return resourcePath; }
    public void setResourcePath(String resourcePath) { this.resourcePath = resourcePath; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }

    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
