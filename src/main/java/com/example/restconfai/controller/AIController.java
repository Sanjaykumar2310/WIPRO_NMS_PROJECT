package com.example.restconfai.controller;

import com.example.restconfai.model.AIAnalysis;
import com.example.restconfai.service.AIAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for AI analysis operations.
 *
 * What: Exposes API endpoints for AI-powered network analysis.
 * Why: Frontend needs to request AI analysis through the backend (never directly).
 * How: Receives configuration data, delegates to AIAnalysisService, returns results.
 * Example: POST /api/ai/analyze with device config data → returns AI analysis.
 *
 * SECURITY: All AI API calls go through this backend controller.
 * The frontend NEVER communicates directly with AI APIs.
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private static final Logger logger = LoggerFactory.getLogger(AIController.class);

    private final AIAnalysisService aiService;
    private final com.example.restconfai.config.AIConfig aiConfig;

    public AIController(AIAnalysisService aiService, com.example.restconfai.config.AIConfig aiConfig) {
        this.aiService = aiService;
        this.aiConfig = aiConfig;
    }

    /**
     * POST /api/ai/analyze — Analyze network configuration
     */
    @PostMapping("/analyze")
    public ResponseEntity<AIAnalysis> analyze(@RequestBody Map<String, String> request) {
        String deviceName = request.getOrDefault("deviceName", "Unknown Device");
        String configData = request.get("configData");

        logger.info("Received AI analysis request for device: {}", deviceName);
        validateInput(configData);

        AIAnalysis result = aiService.analyzeConfiguration(deviceName, configData);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/ai/explain — Explain configuration in simple language
     */
    @PostMapping("/explain")
    public ResponseEntity<AIAnalysis> explain(@RequestBody Map<String, String> request) {
        String deviceName = request.getOrDefault("deviceName", "Unknown Device");
        String configData = request.get("configData");

        logger.info("Received AI explain request for device: {}", deviceName);
        validateInput(configData);

        AIAnalysis result = aiService.explainConfiguration(deviceName, configData);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/ai/issues — Detect potential issues
     */
    @PostMapping("/issues")
    public ResponseEntity<AIAnalysis> detectIssues(@RequestBody Map<String, String> request) {
        String deviceName = request.getOrDefault("deviceName", "Unknown Device");
        String configData = request.get("configData");

        logger.info("Received AI issue detection request for device: {}", deviceName);
        validateInput(configData);

        AIAnalysis result = aiService.detectIssues(deviceName, configData);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/ai/troubleshoot — Get troubleshooting suggestions
     */
    @PostMapping("/troubleshoot")
    public ResponseEntity<AIAnalysis> troubleshoot(@RequestBody Map<String, String> request) {
        String deviceName = request.getOrDefault("deviceName", "Unknown Device");
        String configData = request.get("configData");
        String question = request.getOrDefault("question",
                "What are the potential issues with this configuration?");

        logger.info("Received AI troubleshoot request for device: {}", deviceName);
        validateInput(configData);

        AIAnalysis result = aiService.troubleshoot(deviceName, configData, question);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/ai/recommend — Get configuration recommendations
     */
    @PostMapping("/recommend")
    public ResponseEntity<AIAnalysis> recommend(@RequestBody Map<String, String> request) {
        String deviceName = request.getOrDefault("deviceName", "Unknown Device");
        String configData = request.get("configData");

        logger.info("Received AI recommendations request for device: {}", deviceName);
        validateInput(configData);

        AIAnalysis result = aiService.recommend(deviceName, configData);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/ai/summarize — Generate network summary
     */
    @PostMapping("/summarize")
    public ResponseEntity<AIAnalysis> summarize(@RequestBody Map<String, String> request) {
        String deviceName = request.getOrDefault("deviceName", "Unknown Device");
        String configData = request.get("configData");

        logger.info("Received AI summarize request for device: {}", deviceName);
        validateInput(configData);

        AIAnalysis result = aiService.summarize(deviceName, configData);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/ai/status — Check AI service status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAIStatus() {
        logger.debug("Checking AI service status");
        return ResponseEntity.ok(aiService.getAIStatus());
    }

    /**
     * POST /api/ai/settings — Update AI configuration dynamically
     */
    @PostMapping("/settings")
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, String> request) {
        logger.info("Updating AI settings");
        if (request.containsKey("provider")) aiConfig.setProvider(request.get("provider"));
        if (request.containsKey("geminiApiKey") && !request.get("geminiApiKey").contains("••••")) {
            aiConfig.setGeminiApiKey(request.get("geminiApiKey").trim());
        }
        if (request.containsKey("openaiApiKey") && !request.get("openaiApiKey").contains("••••")) {
            aiConfig.setOpenaiApiKey(request.get("openaiApiKey").trim());
        }
        if (request.containsKey("huggingfaceApiKey") && !request.get("huggingfaceApiKey").contains("••••")) {
            aiConfig.setHuggingfaceApiKey(request.get("huggingfaceApiKey").trim());
        }
        if (request.containsKey("geminiModel")) aiConfig.setGeminiModel(request.get("geminiModel").trim());
        if (request.containsKey("openaiModel")) aiConfig.setOpenaiModel(request.get("openaiModel").trim());

        aiConfig.saveToEnvFile();
        return ResponseEntity.ok(aiService.getAIStatus());
    }

    /**
     * POST /api/ai/test — Test active AI API connection
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        logger.info("Testing AI connection");
        return ResponseEntity.ok(aiService.testAIConnection());
    }

    /**
     * GET /api/ai/history — Get AI analysis history
     */
    @GetMapping("/history")
    public ResponseEntity<List<AIAnalysis>> getAnalysisHistory() {
        logger.debug("Fetching AI analysis history");
        return ResponseEntity.ok(aiService.getAnalysisHistory());
    }

    private void validateInput(String configData) {
        if (configData == null || configData.isBlank()) {
            throw new IllegalArgumentException(
                    "Configuration data is required for AI analysis");
        }
    }
}
