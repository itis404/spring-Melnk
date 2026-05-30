package ru.studymarket.dto;

import java.io.Serializable;

public record CategoryOption(Long id, String name, String slug, String accentColor) implements Serializable {
}
