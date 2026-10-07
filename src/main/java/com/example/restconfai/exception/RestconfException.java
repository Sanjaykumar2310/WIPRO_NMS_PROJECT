package com.example.restconfai.exception;

/**
 * Custom exception for RESTCONF communication errors.
 *
 * What: Exception representing RESTCONF operation failures.
 * Why: Separates RESTCONF errors from generic exceptions for proper handling.
 * How: Thrown by RestconfClient, caught by GlobalExceptionHandler.
 * Example: 404 when requested interface doesn't exist on the device.
 */
public class RestconfException extends RuntimeException {

    private final int statusCode;
    private final String deviceName;
    private final String operation;

    public RestconfException(String message) {
        super(message);
        this.statusCode = 500;
        this.deviceName = "unknown";
        this.operation = "unknown";
    }

    public RestconfException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.deviceName = "unknown";
        this.operation = "unknown";
    }

    public RestconfException(String message, int statusCode, String deviceName, String operation) {
        super(message);
        this.statusCode = statusCode;
        this.deviceName = deviceName;
        this.operation = operation;
    }

    public RestconfException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
        this.deviceName = "unknown";
        this.operation = "unknown";
    }

    public int getStatusCode() { return statusCode; }
    public String getDeviceName() { return deviceName; }
    public String getOperation() { return operation; }
}
