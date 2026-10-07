package com.example.restconfai.restconf;

import com.example.restconfai.config.RestconfConfig;
import com.example.restconfai.exception.RestconfException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;

/**
 * RESTCONF Client for communicating with network devices.
 *
 * What: Java HTTP client component for RESTCONF protocol operations.
 * Why: The application needs to send GET/POST/PUT/PATCH/DELETE to RESTCONF servers.
 * How: Uses Java 17 HttpClient with proper RESTCONF headers and authentication.
 * Example: GET /restconf/data/ietf-interfaces:interfaces from Router-01.
 *
 * Supports two modes:
 * - Simulator: Routes requests to the built-in mock RESTCONF server
 * - Real: Sends requests to an actual RESTCONF-enabled device
 */
@Component
public class RestconfClient {

    private static final Logger logger = LoggerFactory.getLogger(RestconfClient.class);

    private final RestconfConfig config;
    private final HttpClient httpClient;

    public RestconfClient(RestconfConfig config) {
        this.config = config;
        this.httpClient = buildHttpClient();
    }

    /**
     * Execute a RESTCONF operation against a device.
     *
     * @param baseUrl  The RESTCONF base URL of the device
     * @param request  The RESTCONF request details
     * @param username Device username for authentication
     * @param password Device password for authentication
     * @return RestconfResponse containing status and body
     */
    public RestconfResponse execute(String baseUrl, RestconfRequest request,
                                     String username, String password) {
        long startTime = System.currentTimeMillis();

        String url = buildUrl(baseUrl, request.getResourcePath());

        logger.info("RESTCONF {} {} ", request.getMethod(), sanitizeUrl(url));

        try {
            HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", request.getAccept())
                    .header("Content-Type", request.getContentType())
                    .timeout(Duration.ofMillis(config.getReadTimeout()));

            // Add Basic Authentication
            if (username != null && password != null) {
                String auth = Base64.getEncoder().encodeToString(
                        (username + ":" + password).getBytes());
                httpRequestBuilder.header("Authorization", "Basic " + auth);
            }

            // Set HTTP method and body
            HttpRequest httpRequest = switch (request.getMethod().toUpperCase()) {
                case "GET" -> httpRequestBuilder.GET().build();
                case "POST" -> httpRequestBuilder
                        .POST(HttpRequest.BodyPublishers.ofString(
                                request.getBody() != null ? request.getBody() : ""))
                        .build();
                case "PUT" -> httpRequestBuilder
                        .PUT(HttpRequest.BodyPublishers.ofString(
                                request.getBody() != null ? request.getBody() : ""))
                        .build();
                case "PATCH" -> httpRequestBuilder
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(
                                request.getBody() != null ? request.getBody() : ""))
                        .build();
                case "DELETE" -> httpRequestBuilder.DELETE().build();
                default -> throw new RestconfException(
                        "Unsupported HTTP method: " + request.getMethod(), 400);
            };

            HttpResponse<String> httpResponse = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString());

            long executionTime = System.currentTimeMillis() - startTime;

            RestconfResponse response = new RestconfResponse(
                    httpResponse.statusCode(), httpResponse.body());
            response.setExecutionTimeMs(executionTime);
            response.setContentType(
                    httpResponse.headers().firstValue("Content-Type").orElse("unknown"));

            logger.info("RESTCONF response: {} {} ({}ms)",
                    httpResponse.statusCode(),
                    response.isSuccess() ? "SUCCESS" : "FAILED",
                    executionTime);

            return response;

        } catch (RestconfException e) {
            throw e;
        } catch (java.net.ConnectException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Connection refused to {} ({}ms)", sanitizeUrl(url), executionTime);
            throw new RestconfException(
                    "Unable to connect to RESTCONF device at " + sanitizeUrl(url) +
                    ". Please verify: 1) Device IP, 2) RESTCONF service, " +
                    "3) Network connectivity.", 502);
        } catch (java.net.http.HttpTimeoutException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Request timeout to {} ({}ms)", sanitizeUrl(url), executionTime);
            throw new RestconfException(
                    "Connection timed out. The device may be unreachable.", 408);
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("RESTCONF request failed: {} ({}ms)", e.getMessage(), executionTime);
            throw new RestconfException(
                    "RESTCONF operation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a RESTCONF operation using default credentials from config.
     */
    public RestconfResponse execute(String baseUrl, RestconfRequest request) {
        return execute(baseUrl, request, config.getUsername(), config.getPassword());
    }

    /**
     * Build the full RESTCONF URL from base URL and resource path.
     */
    private String buildUrl(String baseUrl, String resourcePath) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        
        // Auto-replace placeholder template {name} if user hasn't replaced it
        path = path.replace("{name}", "GigabitEthernet0/1");
        // Encode any remaining raw curly braces for URI safety
        path = path.replace("{", "%7B").replace("}", "%7D");
        
        return base + path;
    }

    /**
     * Remove credentials from URL for safe logging.
     */
    private String sanitizeUrl(String url) {
        return url.replaceAll("//[^@]*@", "//***@");
    }

    /**
     * Build an HttpClient with appropriate SSL and timeout settings.
     */
    private HttpClient buildHttpClient() {
        try {
            HttpClient.Builder builder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(config.getConnectionTimeout()))
                    .followRedirects(HttpClient.Redirect.NORMAL);

            if (!config.isSslVerify()) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String t) {}
                    public void checkServerTrusted(X509Certificate[] certs, String t) {}
                }}, new java.security.SecureRandom());
                builder.sslContext(sslContext);
            }

            return builder.build();
        } catch (Exception e) {
            logger.warn("Failed to configure custom SSL, using default: {}", e.getMessage());
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(config.getConnectionTimeout()))
                    .build();
        }
    }
}
