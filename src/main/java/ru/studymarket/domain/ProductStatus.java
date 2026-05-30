package ru.studymarket.domain;

public enum ProductStatus {
    ACTIVE("В продаже"),
    RESERVED("Забронировано"),
    SOLD("Продано"),
    HIDDEN("Скрыто");

    private final String label;

    ProductStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
