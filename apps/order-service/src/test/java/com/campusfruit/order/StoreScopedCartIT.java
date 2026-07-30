package com.campusfruit.order;

import com.campusfruit.order.dto.AddToCartRequest;
import com.campusfruit.order.dto.CartResponse;
import com.campusfruit.order.entity.Cart;
import com.campusfruit.order.entity.CartItem;
import com.campusfruit.order.repository.CartItemRepository;
import com.campusfruit.order.repository.CartRepository;
import com.campusfruit.order.service.CartService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 门店范围购物车集成测试。
 * <p>
 * 验证：
 * 1. 同一门店商品正常添加
 * 2. 切换门店时清空旧商品
 * 3. 购物车 CRUD
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StoreScopedCartIT {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @AfterEach
    void tearDown() {
        // 清理测试数据
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
    }

    @Test
    void shouldCreateCartAndAddItem() {
        Long userId = 100L;
        AddToCartRequest request = new AddToCartRequest();
        request.setOfferId(1L);
        request.setQuantity(2);

        cartService.addToCart(userId, request, 1L, "赣南脐橙", "500g盒装", 2990L, 10L, 1);

        CartResponse cart = cartService.getCart(userId);
        assertNotNull(cart);
        assertEquals(1L, cart.getStoreId());
        assertEquals(1, cart.getItems().size());
        assertEquals(5980L, cart.getTotalAmount()); // 2990 * 2
    }

    @Test
    void shouldClearCartWhenStoreChanges() {
        Long userId = 101L;

        // 添加门店1的商品
        AddToCartRequest req1 = new AddToCartRequest();
        req1.setOfferId(1L);
        req1.setQuantity(1);
        cartService.addToCart(userId, req1, 1L, "商品A", "份", 1000L, 10L, 1);

        // 添加门店2的商品（应清空门店1的商品）
        AddToCartRequest req2 = new AddToCartRequest();
        req2.setOfferId(2L);
        req2.setQuantity(3);
        cartService.addToCart(userId, req2, 2L, "商品B", "个", 500L, 10L, 1);

        CartResponse cart = cartService.getCart(userId);
        assertEquals(2L, cart.getStoreId());
        assertEquals(1, cart.getItems().size());
        assertEquals("商品B", cart.getItems().get(0).getFruitVariety());
    }

    @Test
    void shouldRemoveItemFromCart() {
        Long userId = 102L;

        AddToCartRequest request = new AddToCartRequest();
        request.setOfferId(1L);
        request.setQuantity(1);
        cartService.addToCart(userId, request, 1L, "商品A", "份", 1000L, 10L, 1);

        CartResponse cart = cartService.getCart(userId);
        Long itemId = cart.getItems().get(0).getId();

        cartService.removeItem(userId, itemId);

        CartResponse updated = cartService.getCart(userId);
        assertTrue(updated.getItems().isEmpty());
    }

    @Test
    void shouldClearCart() {
        Long userId = 103L;

        AddToCartRequest req1 = new AddToCartRequest();
        req1.setOfferId(1L);
        req1.setQuantity(2);
        cartService.addToCart(userId, req1, 1L, "商品A", "份", 1000L, 10L, 1);

        AddToCartRequest req2 = new AddToCartRequest();
        req2.setOfferId(2L);
        req2.setQuantity(1);
        cartService.addToCart(userId, req2, 1L, "商品B", "个", 500L, 10L, 1);

        cartService.clearCart(userId);

        CartResponse cart = cartService.getCart(userId);
        assertTrue(cart.getItems().isEmpty());
    }
}
