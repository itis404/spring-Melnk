package ru.studymarket.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import ru.studymarket.domain.UserAccount;
import ru.studymarket.service.UserService;

import java.security.Principal;

@ControllerAdvice
public class CurrentUserAdvice {

    private final UserService userService;

    public CurrentUserAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("currentUser")
    public UserAccount currentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userService.requiredByUsername(principal.getName());
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
