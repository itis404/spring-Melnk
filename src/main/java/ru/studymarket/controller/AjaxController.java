package ru.studymarket.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.studymarket.dto.ChatMessageResponse;
import ru.studymarket.dto.FavoriteToggleResponse;
import ru.studymarket.form.ChatMessageForm;
import ru.studymarket.service.ChatService;
import ru.studymarket.service.FavoriteService;

import java.security.Principal;

@RestController
@RequestMapping("/ajax")
public class AjaxController {

    private final FavoriteService favoriteService;
    private final ChatService chatService;

    public AjaxController(FavoriteService favoriteService, ChatService chatService) {
        this.favoriteService = favoriteService;
        this.chatService = chatService;
    }

    @PostMapping("/products/{id}/favorite")
    public FavoriteToggleResponse favorite(@PathVariable Long id, Principal principal) {
        return favoriteService.toggle(id, principal.getName());
    }

    @PostMapping("/products/{id}/messages")
    public ResponseEntity<ChatMessageResponse> message(@PathVariable Long id,
                                                       @Valid ChatMessageForm form,
                                                       Principal principal) {
        return ResponseEntity.ok(chatService.send(id, principal.getName(), form));
    }

    @PostMapping("/products/{id}/messages/{username}")
    public ResponseEntity<ChatMessageResponse> messageToUser(@PathVariable Long id,
                                                             @PathVariable String username,
                                                             @Valid ChatMessageForm form,
                                                             Principal principal) {
        return ResponseEntity.ok(chatService.sendTo(id, principal.getName(), username, form));
    }
}
