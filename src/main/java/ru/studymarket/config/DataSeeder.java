package ru.studymarket.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import ru.studymarket.domain.Category;
import ru.studymarket.domain.Product;
import ru.studymarket.domain.ProductCondition;
import ru.studymarket.domain.Review;
import ru.studymarket.domain.Role;
import ru.studymarket.domain.UserAccount;
import ru.studymarket.repository.CategoryRepository;
import ru.studymarket.repository.ProductRepository;
import ru.studymarket.repository.ReviewRepository;
import ru.studymarket.repository.RoleRepository;
import ru.studymarket.repository.UserRepository;
import ru.studymarket.service.UserService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    @Transactional
    CommandLineRunner seedStudyMarket(RoleRepository roleRepository,
                                      UserRepository userRepository,
                                      CategoryRepository categoryRepository,
                                      ProductRepository productRepository,
                                      ReviewRepository reviewRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            Role userRole = roleRepository.findByName(UserService.USER_ROLE)
                    .orElseGet(() -> roleRepository.save(new Role(UserService.USER_ROLE)));

            if (categoryRepository.count() == 0) {
                categoryRepository.saveAll(List.of(
                        new Category("Учебники", "books", "#2f7df6"),
                        new Category("Конспекты", "notes", "#23a36f"),
                        new Category("Техника", "tech", "#9b5de5"),
                        new Category("Билеты", "tickets", "#f15b50"),
                        new Category("Для общаги", "dorm", "#d49b2a"),
                        new Category("Одежда", "clothes", "#111827")
                ));
            }

            UserAccount seller = userRepository.findByUsernameIgnoreCase("seller")
                    .orElseGet(() -> {
                        UserAccount account = new UserAccount("seller", "seller@study.market",
                                passwordEncoder.encode("password"), "Алина Продавец", null);
                        account.getRoles().add(userRole);
                        return userRepository.save(account);
                    });

            UserAccount buyer = userRepository.findByUsernameIgnoreCase("buyer")
                    .orElseGet(() -> {
                        UserAccount account = new UserAccount("buyer", "buyer@study.market",
                                passwordEncoder.encode("password"), "Данил Покупатель", null);
                        account.getRoles().add(userRole);
                        return userRepository.save(account);
                    });
            if (seller.getAvatarUrl() == null || seller.getAvatarUrl().isBlank()) {
                seller.setAvatarUrl("/images/avatars/alina.svg");
                userRepository.save(seller);
            }
            if (buyer.getAvatarUrl() == null || buyer.getAvatarUrl().isBlank()) {
                buyer.setAvatarUrl("/images/avatars/danil.svg");
                userRepository.save(buyer);
            }

            if (productRepository.count() == 0) {
                List<Category> categories = categoryRepository.findAll();
                Product java = product("Java и Spring: учебник с закладками",
                        "Плотный учебник по Java, Spring MVC и паттернам. Есть аккуратные пометки карандашом и закладки на темах, которые чаще спрашивают на защите.",
                        "ИТМО, Кронверкский", new BigDecimal("1450"), ProductCondition.GOOD, seller,
                        categories.get(0), categories.get(1));
                Product monitor = product("Монитор 24 дюйма для общаги",
                        "Full HD монитор без битых пикселей. Удобен для кода, презентаций и ночных дедлайнов. В комплекте HDMI.",
                        "Общежитие на Вяземском", new BigDecimal("6200"), ProductCondition.EXCELLENT, seller,
                        categories.get(2), categories.get(4));
                Product tickets = product("Два билета на студенческий квиз",
                        "Билеты на пятницу, места рядом. Не получается пойти, отдам по цене ниже кассы.",
                        "Главный корпус", new BigDecimal("900"), ProductCondition.NEW, seller,
                        categories.get(3));
                Product lamp = product("Настольная лампа с теплым светом",
                        "Компактная лампа, три уровня яркости. Хорошо выглядит на столе и не слепит соседа по комнате.",
                        "Общежитие, блок Б", new BigDecimal("1100"), ProductCondition.EXCELLENT, seller,
                        categories.get(4));
                Product calculator = product("Инженерный калькулятор Casio",
                        "Калькулятор для матана, физики и электротехники. Работает стабильно, кнопки не залипают.",
                        "Библиотека", new BigDecimal("1350"), ProductCondition.GOOD, seller,
                        categories.get(2));
                Product hoodie = product("Худи университета, размер M",
                        "Плотное худи с минималистичным принтом. Носилось пару раз, состояние почти новое.",
                        "Спорткомплекс", new BigDecimal("2100"), ProductCondition.EXCELLENT, seller,
                        categories.get(5));
                productRepository.saveAll(List.of(java, monitor, tickets, lamp, calculator, hoodie));
                reviewRepository.save(new Review(java, buyer, seller, 5, "Забрал быстро, учебник реально помог закрыть MVC-блок."));
                reviewRepository.save(new Review(monitor, buyer, seller, 5, "Монитор как новый, продавец все проверил при встрече."));
            }
            refreshDemoProductImages(productRepository);
        };
    }

    private void refreshDemoProductImages(ProductRepository productRepository) {
        List<Product> changed = new ArrayList<>();
        productRepository.findAll().forEach(product -> {
            if (product.getTitle().startsWith("Java и Spring")) {
                product.setImageUrl("/images/products/book-notes.png");
                changed.add(product);
            } else if (product.getTitle().startsWith("Монитор")) {
                product.setImageUrl("/images/products/monitor.png");
                changed.add(product);
            } else if (product.getTitle().startsWith("Два билета")) {
                product.setImageUrl("/images/products/tickets.png");
                changed.add(product);
            } else if (product.getTitle().startsWith("Настольная лампа")) {
                product.setImageUrl("/images/products/lamp.png");
                changed.add(product);
            } else if (product.getTitle().startsWith("Инженерный калькулятор")) {
                product.setImageUrl("/images/products/calculator.png");
                changed.add(product);
            } else if (product.getTitle().startsWith("Худи университета")) {
                product.setImageUrl("/images/products/hoodie.png");
                changed.add(product);
            }
        });
        productRepository.saveAll(changed);
    }

    private Product product(String title,
                            String description,
                            String campus,
                            BigDecimal price,
                            ProductCondition condition,
                            UserAccount seller,
                            Category... categories) {
        Product product = new Product(title, description, price, campus, seller);
        product.setCondition(condition);
        product.setImageUrl("/images/studymarket-hero.png");
        product.getCategories().addAll(List.of(categories));
        return product;
    }
}
