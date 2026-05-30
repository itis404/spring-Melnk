package ru.studymarket.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import ru.studymarket.config.TelegramBotProperties;
import ru.studymarket.service.TelegramNotificationService;

import java.util.Map;

@RestController
public class TelegramWebhookController {

    private final TelegramNotificationService telegramNotificationService;
    private final TelegramBotProperties telegramBotProperties;

    public TelegramWebhookController(TelegramNotificationService telegramNotificationService,
                                     TelegramBotProperties telegramBotProperties) {
        this.telegramNotificationService = telegramNotificationService;
        this.telegramBotProperties = telegramBotProperties;
    }

    @PostMapping("/telegram/webhook")
    public ResponseEntity<Void> handleUpdate(@RequestBody Map<String, Object> update,
                                             @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken) {
        String webhookSecretToken = telegramBotProperties.getWebhookSecretToken();
        if (webhookSecretToken != null && !webhookSecretToken.isBlank() && !webhookSecretToken.equals(secretToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        telegramNotificationService.handleUpdate(update);
        return ResponseEntity.ok().build();
    }
}
