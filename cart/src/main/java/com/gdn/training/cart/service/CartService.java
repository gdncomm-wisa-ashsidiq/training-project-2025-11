package com.gdn.training.cart.service;

import com.gdn.training.cart.client.ProductClient;
import com.gdn.training.cart.dto.ProductDTO;
import com.gdn.training.cart.entity.Cart;
import com.gdn.training.cart.entity.CartItem;
import com.gdn.training.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final ProductClient productClient;

    public Cart getCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().userId(userId).build();
                    Cart saved = cartRepository.save(newCart);
                    log.debug("Created new cart {} for user {}", saved.getId(), userId);
                    return saved;
                });
    }

    public Cart addToCart(String userId, CartItem item) {
        ProductDTO product = productClient.getProductById(item.getProductId());
        if (product == null) {
            log.warn("Product {} not found while adding to cart for {}", item.getProductId(), userId);
            throw new IllegalArgumentException("Product not found");
        }

        item.setProductName(product.getName());
        item.setPrice(product.getPrice());
        item.setImageUrl(product.getImageUrl());

        Cart cart = getCart(userId);

        log.debug("Current cart {} loaded for user {}", cart.getId(), userId);

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(item.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem existing = existingItem.get();
            existing.setQuantity(existing.getQuantity() + item.getQuantity());
            existing.setProductName(item.getProductName());
            existing.setPrice(item.getPrice());
            existing.setImageUrl(item.getImageUrl());
        } else {
            cart.getItems().add(item);
        }

        Cart saved = cartRepository.save(cart);
        log.debug("Cart {} updated for user {}", saved.getId(), userId);
        return saved;
    }

    public Cart removeFromCart(String userId, String productId) {
        Cart cart = getCart(userId);
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        Cart saved = cartRepository.save(cart);
        log.debug("Removed product {} from cart {} (user {})", productId, saved.getId(), userId);
        return saved;
    }
}
