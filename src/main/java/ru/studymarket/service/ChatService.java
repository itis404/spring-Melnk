package ru.studymarket.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studymarket.domain.ChatMessage;
import ru.studymarket.domain.Product;
import ru.studymarket.domain.UserAccount;
import ru.studymarket.dto.ChatConversation;
import ru.studymarket.dto.ChatMessageResponse;
import ru.studymarket.exception.ForbiddenOperationException;
import ru.studymarket.form.ChatMessageForm;
import ru.studymarket.repository.ChatMessageRepository;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ChatService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM HH:mm")
            .withZone(ZoneId.systemDefault());

    private final ChatMessageRepository chatMessageRepository;
    private final ProductService productService;
    private final UserService userService;

    public ChatService(ChatMessageRepository chatMessageRepository, ProductService productService, UserService userService) {
        this.chatMessageRepository = chatMessageRepository;
        this.productService = productService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<ChatConversation> inbox(String username) {
        Map<String, ChatConversation> conversations = new LinkedHashMap<>();
        for (ChatMessage message : chatMessageRepository.findInbox(username)) {
            UserAccount counterpart = counterpart(message, username);
            if (counterpart.getUsername().equalsIgnoreCase(username)) {
                continue;
            }
            String key = message.getProduct().getId() + ":" + counterpart.getId();
            conversations.putIfAbsent(key, new ChatConversation(message.getProduct(), counterpart, message));
        }
        return List.copyOf(conversations.values());
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> thread(Long productId, String username, String counterpartUsername) {
        Product product = productService.detailed(productId);
        validateCounterpart(product, username, counterpartUsername);
        return chatMessageRepository.findThreadForUser(productId, username, counterpartUsername);
    }

    @Transactional(readOnly = true)
    public UserAccount counterpartFor(Long productId, String username, String counterpartUsername) {
        Product product = productService.detailed(productId);
        validateCounterpart(product, username, counterpartUsername);
        return userService.requiredByUsername(counterpartUsername);
    }

    public ChatMessageResponse send(Long productId, String username, ChatMessageForm form) {
        return sendTo(productId, username, null, form);
    }

    public ChatMessageResponse sendTo(Long productId, String username, String recipientUsername, ChatMessageForm form) {
        Product product = productService.detailed(productId);
        UserAccount sender = userService.requiredByUsername(username);
        UserAccount recipient = resolveRecipient(product, username, recipientUsername);
        ChatMessage message = chatMessageRepository.save(new ChatMessage(sender, recipient, product, form.getBody().trim()));
        return new ChatMessageResponse(message.getSender().getFullName(), message.getBody(), DATE_FORMATTER.format(message.getSentAt()));
    }

    private UserAccount counterpart(ChatMessage message, String username) {
        return message.getSender().getUsername().equalsIgnoreCase(username)
                ? message.getRecipient()
                : message.getSender();
    }

    private UserAccount resolveRecipient(Product product, String username, String recipientUsername) {
        boolean sellerWrites = product.getSeller().getUsername().equalsIgnoreCase(username);
        if (!sellerWrites) {
            if (recipientUsername != null && !recipientUsername.isBlank()
                    && !product.getSeller().getUsername().equalsIgnoreCase(recipientUsername)) {
                throw new ForbiddenOperationException("Покупатель может писать только продавцу товара");
            }
            return product.getSeller();
        }
        if (recipientUsername == null || recipientUsername.isBlank()
                || product.getSeller().getUsername().equalsIgnoreCase(recipientUsername)) {
            throw new ForbiddenOperationException("Выберите покупателя для ответа");
        }
        return userService.requiredByUsername(recipientUsername);
    }

    private void validateCounterpart(Product product, String username, String counterpartUsername) {
        boolean sellerOpens = product.getSeller().getUsername().equalsIgnoreCase(username);
        if (sellerOpens && product.getSeller().getUsername().equalsIgnoreCase(counterpartUsername)) {
            throw new ForbiddenOperationException("Выберите покупателя для переписки");
        }
        if (!sellerOpens && !product.getSeller().getUsername().equalsIgnoreCase(counterpartUsername)) {
            throw new ForbiddenOperationException("Переписка доступна только с продавцом товара");
        }
    }
}
