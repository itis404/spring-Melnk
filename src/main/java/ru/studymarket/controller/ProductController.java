package ru.studymarket.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.studymarket.domain.Product;
import ru.studymarket.domain.ProductCondition;
import ru.studymarket.form.CheckoutForm;
import ru.studymarket.form.ChatMessageForm;
import ru.studymarket.form.ProductForm;
import ru.studymarket.form.ReviewForm;
import ru.studymarket.service.CategoryService;
import ru.studymarket.service.FavoriteService;
import ru.studymarket.service.ProductService;

import java.security.Principal;
import java.util.Set;

@Controller
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final FavoriteService favoriteService;

    public ProductController(ProductService productService, CategoryService categoryService, FavoriteService favoriteService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.favoriteService = favoriteService;
    }

    @GetMapping("/products/{id}")
    public String show(@PathVariable Long id, Model model, Principal principal) {
        Product product = productService.detailed(id);
        model.addAttribute("product", product);
        model.addAttribute("checkoutForm", new CheckoutForm());
        model.addAttribute("reviewForm", new ReviewForm());
        model.addAttribute("chatMessageForm", new ChatMessageForm());
        model.addAttribute("owner", principal != null && product.getSeller().getUsername().equalsIgnoreCase(principal.getName()));
        model.addAttribute("favoriteIds", principal == null ? Set.of() : favoriteService.productIdsFor(principal.getName()));
        model.addAttribute("relatedProducts", productService.featured().stream()
                .filter(related -> !related.getId().equals(product.getId()))
                .limit(5)
                .toList());
        return "products/show";
    }

    @GetMapping("/products/new")
    public String create(Model model) {
        fillProductFormModel(model, new ProductForm());
        model.addAttribute("product", null);
        return "products/form";
    }

    @PostMapping("/products")
    public String create(@Valid @ModelAttribute ProductForm productForm,
                         BindingResult bindingResult,
                         Model model,
                         Principal principal,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            fillProductFormModel(model, productForm);
            model.addAttribute("product", null);
            model.addAttribute("error", "Проверьте поля товара.");
            return "products/form";
        }
        Product product = productService.create(productForm, principal.getName());
        redirectAttributes.addFlashAttribute("success", "Товар опубликован.");
        return "redirect:/products/" + product.getId();
    }

    @GetMapping("/products/{id}/edit")
    public String edit(@PathVariable Long id, Model model, Principal principal) {
        Product product = productService.detailed(id);
        fillProductFormModel(model, productService.toForm(product));
        model.addAttribute("product", product);
        return "products/form";
    }

    @PostMapping("/products/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute ProductForm productForm,
                         BindingResult bindingResult,
                         Model model,
                         Principal principal,
                         RedirectAttributes redirectAttributes) {
        Product product = productService.detailed(id);
        if (bindingResult.hasErrors()) {
            fillProductFormModel(model, productForm);
            model.addAttribute("product", product);
            model.addAttribute("error", "Проверьте поля товара.");
            return "products/form";
        }
        productService.update(id, productForm, principal.getName());
        redirectAttributes.addFlashAttribute("success", "Товар обновлен.");
        return "redirect:/products/" + id;
    }

    @PostMapping("/products/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        productService.delete(id, principal.getName());
        redirectAttributes.addFlashAttribute("success", "Товар снят с продажи.");
        return "redirect:/";
    }

    private void fillProductFormModel(Model model, ProductForm form) {
        model.addAttribute("productForm", form);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("conditions", ProductCondition.values());
    }
}
