package com.gdn.training.product.search;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.util.ObjectBuilder;
import lombok.Builder;
import lombok.Singular;

@Builder
public record ElasticsearchSearchRequest<TDocument, TResult>(
        String query,
        Pageable pageable,
        Class<TDocument> documentClass,
        String indexName,
        Function<String, Function<Query.Builder, ObjectBuilder<Query>>> queryFactory,
        Function<TDocument, TResult> mapper,
        @Singular("sortBuilder")
        List<Function<SortOptions.Builder, ObjectBuilder<SortOptions>>> sortBuilders
) {

    public ElasticsearchSearchRequest {
        Assert.hasText(query, "query must not be blank");
        Assert.notNull(pageable, "pageable must not be null");
        Assert.notNull(documentClass, "documentClass must not be null");
        Assert.hasText(indexName, "indexName must not be blank");
        Assert.notNull(queryFactory, "queryFactory must not be null");
        Assert.notNull(mapper, "mapper must not be null");
        sortBuilders = sortBuilders == null ? List.of() : List.copyOf(sortBuilders);
    }
}

