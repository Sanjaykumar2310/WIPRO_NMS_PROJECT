package com.example.restconfai.controller;

import com.example.restconfai.model.Device;
import com.example.restconfai.service.DeviceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for device management.
 *
 * What: Exposes API endpoints for managing network devices.
 * Why: Users need to add, view, update, and remove devices from the application.
 * How: Maps HTTP requests to DeviceService methods.
 * Example: POST /api/devices with device details to register a new device.
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * GET /api/devices — List all registered devices
     */
    @GetMapping
    public ResponseEntity<List<Device>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    /**
     * GET /api/devices/{id} — Get a specific device
     */
    @GetMapping("/{id}")
    public ResponseEntity<Device> getDevice(@PathVariable String id) {
        Device device = deviceService.getDevice(id);
        if (device == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(device);
    }

    /**
     * POST /api/devices — Register a new device
     */
    @PostMapping
    public ResponseEntity<Device> addDevice(@Valid @RequestBody Device device) {
        Device created = deviceService.addDevice(device);
        logger.info("Device registered: {}", created.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/devices/{id} — Update a device
     */
    @PutMapping("/{id}")
    public ResponseEntity<Device> updateDevice(@PathVariable String id,
                                                @RequestBody Device device) {
        Device updated = deviceService.updateDevice(id, device);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/devices/{id} — Remove a device
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDevice(@PathVariable String id) {
        boolean deleted = deviceService.deleteDevice(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("status", "deleted", "deviceId", id));
    }

    /**
     * GET /api/devices/dashboard/stats — Get dashboard statistics
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(deviceService.getDashboardStats());
    }

    /**
     * GET /api/devices/history — Get operation history
     */
    @GetMapping("/history")
    public ResponseEntity<?> getOperationHistory(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(deviceService.getOperationHistory(limit));
    }

    /**
     * DELETE /api/devices/history/{id} — Delete specific operation history record
     */
    @DeleteMapping("/history/{id}")
    public ResponseEntity<Map<String, String>> deleteOperationHistory(@PathVariable String id) {
        deviceService.deleteOperationHistory(id);
        return ResponseEntity.ok(Map.of("status", "deleted", "id", id));
    }

    /**
     * DELETE /api/devices/history — Clear all operation history records
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, String>> clearAllOperationHistory() {
        deviceService.clearAllOperationHistory();
        return ResponseEntity.ok(Map.of("status", "cleared"));
    }
}
