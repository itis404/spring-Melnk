package ru.studymarket.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studymarket.domain.MarketplaceOrder;
import ru.studymarket.domain.OrderItem;
import ru.studymarket.domain.OrderStatus;
import ru.studymarket.domain.Product;
import ru.studymarket.domain.ProductStatus;
import ru.studymarket.domain.UserAccount;
import ru.studymarket.exception.ForbiddenOperationException;
import ru.studymarket.exception.ResourceNotFoundException;
import ru.studymarket.form.CheckoutForm;
import ru.studymarket.repository.OrderRepository;

import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final UserService userService;
    private final TelegramNotificationService telegramNotificationService;

    public OrderService(OrderRepository orderRepository,
                        ProductService productService,
                        UserService userService,
                        TelegramNotificationService telegramNotificationService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.userService = userService;
        this.telegramNotificationService = telegramNotificationService;
    }

    public MarketplaceOrder createForProduct(Long productId, String buyerUsername, CheckoutForm form) {
        Product product = productService.detailed(productId);
        if (product.getSeller().getUsername().equalsIgnoreCase(buyerUsername)) {
            throw new ForbiddenOperationException("Нельзя оформить заказ на свой товар");
        }
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ForbiddenOperationException("Товар уже забронирован или снят с продажи");
        }

        UserAccount buyer = userService.requiredByUsername(buyerUsername);
        MarketplaceOrder order = new MarketplaceOrder(buyer, product.getSeller(), product.getPrice(), form.getContactComment());
        order.addItem(new OrderItem(product, 1, product.getPrice()));
        MarketplaceOrder saved = orderRepository.save(order);
        productService.markReserved(product);
        telegramNotificationService.notifySellerAboutOrder(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<MarketplaceOrder> visibleFor(String username) {
        return orderRepository.findVisibleForUser(username);
    }

    @Transactional(readOnly = true)
    public MarketplaceOrder visibleById(Long id, String username) {
        return orderRepository.findVisibleById(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ не найден"));
    }

    public MarketplaceOrder updateStatus(Long id, String username, OrderStatus nextStatus) {
        MarketplaceOrder order = visibleById(id, username);
        boolean seller = order.getSeller().getUsername().equalsIgnoreCase(username);
        boolean buyer = order.getBuyer().getUsername().equalsIgnoreCase(username);

        if (nextStatus == OrderStatus.CONFIRMED && (!seller || order.getStatus() != OrderStatus.NEW)) {
            throw new ForbiddenOperationException("Подтвердить новый заказ может только продавец");
        }
        if (nextStatus == OrderStatus.COMPLETED && (!buyer || order.getStatus() != OrderStatus.CONFIRMED)) {
            throw new ForbiddenOperationException("Завершить подтвержденный заказ может только покупатель");
        }
        if (nextStatus == OrderStatus.CANCELLED && (!(buyer || seller)
                || (order.getStatus() != OrderStatus.NEW && order.getStatus() != OrderStatus.CONFIRMED))) {
            throw new ForbiddenOperationException("Отменить заказ может только участник сделки");
        }

        order.setStatus(nextStatus);
        order.getItems().stream().map(OrderItem::getProduct).findFirst().ifPresent(product -> {
            if (nextStatus == OrderStatus.CANCELLED) {
                productService.markActive(product);
            } else if (nextStatus == OrderStatus.COMPLETED) {
                productService.markSold(product);
            }
        });
        return orderRepository.save(order);
    }
}
