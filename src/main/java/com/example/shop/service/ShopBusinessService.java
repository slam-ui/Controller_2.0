package com.example.shop.service;

import com.example.shop.entity.*;
import com.example.shop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShopBusinessService {

    private final OrderRepository    orderRepository;
    private final ProductRepository  productRepository;
    private final UserRepository     userRepository;

    // 1. ОФОРМЛЕНИЕ ЗАКАЗА
    @Transactional
    public Order createOrder(Long userId, Map<Long, Integer> productIdsAndQuantities) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден, id=" + userId));

        Order order = new Order();
        order.setUser(user);
        order.setCreatedDate(LocalDateTime.now());
        order.setStatus("NEW");

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> items = productIdsAndQuantities.entrySet().stream()
                .map(entry -> {
                    Product product = productRepository.findById(entry.getKey())
                            .orElseThrow(() -> new RuntimeException("Товар не найден, id=" + entry.getKey()));
                    OrderItem item = new OrderItem();
                    item.setOrder(savedOrder);
                    item.setProduct(product);
                    item.setQuantity(entry.getValue());
                    return item;
                }).toList();

        savedOrder.setItems(items);
        return orderRepository.save(savedOrder);
    }

    // 2. УМНЫЙ ПОИСК (фильтрация по категории и цене)
    public List<Product> searchProducts(String categoryName, BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository.findByCategoryNameAndPriceBetween(categoryName, minPrice, maxPrice);
    }

    // 3. ИСТОРИЯ ЗАКАЗОВ — эффективный запрос через репозиторий
    public List<Order> getUserHistory(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    // 4. МАССОВОЕ ИЗМЕНЕНИЕ ЦЕН — пакетный UPDATE одним SQL-запросом
    @Transactional
    public int applyDiscountToCategory(Long categoryId, int discountPercent) {
        BigDecimal discountFraction = BigDecimal.valueOf(discountPercent)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return productRepository.applyDiscountByCategory(categoryId, discountFraction);
    }

    // 5. СТАТУС ЗАКАЗА
    public String checkOrderStatus(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден, id=" + orderId));
        return "Заказ #" + orderId + " | Статус: " + order.getStatus()
                + " | Создан: " + order.getCreatedDate();
    }
}
