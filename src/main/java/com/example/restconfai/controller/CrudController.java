package com.example.restconfai.controller;

import com.example.restconfai.restconf.RestconfResponse;
import com.example.restconfai.service.CrudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for RESTCONF CRUD operations.
 *
 * What: Exposes API endpoints for performing CRUD operations on network devices.
 * Why: The frontend needs endpoints to trigger CREATE, READ, UPDATE, DELETE via RESTCONF.
 * How: Receives requests from frontend, delegates to CrudService → RestconfClient → Device.
 * Example: POST /api/restconf/read with deviceId and resourcePath to read device config.
 */
@RestController
@RequestMapping("/api/restconf")
public class CrudController {

    private static final Logger logger = LoggerFactory.getLogger(CrudController.class);

    private final CrudService crudService;

    public CrudController(CrudService crudService) {
        this.crudService = crudService;
    }

    /**
     * Generic RESTCONF operation endpoint.
     * POST /api/restconf/execute
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeOperation(
            @RequestBody Map<String, String> request) {

        String deviceId = request.get("deviceId");
        String method = request.get("method");
        String resourcePath = request.get("resourcePath");
        String body = request.get("body");

        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("HTTP method is required for executing operation");
        }

        logger.info("Executing RESTCONF operation [{}] on device {} at path {}", method, deviceId, resourcePath);
        RestconfResponse response = crudService.executeOperation(
                deviceId, method, resourcePath, body);

        return ResponseEntity.ok(buildResponseMap(response, method));
    }

    /**
     * CREATE — POST operation
     * POST /api/restconf/create
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody Map<String, String> request) {

        String deviceId = request.get("deviceId");
        String resourcePath = request.get("resourcePath");
        String body = request.get("body");

        logger.info("RESTCONF CREATE (POST) on device {} at path {}", deviceId, resourcePath);
        RestconfResponse response = crudService.create(deviceId, resourcePath, body);
        return ResponseEntity.ok(buildResponseMap(response, "POST"));
    }

    /**
     * READ — GET operation
     * POST /api/restconf/read (uses POST to send parameters in body)
     */
    @PostMapping("/read")
    public ResponseEntity<Map<String, Object>> read(
            @RequestBody Map<String, String> request) {

        String deviceId = request.get("deviceId");
        String resourcePath = request.get("resourcePath");

        logger.info("RESTCONF READ (GET) on device {} at path {}", deviceId, resourcePath);
        RestconfResponse response = crudService.read(deviceId, resourcePath);
        return ResponseEntity.ok(buildResponseMap(response, "GET"));
    }

    /**
     * UPDATE — PUT operation
     * PUT /api/restconf/update
     */
    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> update(
            @RequestBody Map<String, String> request) {

        String deviceId = request.get("deviceId");
        String resourcePath = request.get("resourcePath");
        String body = request.get("body");

        logger.info("RESTCONF UPDATE (PUT) on device {} at path {}", deviceId, resourcePath);
        RestconfResponse response = crudService.update(deviceId, resourcePath, body);
        return ResponseEntity.ok(buildResponseMap(response, "PUT"));
    }

    /**
     * UPDATE (partial) — PATCH operation
     * PATCH /api/restconf/update
     */
    @PatchMapping("/update")
    public ResponseEntity<Map<String, Object>> patch(
            @RequestBody Map<String, String> request) {

        String deviceId = request.get("deviceId");
        String resourcePath = request.get("resourcePath");
        String body = request.get("body");

        logger.info("RESTCONF PATCH on device {} at path {}", deviceId, resourcePath);
        RestconfResponse response = crudService.patch(deviceId, resourcePath, body);
        return ResponseEntity.ok(buildResponseMap(response, "PATCH"));
    }

    /**
     * DELETE operation
     * POST /api/restconf/delete (uses POST to send parameters in body)
     */
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> delete(
            @RequestBody Map<String, String> request) {

        String deviceId = request.get("deviceId");
        String resourcePath = request.get("resourcePath");

        logger.info("RESTCONF DELETE on device {} at path {}", deviceId, resourcePath);
        RestconfResponse response = crudService.delete(deviceId, resourcePath);
        return ResponseEntity.ok(buildResponseMap(response, "DELETE"));
    }

    // ==================== Quick Operations ====================

    /**
     * Quick read device configuration
     */
    @GetMapping("/device/{deviceId}/config")
    public ResponseEntity<Map<String, Object>> readDeviceConfig(
            @PathVariable String deviceId) {
        logger.debug("Quick read config for device {}", deviceId);
        RestconfResponse response = crudService.readDeviceConfig(deviceId);
        return ResponseEntity.ok(buildResponseMap(response, "GET"));
    }

    /**
     * Quick read all interfaces
     */
    @GetMapping("/device/{deviceId}/interfaces")
    public ResponseEntity<Map<String, Object>> readInterfaces(
            @PathVariable String deviceId) {
        logger.debug("Quick read interfaces for device {}", deviceId);
        RestconfResponse response = crudService.readInterfaces(deviceId);
        return ResponseEntity.ok(buildResponseMap(response, "GET"));
    }

    /**
     * Quick read specific interface
     */
    @GetMapping("/device/{deviceId}/interface/{name}")
    public ResponseEntity<Map<String, Object>> readInterface(
            @PathVariable String deviceId, @PathVariable String name) {
        logger.debug("Quick read interface {} for device {}", name, deviceId);
        RestconfResponse response = crudService.readInterface(deviceId, name);
        return ResponseEntity.ok(buildResponseMap(response, "GET"));
    }

    /**
     * Get available resource paths for a device
     */
    @GetMapping("/device/{deviceId}/resources")
    public ResponseEntity<List<Map<String, String>>> getResourcePaths(
            @PathVariable String deviceId) {
        logger.debug("Fetching resource paths for device {}", deviceId);
        return ResponseEntity.ok(crudService.getResourcePaths(deviceId));
    }

    // ==================== Helper ====================

    private Map<String, Object> buildResponseMap(RestconfResponse response, String method) {
        Map<String, Object> result = new HashMap<>();
        result.put("method", method);
        result.put("statusCode", response.getStatusCode());
        result.put("statusMessage", response.getStatusMessage());
        result.put("success", response.isSuccess());
        result.put("body", response.getBody());
        result.put("executionTimeMs", response.getExecutionTimeMs());
        return result;
    }
}
