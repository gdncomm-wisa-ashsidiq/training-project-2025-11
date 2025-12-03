package com.gdn.training.product.search;

import org.springframework.stereotype.Component;

import com.gdn.training.product.entity.Product;

@Component
public class ProductSearchMapper {

    public Product toEntity(ProductSearchDocument document) {
        if (document == null) {
            return null;
        }

        return Product.builder()
                .id(document.getId())
                .name(document.getName())
                .description(document.getDescription())
                .price(document.getPrice())
                .quantity(document.getQuantity())
                .imageUrl(document.getImageUrl())
                .build();
    }

    public ProductSearchDocument toDocument(Product product) {
        if (product == null) {
            return null;
        }

        return ProductSearchDocument.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .imageUrl(product.getImageUrl())
                .build();
    }
}

