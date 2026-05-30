package ru.studymarket.dto;

import ru.studymarket.domain.ChatMessage;
import ru.studymarket.domain.Product;
import ru.studymarket.domain.UserAccount;

public record ChatConversation(Product product, UserAccount counterpart, ChatMessage lastMessage) {
}
