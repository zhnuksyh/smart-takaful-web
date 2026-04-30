package com.muqmeen.takaful.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toyyibpay")
public class ToyyibPayProperties {

    private String mode = "mock";
    private String secretKey;
    private String categoryCode;
    private String baseUrl = "https://dev.toyyibpay.com";

    public boolean isMockMode() {
        return mode == null || mode.isBlank() || "mock".equalsIgnoreCase(mode);
    }

    public boolean isConfiguredForGateway() {
        return !isMockMode()
                && secretKey != null && !secretKey.isBlank()
                && categoryCode != null && !categoryCode.isBlank()
                && baseUrl != null && !baseUrl.isBlank();
    }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = stripTrailingSlash(baseUrl); }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "https://dev.toyyibpay.com";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
