package com.gdn.training.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdn.training.cart.entity.Cart;
import com.gdn.training.cart.entity.CartItem;
import com.gdn.training.cart.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @Test
    void getCartReturnsCartForUser() throws Exception {
        Cart cart = Cart.builder()
                .id("cart-1")
                .userId("1")
                .items(new ArrayList<>())
                .build();
        when(cartService.getCart("1")).thenReturn(cart);

        mockMvc.perform(get("/").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is("cart-1")));

        verify(cartService).getCart("1");
    }

    @Test
    void addToCartReturnsUpdatedCart() throws Exception {
        Cart updated = Cart.builder()
                .id("cart-1")
                .userId("1")
                .items(List.of(CartItem.builder()
                        .productId("product-1")
                        .productName("Gadget Prime")
                        .price(BigDecimal.valueOf(499))
                        .quantity(3)
                        .imageUrl("https://example.com/gadget.jpg")
                        .build()))
                .build();

        when(cartService.addToCart(eq("1"), any(CartItem.class))).thenReturn(updated);

        CartItem payload = CartItem.builder()
                .productId("product-1")
                .productName("irrelevant")
                .price(BigDecimal.ZERO)
                .quantity(3)
                .build();

        mockMvc.perform(post("/")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Item added to cart")))
                .andExpect(jsonPath("$.data.items[0].quantity", is(3)));

        verify(cartService).addToCart(eq("1"), any(CartItem.class));
    }

    @Test
    void removeFromCartReturnsUpdatedCart() throws Exception {
        Cart cart = Cart.builder()
                .id("cart-1")
                .userId("1")
                .items(new ArrayList<>())
                .build();
        when(cartService.removeFromCart("1", "product-1")).thenReturn(cart);

        mockMvc.perform(delete("/{productId}", "product-1").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Item removed from cart")));

        verify(cartService).removeFromCart("1", "product-1");
    }
}

