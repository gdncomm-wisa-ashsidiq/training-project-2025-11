package com.gdn.training.product.service;

import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gdn.training.product.entity.Product;
import com.gdn.training.product.repository.ProductRepository;
import com.gdn.training.product.search.ProductSearchClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ObjectProvider<ProductSearchClient> productSearchClient;

    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String query, Pageable pageable) {
        if (!StringUtils.hasText(query)) {
            log.debug("Listing all products - page {}", pageable.getPageNumber());
            return productRepository.findAll(pageable);
        }

        String trimmed = query.trim();
        if (containsWildcard(trimmed)) {
            String pattern = toLikePattern(trimmed);
            log.info("Searching products with wildcard pattern {}", pattern);
            return productRepository.findByNameLikeIgnoreCase(pattern, pageable);
        }

        return searchUsingElasticsearch(trimmed, pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productById", key = "#id")
    public Product getProductById(UUID id) {
        return productRepository.findById(id)
                .map(product -> {
                    log.debug("Product {} found", id);
                    return product;
                })
                .orElseThrow(() -> {
                    log.warn("Product {} not found", id);
                    return new IllegalArgumentException("Product not found");
                });
    }

    private boolean containsWildcard(String query) {
        return query.contains("*") || query.contains("?");
    }

    private String toLikePattern(String query) {
        String pattern = query
                .replace('*', '%')
                .replace('?', '_');
        if (!pattern.contains("%") && !pattern.contains("_")) {
            pattern = "%" + pattern + "%";
        }
        return pattern;
    }

    private Page<Product> searchUsingElasticsearch(String query, Pageable pageable) {
        ProductSearchClient searchClient = productSearchClient.getIfAvailable();
        if (searchClient == null) {
            log.debug("Elasticsearch client unavailable, falling back to database search");
            return productRepository.findByNameContainingIgnoreCase(query, pageable);
        }

        try {
            Page<Product> result = searchClient.search(query, pageable);
            if (result.hasContent()) {
                return result;
            }
            log.debug("Elasticsearch returned empty result for '{}', retrying via database", query);
            return productRepository.findByNameContainingIgnoreCase(query, pageable);
        } catch (Exception ex) {
            log.warn("Elasticsearch search failed for '{}', falling back to database lookup", query, ex);
            return productRepository.findByNameContainingIgnoreCase(query, pageable);
        }
    }
}
