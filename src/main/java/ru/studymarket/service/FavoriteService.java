package ru.studymarket.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studymarket.domain.Favorite;
import ru.studymarket.domain.Product;
import ru.studymarket.domain.UserAccount;
import ru.studymarket.dto.FavoriteToggleResponse;
import ru.studymarket.exception.ResourceNotFoundException;
import ru.studymarket.repository.FavoriteRepository;
import ru.studymarket.repository.ProductRepository;

import java.util.Set;

@Service
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    public FavoriteService(FavoriteRepository favoriteRepository, ProductRepository productRepository, UserService userService) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
        this.userService = userService;
    }

    public FavoriteToggleResponse toggle(Long productId, String username) {
        UserAccount user = userService.requiredByUsername(username);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден"));

        boolean active = favoriteRepository.findByUserAndProduct(user, product)
                .map(favorite -> {
                    favoriteRepository.delete(favorite);
                    return false;
                })
                .orElseGet(() -> {
                    favoriteRepository.save(new Favorite(user, product));
                    return true;
                });

        return new FavoriteToggleResponse(active, favoriteRepository.countByProductId(productId));
    }

    @Transactional(readOnly = true)
    public Set<Long> productIdsFor(String username) {
        return favoriteRepository.findProductIdsByUsername(username);
    }
}
