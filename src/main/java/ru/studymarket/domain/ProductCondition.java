package ru.studymarket.domain;

public enum ProductCondition {
    NEW("Новое"),
    EXCELLENT("Отличное"),
    GOOD("Хорошее"),
    USED("С заметным следом учебы");

    private final String label;

    ProductCondition(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
