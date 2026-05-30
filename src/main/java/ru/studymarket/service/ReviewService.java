package ru.studymarket.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studymarket.domain.Product;
import ru.studymarket.domain.Review;
import ru.studymarket.domain.UserAccount;
import ru.studymarket.exception.ForbiddenOperationException;
import ru.studymarket.form.ReviewForm;
import ru.studymarket.repository.ReviewRepository;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductService productService;
    private final UserService userService;

    public ReviewService(ReviewRepository reviewRepository, ProductService productService, UserService userService) {
        this.reviewRepository = reviewRepository;
        this.productService = productService;
        this.userService = userService;
    }

    public Review add(Long productId, ReviewForm form, String username) {
        Product product = productService.detailed(productId);
        if (product.getSeller().getUsername().equalsIgnoreCase(username)) {
            throw new ForbiddenOperationException("Нельзя оставить отзыв на свой товар");
        }
        UserAccount author = userService.requiredByUsername(username);
        return reviewRepository.save(new Review(product, author, product.getSeller(), form.getRating(), form.getComment().trim()));
    }
}
