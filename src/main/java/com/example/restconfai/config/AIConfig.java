package com.example.restconfai.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Configuration for AI service integration.
 *
 * What: Centralizes AI provider settings (Gemini, OpenAI, HuggingFace).
 * Why: Allows switching AI providers without code changes and supports dynamic key updates.
 * How: Reads provider choice and API keys from environment variables and .env file.
 */
@Configuration
public class AIConfig {

    private static final Logger logger = LoggerFactory.getLogger(AIConfig.class);

    @Value("${ai.provider:auto}")
    private String provider = "auto";

    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey = "";

    @Value("${ai.gemini.model:gemini-1.5-flash}")
    private String geminiModel = "gemini-1.5-flash";

    @Value("${ai.gemini.api-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiUrl = "https://generativelanguage.googleapis.com/v1beta/models";

    @Value("${ai.openai.api-key:}")
    private String openaiApiKey = "";

    @Value("${ai.openai.model:gpt-3.5-turbo}")
    private String openaiModel = "gpt-3.5-turbo";

    @Value("${ai.openai.api-url:https://api.openai.com/v1/chat/completions}")
    private String openaiApiUrl = "https://api.openai.com/v1/chat/completions";

    @Value("${ai.huggingface.api-key:}")
    private String huggingfaceApiKey = "";

    @Value("${ai.huggingface.model:Qwen/Qwen2.5-Coder-32B-Instruct}")
    private String huggingfaceModel = "Qwen/Qwen2.5-Coder-32B-Instruct";

    @Value("${ai.huggingface.api-url:https://router.huggingface.co/v1/chat/completions}")
    private String huggingfaceApiUrl = "https://router.huggingface.co/v1/chat/completions";

    @Value("${ai.request-timeout:60000}")
    private int requestTimeout = 60000;

    @Value("${ai.max-tokens:2048}")
    private int maxTokens = 2048;

    @PostConstruct
    public void init() {
        loadFromEnvFile();
    }

    public synchronized void loadFromEnvFile() {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            envFile = new File(".env.example");
        }
        if (!envFile.exists()) return;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(envFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String key = line.substring(0, eq).trim();
                    String val = line.substring(eq + 1).trim();
                    if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                        val = val.substring(1, val.length() - 1);
                    }
                    // Skip placeholders
                    if (val.contains("your-") || val.contains("-here")) continue;

                    switch (key) {
                        case "AI_PROVIDER" -> {
                            if (!val.isBlank()) this.provider = val;
                        }
                        case "GEMINI_API_KEY" -> {
                            if (!val.isBlank()) this.geminiApiKey = val;
                        }
                        case "OPENAI_API_KEY" -> {
                            if (!val.isBlank()) this.openaiApiKey = val;
                        }
                        case "HUGGINGFACE_API_KEY" -> {
                            if (!val.isBlank()) this.huggingfaceApiKey = val;
                        }
                        case "GEMINI_MODEL" -> {
                            if (!val.isBlank()) this.geminiModel = val;
                        }
                        case "OPENAI_MODEL" -> {
                            if (!val.isBlank()) this.openaiModel = val;
                        }
                        case "AI_MAX_TOKENS", "MAX_TOKENS" -> {
                            try {
                                if (!val.isBlank()) this.maxTokens = Integer.parseInt(val);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
            logger.info("Loaded environment settings from {}", envFile.getName());
        } catch (Exception e) {
            logger.warn("Could not read environment file: {}", e.getMessage());
        }
    }

    public synchronized void saveToEnvFile() {
        File envFile = new File(".env");
        Map<String, String> envMap = new LinkedHashMap<>();

        if (envFile.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(envFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        envMap.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                    }
                }
            } catch (Exception ignored) {}
        }

        envMap.put("AI_PROVIDER", provider != null ? provider : "auto");
        envMap.put("GEMINI_API_KEY", geminiApiKey != null ? geminiApiKey : "");
        envMap.put("OPENAI_API_KEY", openaiApiKey != null ? openaiApiKey : "");
        envMap.put("HUGGINGFACE_API_KEY", huggingfaceApiKey != null ? huggingfaceApiKey : "");
        envMap.put("GEMINI_MODEL", geminiModel != null ? geminiModel : "gemini-1.5-flash");
        envMap.put("OPENAI_MODEL", openaiModel != null ? openaiModel : "gpt-3.5-turbo");
        envMap.put("AI_MAX_TOKENS", String.valueOf(maxTokens));

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(envFile), StandardCharsets.UTF_8))) {
            pw.println("# ============================================");
            pw.println("# Environment Variables for RESTCONF AI Project");
            pw.println("# Updated via Web Settings");
            pw.println("# ============================================");
            for (Map.Entry<String, String> entry : envMap.entrySet()) {
                pw.println(entry.getKey() + "=" + entry.getValue());
            }
            logger.info("Saved updated settings to .env file");
        } catch (Exception e) {
            logger.error("Failed to write .env file: {}", e.getMessage());
        }
    }

    public String getActiveProvider() {
        if (provider != null && !provider.isBlank() && !"none".equalsIgnoreCase(provider) && !"auto".equalsIgnoreCase(provider)) {
            return provider.toLowerCase();
        }
        // Auto-detect first available key
        if (huggingfaceApiKey != null && !huggingfaceApiKey.isBlank()) return "huggingface";
        if (geminiApiKey != null && !geminiApiKey.isBlank()) return "gemini";
        if (openaiApiKey != null && !openaiApiKey.isBlank()) return "openai";

        return "none";
    }

    public boolean isConfigured() {
        String active = getActiveProvider();
        return switch (active.toLowerCase()) {
            case "gemini" -> geminiApiKey != null && !geminiApiKey.isBlank();
            case "openai" -> openaiApiKey != null && !openaiApiKey.isBlank();
            case "huggingface" -> huggingfaceApiKey != null && !huggingfaceApiKey.isBlank();
            default -> false;
        };
    }

    // Getters and Setters
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getGeminiApiKey() { return geminiApiKey; }
    public void setGeminiApiKey(String geminiApiKey) { this.geminiApiKey = geminiApiKey; }

    public String getGeminiModel() { return geminiModel; }
    public void setGeminiModel(String geminiModel) { this.geminiModel = geminiModel; }

    public String getGeminiApiUrl() { return geminiApiUrl; }
    public void setGeminiApiUrl(String geminiApiUrl) { this.geminiApiUrl = geminiApiUrl; }

    public String getOpenaiApiKey() { return openaiApiKey; }
    public void setOpenaiApiKey(String openaiApiKey) { this.openaiApiKey = openaiApiKey; }

    public String getOpenaiModel() { return openaiModel; }
    public void setOpenaiModel(String openaiModel) { this.openaiModel = openaiModel; }

    public String getOpenaiApiUrl() { return openaiApiUrl; }
    public void setOpenaiApiUrl(String openaiApiUrl) { this.openaiApiUrl = openaiApiUrl; }

    public String getHuggingfaceApiKey() { return huggingfaceApiKey; }
    public void setHuggingfaceApiKey(String huggingfaceApiKey) { this.huggingfaceApiKey = huggingfaceApiKey; }

    public String getHuggingfaceModel() { return huggingfaceModel; }
    public void setHuggingfaceModel(String huggingfaceModel) { this.huggingfaceModel = huggingfaceModel; }

    public String getHuggingfaceApiUrl() { return huggingfaceApiUrl; }
    public void setHuggingfaceApiUrl(String huggingfaceApiUrl) { this.huggingfaceApiUrl = huggingfaceApiUrl; }

    public int getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(int requestTimeout) { this.requestTimeout = requestTimeout; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
}

