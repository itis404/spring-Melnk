package ru.studymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.studymarket.domain.ProductCondition;
import ru.studymarket.service.CategoryService;
import ru.studymarket.service.FavoriteService;
import ru.studymarket.service.ProductService;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Set;

@Controller
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final FavoriteService favoriteService;

    public HomeController(ProductService productService, CategoryService categoryService, FavoriteService favoriteService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.favoriteService = favoriteService;
    }

    @GetMapping("/")
    public String index(@RequestParam(required = false) String q,
                        @RequestParam(required = false) Long categoryId,
                        @RequestParam(required = false) ProductCondition condition,
                        @RequestParam(required = false) BigDecimal minPrice,
                        @RequestParam(required = false) BigDecimal maxPrice,
                        @RequestParam(defaultValue = "newest") String sort,
                        Model model,
                        Principal principal) {
        model.addAttribute("products", productService.browse(q, categoryId, condition, minPrice, maxPrice, sort));
        model.addAttribute("featured", productService.featured());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("conditions", ProductCondition.values());
        model.addAttribute("favoriteIds", principal == null ? Set.of() : favoriteService.productIdsFor(principal.getName()));
        model.addAttribute("q", q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("condition", condition);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);
        return "index";
    }
}
