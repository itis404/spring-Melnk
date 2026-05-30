package ru.studymarket.exception;

public class TelegramBotException extends RuntimeException {

    private final String userMessage;

    public TelegramBotException(String userMessage) {
        super(userMessage);
        this.userMessage = userMessage;
    }

    public TelegramBotException(String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.userMessage = userMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
