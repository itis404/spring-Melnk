package ru.studymarket.domain;

public enum OrderStatus {
    NEW("Новый"),
    CONFIRMED("Подтвержден"),
    COMPLETED("Завершен"),
    CANCELLED("Отменен");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
