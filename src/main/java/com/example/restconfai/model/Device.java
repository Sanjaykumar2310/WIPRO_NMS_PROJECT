package com.example.restconfai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a network device managed through RESTCONF.
 *
 * What: Data model for a network device with connection details.
 * Why: The application needs to track devices it manages via RESTCONF.
 * How: Stored in-memory; used by DeviceService and CrudService.
 * Example: Router-01 at 192.168.1.1 with RESTCONF enabled.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Device {

    private String id;

    @NotBlank(message = "Device name is required")
    private String name;

    @NotBlank(message = "IP address is required")
    @Pattern(regexp = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}(/\\d{1,2})?$",
             message = "Invalid IP address format")
    private String ipAddress;

    private String restconfUrl;

    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String vendor;
    private String model;
    private String status;
    private boolean connected;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;

    public Device() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.status = "unknown";
        this.connected = false;
        this.createdAt = LocalDateTime.now();
    }

    public Device(String name, String ipAddress, String restconfUrl) {
        this();
        this.name = name;
        this.ipAddress = ipAddress;
        this.restconfUrl = restconfUrl;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getRestconfUrl() { return restconfUrl; }
    public void setRestconfUrl(String restconfUrl) { this.restconfUrl = restconfUrl; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Device{name='" + name + "', ip='" + ipAddress + "', status='" + status + "'}";
    }
}
