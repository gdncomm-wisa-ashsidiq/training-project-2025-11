package com.gdn.training.product.controller;

import com.gdn.training.product.entity.Product;
import com.gdn.training.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void searchProductsEndpointReturnsPersistedData() throws Exception {
        productRepository.save(Product.builder()
                .name("Gadget Alpha")
                .description("flagship gadget")
                .price(BigDecimal.valueOf(199))
                .quantity(Integer.MAX_VALUE)
                .imageUrl("https://example.com/gadget-alpha")
                .build());
        productRepository.save(Product.builder()
                .name("Widget Beta")
                .description("helper widget")
                .price(BigDecimal.valueOf(49))
                .quantity(Integer.MAX_VALUE)
                .imageUrl("https://example.com/widget-beta")
                .build());

        mockMvc.perform(get("/products").param("query", "Gadget"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", is("Gadget Alpha")));
    }

    @Test
    void getProductEndpointReturnsProduct() throws Exception {
        Product saved = productRepository.save(Product.builder()
                .name("Camera Pro")
                .description("High-end camera")
                .price(BigDecimal.valueOf(899))
                .quantity(Integer.MAX_VALUE)
                .imageUrl("https://example.com/camera-pro")
                .build());

        mockMvc.perform(get("/products/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Camera Pro")))
                .andExpect(jsonPath("$.data.id", is(saved.getId().toString())));
    }

    @Test
    void searchProductsReturnsAllWhenQueryBlank() throws Exception {
        productRepository.save(Product.builder()
                .name("Alpha Product")
                .description("first product")
                .price(BigDecimal.TEN)
                .quantity(Integer.MAX_VALUE)
                .imageUrl("https://example.com/alpha")
                .build());
        productRepository.save(Product.builder()
                .name("Beta Product")
                .description("second product")
                .price(BigDecimal.TEN)
                .quantity(Integer.MAX_VALUE)
                .imageUrl("https://example.com/beta")
                .build());

        mockMvc.perform(get("/products").param("query", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    void searchProductsUsesWildcardQueryWhenPatternPresent() throws Exception {
        productRepository.save(Product.builder()
                .name("Gadget Pro")
                .description("pro gadget")
                .price(BigDecimal.TEN)
                .quantity(Integer.MAX_VALUE)
                .imageUrl("https://example.com/gadget-pro")
                .build());
        productRepository.save(Product.builder()
                .name("Widget Pro")
                .description("pro widget")
                .price(BigDecimal.TEN)
                .quantity(Integer.MAX_VALUE)
                .imageUrl("https://example.com/widget-pro")
                .build());

        mockMvc.perform(get("/products").param("query", "Gadget*Pro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", is("Gadget Pro")));
    }

    @Test
    void searchProductsFallsBackToContainsSearch() throws Exception {
        productRepository.save(Product.builder()
                .name("Widget Beta")
                .description("beta widget")
                .price(BigDecimal.TEN)
                .quantity(Integer.MAX_VALUE)
                .imageUrl("https://example.com/widget-beta")
                .build());
        productRepository.save(Product.builder()
                .name("Other Product")
                .description("other")
                .price(BigDecimal.TEN)
                .quantity(Integer.MAX_VALUE)
                .imageUrl("https://example.com/other")
                .build());

        mockMvc.perform(get("/products").param("query", "widget"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", is("Widget Beta")));
    }

    @Test
    void getProductByIdThrowsWhenMissing() throws Exception {
        mockMvc.perform(get("/products/{id}", java.util.UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Product not found")));
    }
}

