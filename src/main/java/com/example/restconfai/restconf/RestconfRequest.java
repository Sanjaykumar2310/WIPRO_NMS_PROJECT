package com.example.restconfai.restconf;

/**
 * Represents a RESTCONF request to be sent to a network device.
 *
 * What: Encapsulates all parameters needed for a RESTCONF HTTP request.
 * Why: Provides a clean abstraction for building RESTCONF requests.
 * How: Used by RestconfClient to construct HTTP requests with proper headers.
 * Example: GET request to /restconf/data/ietf-interfaces:interfaces/interface=GigabitEthernet0/1
 */
public class RestconfRequest {

    private String method;
    private String resourcePath;
    private String body;
    private String contentType;
    private String accept;

    public RestconfRequest() {
        this.contentType = "application/yang-data+json";
        this.accept = "application/yang-data+json";
    }

    public RestconfRequest(String method, String resourcePath) {
        this();
        this.method = method;
        this.resourcePath = resourcePath;
    }

    public RestconfRequest(String method, String resourcePath, String body) {
        this(method, resourcePath);
        this.body = body;
    }

    // Getters and Setters
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getResourcePath() { return resourcePath; }
    public void setResourcePath(String resourcePath) { this.resourcePath = resourcePath; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getAccept() { return accept; }
    public void setAccept(String accept) { this.accept = accept; }

    @Override
    public String toString() {
        return method + " " + resourcePath;
    }
}
