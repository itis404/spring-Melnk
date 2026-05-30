package ru.studymarket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.telegram")
public class TelegramBotProperties {

    private String botToken = "";
    private String apiBaseUrl = "https://api.telegram.org";
    private String webhookSecretToken = "";
    private boolean commandsEnabled = true;
    private boolean pollingEnabled = true;
    private String proxyHost = "";
    private Integer proxyPort;
    private String proxyType = "HTTP";
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 40;

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = normalize(botToken);
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = normalize(apiBaseUrl).isBlank() ? "https://api.telegram.org" : trimTrailingSlash(apiBaseUrl.trim());
    }

    public String getWebhookSecretToken() {
        return webhookSecretToken;
    }

    public void setWebhookSecretToken(String webhookSecretToken) {
        this.webhookSecretToken = normalize(webhookSecretToken);
    }

    public boolean isPollingEnabled() {
        return pollingEnabled;
    }

    public boolean isCommandsEnabled() {
        return commandsEnabled;
    }

    public void setCommandsEnabled(boolean commandsEnabled) {
        this.commandsEnabled = commandsEnabled;
    }

    public void setPollingEnabled(boolean pollingEnabled) {
        this.pollingEnabled = pollingEnabled;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = normalize(proxyHost);
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyType() {
        return proxyType;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = normalize(proxyType).isBlank() ? "HTTP" : proxyType.trim();
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = Math.max(1, connectTimeoutSeconds);
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = Math.max(5, readTimeoutSeconds);
    }

    public boolean hasBotToken() {
        return botToken != null && !botToken.isBlank();
    }

    public boolean hasProxy() {
        return proxyHost != null && !proxyHost.isBlank() && proxyPort != null && proxyPort > 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
