package com.example.restconfai.model;

import java.time.LocalDateTime;

/**
 * Holds the result of an AI analysis on network data.
 *
 * What: Data model for AI-generated analysis results.
 * Why: AI analysis results need a structured format for frontend display.
 * How: Created by AIAnalysisService after processing RESTCONF data through AI.
 * Example: AI analyzes a disabled interface and returns explanation + recommendations.
 */
public class AIAnalysis {

    private String id;
    private String type;
    private String deviceName;
    private String resourcePath;
    private String inputData;
    private String analysis;
    private String recommendations;
    private String severity;
    private String aiProvider;
    private LocalDateTime timestamp;
    private long processingTimeMs;

    public AIAnalysis() {
        this.timestamp = LocalDateTime.now();
    }

    public AIAnalysis(String type, String analysis, String recommendations) {
        this();
        this.type = type;
        this.analysis = analysis;
        this.recommendations = recommendations;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getResourcePath() { return resourcePath; }
    public void setResourcePath(String resourcePath) { this.resourcePath = resourcePath; }

    public String getInputData() { return inputData; }
    public void setInputData(String inputData) { this.inputData = inputData; }

    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }

    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getAiProvider() { return aiProvider; }
    public void setAiProvider(String aiProvider) { this.aiProvider = aiProvider; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
}
