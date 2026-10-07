package com.example.restconfai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the complete device configuration as modeled by YANG.
 *
 * What: Aggregate model holding hostname and all interface configurations.
 * Why: RESTCONF returns device configuration in a hierarchical structure defined by YANG.
 * How: Maps to the YANG module 'network-device' with containers and lists.
 * Example: Router-01 with hostname, vendor info, and a list of interfaces.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceConfig {

    private String hostname;
    private String vendor;
    private String model;
    private String osVersion;
    private String serialNumber;
    private String status;
    private long uptime;
    private List<InterfaceConfig> interfaces;

    public DeviceConfig() {
        this.interfaces = new ArrayList<>();
        this.status = "running";
    }

    // Getters and Setters
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getUptime() { return uptime; }
    public void setUptime(long uptime) { this.uptime = uptime; }

    public List<InterfaceConfig> getInterfaces() { return interfaces; }
    public void setInterfaces(List<InterfaceConfig> interfaces) { this.interfaces = interfaces; }

    @Override
    public String toString() {
        return "DeviceConfig{hostname='" + hostname + "', interfaces=" + interfaces.size() + "}";
    }
}
