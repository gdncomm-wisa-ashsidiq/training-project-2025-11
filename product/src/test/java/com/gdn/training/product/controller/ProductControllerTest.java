package com.gdn.training.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdn.training.product.entity.Product;
import com.gdn.training.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void searchProductsReturnsPage() throws Exception {
        Product product = sampleProduct("Gadget Alpha");
        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);
        when(productService.searchProducts(any(), any())).thenReturn(page);

        mockMvc.perform(get("/products").param("query", "gadget").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].name", is("Gadget Alpha")));
    }

    @Test
    void getProductByIdReturnsProduct() throws Exception {
        Product product = sampleProduct("Widget Beta");
        product.setId(UUID.randomUUID());
        when(productService.getProductById(product.getId())).thenReturn(product);

        mockMvc.perform(get("/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Widget Beta")));
    }

    private Product sampleProduct(String name) {
        return Product.builder()
                .id(UUID.randomUUID())
                .name(name)
                .description("desc")
                .price(BigDecimal.valueOf(100))
                .quantity(Integer.MAX_VALUE)
                .imageUrl("https://example.com/" + name)
                .build();
    }
}

