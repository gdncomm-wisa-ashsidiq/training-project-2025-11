package com.gdn.training.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdn.training.cart.client.ProductClient;
import com.gdn.training.cart.dto.ProductDTO;
import com.gdn.training.cart.entity.Cart;
import com.gdn.training.cart.entity.CartItem;
import com.gdn.training.cart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductClient productClient;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
    }

    @Test
    void getCartCreatesNewCartWhenNoneExists() throws Exception {
        mockMvc.perform(get("/").header("X-User-Id", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is("user-1")))
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }

    @Test
    void getCartReturnsExistingCart() throws Exception {
        Cart cart = Cart.builder()
                .userId("user-2")
                .build();
        cartRepository.save(cart);

        mockMvc.perform(get("/").header("X-User-Id", "user-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is("user-2")));
    }

    @Test
    void addToCartAddsProductSuccessfully() throws Exception {
        ProductDTO product = ProductDTO.builder()
                .id("product-1")
                .name("Test Product")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .imageUrl("http://example.com/image.jpg")
                .build();

        when(productClient.getProductById("product-1")).thenReturn(product);

        CartItem itemToAdd = CartItem.builder()
                .productId("product-1")
                .quantity(2)
                .build();

        mockMvc.perform(post("/")
                        .header("X-User-Id", "user-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(itemToAdd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].productId", is("product-1")))
                .andExpect(jsonPath("$.data.items[0].productName", is("Test Product")))
                .andExpect(jsonPath("$.data.items[0].quantity", is(2)));

        Cart fromDb = cartRepository.findByUserId("user-3").orElseThrow();
        assertThat(fromDb.getItems()).hasSize(1);
        assertThat(fromDb.getItems().get(0).getProductId()).isEqualTo("product-1");
    }

    @Test
    void addToCartStacksQuantityForExistingProduct() throws Exception {
        ProductDTO product = ProductDTO.builder()
                .id("product-2")
                .name("Stackable Product")
                .price(BigDecimal.valueOf(49.99))
                .quantity(100)
                .imageUrl("http://example.com/product2.jpg")
                .build();

        when(productClient.getProductById("product-2")).thenReturn(product);

        CartItem itemToAdd = CartItem.builder()
                .productId("product-2")
                .quantity(3)
                .build();

        mockMvc.perform(post("/")
                .header("X-User-Id", "user-4")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(itemToAdd)));

        mockMvc.perform(post("/")
                        .header("X-User-Id", "user-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(itemToAdd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].quantity", is(6)));

        Cart fromDb = cartRepository.findByUserId("user-4").orElseThrow();
        assertThat(fromDb.getItems()).hasSize(1);
        assertThat(fromDb.getItems().get(0).getQuantity()).isEqualTo(6);
    }

    @Test
    void removeFromCartDeletesProduct() throws Exception {
        ProductDTO product = ProductDTO.builder()
                .id("product-3")
                .name("Removable Product")
                .price(BigDecimal.valueOf(29.99))
                .quantity(50)
                .imageUrl("http://example.com/product3.jpg")
                .build();

        when(productClient.getProductById(anyString())).thenReturn(product);

        CartItem itemToAdd = CartItem.builder()
                .productId("product-3")
                .quantity(1)
                .build();

        mockMvc.perform(post("/")
                .header("X-User-Id", "user-5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(itemToAdd)));

        mockMvc.perform(delete("/{productId}", "product-3")
                        .header("X-User-Id", "user-5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)));

        Cart fromDb = cartRepository.findByUserId("user-5").orElseThrow();
        assertThat(fromDb.getItems()).isEmpty();
    }

    @Test
    void addToCartReturns400WhenProductNotFound() throws Exception {
        when(productClient.getProductById("missing-product")).thenReturn(null);

        CartItem itemToAdd = CartItem.builder()
                .productId("missing-product")
                .quantity(1)
                .build();

        mockMvc.perform(post("/")
                        .header("X-User-Id", "user-fail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(itemToAdd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Product not found")));
    }
}
