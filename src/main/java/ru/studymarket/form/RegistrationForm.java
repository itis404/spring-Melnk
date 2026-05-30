package ru.studymarket.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationForm {

    @NotBlank(message = "Введите логин")
    @Size(min = 3, max = 40, message = "Логин должен быть от 3 до 40 символов")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Логин может содержать латиницу, цифры, точку, дефис и подчеркивание")
    private String username;

    @NotBlank(message = "Введите имя")
    @Size(max = 120, message = "Имя слишком длинное")
    private String fullName;

    @Email(message = "Введите корректный email")
    @NotBlank(message = "Введите email")
    private String email;

    @NotBlank(message = "Введите пароль")
    @Size(min = 6, max = 80, message = "Пароль должен быть от 6 символов")
    private String password;

    @Size(max = 80, message = "Telegram chat id слишком длинный")
    private String telegramChatId;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }
}
