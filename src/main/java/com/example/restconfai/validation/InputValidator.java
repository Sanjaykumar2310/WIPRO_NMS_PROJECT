package com.example.restconfai.validation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Input validator for RESTCONF operations and device management.
 *
 * What: Validates user inputs before processing.
 * Why: Prevents malformed requests from reaching the RESTCONF device.
 * How: Uses regex patterns and rule-based validation.
 * Example: Validates IP address format, RESTCONF URL, and JSON payloads.
 */
@Component
public class InputValidator {

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}(/\\d{1,2})?$");

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^https?://[\\w.-]+(:\\d+)?(/.*)?$");

    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z][a-zA-Z0-9._-]{0,63}$");

    private static final Pattern INTERFACE_NAME_PATTERN = Pattern.compile(
            "^[a-zA-Z][a-zA-Z0-9/.-]{0,63}$");

    public List<String> validateDeviceInput(String name, String ipAddress, String restconfUrl) {
        List<String> errors = new ArrayList<>();

        if (name == null || name.isBlank()) {
            errors.add("Device name is required");
        } else if (name.length() > 64) {
            errors.add("Device name must be 64 characters or less");
        }

        if (ipAddress == null || ipAddress.isBlank()) {
            errors.add("IP address is required");
        } else if (!IP_PATTERN.matcher(ipAddress).matches()) {
            errors.add("Invalid IP address format: " + ipAddress);
        }

        if (restconfUrl != null && !restconfUrl.isBlank()) {
            if (!URL_PATTERN.matcher(restconfUrl).matches()) {
                errors.add("Invalid RESTCONF URL format");
            }
        }

        return errors;
    }

    public boolean isValidIpAddress(String ip) {
        return ip != null && IP_PATTERN.matcher(ip).matches();
    }

    public boolean isValidUrl(String url) {
        return url != null && URL_PATTERN.matcher(url).matches();
    }

    public boolean isValidHostname(String hostname) {
        return hostname != null && HOSTNAME_PATTERN.matcher(hostname).matches();
    }

    public boolean isValidInterfaceName(String name) {
        return name != null && INTERFACE_NAME_PATTERN.matcher(name).matches();
    }

    public List<String> validateRestconfOperation(String deviceId, String method, String resourcePath) {
        List<String> errors = new ArrayList<>();

        if (deviceId == null || deviceId.isBlank()) {
            errors.add("Device ID is required");
        }

        if (method == null || method.isBlank()) {
            errors.add("HTTP method is required");
        } else {
            String upper = method.toUpperCase();
            if (!List.of("GET", "POST", "PUT", "PATCH", "DELETE").contains(upper)) {
                errors.add("Invalid HTTP method: " + method);
            }
        }

        if (resourcePath == null || resourcePath.isBlank()) {
            errors.add("Resource path is required");
        }

        return errors;
    }

    public boolean isValidJson(String json) {
        if (json == null || json.isBlank()) return false;
        json = json.trim();
        return (json.startsWith("{") && json.endsWith("}")) ||
               (json.startsWith("[") && json.endsWith("]"));
    }
}
