package ru.studymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.studymarket.form.ChatMessageForm;
import ru.studymarket.service.ChatService;
import ru.studymarket.service.ProductService;

import java.security.Principal;

@Controller
public class ChatController {

    private final ChatService chatService;
    private final ProductService productService;

    public ChatController(ChatService chatService, ProductService productService) {
        this.chatService = chatService;
        this.productService = productService;
    }

    @GetMapping("/chats")
    public String inbox(Model model, Principal principal) {
        var conversations = chatService.inbox(principal.getName());
        if (!conversations.isEmpty()) {
            var first = conversations.getFirst();
            return "redirect:/chats/products/" + first.product().getId() + "/users/" + first.counterpart().getUsername();
        }
        model.addAttribute("conversations", conversations);
        return "chats/list";
    }

    @GetMapping("/chats/products/{id}")
    public String thread(@PathVariable Long id, Model model, Principal principal) {
        var product = productService.detailed(id);
        if (product.getSeller().getUsername().equalsIgnoreCase(principal.getName())) {
            return "redirect:/chats";
        }
        return threadWithUser(id, product.getSeller().getUsername(), model, principal);
    }

    @GetMapping("/chats/products/{id}/users/{username}")
    public String threadWithUser(@PathVariable Long id,
                                 @PathVariable String username,
                                 Model model,
                                 Principal principal) {
        model.addAttribute("product", productService.detailed(id));
        model.addAttribute("counterpart", chatService.counterpartFor(id, principal.getName(), username));
        model.addAttribute("messages", chatService.thread(id, principal.getName(), username));
        model.addAttribute("conversations", chatService.inbox(principal.getName()));
        model.addAttribute("chatMessageForm", new ChatMessageForm());
        return "chats/thread";
    }
}
