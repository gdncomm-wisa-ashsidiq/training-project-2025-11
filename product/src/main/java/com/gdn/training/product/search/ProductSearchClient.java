package com.gdn.training.product.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gdn.training.product.entity.Product;

/**
 * Abstraction over the underlying search engine so the application can evolve independently of the datastore.
 */
public interface ProductSearchClient {

    /**
     * Executes a full-text search using the configured search backend.
     *
     * @param query search keywords (already sanitized by caller)
     * @param pageable pagination metadata
     * @return paginated list of products ordered by relevance
     */
    Page<Product> search(String query, Pageable pageable);
}

