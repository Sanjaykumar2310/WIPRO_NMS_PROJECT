package com.example.restconfai.simulator;

import com.example.restconfai.model.InterfaceConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Mock RESTCONF Server / Simulator.
 *
 * What: A Spring Boot REST controller that mimics a RESTCONF-enabled network device.
 * Why: Allows the project to be demonstrated without physical Cisco routers/switches.
 * How: Exposes /restconf/data/... endpoints that follow RFC 8040 URL patterns.
 * Example: GET /restconf/data/network-device:device returns simulated device config.
 *
 * This simulator follows RESTCONF URL conventions:
 * - /restconf/data/{module}:{container}
 * - /restconf/data/{module}:{container}/{list}={key}
 *
 * Responses use application/yang-data+json content type as per RFC 8040.
 */
@RestController
@RequestMapping("/restconf")
public class RestconfSimulator {

    private static final Logger logger = LoggerFactory.getLogger(RestconfSimulator.class);
    private static final String YANG_JSON = "application/yang-data+json";

    private final SimulatorDataStore dataStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RestconfSimulator(SimulatorDataStore dataStore) {
        this.dataStore = dataStore;
    }

    // ==================== RESTCONF Root Discovery ====================

    /**
     * RESTCONF root resource (RFC 8040 Section 3.1)
     */
    @GetMapping(value = "/.well-known/host-meta", produces = MediaType.APPLICATION_XML_VALUE)
    public String hostMeta() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <XRD xmlns="http://docs.oasis-open.org/ns/xri/xrd-1.0">
              <Link rel="restconf" href="/restconf"/>
            </XRD>
            """;
    }

    /**
     * RESTCONF data root
     */
    @GetMapping(value = "/data", produces = YANG_JSON)
    public ResponseEntity<String> getDataRoot() {
        logger.debug("RESTCONF GET /data (root)");
        String json = "{\"ietf-restconf:data\": {\"network-device:devices\": " +
                       toJson(dataStore.getAllDevices()) + "}}";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(YANG_JSON))
                .body(json);
    }

    // ==================== Device Operations ====================

    /**
     * READ - Get specific device configuration
     * GET /restconf/data/network-device:device={deviceId}
     */
    @GetMapping(value = "/data/network-device:device={deviceId}", produces = YANG_JSON)
    public ResponseEntity<String> getDevice(@PathVariable String deviceId) {
        logger.info("RESTCONF GET device: {}", deviceId);

        String json = dataStore.getDeviceAsJson(deviceId);
        if (json == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.parseMediaType(YANG_JSON))
                    .body("{\"ietf-restconf:errors\":{\"error\":[{\"error-type\":\"application\"," +
                          "\"error-tag\":\"data-missing\",\"error-message\":\"Device not found: " +
                          deviceId + "\"}]}}");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(YANG_JSON))
                .body(json);
    }

    // ==================== Interface Operations ====================

    /**
     * READ - Get all interfaces for a device
     * GET /restconf/data/network-device:device={deviceId}/interfaces
     */
    @GetMapping(value = "/data/network-device:device={deviceId}/interfaces",
                produces = YANG_JSON)
    public ResponseEntity<String> getInterfaces(@PathVariable String deviceId) {
        logger.info("RESTCONF GET interfaces for device: {}", deviceId);

        String json = dataStore.getAllInterfacesAsJson(deviceId);
        if (json == null) {
            return notFoundResponse("Device not found: " + deviceId);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(YANG_JSON))
                .body(json);
    }

    /**
     * READ - Get specific interface
     * GET /restconf/data/network-device:device={deviceId}/interface={name}
     */
    @GetMapping(value = "/data/network-device:device={deviceId}/interface={name:.+}",
                produces = YANG_JSON)
    public ResponseEntity<String> getInterface(@PathVariable String deviceId,
                                                @PathVariable String name) {
        logger.info("RESTCONF GET interface: {} on device: {}", name, deviceId);

        String decoded = name.replace("%2F", "/").replace("%2f", "/");
        String json = dataStore.getInterfaceAsJson(deviceId, decoded);
        if (json == null) {
            return notFoundResponse("Interface not found: " + decoded);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(YANG_JSON))
                .body(json);
    }

    /**
     * CREATE - Add a new interface
     * POST /restconf/data/network-device:device={deviceId}/interfaces
     */
    @PostMapping(value = "/data/network-device:device={deviceId}/interfaces",
                 consumes = YANG_JSON, produces = YANG_JSON)
    public ResponseEntity<String> createInterface(@PathVariable String deviceId,
                                                   @RequestBody(required = false) String body) {
        logger.info("RESTCONF POST interface on device: {}", deviceId);

        try {
            if (body == null || body.isBlank()) {
                return badRequestResponse("Request body is required for interface creation");
            }
            InterfaceConfig newIface = objectMapper.readValue(body, InterfaceConfig.class);

            if (newIface.getName() == null || newIface.getName().isBlank()) {
                return badRequestResponse("Interface name is required");
            }

            boolean created = dataStore.addInterface(deviceId, newIface);
            if (!created) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .contentType(MediaType.parseMediaType(YANG_JSON))
                        .body("{\"ietf-restconf:errors\":{\"error\":[{\"error-type\":\"application\"," +
                              "\"error-tag\":\"resource-denied\"," +
                              "\"error-message\":\"Interface already exists: " +
                              newIface.getName() + "\"}]}}");
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.parseMediaType(YANG_JSON))
                    .body("{\"status\":\"created\",\"interface\":\"" + newIface.getName() + "\"}");

        } catch (Exception e) {
            logger.error("Error creating interface: {}", e.getMessage());
            return badRequestResponse("Invalid interface data: " + e.getMessage());
        }
    }

    /**
     * UPDATE (full replace) - Replace interface configuration
     * PUT /restconf/data/network-device:device={deviceId}/interface={name:.+}
     */
    @PutMapping(value = "/data/network-device:device={deviceId}/interface={name:.+}",
                consumes = YANG_JSON, produces = YANG_JSON)
    public ResponseEntity<String> updateInterface(@PathVariable String deviceId,
                                                   @PathVariable String name,
                                                   @RequestBody(required = false) String body) {
        String decoded = name.replace("%2F", "/").replace("%2f", "/");
        logger.info("RESTCONF PUT interface: {} on device: {}", decoded, deviceId);

        try {
            if (body == null || body.isBlank()) {
                return badRequestResponse("Request body is required for interface update");
            }
            InterfaceConfig updatedIface = objectMapper.readValue(body, InterfaceConfig.class);
            updatedIface.setName(decoded);

            boolean updated = dataStore.updateInterface(deviceId, updatedIface);
            if (!updated) {
                return notFoundResponse("Interface not found: " + decoded);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(YANG_JSON))
                    .body("{\"status\":\"updated\",\"interface\":\"" + decoded + "\"}");

        } catch (Exception e) {
            logger.error("Error updating interface: {}", e.getMessage());
            return badRequestResponse("Invalid interface data: " + e.getMessage());
        }
    }

    /**
     * UPDATE (partial) - Patch interface configuration
     * PATCH /restconf/data/network-device:device={deviceId}/interface={name:.+}
     */
    @PatchMapping(value = "/data/network-device:device={deviceId}/interface={name:.+}",
                  consumes = YANG_JSON, produces = YANG_JSON)
    public ResponseEntity<String> patchInterface(@PathVariable String deviceId,
                                                  @PathVariable String name,
                                                  @RequestBody(required = false) String body) {
        String decoded = name.replace("%2F", "/").replace("%2f", "/");
        logger.info("RESTCONF PATCH interface: {} on device: {}", decoded, deviceId);

        try {
            if (body == null || body.isBlank()) {
                return badRequestResponse("Request body is required for interface patch");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> patchData = objectMapper.readValue(body, Map.class);

            boolean patched = dataStore.patchInterface(deviceId, decoded, patchData);
            if (!patched) {
                return notFoundResponse("Interface not found: " + decoded);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(YANG_JSON))
                    .body("{\"status\":\"patched\",\"interface\":\"" + decoded + "\"}");

        } catch (Exception e) {
            logger.error("Error patching interface: {}", e.getMessage());
            return badRequestResponse("Invalid patch data: " + e.getMessage());
        }
    }

    /**
     * DELETE - Remove an interface
     * DELETE /restconf/data/network-device:device={deviceId}/interface={name:.+}
     */
    @DeleteMapping(value = "/data/network-device:device={deviceId}/interface={name:.+}",
                   produces = YANG_JSON)
    public ResponseEntity<String> deleteInterface(@PathVariable String deviceId,
                                                   @PathVariable String name) {
        String decoded = name.replace("%2F", "/").replace("%2f", "/");
        logger.info("RESTCONF DELETE interface: {} on device: {}", decoded, deviceId);

        boolean deleted = dataStore.deleteInterface(deviceId, decoded);
        if (!deleted) {
            return notFoundResponse("Interface not found: " + decoded);
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * Reset simulator data to initial state
     */
    @PostMapping("/operations/reset")
    public ResponseEntity<String> resetSimulator() {
        logger.info("RESTCONF simulator data reset");
        dataStore.resetData();
        return ResponseEntity.ok("{\"status\":\"reset\",\"message\":\"Simulator data restored\"}");
    }

    // ==================== Helper Methods ====================

    private ResponseEntity<String> notFoundResponse(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.parseMediaType(YANG_JSON))
                .body("{\"ietf-restconf:errors\":{\"error\":[{\"error-type\":\"application\"," +
                      "\"error-tag\":\"data-missing\",\"error-message\":\"" + message + "\"}]}}");
    }

    private ResponseEntity<String> badRequestResponse(String message) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.parseMediaType(YANG_JSON))
                .body("{\"ietf-restconf:errors\":{\"error\":[{\"error-type\":\"protocol\"," +
                      "\"error-tag\":\"invalid-value\",\"error-message\":\"" + message + "\"}]}}");
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
