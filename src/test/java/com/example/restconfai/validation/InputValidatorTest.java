package com.example.restconfai.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InputValidator.
 * Tests IP address, URL, hostname, interface name, and RESTCONF operation validation.
 */
class InputValidatorTest {

    private InputValidator validator;

    @BeforeEach
    void setUp() {
        validator = new InputValidator();
    }

    // ---- IP Address Validation ----

    @ParameterizedTest
    @ValueSource(strings = {"192.168.1.1", "10.0.0.1", "255.255.255.255", "0.0.0.0", "172.16.0.254"})
    @DisplayName("Should accept valid IP addresses")
    void testValidIpAddresses(String ip) {
        assertTrue(validator.isValidIpAddress(ip));
    }

    @ParameterizedTest
    @ValueSource(strings = {"256.0.0.1", "192.168.1", "abc.def.ghi.jkl", "1.2.3.4.5", ""})
    @DisplayName("Should reject invalid IP addresses")
    void testInvalidIpAddresses(String ip) {
        assertFalse(validator.isValidIpAddress(ip));
    }

    @Test
    @DisplayName("Should reject null IP address")
    void testNullIpAddress() {
        assertFalse(validator.isValidIpAddress(null));
    }

    // ---- URL Validation ----

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:8080/restconf", "https://192.168.1.1:443/restconf",
            "http://router.example.com/restconf"})
    @DisplayName("Should accept valid URLs")
    void testValidUrls(String url) {
        assertTrue(validator.isValidUrl(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://server", "not-a-url", "://missing-scheme"})
    @DisplayName("Should reject invalid URLs")
    void testInvalidUrls(String url) {
        assertFalse(validator.isValidUrl(url));
    }

    // ---- Hostname Validation ----

    @ParameterizedTest
    @ValueSource(strings = {"Router-01", "Switch01", "fw.core", "R1"})
    @DisplayName("Should accept valid hostnames")
    void testValidHostnames(String hostname) {
        assertTrue(validator.isValidHostname(hostname));
    }

    // ---- Interface Name Validation ----

    @ParameterizedTest
    @ValueSource(strings = {"GigabitEthernet0/1", "Loopback0", "Vlan10", "GigabitEthernet1/0/1"})
    @DisplayName("Should accept valid interface names")
    void testValidInterfaceNames(String name) {
        assertTrue(validator.isValidInterfaceName(name));
    }

    // ---- Device Input Validation ----

    @Test
    @DisplayName("Should validate complete device input")
    void testValidDeviceInput() {
        List<String> errors = validator.validateDeviceInput("Router-01", "192.168.1.1",
                "http://localhost:8080/restconf");
        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("Should detect missing device name")
    void testMissingDeviceName() {
        List<String> errors = validator.validateDeviceInput("", "192.168.1.1", null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("name")));
    }

    @Test
    @DisplayName("Should detect missing IP address")
    void testMissingIpAddress() {
        List<String> errors = validator.validateDeviceInput("Router", "", null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("IP")));
    }

    // ---- RESTCONF Operation Validation ----

    @Test
    @DisplayName("Should validate correct RESTCONF operation")
    void testValidRestconfOperation() {
        List<String> errors = validator.validateRestconfOperation("device-001", "GET", "/data/test");
        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("Should reject invalid HTTP method")
    void testInvalidHttpMethod() {
        List<String> errors = validator.validateRestconfOperation("device-001", "INVALID", "/data/test");
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("method")));
    }

    @Test
    @DisplayName("Should reject empty device ID")
    void testEmptyDeviceId() {
        List<String> errors = validator.validateRestconfOperation("", "GET", "/data/test");
        assertFalse(errors.isEmpty());
    }

    @Test
    @DisplayName("Should reject empty resource path")
    void testEmptyResourcePath() {
        List<String> errors = validator.validateRestconfOperation("device-001", "GET", "");
        assertFalse(errors.isEmpty());
    }

    // ---- JSON Validation ----

    @Test
    @DisplayName("Should accept valid JSON objects")
    void testValidJson() {
        assertTrue(validator.isValidJson("{\"key\":\"value\"}"));
        assertTrue(validator.isValidJson("[1,2,3]"));
    }

    @Test
    @DisplayName("Should reject invalid JSON")
    void testInvalidJson() {
        assertFalse(validator.isValidJson("not json"));
        assertFalse(validator.isValidJson(""));
        assertFalse(validator.isValidJson(null));
    }
}
