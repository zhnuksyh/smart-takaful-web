package com.muqmeen.takaful.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String baseUrl = "http://localhost:8080";

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = stripTrailingSlash(baseUrl); }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "http://localhost:8080";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
