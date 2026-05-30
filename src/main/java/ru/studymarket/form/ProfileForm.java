package ru.studymarket.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileForm {

    @NotBlank(message = "Введите имя")
    @Size(max = 120, message = "Имя слишком длинное")
    private String fullName;

    @Email(message = "Введите корректный email")
    @NotBlank(message = "Введите email")
    private String email;

    @Size(max = 80, message = "Telegram chat id слишком длинный")
    private String telegramChatId;

    @Size(max = 700, message = "Ссылка на аватар слишком длинная")
    private String avatarUrl;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
