package com.example.restconfai.service;

import com.example.restconfai.config.RestconfConfig;
import com.example.restconfai.exception.RestconfException;
import com.example.restconfai.model.Device;
import com.example.restconfai.model.OperationHistory;
import com.example.restconfai.restconf.RestconfClient;
import com.example.restconfai.restconf.RestconfRequest;
import com.example.restconfai.restconf.RestconfResponse;
import com.example.restconfai.validation.InputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * CRUD Service for RESTCONF operations.
 *
 * What: Orchestrates CRUD operations through RESTCONF.
 * Why: Provides a clean interface between controllers and the RESTCONF client.
 * How: Validates inputs, builds RESTCONF requests, executes them, and records history.
 * Example: User clicks READ → CrudService builds GET request → RestconfClient sends it.
 *
 * Flow: Controller → CrudService → RestconfClient → Network Device
 */
@Service
public class CrudService {

    private static final Logger logger = LoggerFactory.getLogger(CrudService.class);

    private final RestconfClient restconfClient;
    private final DeviceService deviceService;
    private final RestconfConfig config;
    private final InputValidator validator;

    public CrudService(RestconfClient restconfClient, DeviceService deviceService,
                       RestconfConfig config, InputValidator validator) {
        this.restconfClient = restconfClient;
        this.deviceService = deviceService;
        this.config = config;
        this.validator = validator;
        logger.info("CrudService initialized [Mode: {}, SSL Verify: {}]", 
                    config.getMode(), config.isSslVerify());
    }

    /**
     * CREATE - Create a new resource on the device via POST.
     */
    public RestconfResponse create(String deviceId, String resourcePath, String jsonBody) {
        validateOperation(deviceId, "POST", resourcePath);

        Device device = getDeviceOrThrow(deviceId);
        RestconfRequest request = new RestconfRequest("POST", resourcePath, jsonBody);

        RestconfResponse response = executeAndRecord(device, request, "CREATE");
        return response;
    }

    /**
     * READ - Retrieve resource configuration from the device via GET.
     */
    public RestconfResponse read(String deviceId, String resourcePath) {
        validateOperation(deviceId, "GET", resourcePath);

        Device device = getDeviceOrThrow(deviceId);
        RestconfRequest request = new RestconfRequest("GET", resourcePath);

        return executeAndRecord(device, request, "READ");
    }

    /**
     * UPDATE (full replace) - Replace a resource configuration via PUT.
     */
    public RestconfResponse update(String deviceId, String resourcePath, String jsonBody) {
        validateOperation(deviceId, "PUT", resourcePath);

        Device device = getDeviceOrThrow(deviceId);
        RestconfRequest request = new RestconfRequest("PUT", resourcePath, jsonBody);

        return executeAndRecord(device, request, "UPDATE");
    }

    /**
     * UPDATE (partial) - Patch a resource configuration via PATCH.
     */
    public RestconfResponse patch(String deviceId, String resourcePath, String jsonBody) {
        validateOperation(deviceId, "PATCH", resourcePath);

        Device device = getDeviceOrThrow(deviceId);
        RestconfRequest request = new RestconfRequest("PATCH", resourcePath, jsonBody);

        return executeAndRecord(device, request, "PATCH");
    }

    /**
     * DELETE - Remove a resource from the device via DELETE.
     */
    public RestconfResponse delete(String deviceId, String resourcePath) {
        validateOperation(deviceId, "DELETE", resourcePath);

        Device device = getDeviceOrThrow(deviceId);
        RestconfRequest request = new RestconfRequest("DELETE", resourcePath);

        return executeAndRecord(device, request, "DELETE");
    }

    /**
     * Generic RESTCONF operation for flexibility.
     */
    public RestconfResponse executeOperation(String deviceId, String method,
                                              String resourcePath, String jsonBody) {
        return switch (method.toUpperCase()) {
            case "GET" -> read(deviceId, resourcePath);
            case "POST" -> create(deviceId, resourcePath, jsonBody);
            case "PUT" -> update(deviceId, resourcePath, jsonBody);
            case "PATCH" -> patch(deviceId, resourcePath, jsonBody);
            case "DELETE" -> delete(deviceId, resourcePath);
            default -> throw new RestconfException("Unsupported method: " + method, 400);
        };
    }

    // ==================== Quick Operations ====================

    /**
     * Quick READ of device configuration.
     */
    public RestconfResponse readDeviceConfig(String deviceId) {
        return read(deviceId, "/data/network-device:device=" + deviceId);
    }

    /**
     * Quick READ of all interfaces for a device.
     */
    public RestconfResponse readInterfaces(String deviceId) {
        return read(deviceId, "/data/network-device:device=" + deviceId + "/interfaces");
    }

    /**
     * Quick READ of a specific interface.
     */
    public RestconfResponse readInterface(String deviceId, String interfaceName) {
        String encodedName = interfaceName.replace("/", "%2F");
        return read(deviceId,
                "/data/network-device:device=" + deviceId + "/interface=" + encodedName);
    }

    /**
     * Get available resource paths for a device.
     */
    public List<Map<String, String>> getResourcePaths(String deviceId) {
        return List.of(
            Map.of("path", "/data/network-device:device=" + deviceId,
                   "description", "Full device configuration",
                   "methods", "GET"),
            Map.of("path", "/data/network-device:device=" + deviceId + "/interfaces",
                   "description", "All interfaces",
                   "methods", "GET, POST"),
            Map.of("path", "/data/network-device:device=" + deviceId + "/interface=GigabitEthernet0/1",
                   "description", "Specific interface (GigabitEthernet0/1)",
                   "methods", "GET, PUT, PATCH, DELETE")
        );
    }

    // ==================== Private Helpers ====================

    private Device getDeviceOrThrow(String deviceId) {
        Device device = deviceService.getDevice(deviceId);
        if (device == null) {
            throw new RestconfException("Device not found: " + deviceId, 404,
                    deviceId, "LOOKUP");
        }
        return device;
    }

    private void validateOperation(String deviceId, String method, String resourcePath) {
        List<String> errors = validator.validateRestconfOperation(deviceId, method, resourcePath);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "Validation failed: " + String.join(", ", errors));
        }
    }

    private RestconfResponse executeAndRecord(Device device, RestconfRequest request,
                                               String operationName) {
        long startTime = System.currentTimeMillis();

        try {
            String baseUrl = device.getRestconfUrl();
            RestconfResponse response = restconfClient.execute(
                    baseUrl, request, device.getUsername(), device.getPassword());

            long executionTime = System.currentTimeMillis() - startTime;

            // Record in operation history
            OperationHistory history = new OperationHistory();
            history.setDeviceId(device.getId());
            history.setDeviceName(device.getName());
            history.setOperation(operationName);
            history.setMethod(request.getMethod());
            history.setResourcePath(request.getResourcePath());
            history.setStatusCode(response.getStatusCode());
            history.setStatusMessage(response.getStatusMessage());
            history.setRequestBody(request.getBody());
            history.setResponseBody(response.getBody());
            history.setExecutionTimeMs(executionTime);
            history.setSuccess(response.isSuccess());

            deviceService.addOperationHistory(history);

            logger.info("{} operation on {} completed [mode: {}]: {} ({}ms)",
                    operationName, device.getName(), config.getMode(), response.getStatusCode(), executionTime);

            return response;

        } catch (RestconfException e) {
            long executionTime = System.currentTimeMillis() - startTime;

            OperationHistory history = new OperationHistory();
            history.setDeviceId(device.getId());
            history.setDeviceName(device.getName());
            history.setOperation(operationName);
            history.setMethod(request.getMethod());
            history.setResourcePath(request.getResourcePath());
            history.setStatusCode(e.getStatusCode());
            history.setStatusMessage(e.getMessage());
            history.setExecutionTimeMs(executionTime);
            history.setSuccess(false);

            deviceService.addOperationHistory(history);
            throw e;
        }
    }
}
