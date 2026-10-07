package com.example.restconfai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Represents a network interface configuration.
 *
 * What: Data model for a network interface (matches YANG model).
 * Why: RESTCONF manages interface configurations on network devices.
 * How: Serialized to/from JSON for RESTCONF communication.
 * Example: GigabitEthernet0/1, IP 192.168.1.1, enabled, status up.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterfaceConfig {

    @NotBlank(message = "Interface name is required")
    private String name;

    @Pattern(regexp = "^$|^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}(/\\d{1,2})?$",
             message = "Invalid IP address format")
    private String ipAddress;

    private String subnetMask;
    private boolean enabled;
    private String status;
    private String description;
    private String speed;
    private String mtu;

    public InterfaceConfig() {
        this.enabled = true;
        this.status = "up";
        this.subnetMask = "255.255.255.0";
        this.mtu = "1500";
    }

    public InterfaceConfig(String name, String ipAddress, boolean enabled, String status, String description) {
        this();
        this.name = name;
        this.ipAddress = ipAddress;
        this.enabled = enabled;
        this.status = status;
        this.description = description;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getSubnetMask() { return subnetMask; }
    public void setSubnetMask(String subnetMask) { this.subnetMask = subnetMask; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSpeed() { return speed; }
    public void setSpeed(String speed) { this.speed = speed; }

    public String getMtu() { return mtu; }
    public void setMtu(String mtu) { this.mtu = mtu; }

    @Override
    public String toString() {
        return "Interface{name='" + name + "', ip='" + ipAddress +
               "', enabled=" + enabled + ", status='" + status + "'}";
    }
}
