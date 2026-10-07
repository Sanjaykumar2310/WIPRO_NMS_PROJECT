package com.example.restconfai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for RESTCONF connection settings.
 *
 * What: Centralizes all RESTCONF-related configuration.
 * Why: Makes RESTCONF base URL, credentials, and mode easily configurable.
 * How: Reads from application.properties / environment variables.
 * Example: Switch between simulator and real mode by changing restconf.mode property.
 */
@Configuration
public class RestconfConfig {

    @Value("${restconf.mode:simulator}")
    private String mode;

    @Value("${restconf.base-url:https://localhost:8443/restconf}")
    private String baseUrl;

    @Value("${restconf.username:admin}")
    private String username;

    @Value("${restconf.password:admin}")
    private String password;

    @Value("${restconf.connection-timeout:5000}")
    private int connectionTimeout;

    @Value("${restconf.read-timeout:10000}")
    private int readTimeout;

    @Value("${restconf.ssl-verify:false}")
    private boolean sslVerify;

    public boolean isSimulatorMode() {
        return "simulator".equalsIgnoreCase(mode);
    }

    public String getMode() { return mode; }
    public String getBaseUrl() { return baseUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public int getReadTimeout() { return readTimeout; }
    public boolean isSslVerify() { return sslVerify; }
}
