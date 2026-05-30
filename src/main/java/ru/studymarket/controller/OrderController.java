package ru.studymarket.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.studymarket.domain.MarketplaceOrder;
import ru.studymarket.domain.OrderStatus;
import ru.studymarket.domain.Product;
import ru.studymarket.form.CheckoutForm;
import ru.studymarket.service.OrderService;
import ru.studymarket.service.ProductService;

import java.security.Principal;
import java.util.Set;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final ProductService productService;

    public OrderController(OrderService orderService, ProductService productService) {
        this.orderService = orderService;
        this.productService = productService;
    }

    @PostMapping("/products/{id}/orders")
    public String create(@PathVariable Long id,
                         @Valid CheckoutForm checkoutForm,
                         BindingResult bindingResult,
                         Principal principal,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Product product = productService.detailed(id);
            model.addAttribute("product", product);
            model.addAttribute("reviewForm", new ru.studymarket.form.ReviewForm());
            model.addAttribute("chatMessageForm", new ru.studymarket.form.ChatMessageForm());
            model.addAttribute("owner", false);
            model.addAttribute("favoriteIds", Set.of());
            model.addAttribute("relatedProducts", productService.featured().stream()
                    .filter(related -> !related.getId().equals(product.getId()))
                    .limit(5)
                    .toList());
            model.addAttribute("error", "Проверьте комментарий к заказу.");
            return "products/show";
        }
        var order = orderService.createForProduct(id, principal.getName(), checkoutForm);
        if (order.getSeller().getTelegramChatId() == null || order.getSeller().getTelegramChatId().isBlank()) {
            redirectAttributes.addFlashAttribute("success", "Заказ создан.");
            redirectAttributes.addFlashAttribute("error", "Telegram-уведомление не отправлено: у продавца не указан chat id.");
        } else {
            redirectAttributes.addFlashAttribute("success", "Заказ создан. Продавцу отправлено Telegram-уведомление.");
        }
        return "redirect:/orders/" + order.getId();
    }

    @GetMapping("/orders")
    public String orders(Model model, Principal principal) {
        var orders = orderService.visibleFor(principal.getName());
        model.addAttribute("orders", orders);
        model.addAttribute("purchaseOrders", orders.stream()
                .filter(order -> order.getBuyer().getUsername().equalsIgnoreCase(principal.getName()))
                .toList());
        model.addAttribute("saleOrders", orders.stream()
                .filter(order -> order.getSeller().getUsername().equalsIgnoreCase(principal.getName()))
                .toList());
        return "orders/list";
    }

    @GetMapping("/orders/{id}")
    public String order(@PathVariable Long id, Model model, Principal principal) {
        model.addAttribute("order", orderService.visibleById(id, principal.getName()));
        return "orders/show";
    }

    @PostMapping("/orders/{id}/confirm")
    public String confirm(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        return updateStatus(id, principal, redirectAttributes, OrderStatus.CONFIRMED, "Заказ подтвержден.");
    }

    @PostMapping("/orders/{id}/complete")
    public String complete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        return updateStatus(id, principal, redirectAttributes, OrderStatus.COMPLETED, "Заказ завершен.");
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        return updateStatus(id, principal, redirectAttributes, OrderStatus.CANCELLED, "Заказ отменен.");
    }

    private String updateStatus(Long id,
                                Principal principal,
                                RedirectAttributes redirectAttributes,
                                OrderStatus status,
                                String successMessage) {
        try {
            orderService.updateStatus(id, principal.getName(), status);
            redirectAttributes.addFlashAttribute("success", successMessage);
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/orders/" + id;
    }
}
