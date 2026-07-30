package com.campusfruit.order.controller;

import com.campusfruit.order.dto.AddToCartRequest;
import com.campusfruit.order.dto.CartResponse;
import com.campusfruit.order.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * 添加商品到购物车。
     */
    @PostMapping("/items")
    public ResponseEntity<Map<String, String>> addItem(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddToCartRequest request) {
        Long userId = Long.valueOf(jwt.getSubject());

        // TODO: 实际实现应通过 Offer Service 校验 offer 有效性，获取门店/价格/规格信息
        // 此处简化，直接使用默认值
        cartService.addToCart(userId, request, 1L, "未知品种", "份", 0L, null, null);

        return ResponseEntity.ok(Map.of("message", "已添加到购物车"));
    }

    /**
     * 查看购物车。
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        CartResponse cart = cartService.getCart(userId);
        if (cart == null) {
            return ResponseEntity.ok(new CartResponse());
        }
        return ResponseEntity.ok(cart);
    }

    /**
     * 移除购物车商品。
     */
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Map<String, String>> removeItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long itemId) {
        Long userId = Long.valueOf(jwt.getSubject());
        cartService.removeItem(userId, itemId);
        return ResponseEntity.ok(Map.of("message", "已移除"));
    }

    /**
     * 清空购物车。
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearCart(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        cartService.clearCart(userId);
        return ResponseEntity.ok(Map.of("message", "已清空购物车"));
    }
}
