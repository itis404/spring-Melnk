package ru.studymarket.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.studymarket.domain.UserAccount;
import ru.studymarket.exception.DuplicateUserException;
import ru.studymarket.exception.TelegramBotException;
import ru.studymarket.form.ProfileForm;
import ru.studymarket.service.ProductService;
import ru.studymarket.service.TelegramNotificationService;
import ru.studymarket.service.UserService;

import java.security.Principal;
import java.util.Set;

@Controller
public class ProfileController {

    private final UserService userService;
    private final ProductService productService;
    private final TelegramNotificationService telegramNotificationService;

    public ProfileController(UserService userService,
                             ProductService productService,
                             TelegramNotificationService telegramNotificationService) {
        this.userService = userService;
        this.productService = productService;
        this.telegramNotificationService = telegramNotificationService;
    }

    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        UserAccount user = userService.requiredByUsername(principal.getName());
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", userService.toProfileForm(user));
        }
        fillProfileModel(model, user);
        return "profile/show";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute ProfileForm profileForm,
                                BindingResult bindingResult,
                                Model model,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        UserAccount user = userService.requiredByUsername(principal.getName());
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Проверьте поля профиля.");
            fillProfileModel(model, user);
            return "profile/show";
        }
        try {
            userService.updateProfile(principal.getName(), profileForm);
        } catch (DuplicateUserException exception) {
            bindingResult.rejectValue("email", "duplicate", exception.getMessage());
            model.addAttribute("error", exception.getMessage());
            fillProfileModel(model, user);
            return "profile/show";
        }
        redirectAttributes.addFlashAttribute("success", "Профиль обновлен.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/telegram-test")
    public String sendTelegramTest(Principal principal, RedirectAttributes redirectAttributes) {
        UserAccount user = userService.requiredByUsername(principal.getName());
        try {
            telegramNotificationService.sendTestNotification(user);
            redirectAttributes.addFlashAttribute("success", "Тестовое уведомление отправлено в Telegram.");
        } catch (TelegramBotException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getUserMessage());
        }
        return "redirect:/profile";
    }

    private void fillProfileModel(Model model, UserAccount user) {
        model.addAttribute("profileUser", user);
        model.addAttribute("activeProducts", productService.activeBySeller(user.getUsername()));
        model.addAttribute("archivedProducts", productService.archivedBySeller(user.getUsername()));
        model.addAttribute("favoriteIds", Set.of());
    }
}
