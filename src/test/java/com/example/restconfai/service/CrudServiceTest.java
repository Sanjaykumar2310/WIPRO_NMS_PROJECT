package com.example.restconfai.service;

import com.example.restconfai.config.RestconfConfig;
import com.example.restconfai.exception.RestconfException;
import com.example.restconfai.model.OperationHistory;
import com.example.restconfai.restconf.RestconfClient;
import com.example.restconfai.restconf.RestconfResponse;
import com.example.restconfai.validation.InputValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CrudService.
 * Tests CRUD operations, validation, error handling, and history recording.
 */
@ExtendWith(MockitoExtension.class)
class CrudServiceTest {

    @Mock private RestconfClient restconfClient;
    @Mock private RestconfConfig restconfConfig;

    private CrudService crudService;
    private DeviceService deviceService;
    private InputValidator validator;

    @BeforeEach
    void setUp() {
        validator = new InputValidator();
        deviceService = new DeviceService(validator);
        crudService = new CrudService(restconfClient, deviceService, restconfConfig, validator);
    }

    @Test
    @DisplayName("READ - should successfully read device config")
    void testReadSuccess() {
        RestconfResponse mockResponse = new RestconfResponse(200, "{\"hostname\":\"Router-01\"}");
        mockResponse.setExecutionTimeMs(45);
        when(restconfClient.execute(anyString(), any(), anyString(), anyString()))
                .thenReturn(mockResponse);

        RestconfResponse result = crudService.read("device-001", "/data/network-device:device=device-001");

        assertNotNull(result);
        assertEquals(200, result.getStatusCode());
        assertTrue(result.isSuccess());
        assertTrue(result.getBody().contains("Router-01"));
    }

    @Test
    @DisplayName("CREATE - should successfully create interface")
    void testCreateSuccess() {
        RestconfResponse mockResponse = new RestconfResponse(201, "{\"status\":\"created\"}");
        when(restconfClient.execute(anyString(), any(), anyString(), anyString()))
                .thenReturn(mockResponse);

        String body = "{\"name\":\"GigabitEthernet0/4\",\"ipAddress\":\"10.0.0.4\"}";
        RestconfResponse result = crudService.create("device-001",
                "/data/network-device:device=device-001/interfaces", body);

        assertNotNull(result);
        assertEquals(201, result.getStatusCode());
    }

    @Test
    @DisplayName("UPDATE - should successfully update interface via PUT")
    void testUpdateSuccess() {
        RestconfResponse mockResponse = new RestconfResponse(200, "{\"status\":\"updated\"}");
        when(restconfClient.execute(anyString(), any(), anyString(), anyString()))
                .thenReturn(mockResponse);

        String body = "{\"ipAddress\":\"10.0.0.5\",\"enabled\":true}";
        RestconfResponse result = crudService.update("device-001",
                "/data/network-device:device=device-001/interface=GigabitEthernet1", body);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    @DisplayName("DELETE - should successfully delete interface")
    void testDeleteSuccess() {
        RestconfResponse mockResponse = new RestconfResponse(204, "");
        when(restconfClient.execute(anyString(), any(), anyString(), anyString()))
                .thenReturn(mockResponse);

        RestconfResponse result = crudService.delete("device-001",
                "/data/network-device:device=device-001/interface=GigabitEthernet3");

        assertNotNull(result);
        assertEquals(204, result.getStatusCode());
    }

    @Test
    @DisplayName("Should throw exception for non-existent device")
    void testDeviceNotFound() {
        assertThrows(RestconfException.class, () ->
                crudService.read("non-existent", "/data/test"));
    }

    @Test
    @DisplayName("Should throw exception for invalid method")
    void testInvalidMethod() {
        assertThrows(IllegalArgumentException.class, () ->
                crudService.executeOperation("device-001", "", "/data/test", null));
    }

    @Test
    @DisplayName("Should record operation in history after successful READ")
    void testOperationHistoryRecorded() {
        RestconfResponse mockResponse = new RestconfResponse(200, "{}");
        when(restconfClient.execute(anyString(), any(), anyString(), anyString()))
                .thenReturn(mockResponse);

        crudService.read("device-001", "/data/network-device:device=device-001");

        var history = deviceService.getOperationHistory();
        assertFalse(history.isEmpty());
        OperationHistory latest = history.get(0);
        assertEquals("READ", latest.getOperation());
        assertEquals("GET", latest.getMethod());
        assertTrue(latest.isSuccess());
    }

    @Test
    @DisplayName("Should record failed operations in history")
    void testFailedOperationRecorded() {
        when(restconfClient.execute(anyString(), any(), anyString(), anyString()))
                .thenThrow(new RestconfException("Connection refused", 502));

        assertThrows(RestconfException.class, () ->
                crudService.read("device-001", "/data/test"));

        var history = deviceService.getOperationHistory();
        assertFalse(history.isEmpty());
        assertFalse(history.get(0).isSuccess());
    }

    @Test
    @DisplayName("PATCH - should successfully patch interface")
    void testPatchSuccess() {
        RestconfResponse mockResponse = new RestconfResponse(200, "{\"status\":\"patched\"}");
        when(restconfClient.execute(anyString(), any(), anyString(), anyString()))
                .thenReturn(mockResponse);

        RestconfResponse result = crudService.patch("device-001",
                "/data/network-device:device=device-001/interface=GigabitEthernet1",
                "{\"enabled\":false}");

        assertNotNull(result);
        assertEquals(200, result.getStatusCode());
    }
}
