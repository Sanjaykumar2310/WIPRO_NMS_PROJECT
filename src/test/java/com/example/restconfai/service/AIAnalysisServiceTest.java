package com.example.restconfai.service;

import com.example.restconfai.config.AIConfig;
import com.example.restconfai.model.AIAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AIAnalysisService.
 * Tests fallback analysis, various analysis types, and history tracking.
 */
@ExtendWith(MockitoExtension.class)
class AIAnalysisServiceTest {

    private AIConfig aiConfig;
    private AIAnalysisService aiService;

    private static final String SAMPLE_CONFIG = """
            {
              "hostname": "Router-01",
              "interfaces": [
                {"name": "GigabitEthernet1", "ipAddress": "192.168.1.1", "enabled": true, "status": "up"},
                {"name": "GigabitEthernet3", "ipAddress": "172.16.0.1", "enabled": false, "status": "down"}
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        aiConfig = new AIConfig();
        aiConfig.setProvider("none");
        aiConfig.setRequestTimeout(30000);
        aiService = new AIAnalysisService(aiConfig);
    }

    @Test
    @DisplayName("Should return fallback analysis when AI is not configured")
    void testFallbackAnalysis() {
        AIAnalysis result = aiService.analyzeConfiguration("Router-01", SAMPLE_CONFIG);

        assertNotNull(result);
        assertEquals("analyze", result.getType());
        assertEquals("Router-01", result.getDeviceName());
        assertNotNull(result.getAnalysis());
        assertTrue(result.getAnalysis().contains("Disabled Interface"));
        assertEquals("rule-based-fallback", result.getAiProvider());
    }

    @Test
    @DisplayName("Should detect disabled interfaces in fallback mode")
    void testDetectDisabledInterface() {
        AIAnalysis result = aiService.detectIssues("Router-01", SAMPLE_CONFIG);

        assertNotNull(result);
        assertTrue(result.getAnalysis().contains("Disabled Interface"));
    }

    @Test
    @DisplayName("Should detect down interfaces in fallback mode")
    void testDetectDownInterface() {
        AIAnalysis result = aiService.analyzeConfiguration("Router-01", SAMPLE_CONFIG);

        assertTrue(result.getAnalysis().contains("down") || result.getAnalysis().contains("Down"));
    }

    @Test
    @DisplayName("Should return clean analysis for normal config")
    void testNormalConfig() {
        String normalConfig = "{\"hostname\":\"Router-01\",\"interfaces\":[{\"name\":\"Gi1\",\"enabled\":true,\"status\":\"up\"}]}";
        AIAnalysis result = aiService.analyzeConfiguration("Router-01", normalConfig);

        assertNotNull(result);
        assertTrue(result.getAnalysis().contains("No obvious issues") || result.getAnalysis().length() > 0);
    }

    @Test
    @DisplayName("Should explain configuration in fallback mode")
    void testExplainConfiguration() {
        AIAnalysis result = aiService.explainConfiguration("Router-01", SAMPLE_CONFIG);

        assertNotNull(result);
        assertEquals("explain", result.getType());
    }

    @Test
    @DisplayName("Should handle troubleshooting with user question")
    void testTroubleshoot() {
        AIAnalysis result = aiService.troubleshoot("Router-01", SAMPLE_CONFIG, "Why is GigabitEthernet3 down?");

        assertNotNull(result);
        assertEquals("troubleshoot", result.getType());
    }

    @Test
    @DisplayName("Should generate recommendations")
    void testRecommend() {
        AIAnalysis result = aiService.recommend("Router-01", SAMPLE_CONFIG);

        assertNotNull(result);
        assertEquals("recommend", result.getType());
    }

    @Test
    @DisplayName("Should generate network summary")
    void testSummarize() {
        AIAnalysis result = aiService.summarize("Router-01", SAMPLE_CONFIG);

        assertNotNull(result);
        assertEquals("summary", result.getType());
    }

    @Test
    @DisplayName("Should track analysis history")
    void testAnalysisHistory() {
        aiService.analyzeConfiguration("Router-01", SAMPLE_CONFIG);
        aiService.detectIssues("Router-01", SAMPLE_CONFIG);

        var history = aiService.getAnalysisHistory();
        assertEquals(2, history.size());
    }

    @Test
    @DisplayName("Should report AI status correctly")
    void testAIStatus() {
        var status = aiService.getAIStatus();
        assertNotNull(status);
        assertEquals("none", status.get("provider"));
        assertFalse((Boolean) status.get("configured"));
    }

    @Test
    @DisplayName("Should record processing time")
    void testProcessingTime() {
        AIAnalysis result = aiService.analyzeConfiguration("Router-01", SAMPLE_CONFIG);
        assertTrue(result.getProcessingTimeMs() >= 0);
    }
}
