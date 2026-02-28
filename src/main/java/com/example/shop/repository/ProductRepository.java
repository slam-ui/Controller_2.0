package com.example.shop.repository;

import com.example.shop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Поиск по категории и диапазону цен (используется в бизнес-операциях)
    List<Product> findByCategoryNameAndPriceBetween(String categoryName, BigDecimal min, BigDecimal max);

    // Эффективный запрос для массового изменения цен — без загрузки всех товаров в память
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId")
    List<Product> findByCategoryId(Long categoryId);

    // Пакетное обновление скидки через JPQL (ещё более эффективный вариант)
    @Modifying
    @Query("UPDATE Product p SET p.price = p.price * (1 - :discountFraction) WHERE p.category.id = :categoryId")
    int applyDiscountByCategory(Long categoryId, BigDecimal discountFraction);
}
