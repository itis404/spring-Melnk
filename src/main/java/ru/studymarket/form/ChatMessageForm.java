package ru.studymarket.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatMessageForm {

    @NotBlank(message = "Сообщение не может быть пустым")
    @Size(max = 1200, message = "Сообщение слишком длинное")
    private String body;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
