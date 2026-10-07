package com.example.restconfai.service;

import com.example.restconfai.config.AIConfig;
import com.example.restconfai.model.AIAnalysis;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI Analysis Service for network configuration intelligence.
 *
 * What: Sends network data to an AI API and returns analysis/recommendations.
 * Why: Provides intelligent insights that help administrators understand and
 *      troubleshoot network configurations.
 * How: Constructs structured prompts, sends them to the configured AI provider
 *      (Gemini/OpenAI/HuggingFace), and parses the response.
 * Example: Sends a disabled interface's data → AI returns "Interface is
 *          administratively down. Verify if intentional."
 *
 * IMPORTANT: AI provides recommendations only. It does NOT make configuration
 * changes. All changes must be explicitly performed through RESTCONF by the user.
 */
@Service
public class AIAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(AIAnalysisService.class);

    private static final String SYSTEM_INSTRUCTION =
            "You are a network configuration analysis assistant. " +
            "Analyze the provided RESTCONF network data. " +
            "Identify potential configuration issues, explain the observed state, " +
            "and provide safe troubleshooting recommendations. " +
            "Do not invent information that is not present in the supplied data. " +
            "Clearly distinguish facts from assumptions. " +
            "Format your response with clear sections: Analysis, Issues Found, and Recommendations. " +
            "Ensure your response is concise, thorough, and complete without being cut off mid-sentence. " +
            "Never claim that a configuration change was performed unless explicitly told so.";

    private final AIConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final List<AIAnalysis> analysisHistory = Collections.synchronizedList(new ArrayList<>());

    public AIAnalysisService(AIConfig aiConfig) {
        this.aiConfig = aiConfig;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(aiConfig.getRequestTimeout()))
                .build();
    }

    /**
     * Analyze network configuration data.
     */
    public AIAnalysis analyzeConfiguration(String deviceName, String configData) {
        String prompt = buildAnalysisPrompt("configuration analysis", deviceName, configData,
                "Analyze this network device configuration. Identify the current state of each " +
                "interface, potential issues, and provide recommendations for improvement.");
        return executeAIRequest("analyze", deviceName, configData, prompt);
    }

    /**
     * Explain network configuration in simple language.
     */
    public AIAnalysis explainConfiguration(String deviceName, String configData) {
        String prompt = buildAnalysisPrompt("configuration explanation", deviceName, configData,
                "Explain this network configuration in simple, non-technical language. " +
                "Describe what each interface does, its current state, and how the " +
                "components are connected.");
        return executeAIRequest("explain", deviceName, configData, prompt);
    }

    /**
     * Detect potential issues in the configuration.
     */
    public AIAnalysis detectIssues(String deviceName, String configData) {
        String prompt = buildAnalysisPrompt("issue detection", deviceName, configData,
                "Examine this network configuration for potential issues. Look for: " +
                "disabled interfaces that might need to be active, missing IP addresses, " +
                "duplicate subnets, security concerns, and any configuration inconsistencies. " +
                "Rate each issue severity as: Critical, Warning, or Informational.");
        return executeAIRequest("issues", deviceName, configData, prompt);
    }

    /**
     * Generate troubleshooting suggestions.
     */
    public AIAnalysis troubleshoot(String deviceName, String configData, String userQuestion) {
        String prompt = buildAnalysisPrompt("troubleshooting", deviceName, configData,
                "The administrator has the following concern: \"" + userQuestion + "\". " +
                "Based on the provided configuration data, provide step-by-step " +
                "troubleshooting recommendations. Be specific and actionable.");
        return executeAIRequest("troubleshoot", deviceName, configData, prompt);
    }

    /**
     * Generate configuration recommendations.
     */
    public AIAnalysis recommend(String deviceName, String configData) {
        String prompt = buildAnalysisPrompt("recommendation", deviceName, configData,
                "Review this network configuration and suggest improvements. " +
                "Consider security best practices, redundancy, performance optimization, " +
                "and operational best practices. Clearly label each recommendation.");
        return executeAIRequest("recommend", deviceName, configData, prompt);
    }

    /**
     * Generate a human-readable network summary.
     */
    public AIAnalysis summarize(String deviceName, String configData) {
        String prompt = buildAnalysisPrompt("network summary", deviceName, configData,
                "Generate a concise summary of this network device. Include: " +
                "device identity, number of interfaces, active vs inactive interfaces, " +
                "IP addressing scheme, and overall health assessment.");
        return executeAIRequest("summary", deviceName, configData, prompt);
    }

    /**
     * Get AI analysis history.
     */
    public List<AIAnalysis> getAnalysisHistory() {
        return new ArrayList<>(analysisHistory);
    }

    /**
     * Check if AI is configured and available.
    /**
     * Check if AI is configured and available.
     */
    public Map<String, Object> getAIStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("provider", aiConfig.getProvider());
        status.put("activeProvider", aiConfig.getActiveProvider());
        status.put("configured", aiConfig.isConfigured());
        status.put("totalAnalyses", analysisHistory.size());
        status.put("geminiApiKeyMasked", maskKey(aiConfig.getGeminiApiKey()));
        status.put("openaiApiKeyMasked", maskKey(aiConfig.getOpenaiApiKey()));
        status.put("huggingfaceApiKeyMasked", maskKey(aiConfig.getHuggingfaceApiKey()));
        status.put("geminiModel", aiConfig.getGeminiModel());
        status.put("openaiModel", aiConfig.getOpenaiModel());
        return status;
    }

    private String maskKey(String key) {
        if (key == null || key.isBlank()) return "";
        if (key.length() <= 8) return "••••••••";
        return key.substring(0, 4) + "••••••••" + key.substring(key.length() - 4);
    }

    /**
     * Test connection to the active AI API provider.
     */
    public Map<String, Object> testAIConnection() {
        Map<String, Object> response = new HashMap<>();
        if (!aiConfig.isConfigured()) {
            response.put("success", false);
            response.put("message", "No AI API key configured. Please enter an API key in Settings.");
            return response;
        }

        String activeProvider = aiConfig.getActiveProvider();
        try {
            String aiResult = callAIProvider("Hello! Test request: reply with 'Connection successful'.");
            response.put("success", true);
            response.put("provider", activeProvider);
            response.put("message", "Connected to " + activeProvider.toUpperCase() + " successfully! Response: " + aiResult);
        } catch (Exception e) {
            logger.error("AI connection test failed for {}: ", activeProvider, e);
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) {
                msg = e.getCause() != null && e.getCause().getMessage() != null 
                        ? e.getCause().getMessage() 
                        : e.getClass().getSimpleName();
            }
            response.put("success", false);
            response.put("provider", activeProvider);
            response.put("message", "Connection failed for " + activeProvider + ": " + msg);
        }
        return response;
    }

    // ==================== Private Methods ====================

    private String buildAnalysisPrompt(String analysisType, String deviceName,
                                        String configData, String instruction) {
        return String.format(
                "Network Device: %s\n" +
                "Analysis Type: %s\n" +
                "Configuration Data:\n%s\n\n" +
                "Instruction: %s",
                deviceName, analysisType, configData, instruction);
    }

    private AIAnalysis executeAIRequest(String type, String deviceName,
                                         String configData, String prompt) {
        long startTime = System.currentTimeMillis();

        AIAnalysis analysis = new AIAnalysis();
        analysis.setId(UUID.randomUUID().toString().substring(0, 8));
        analysis.setType(type);
        analysis.setDeviceName(deviceName);
        analysis.setInputData(configData);
        analysis.setAiProvider(aiConfig.getActiveProvider());
        analysis.setTimestamp(LocalDateTime.now());

        try {
            if (!aiConfig.isConfigured()) {
                // Fallback: provide basic rule-based analysis when no AI API is configured
                String fallbackAnalysis = generateFallbackAnalysis(type, configData);
                analysis.setAnalysis(fallbackAnalysis);
                analysis.setRecommendations(
                        "Note: AI API is not configured. This is a basic rule-based analysis. " +
                        "Configure an AI provider (Gemini/OpenAI/HuggingFace) for detailed insights.");
                analysis.setSeverity("info");
                analysis.setAiProvider("rule-based-fallback");
            } else {
                String aiResponse = callAIProvider(prompt);
                analysis.setAnalysis(aiResponse);
                analysis.setRecommendations(extractRecommendations(aiResponse));
                analysis.setSeverity(determineSeverity(aiResponse));
            }

        } catch (Exception e) {
            logger.error("AI analysis failed: {}", e.getMessage());
            analysis.setAnalysis("AI analysis could not be completed: " + e.getMessage());
            analysis.setRecommendations(
                    "Please verify AI API configuration and try again.");
            analysis.setSeverity("error");
        }

        long processingTime = System.currentTimeMillis() - startTime;
        analysis.setProcessingTimeMs(processingTime);

        // Store in history
        analysisHistory.add(0, analysis);
        if (analysisHistory.size() > 50) {
            analysisHistory.subList(50, analysisHistory.size()).clear();
        }

        logger.info("AI {} analysis for {} completed in {}ms using {}",
                type, deviceName, processingTime, aiConfig.getActiveProvider());

        return analysis;
    }

    private String callAIProvider(String prompt) throws Exception {
        String activeProvider = aiConfig.getActiveProvider();
        try {
            return switch (activeProvider.toLowerCase()) {
                case "gemini" -> callGemini(prompt);
                case "openai" -> callOpenAI(prompt);
                case "huggingface" -> callHuggingFace(prompt);
                default -> throw new IllegalArgumentException(
                        "Unknown or unconfigured AI provider: " + activeProvider);
            };
        } catch (Exception e) {
            logger.warn("AI Provider [{}] failed: {}. Attempting provider fallback...", activeProvider, e.getMessage());
            // Fallback to OpenAI if Gemini fails and OpenAI key is present
            if (!"openai".equalsIgnoreCase(activeProvider) && aiConfig.getOpenaiApiKey() != null && !aiConfig.getOpenaiApiKey().isBlank()) {
                logger.info("Falling back to OpenAI provider...");
                return callOpenAI(prompt);
            }
            // Fallback to Gemini if OpenAI fails and Gemini key is present
            if (!"gemini".equalsIgnoreCase(activeProvider) && aiConfig.getGeminiApiKey() != null && !aiConfig.getGeminiApiKey().isBlank()) {
                logger.info("Falling back to Gemini provider...");
                return callGemini(prompt);
            }
            throw e;
        }
    }

    private String callGemini(String prompt) throws Exception {
        String model = aiConfig.getGeminiModel();
        if (model == null || model.isBlank()) model = "gemini-1.5-flash";
        
        try {
            return executeGeminiCall(model, prompt);
        } catch (Exception e) {
            // If 404 or model error, fallback to gemini-1.5-flash or gemini-1.5-pro
            if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().contains("not found") || e.getMessage().contains("gemini-2.0-flash"))) {
                logger.warn("Gemini model {} returned error, falling back to gemini-1.5-flash", model);
                return executeGeminiCall("gemini-1.5-flash", prompt);
            }
            throw e;
        }
    }

    private String executeGeminiCall(String model, String prompt) throws Exception {
        String url = aiConfig.getGeminiApiUrl() + "/" + model +
                     ":generateContent?key=" + aiConfig.getGeminiApiKey();

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))),
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "maxOutputTokens", aiConfig.getMaxTokens())
        ));

        String responseBody = sendHttpRequest(url, requestBody, null);

        // Parse Gemini response
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                return parts.get(0).path("text").asText("No response from AI");
            }
        }

        // Check for error
        if (root.has("error")) {
            throw new RuntimeException("Gemini API error: " +
                    root.path("error").path("message").asText());
        }

        return "No response from Gemini AI";
    }

    private String callOpenAI(String prompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", aiConfig.getOpenaiModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_INSTRUCTION),
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", aiConfig.getMaxTokens()
        ));

        String responseBody = sendHttpRequest(
                aiConfig.getOpenaiApiUrl(), requestBody,
                "Bearer " + aiConfig.getOpenaiApiKey());

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            return choices.get(0).path("message").path("content").asText("No response from AI");
        }
        return "No response from OpenAI";
    }

    private String callHuggingFace(String prompt) throws Exception {
        String model = aiConfig.getHuggingfaceModel();
        if (model == null || model.isBlank() || model.contains("Mistral-7B-Instruct-v0.1")) {
            model = "Qwen/Qwen2.5-Coder-32B-Instruct";
        }

        String apiKey = aiConfig.getHuggingfaceApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Hugging Face API key is missing. Please enter your API key in Settings.");
        }

        String url = aiConfig.getHuggingfaceApiUrl();
        if (url == null || url.isBlank() || url.contains("api-inference.huggingface.co")) {
            url = "https://router.huggingface.co/v1/chat/completions";
        }

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_INSTRUCTION),
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", aiConfig.getMaxTokens()
        ));

        String responseBody = sendHttpRequest(url, requestBody, "Bearer " + apiKey.trim());

        JsonNode root = objectMapper.readTree(responseBody);

        // Parse OpenAI router format: {"choices": [{"message": {"content": "..."}}]}
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode msg = choices.get(0).path("message").path("content");
            if (!msg.isMissingNode()) {
                return msg.asText();
            }
        }

        // Direct array response fallback: [{"generated_text": "..."}]
        if (root.isArray() && !root.isEmpty()) {
            JsonNode first = root.get(0);
            if (first.has("generated_text")) return first.get("generated_text").asText();
            return first.asText();
        }

        // Direct object response fallback
        if (root.has("generated_text")) {
            return root.get("generated_text").asText();
        }

        // Error object check
        if (root.has("error")) {
            JsonNode err = root.get("error");
            String errStr = err.isObject() ? err.path("message").asText(err.toString()) : err.asText();
            throw new RuntimeException("HuggingFace Error: " + errStr);
        }

        return "No response from Hugging Face AI";
    }

    private String sendHttpRequest(String url, String body, String authHeader) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(aiConfig.getRequestTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (authHeader != null) {
            builder.header("Authorization", authHeader);
        }

        HttpResponse<String> response = httpClient.send(
                builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("AI API returned " + response.statusCode() +
                    ": " + response.body());
        }

        return response.body();
    }

    /**
     * Basic rule-based analysis fallback when no AI provider is configured.
     */
    private String generateFallbackAnalysis(String type, String configData) {
        StringBuilder analysis = new StringBuilder();
        String data = configData.toLowerCase();

        analysis.append("## Basic Configuration Analysis\n\n");

        // Check for disabled interfaces
        if (data.contains("\"enabled\":false") || data.contains("\"enabled\": false")) {
            analysis.append("⚠️ **Disabled Interface(s) Detected**: One or more interfaces are " +
                    "administratively disabled. Verify if this is intentional.\n\n");
        }

        // Check for down interfaces
        if (data.contains("\"status\":\"down\"") || data.contains("\"status\": \"down\"")) {
            analysis.append("⚠️ **Interface(s) Down**: One or more interfaces show 'down' status. " +
                    "This could indicate connectivity issues or administrative shutdown.\n\n");
        }

        // Check for interfaces without IP
        if (data.contains("\"ipaddress\":\"\"") || data.contains("\"ipAddress\":\"\"")) {
            analysis.append("ℹ️ **Interface(s) Without IP**: Some interfaces have no IP address " +
                    "configured. This is normal for L2 switchports but unusual for L3 interfaces.\n\n");
        }

        // Check for running status
        if (data.contains("\"status\":\"running\"") || data.contains("\"status\": \"running\"")) {
            analysis.append("✅ **Device Running**: The device is in operational state.\n\n");
        }

        if (analysis.toString().equals("## Basic Configuration Analysis\n\n")) {
            analysis.append("✅ No obvious issues detected in the configuration.\n");
        }

        analysis.append("\n---\n*This is a rule-based analysis. Configure an AI API key " +
                "for more detailed, context-aware insights.*");

        return analysis.toString();
    }

    private String extractRecommendations(String aiResponse) {
        // Try to extract recommendations section from AI response
        String lower = aiResponse.toLowerCase();
        int recIndex = lower.indexOf("recommendation");
        if (recIndex >= 0) {
            return aiResponse.substring(recIndex);
        }
        return "See the full analysis above for detailed recommendations.";
    }

    private String determineSeverity(String aiResponse) {
        String lower = aiResponse.toLowerCase();
        if (lower.contains("critical") || lower.contains("severe") || lower.contains("urgent")) {
            return "critical";
        } else if (lower.contains("warning") || lower.contains("caution") || lower.contains("issue")) {
            return "warning";
        }
        return "info";
    }
}
