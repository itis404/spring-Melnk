package ru.studymarket.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import ru.studymarket.exception.TelegramBotException;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class TelegramBotPollingService {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotPollingService.class);

    private final TelegramBotClient telegramBotClient;
    private final TelegramNotificationService telegramNotificationService;

    private volatile boolean running;
    private ExecutorService executorService;
    private long offset;

    public TelegramBotPollingService(TelegramBotClient telegramBotClient,
                                     TelegramNotificationService telegramNotificationService) {
        this.telegramBotClient = telegramBotClient;
        this.telegramNotificationService = telegramNotificationService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!telegramBotClient.isConfigured()) {
            log.info("Telegram bot polling skipped: token is not configured");
            return;
        }
        if (telegramBotClient.isCommandsEnabled()) {
            configureBotCommands();
        }

        if (!telegramBotClient.isPollingEnabled()) {
            log.info("Telegram bot polling is disabled");
            return;
        }

        running = true;
        executorService = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "telegram-bot-polling");
            thread.setDaemon(true);
            return thread;
        });
        executorService.submit(this::pollLoop);
        log.info("Telegram bot polling started");
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private void pollLoop() {
        deleteWebhookForPolling();
        while (running) {
            try {
                TelegramBotClient.TelegramUpdatesResponse response = telegramBotClient.getUpdates(offset, 25);

                if (response == null || !response.ok()) {
                    log.warn("Telegram getUpdates returned unsuccessful response: {}", response == null ? "empty" : response.description());
                    pauseAfterError();
                    continue;
                }

                for (Map<String, Object> update : response.result()) {
                    updateOffset(update);
                    telegramNotificationService.handleUpdate(update);
                }
            } catch (TelegramBotException exception) {
                log.warn("Telegram getUpdates request failed: {}", telegramError(exception));
                pauseAfterError();
            } catch (RuntimeException exception) {
                log.warn("Telegram update processing failed", exception);
                pauseAfterError();
            }
        }
    }

    private void deleteWebhookForPolling() {
        try {
            telegramBotClient.deleteWebhook(false);
            log.info("Telegram webhook disabled for polling mode");
        } catch (TelegramBotException exception) {
            log.warn("Telegram deleteWebhook request failed; polling will retry getUpdates: {}", telegramError(exception));
        }
    }

    private void configureBotCommands() {
        try {
            telegramBotClient.setStudyMarketCommands();
            log.info("Telegram bot commands configured");
        } catch (TelegramBotException exception) {
            log.warn("Telegram setMyCommands request failed: {}", telegramError(exception));
        }
    }


    private void updateOffset(Map<String, Object> update) {
        Object updateId = update.get("update_id");
        if (updateId instanceof Number number) {
            offset = Math.max(offset, number.longValue() + 1);
        }
    }

    private void pauseAfterError() {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    private String telegramError(TelegramBotException exception) {
        return exception.getUserMessage();
    }
}
