package com.example.restconfai.simulator;

import com.example.restconfai.model.DeviceConfig;
import com.example.restconfai.model.InterfaceConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory data store for the RESTCONF simulator.
 *
 * What: Manages simulated network device data in memory.
 * Why: Allows the project to be demonstrated without physical network hardware.
 * How: Loads initial data from simulator-data.json and supports CRUD operations.
 * Example: Stores Router-01 config with interfaces, responds to GET/POST/PUT/DELETE.
 */
@Component
public class SimulatorDataStore {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorDataStore.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, DeviceConfig> devices = new ConcurrentHashMap<>();

    @Value("${simulator.data-file:classpath:data/simulator-data.json}")
    private Resource dataFile;

    @PostConstruct
    public void init() {
        loadInitialData();
    }

    private void loadInitialData() {
        try {
            InputStream is = dataFile.getInputStream();
            JsonNode root = objectMapper.readTree(is);
            JsonNode devicesNode = root.get("devices");

            if (devicesNode != null) {
                Iterator<Map.Entry<String, JsonNode>> fields = devicesNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    DeviceConfig config = objectMapper.treeToValue(
                            entry.getValue(), DeviceConfig.class);
                    devices.put(entry.getKey(), config);
                }
            }

            logger.info("Simulator loaded {} devices from data file", devices.size());
        } catch (Exception e) {
            logger.warn("Could not load simulator data, creating defaults: {}", e.getMessage());
            createDefaultData();
        }
    }

    private void createDefaultData() {
        DeviceConfig router = new DeviceConfig();
        router.setHostname("Router-01");
        router.setVendor("Cisco");
        router.setModel("CSR1000V");
        router.setOsVersion("IOS-XE 17.3.4");
        router.setSerialNumber("SN-DEFAULT-001");
        router.setStatus("running");
        router.setUptime(864000);

        InterfaceConfig gi1 = new InterfaceConfig(
                "GigabitEthernet1", "192.168.1.1", true, "up", "Management Interface");
        InterfaceConfig gi2 = new InterfaceConfig(
                "GigabitEthernet2", "10.0.0.1", true, "up", "LAN Interface");
        InterfaceConfig gi3 = new InterfaceConfig(
                "GigabitEthernet3", "172.16.0.1", false, "down", "WAN Interface");

        router.setInterfaces(new ArrayList<>(List.of(gi1, gi2, gi3)));
        devices.put("device-001", router);

        logger.info("Created default simulator data with 1 device");
    }

    // ==================== Device Operations ====================

    public Map<String, DeviceConfig> getAllDevices() {
        return Collections.unmodifiableMap(devices);
    }

    public DeviceConfig getDevice(String deviceId) {
        DeviceConfig device = devices.get(deviceId);
        if (device == null && deviceId != null && !deviceId.isBlank()) {
            DeviceConfig base = devices.get("device-001");
            if (base != null) {
                device = new DeviceConfig();
                device.setHostname("Device-" + deviceId.substring(0, Math.min(6, deviceId.length())));
                device.setVendor(base.getVendor());
                device.setModel(base.getModel());
                device.setOsVersion(base.getOsVersion());
                device.setSerialNumber("SN-" + deviceId.toUpperCase());
                device.setStatus("running");
                device.setUptime(864000);

                List<InterfaceConfig> ifaces = new ArrayList<>();
                if (base.getInterfaces() != null) {
                    for (InterfaceConfig ic : base.getInterfaces()) {
                        ifaces.add(new InterfaceConfig(ic.getName(), ic.getIpAddress(), ic.isEnabled(), ic.getStatus(), ic.getDescription()));
                    }
                }
                device.setInterfaces(ifaces);
                devices.put(deviceId, device);
            }
        }
        return device;
    }

    public String getDeviceAsJson(String deviceId) {
        try {
            DeviceConfig device = devices.get(deviceId);
            if (device == null) return null;

            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.set("network-device:device", objectMapper.valueToTree(device));
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapper);
        } catch (Exception e) {
            logger.error("Error serializing device: {}", e.getMessage());
            return null;
        }
    }

    public void updateDeviceHostname(String deviceId, String hostname) {
        DeviceConfig device = devices.get(deviceId);
        if (device != null) {
            device.setHostname(hostname);
        }
    }

    // ==================== Interface Operations ====================

    public String getAllInterfacesAsJson(String deviceId) {
        try {
            DeviceConfig device = devices.get(deviceId);
            if (device == null) return null;

            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.set("network-device:interfaces", objectMapper.valueToTree(device.getInterfaces()));
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapper);
        } catch (Exception e) {
            logger.error("Error serializing interfaces: {}", e.getMessage());
            return null;
        }
    }

    public InterfaceConfig getInterface(String deviceId, String interfaceName) {
        DeviceConfig device = devices.get(deviceId);
        if (device == null) return null;

        return device.getInterfaces().stream()
                .filter(i -> i.getName().equals(interfaceName))
                .findFirst()
                .orElse(null);
    }

    public String getInterfaceAsJson(String deviceId, String interfaceName) {
        try {
            InterfaceConfig iface = getInterface(deviceId, interfaceName);
            if (iface == null) return null;

            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.set("network-device:interface", objectMapper.valueToTree(iface));
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapper);
        } catch (Exception e) {
            logger.error("Error serializing interface: {}", e.getMessage());
            return null;
        }
    }

    public boolean addInterface(String deviceId, InterfaceConfig newInterface) {
        DeviceConfig device = devices.get(deviceId);
        if (device == null) return false;

        // Check if interface already exists
        boolean exists = device.getInterfaces().stream()
                .anyMatch(i -> i.getName().equals(newInterface.getName()));
        if (exists) return false;

        device.getInterfaces().add(newInterface);
        return true;
    }

    public boolean updateInterface(String deviceId, InterfaceConfig updatedInterface) {
        DeviceConfig device = devices.get(deviceId);
        if (device == null) return false;

        for (int i = 0; i < device.getInterfaces().size(); i++) {
            if (device.getInterfaces().get(i).getName().equals(updatedInterface.getName())) {
                device.getInterfaces().set(i, updatedInterface);
                return true;
            }
        }
        return false;
    }

    public boolean patchInterface(String deviceId, String interfaceName,
                                   Map<String, Object> patchData) {
        DeviceConfig device = devices.get(deviceId);
        if (device == null) return false;

        for (InterfaceConfig iface : device.getInterfaces()) {
            if (iface.getName().equals(interfaceName)) {
                if (patchData.containsKey("ipAddress"))
                    iface.setIpAddress((String) patchData.get("ipAddress"));
                if (patchData.containsKey("enabled"))
                    iface.setEnabled((Boolean) patchData.get("enabled"));
                if (patchData.containsKey("status"))
                    iface.setStatus((String) patchData.get("status"));
                if (patchData.containsKey("description"))
                    iface.setDescription((String) patchData.get("description"));
                if (patchData.containsKey("mtu"))
                    iface.setMtu((String) patchData.get("mtu"));
                return true;
            }
        }
        return false;
    }

    public boolean deleteInterface(String deviceId, String interfaceName) {
        DeviceConfig device = devices.get(deviceId);
        if (device == null) return false;

        return device.getInterfaces().removeIf(i -> i.getName().equals(interfaceName));
    }

    // ==================== Device CRUD ====================

    public String addDevice(String deviceId, DeviceConfig config) {
        if (devices.containsKey(deviceId)) return null;
        devices.put(deviceId, config);
        return deviceId;
    }

    public boolean deleteDevice(String deviceId) {
        return devices.remove(deviceId) != null;
    }

    public void resetData() {
        devices.clear();
        loadInitialData();
        logger.info("Simulator data reset to initial state");
    }
}
