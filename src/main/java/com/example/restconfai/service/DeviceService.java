package com.example.restconfai.service;

import com.example.restconfai.model.Device;
import com.example.restconfai.model.OperationHistory;
import com.example.restconfai.validation.InputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing network devices.
 *
 * What: Manages the list of network devices the application connects to.
 * Why: Users need to add/remove/view devices before performing RESTCONF operations.
 * How: Stores devices in-memory with a ConcurrentHashMap.
 * Example: Add Router-01 at 192.168.1.1 with RESTCONF URL and credentials.
 */
@Service
public class DeviceService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);

    private final Map<String, Device> devices = new ConcurrentHashMap<>();
    private final List<OperationHistory> operationHistory = Collections.synchronizedList(new ArrayList<>());
    private final InputValidator validator;

    public DeviceService(InputValidator validator) {
        this.validator = validator;
        initDefaultDevices();
    }

    private void initDefaultDevices() {
        // Pre-register simulator devices for demo
        Device router = new Device("Router-01", "192.168.1.1",
                "http://localhost:8080/restconf");
        router.setId("device-001");
        router.setVendor("Cisco");
        router.setModel("CSR1000V");
        router.setUsername("admin");
        router.setPassword("admin");
        router.setStatus("online");
        router.setConnected(true);
        router.setLastSeen(LocalDateTime.now());
        devices.put(router.getId(), router);

        Device switchDev = new Device("Switch-01", "192.168.1.2",
                "http://localhost:8080/restconf");
        switchDev.setId("device-002");
        switchDev.setVendor("Cisco");
        switchDev.setModel("Catalyst 9300");
        switchDev.setUsername("admin");
        switchDev.setPassword("admin");
        switchDev.setStatus("online");
        switchDev.setConnected(true);
        switchDev.setLastSeen(LocalDateTime.now());
        devices.put(switchDev.getId(), switchDev);

        Device firewall = new Device("Firewall-01", "192.168.1.254",
                "http://localhost:8080/restconf");
        firewall.setId("device-003");
        firewall.setVendor("Cisco");
        firewall.setModel("ASAv");
        firewall.setUsername("admin");
        firewall.setPassword("admin");
        firewall.setStatus("online");
        firewall.setConnected(true);
        firewall.setLastSeen(LocalDateTime.now());
        devices.put(firewall.getId(), firewall);

        logger.info("Initialized {} default simulator devices", devices.size());
    }

    public List<Device> getAllDevices() {
        return new ArrayList<>(devices.values());
    }

    public Device getDevice(String id) {
        return devices.get(id);
    }

    public Device addDevice(Device device) {
        List<String> errors = validator.validateDeviceInput(
                device.getName(), device.getIpAddress(), device.getRestconfUrl());

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Validation failed: " + String.join(", ", errors));
        }

        if (device.getRestconfUrl() == null || device.getRestconfUrl().isBlank()) {
            device.setRestconfUrl("http://localhost:8080/restconf");
        }

        device.setStatus("online");
        device.setConnected(true);
        device.setLastSeen(LocalDateTime.now());
        device.setCreatedAt(LocalDateTime.now());

        devices.put(device.getId(), device);
        logger.info("Device added: {} ({})", device.getName(), device.getId());
        return device;
    }

    public Device updateDevice(String id, Device updatedDevice) {
        Device existing = devices.get(id);
        if (existing == null) {
            throw new IllegalArgumentException("Device not found: " + id);
        }

        if (updatedDevice.getName() != null) existing.setName(updatedDevice.getName());
        if (updatedDevice.getIpAddress() != null) existing.setIpAddress(updatedDevice.getIpAddress());
        if (updatedDevice.getRestconfUrl() != null) existing.setRestconfUrl(updatedDevice.getRestconfUrl());
        if (updatedDevice.getUsername() != null) existing.setUsername(updatedDevice.getUsername());
        if (updatedDevice.getPassword() != null) existing.setPassword(updatedDevice.getPassword());
        if (updatedDevice.getVendor() != null) existing.setVendor(updatedDevice.getVendor());
        if (updatedDevice.getModel() != null) existing.setModel(updatedDevice.getModel());

        logger.info("Device updated: {} ({})", existing.getName(), id);
        return existing;
    }

    public boolean deleteDevice(String id) {
        Device removed = devices.remove(id);
        if (removed != null) {
            logger.info("Device deleted: {} ({})", removed.getName(), id);
            return true;
        }
        return false;
    }

    // ==================== Operation History ====================

    public void addOperationHistory(OperationHistory history) {
        history.setId(UUID.randomUUID().toString().substring(0, 8));
        operationHistory.add(0, history); // newest first

        // Keep only last 100 entries
        if (operationHistory.size() > 100) {
            operationHistory.subList(100, operationHistory.size()).clear();
        }
    }

    public List<OperationHistory> getOperationHistory() {
        return new ArrayList<>(operationHistory);
    }

    public List<OperationHistory> getOperationHistory(int limit) {
        return operationHistory.stream().limit(limit).toList();
    }

    public boolean deleteOperationHistory(String id) {
        return operationHistory.removeIf(h -> 
            Objects.equals(h.getId(), id) || 
            (h.getTimestamp() != null && Objects.equals(h.getTimestamp().toString(), id))
        );
    }

    public void clearAllOperationHistory() {
        operationHistory.clear();
    }

    // ==================== Dashboard Stats ====================

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDevices", devices.size());
        stats.put("onlineDevices", devices.values().stream()
                .filter(d -> "online".equals(d.getStatus())).count());
        stats.put("offlineDevices", devices.values().stream()
                .filter(d -> !"online".equals(d.getStatus())).count());
        stats.put("totalOperations", operationHistory.size());
        stats.put("successfulOperations", operationHistory.stream()
                .filter(o -> o.isSuccess()).count());
        stats.put("failedOperations", operationHistory.stream()
                .filter(o -> !o.isSuccess()).count());
        stats.put("recentOperations", getOperationHistory(5));
        return stats;
    }
}
