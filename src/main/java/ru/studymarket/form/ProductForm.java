package ru.studymarket.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.studymarket.domain.ProductCondition;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

public class ProductForm {

    @NotBlank(message = "Введите название")
    @Size(max = 140, message = "Название слишком длинное")
    private String title;

    @NotBlank(message = "Опишите товар")
    @Size(min = 20, max = 2500, message = "Описание должно быть от 20 до 2500 символов")
    private String description;

    @NotNull(message = "Укажите цену")
    @DecimalMin(value = "0.01", message = "Цена должна быть больше нуля")
    private BigDecimal price;

    @NotBlank(message = "Укажите кампус или место встречи")
    @Size(max = 160, message = "Место слишком длинное")
    private String campus;

    @Size(max = 700, message = "Ссылка на изображение слишком длинная")
    private String imageUrl;

    @NotNull(message = "Выберите состояние")
    private ProductCondition condition = ProductCondition.GOOD;

    @NotEmpty(message = "Выберите хотя бы одну категорию")
    private Set<Long> categoryIds = new LinkedHashSet<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public ProductCondition getCondition() {
        return condition;
    }

    public void setCondition(ProductCondition condition) {
        this.condition = condition;
    }

    public Set<Long> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(Set<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }
}
