package ru.studymarket.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReviewForm {

    @Min(value = 1, message = "Минимальная оценка 1")
    @Max(value = 5, message = "Максимальная оценка 5")
    private int rating = 5;

    @NotBlank(message = "Напишите отзыв")
    @Size(max = 1200, message = "Отзыв слишком длинный")
    private String comment;

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
