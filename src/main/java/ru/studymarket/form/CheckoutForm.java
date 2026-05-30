package ru.studymarket.form;

import jakarta.validation.constraints.Size;

public class CheckoutForm {

    @Size(max = 1200, message = "Комментарий слишком длинный")
    private String contactComment;

    public String getContactComment() {
        return contactComment;
    }

    public void setContactComment(String contactComment) {
        this.contactComment = contactComment;
    }
}
