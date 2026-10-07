package com.example.restconfai.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CRUD Controller.
 * Tests end-to-end: Controller → Service → RestconfClient → Simulator
 */
@SpringBootTest
@AutoConfigureMockMvc
class CrudControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/devices should return device list")
    void testGetDevices() throws Exception {
        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    @DisplayName("GET /api/devices/dashboard/stats should return stats")
    void testGetDashboardStats() throws Exception {
        mockMvc.perform(get("/api/devices/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDevices").exists());
    }

    @Test
    @DisplayName("POST /api/restconf/read should read device config via RESTCONF")
    void testReadDeviceConfig() throws Exception {
        String requestBody = """
                {
                    "deviceId": "device-001",
                    "method": "GET",
                    "resourcePath": "/data/network-device:device=device-001"
                }
                """;

        mockMvc.perform(post("/api/restconf/read")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200));
    }

    @Test
    @DisplayName("GET /api/restconf/device/{id}/config should return device config")
    void testQuickReadDeviceConfig() throws Exception {
        mockMvc.perform(get("/api/restconf/device/device-001/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/restconf/device/{id}/interfaces should return interfaces")
    void testReadInterfaces() throws Exception {
        mockMvc.perform(get("/api/restconf/device/device-001/interfaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/restconf/create should create new interface")
    void testCreateInterface() throws Exception {
        String requestBody = """
                {
                    "deviceId": "device-001",
                    "resourcePath": "/data/network-device:device=device-001/interfaces",
                    "body": "{\\"name\\":\\"GigabitEthernet99\\",\\"ipAddress\\":\\"10.99.0.1\\",\\"enabled\\":true,\\"status\\":\\"up\\",\\"description\\":\\"Test Interface\\"}"
                }
                """;

        mockMvc.perform(post("/api/restconf/create")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/restconf/delete should delete interface")
    void testDeleteInterface() throws Exception {
        // First create, then delete
        String deleteBody = """
                {
                    "deviceId": "device-001",
                    "resourcePath": "/data/network-device:device=device-001/interface=Loopback0"
                }
                """;

        mockMvc.perform(post("/api/restconf/delete")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(deleteBody))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/ai/analyze should return AI analysis")
    void testAIAnalysis() throws Exception {
        String requestBody = """
                {
                    "deviceName": "Router-01",
                    "configData": "{\\"hostname\\":\\"Router-01\\",\\"interfaces\\":[{\\"name\\":\\"Gig1\\",\\"status\\":\\"up\\"}]}"
                }
                """;

        mockMvc.perform(post("/api/ai/analyze")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis").exists())
                .andExpect(jsonPath("$.type").value("analyze"));
    }

    @Test
    @DisplayName("GET /api/ai/status should return AI status")
    void testAIStatus() throws Exception {
        mockMvc.perform(get("/api/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").exists())
                .andExpect(jsonPath("$.configured").exists());
    }

    @Test
    @DisplayName("GET /api/devices/history should return operation history")
    void testOperationHistory() throws Exception {
        mockMvc.perform(get("/api/devices/history"))
                .andExpect(status().isOk());
    }
}
