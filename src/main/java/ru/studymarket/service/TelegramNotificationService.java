package ru.studymarket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.studymarket.domain.MarketplaceOrder;
import ru.studymarket.domain.UserAccount;
import ru.studymarket.exception.TelegramBotException;

import java.util.Map;
import java.util.Optional;

@Service
public class TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final TelegramBotClient telegramBotClient;

    public TelegramNotificationService(TelegramBotClient telegramBotClient) {
        this.telegramBotClient = telegramBotClient;
    }

    public void notifySellerAboutOrder(MarketplaceOrder order) {
        String chatId = order.getSeller().getTelegramChatId();
        if (!telegramBotClient.isConfigured() || chatId == null || chatId.isBlank()) {
            log.info("Telegram notification skipped for order {}: token or seller chat id is not configured", order.getId());
            return;
        }

        String productNames = order.getItems().stream()
                .map(item -> item.getProduct().getTitle())
                .reduce((left, right) -> left + ", " + right)
                .orElse("товар");

        String text = """
                Новый заказ в StudyMarket

                Товар: %s
                Покупатель: %s
                Сумма: %s ₽
                Комментарий: %s
                """.formatted(
                productNames,
                order.getBuyer().getFullName(),
                order.getTotalPrice(),
                order.getContactComment() == null || order.getContactComment().isBlank() ? "без комментария" : order.getContactComment()
        );

        sendMessageQuietly(chatId, text, "order " + order.getId());
    }

    public void notifyUserAboutRegistration(UserAccount account) {
        String chatId = account.getTelegramChatId();
        if (!telegramBotClient.isConfigured() || chatId == null || chatId.isBlank()) {
            log.info("Telegram registration notification skipped for user {}: token or chat id is not configured", account.getUsername());
            return;
        }

        String text = """
                Добро пожаловать в StudyMarket, %s!

                Регистрация прошла успешно. Теперь можно публиковать объявления, покупать товары и писать продавцам в чат.
                """.formatted(account.getFullName());
        sendMessageQuietly(chatId, text, "registration " + account.getUsername());
    }

    public void sendTestNotification(UserAccount account) {
        if (!telegramBotClient.isConfigured()) {
            throw new TelegramBotException("Не указан TELEGRAM_BOT_TOKEN.");
        }
        if (account.getTelegramChatId() == null || account.getTelegramChatId().isBlank()) {
            throw new TelegramBotException("Укажите Telegram chat id в профиле.");
        }
        String text = """
                Тестовое уведомление StudyMarket

                %s, Telegram подключен корректно.
                """.formatted(account.getFullName());
        sendMessage(account.getTelegramChatId(), text, "test notification " + account.getUsername());
    }

    public void handleUpdate(Map<String, Object> update) {
        extractMessage(update).ifPresent(message -> {
            String text = asString(message.get("text"));
            if (text == null || text.isBlank()) {
                return;
            }

            String command = text.trim().split("\\s+", 2)[0];
            String commandName = command.split("@", 2)[0];
            if (!isSupportedCommand(commandName)) {
                return;
            }

            Map<String, Object> chat = asMap(message.get("chat"));
            if (chat.isEmpty()) {
                return;
            }

            String chatId = asString(chat.get("id"));
            if (chatId == null || chatId.isBlank()) {
                return;
            }
            if (!telegramBotClient.isConfigured()) {
                log.info("Telegram command response skipped: bot token is not configured");
                return;
            }

            String chatType = asString(chat.get("type"));
            Map<String, Object> from = asMap(message.get("from"));
            String userId = asString(from.get("id"));
            String username = asString(from.get("username"));

            if (isStartCommand(commandName)) {
                String reply = """
                        Привет! Я бот StudyMarket.

                        Команда /id покажет chat id, который нужно вставить в профиль StudyMarket для уведомлений о заказах.
                        """;
                sendMessageQuietly(chatId, reply, "/start command in " + (chatType == null ? "chat" : chatType));
                return;
            }

            String reply = """
                    StudyMarket Telegram ID

                    Chat id: %s
                    User id: %s%s

                    Скопируйте Chat id в поле Telegram chat id в профиле StudyMarket.
                    Для уведомлений используется chat id, а не user id. В личном чате они обычно совпадают, а в группах и каналах отличаются.
                    """.formatted(
                    chatId,
                    userId == null ? "не найден" : userId,
                    username == null ? "" : "\nUsername: @" + username
            );
            sendMessageQuietly(chatId, reply, "/id command in " + (chatType == null ? "chat" : chatType));
        });
    }

    private void sendMessageQuietly(String chatId, String text, String context) {
        try {
            sendMessage(chatId, text, context);
        } catch (TelegramBotException exception) {
            log.warn("Telegram notification failed for {}: {}", context, telegramError(exception));
        }
    }

    private void sendMessage(String chatId, String text, String context) {
        telegramBotClient.sendMessage(chatId, text);
        log.info("Telegram notification sent for {}", context);
    }

    private Optional<Map<String, Object>> extractMessage(Map<String, Object> update) {
        Map<String, Object> message = asMap(update.get("message"));
        if (!message.isEmpty()) {
            return Optional.of(message);
        }
        Map<String, Object> editedMessage = asMap(update.get("edited_message"));
        return editedMessage.isEmpty() ? Optional.empty() : Optional.of(editedMessage);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean isSupportedCommand(String commandName) {
        return isStartCommand(commandName) || isIdCommand(commandName);
    }

    private boolean isStartCommand(String commandName) {
        return "/start".equalsIgnoreCase(commandName) || "/ыефке".equalsIgnoreCase(commandName);
    }

    private boolean isIdCommand(String commandName) {
        return "/id".equalsIgnoreCase(commandName) || "/шв".equalsIgnoreCase(commandName);
    }

    private String telegramError(TelegramBotException exception) {
        return exception.getUserMessage();
    }
}
