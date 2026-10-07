package com.example.restconfai.service;

import com.example.restconfai.model.Device;
import com.example.restconfai.model.OperationHistory;
import com.example.restconfai.validation.InputValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DeviceService.
 * Tests device CRUD, dashboard stats, and operation history management.
 */
class DeviceServiceTest {

    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        deviceService = new DeviceService(new InputValidator());
    }

    @Test
    @DisplayName("Should load default simulator devices on init")
    void testDefaultDevicesLoaded() {
        List<Device> devices = deviceService.getAllDevices();
        assertFalse(devices.isEmpty());
        assertTrue(devices.size() >= 3);
    }

    @Test
    @DisplayName("Should retrieve device by ID")
    void testGetDevice() {
        Device device = deviceService.getDevice("device-001");
        assertNotNull(device);
        assertEquals("Router-01", device.getName());
        assertEquals("192.168.1.1", device.getIpAddress());
    }

    @Test
    @DisplayName("Should return null for non-existent device")
    void testGetNonExistentDevice() {
        assertNull(deviceService.getDevice("non-existent"));
    }

    @Test
    @DisplayName("Should add a new device")
    void testAddDevice() {
        Device newDevice = new Device("Router-99", "10.0.0.99", "http://localhost:8080/restconf");
        Device added = deviceService.addDevice(newDevice);

        assertNotNull(added);
        assertEquals("Router-99", added.getName());
        assertNotNull(deviceService.getDevice(added.getId()));
    }

    @Test
    @DisplayName("Should reject device with invalid IP")
    void testAddDeviceInvalidIp() {
        Device bad = new Device("Bad", "999.999.999.999", "http://localhost:8080/restconf");
        assertThrows(IllegalArgumentException.class, () -> deviceService.addDevice(bad));
    }

    @Test
    @DisplayName("Should reject device with empty name")
    void testAddDeviceEmptyName() {
        Device bad = new Device("", "10.0.0.1", "http://localhost:8080/restconf");
        assertThrows(IllegalArgumentException.class, () -> deviceService.addDevice(bad));
    }

    @Test
    @DisplayName("Should update device fields")
    void testUpdateDevice() {
        Device update = new Device();
        update.setName("Router-01-Updated");
        Device result = deviceService.updateDevice("device-001", update);

        assertEquals("Router-01-Updated", result.getName());
    }

    @Test
    @DisplayName("Should throw on update for non-existent device")
    void testUpdateNonExistentDevice() {
        assertThrows(IllegalArgumentException.class, () ->
                deviceService.updateDevice("fake-id", new Device()));
    }

    @Test
    @DisplayName("Should delete device")
    void testDeleteDevice() {
        assertTrue(deviceService.deleteDevice("device-003"));
        assertNull(deviceService.getDevice("device-003"));
    }

    @Test
    @DisplayName("Should return false when deleting non-existent device")
    void testDeleteNonExistentDevice() {
        assertFalse(deviceService.deleteDevice("fake-id"));
    }

    @Test
    @DisplayName("Should add and retrieve operation history")
    void testOperationHistory() {
        OperationHistory history = new OperationHistory();
        history.setDeviceId("device-001");
        history.setDeviceName("Router-01");
        history.setOperation("READ");
        history.setMethod("GET");
        history.setStatusCode(200);
        history.setSuccess(true);

        deviceService.addOperationHistory(history);

        List<OperationHistory> result = deviceService.getOperationHistory();
        assertFalse(result.isEmpty());
        assertEquals("READ", result.get(0).getOperation());
    }

    @Test
    @DisplayName("Should generate dashboard stats")
    void testDashboardStats() {
        var stats = deviceService.getDashboardStats();
        assertNotNull(stats);
        assertTrue((int) stats.get("totalDevices") >= 3);
        assertNotNull(stats.get("onlineDevices"));
        assertNotNull(stats.get("totalOperations"));
    }
}
