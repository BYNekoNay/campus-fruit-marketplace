package com.campusfruit.order.service;

import com.campusfruit.order.dto.AddToCartRequest;
import com.campusfruit.order.dto.CartItemResponse;
import com.campusfruit.order.dto.CartResponse;
import com.campusfruit.order.dto.SwitchCartConfirmDTO;
import com.campusfruit.order.entity.Cart;
import com.campusfruit.order.entity.CartItem;
import com.campusfruit.order.repository.CartItemRepository;
import com.campusfruit.order.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    /**
     * 添加商品到购物车。
     * <p>
     * 校验同一门店约束：如果购物车中已有其他门店的商品，则清空后重新添加。
     *
     * @param userId 用户ID
     * @param dto    添加购物车请求
     * @param storeId 报价所属门店ID
     * @param fruitVariety 水果品种
     * @param salesUnit 销售单位
     * @param unitPrice 单价（分）
     * @param canonicalFruitId 标准水果ID（可选）
     */
    @Transactional
    public void addToCart(Long userId, AddToCartRequest dto, Long storeId,
                           String fruitVariety, String salesUnit, Long unitPrice,
                           Long canonicalFruitId, Integer offerVersion) {
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            newCart.setStoreId(storeId);
            return cartRepository.save(newCart);
        });

        // 同一门店约束：不同门店则清空重新添加
        if (!cart.getStoreId().equals(storeId)) {
            log.info("用户 {} 切换门店 {} → {}，清空购物车", userId, cart.getStoreId(), storeId);
            cartItemRepository.deleteByCartId(cart.getId());
            cart.setStoreId(storeId);
            cartRepository.save(cart);
        }

        CartItem item = new CartItem();
        item.setCartId(cart.getId());
        item.setOfferId(dto.getOfferId());
        item.setCanonicalFruitId(canonicalFruitId);
        item.setFruitVariety(fruitVariety);
        item.setSalesUnit(salesUnit);
        item.setUnitPrice(unitPrice);
        item.setQuantity(dto.getQuantity());
        item.setOfferVersion(offerVersion);
        cartItemRepository.save(item);

        log.info("用户 {} 添加商品到购物车: offerId={}, quantity={}, version={}", userId, dto.getOfferId(), dto.getQuantity(), offerVersion);
    }

    /**
     * 获取用户购物车。
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        if (cartOpt.isEmpty()) {
            return null;
        }
        Cart cart = cartOpt.get();
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        CartResponse response = new CartResponse();
        response.setCartId(cart.getId());
        response.setStoreId(cart.getStoreId());

        List<CartItemResponse> itemResponses = new ArrayList<>();
        long totalAmount = 0L;
        for (CartItem item : items) {
            CartItemResponse ir = new CartItemResponse();
            ir.setId(item.getId());
            ir.setOfferId(item.getOfferId());
            ir.setFruitVariety(item.getFruitVariety());
            ir.setSalesUnit(item.getSalesUnit());
            ir.setUnitPrice(item.getUnitPrice());
            ir.setQuantity(item.getQuantity());
            itemResponses.add(ir);
            totalAmount += item.getUnitPrice() * item.getQuantity();
        }
        response.setItems(itemResponses);
        response.setTotalAmount(totalAmount);

        return response;
    }

    /**
     * 移除购物车中的商品。
     */
    @Transactional
    public void removeItem(Long userId, Long itemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("购物车不存在"));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));

        if (!item.getCartId().equals(cart.getId())) {
            throw new IllegalArgumentException("商品不属于当前购物车");
        }

        cartItemRepository.delete(item);
        log.info("用户 {} 移除购物车商品: itemId={}", userId, itemId);
    }

    /**
     * 清空购物车。
     */
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null) {
            cartItemRepository.deleteByCartId(cart.getId());
            log.info("用户 {} 清空购物车", userId);
        }
    }

    /**
     * 切换到新门店。
     * 如果当前购物车已属于新门店，无需切换。
     * 如果不同，返回当前购物车信息供用户确认；用户确认后清空旧购物车并创建新购物车。
     *
     * @param userId       用户ID
     * @param newStoreId   新门店ID
     * @param newStoreName 新门店名称（可选，用于前端展示）
     * @return 切换确认信息 DTO
     */
    @Transactional(readOnly = true)
    public SwitchCartConfirmDTO switchStore(Long userId, Long newStoreId, String newStoreName) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);

        SwitchCartConfirmDTO dto = new SwitchCartConfirmDTO();
        dto.setNewStoreName(newStoreName != null ? newStoreName : "门店#" + newStoreId);

        if (cartOpt.isEmpty()) {
            // 无购物车，直接确认切换
            dto.setRequiresConfirmation(false);
            return dto;
        }

        Cart cart = cartOpt.get();
        dto.setCurrentStoreName("门店#" + cart.getStoreId());

        if (cart.getStoreId().equals(newStoreId)) {
            // 同一门店，无需切换
            dto.setRequiresConfirmation(false);
            return dto;
        }

        // 不同门店，需要确认
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartItemResponse> itemResponses = new ArrayList<>();
        for (CartItem item : items) {
            CartItemResponse ir = new CartItemResponse();
            ir.setId(item.getId());
            ir.setOfferId(item.getOfferId());
            ir.setFruitVariety(item.getFruitVariety());
            ir.setSalesUnit(item.getSalesUnit());
            ir.setUnitPrice(item.getUnitPrice());
            ir.setQuantity(item.getQuantity());
            itemResponses.add(ir);
        }
        dto.setItems(itemResponses);
        dto.setRequiresConfirmation(true);

        log.info("用户 {} 请求切换门店 {} → {}，需确认", userId, cart.getStoreId(), newStoreId);
        return dto;
    }

    /**
     * 确认切换门店：清空旧购物车，创建新购物车。
     *
     * @param userId     用户ID
     * @param newStoreId 新门店ID
     */
    @Transactional
    public Cart confirmSwitchStore(Long userId, Long newStoreId) {
        // 清空旧购物车
        Cart oldCart = cartRepository.findByUserId(userId).orElse(null);
        if (oldCart != null) {
            cartItemRepository.deleteByCartId(oldCart.getId());
            cartRepository.delete(oldCart);
            log.info("用户 {} 清空旧购物车: storeId={}", userId, oldCart.getStoreId());
        }

        // 创建新购物车
        Cart newCart = new Cart();
        newCart.setUserId(userId);
        newCart.setStoreId(newStoreId);
        newCart = cartRepository.save(newCart);

        log.info("用户 {} 切换到新门店: storeId={}", userId, newStoreId);
        return newCart;
    }

    /**
     * 校验购物车是否只含一家门店。
     * 用于下单前的门店范围校验。
     *
     * @param cartId 购物车ID
     * @throws IllegalArgumentException 如果购物车为空
     */
    @Transactional(readOnly = true)
    public void validateStoreScope(Long cartId) {
        List<CartItem> items = cartItemRepository.findByCartId(cartId);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("购物车为空，无法下单");
        }

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("购物车不存在"));
        log.debug("购物车 {} 门店校验通过: storeId={}, itemCount={}", cartId, cart.getStoreId(), items.size());
    }
}
