package com.gdn.training.product.controller;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gdn.training.common.model.BaseResponse;
import com.gdn.training.product.entity.Product;
import com.gdn.training.product.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Products", description = "Product catalog and search endpoints")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Search products", description = "Search and list products with pagination")
    public ResponseEntity<BaseResponse<Page<Product>>> searchProducts(
            @RequestParam(required = false) String query,
            @ParameterObject Pageable pageable
    ) {
        log.info("Incoming product search query='{}', page={}, size={}", query, pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(BaseResponse.success(productService.searchProducts(query, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product details", description = "Get detailed information about a specific product")
    public ResponseEntity<BaseResponse<Product>> getProductById(@PathVariable UUID id) {
        log.info("Fetching product detail for {}", id);
        return ResponseEntity.ok(BaseResponse.success(productService.getProductById(id)));
    }
}
