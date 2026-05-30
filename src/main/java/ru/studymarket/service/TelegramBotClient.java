package ru.studymarket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.studymarket.config.TelegramBotProperties;
import ru.studymarket.exception.TelegramBotException;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotClient.class);
    private static final Pattern TELEGRAM_DESCRIPTION = Pattern.compile("\"description\"\\s*:\\s*\"([^\"]+)\"");

    private final TelegramBotProperties properties;
    private final RestClient restClient;

    public TelegramBotClient(TelegramBotProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory(properties))
                .build();
    }

    public boolean isConfigured() {
        return properties.hasBotToken();
    }

    public boolean isPollingEnabled() {
        return properties.isPollingEnabled();
    }

    public boolean isCommandsEnabled() {
        return properties.isCommandsEnabled();
    }

    public void sendMessage(String chatId, String text) {
        ensureConfigured();
        post("sendMessage", Map.of(
                "chat_id", chatId,
                "text", text
        ), TelegramApiResponse.class);
    }

    public void deleteWebhook(boolean dropPendingUpdates) {
        ensureConfigured();
        post("deleteWebhook", Map.of("drop_pending_updates", dropPendingUpdates), TelegramApiResponse.class);
    }

    public void setStudyMarketCommands() {
        ensureConfigured();
        post("setMyCommands", Map.of("commands", List.of(
                Map.of("command", "start", "description", "Как подключить StudyMarket"),
                Map.of("command", "id", "description", "Узнать chat id для уведомлений")
        )), TelegramApiResponse.class);
    }

    public TelegramUpdatesResponse getUpdates(long offset, int timeoutSeconds) {
        ensureConfigured();
        return post("getUpdates", Map.of(
                "offset", offset,
                "timeout", timeoutSeconds,
                "allowed_updates", List.of("message", "edited_message")
        ), TelegramUpdatesResponse.class);
    }

    public void checkConnection() {
        ensureConfigured();
        post("getMe", Map.of(), TelegramApiResponse.class);
    }

    private <T> T post(String method, Object body, Class<T> responseType) {
        int attempts = "getUpdates".equals(method) ? 1 : 3;
        TelegramBotException lastException = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return postOnce(method, body, responseType);
            } catch (TelegramBotException exception) {
                lastException = exception;
                if (attempt == attempts || !isRetryable(exception)) {
                    throw exception;
                }
                log.info("Retrying Telegram Bot API method {} after failure: attempt {}/{}", method, attempt + 1, attempts);
                pauseBeforeRetry(attempt);
            }
        }
        throw lastException == null ? new TelegramBotException("Ошибка запроса к Telegram Bot API.") : lastException;
    }

    private <T> T postOnce(String method, Object body, Class<T> responseType) {
        try {
            T response = restClient.post()
                    .uri(botApiUri(method))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(responseType);

            if (response instanceof TelegramApiResponse apiResponse && !apiResponse.ok()) {
                throw apiError(apiResponse.description());
            }
            if (response instanceof TelegramUpdatesResponse updatesResponse && !updatesResponse.ok()) {
                throw apiError(updatesResponse.description());
            }
            return response;
        } catch (TelegramBotException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw wrapTelegramException(method, exception);
        }
    }

    private URI botApiUri(String method) {
        return URI.create(properties.getApiBaseUrl() + "/bot" + properties.getBotToken() + "/" + method);
    }

    private void ensureConfigured() {
        if (!properties.hasBotToken()) {
            throw new TelegramBotException("Не указан TELEGRAM_BOT_TOKEN.");
        }
    }

    private TelegramBotException wrapTelegramException(String method, RestClientException exception) {
        log.warn("Telegram Bot API method {} failed: {}", method, exception.getClass().getSimpleName());
        if (exception instanceof RestClientResponseException responseException) {
            String description = extractDescription(responseException.getResponseBodyAsString());
            return apiError(description == null ? "HTTP " + responseException.getStatusCode().value() : description);
        }
        if (exception instanceof ResourceAccessException) {
            return new TelegramBotException("""
                    Приложение не может подключиться к Telegram Bot API.
                    Проверьте доступ к api.telegram.org из среды запуска или настройте TELEGRAM_PROXY_HOST/TELEGRAM_PROXY_PORT либо TELEGRAM_API_BASE_URL.
                    """, exception);
        }
        return new TelegramBotException("Ошибка запроса к Telegram Bot API.", exception);
    }

    private boolean isRetryable(TelegramBotException exception) {
        String message = exception.getUserMessage();
        return message.contains("подключиться") || message.contains("HTTP 502") || message.contains("upstream");
    }

    private void pauseBeforeRetry(int attempt) {
        try {
            Thread.sleep(500L * attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private String extractDescription(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        Matcher matcher = TELEGRAM_DESCRIPTION.matcher(responseBody);
        return matcher.find() ? matcher.group(1) : null;
    }

    private TelegramBotException apiError(String details) {
        return new TelegramBotException("Telegram Bot API вернул ошибку: " + details + ". Проверьте токен бота и chat id.");
    }

    private SimpleClientHttpRequestFactory requestFactory(TelegramBotProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(properties.getConnectTimeoutSeconds()).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(properties.getReadTimeoutSeconds()).toMillis());
        if (properties.hasProxy()) {
            Proxy.Type proxyType = "SOCKS".equalsIgnoreCase(properties.getProxyType()) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
            factory.setProxy(new Proxy(proxyType, new InetSocketAddress(properties.getProxyHost(), properties.getProxyPort())));
        }
        return factory;
    }

    public record TelegramApiResponse(boolean ok, String description) {
    }

    public record TelegramUpdatesResponse(boolean ok, List<Map<String, Object>> result, String description) {
        public TelegramUpdatesResponse {
            result = result == null ? List.of() : result;
        }
    }
}
