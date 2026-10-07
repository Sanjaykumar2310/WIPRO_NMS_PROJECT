package com.example.restconfai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the RESTCONF AI Application.
 *
 * This application demonstrates CRUD operations with RESTCONF protocol
 * and integrates AI for intelligent network configuration analysis.
 */
@SpringBootApplication
public class RestconfAiApplication {

    private static final Logger logger = LoggerFactory.getLogger(RestconfAiApplication.class);

    public static void main(String[] args) {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
        SpringApplication.run(RestconfAiApplication.class, args);
        logger.info("========================================");
        logger.info("  RESTCONF AI Application Started");
        logger.info("  Dashboard: http://localhost:8080");
        logger.info("========================================");
    }
}
