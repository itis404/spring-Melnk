package ru.studymarket.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.studymarket.form.ReviewForm;
import ru.studymarket.service.ReviewService;

import java.security.Principal;

@Controller
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/products/{id}/reviews")
    public String add(@PathVariable Long id,
                      @Valid ReviewForm reviewForm,
                      BindingResult bindingResult,
                      Principal principal,
                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Проверьте текст отзыва и оценку.");
            return "redirect:/products/" + id;
        }
        reviewService.add(id, reviewForm, principal.getName());
        redirectAttributes.addFlashAttribute("success", "Отзыв опубликован.");
        return "redirect:/products/" + id;
    }
}
