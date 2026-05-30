package ru.studymarket.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.studymarket.exception.DuplicateUserException;
import ru.studymarket.form.RegistrationForm;
import ru.studymarket.service.UserService;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegistrationForm registrationForm,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Проверьте поля регистрации.");
            return "auth/register";
        }
        try {
            userService.register(registrationForm);
        } catch (DuplicateUserException exception) {
            bindingResult.reject("duplicate", exception.getMessage());
            model.addAttribute("error", exception.getMessage());
            return "auth/register";
        }
        redirectAttributes.addFlashAttribute("success", "Аккаунт создан. Теперь можно войти.");
        return "redirect:/login";
    }
}
