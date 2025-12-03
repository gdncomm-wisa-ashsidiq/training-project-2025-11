package com.gdn.training.product.search;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ElasticsearchSearchExecutor {

    private final ElasticsearchOperations elasticsearchOperations;

    public <TDocument, TResult> Page<TResult> search(ElasticsearchSearchRequest<TDocument, TResult> request) {
        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withQuery(request.queryFactory().apply(request.query()))
                .withPageable(request.pageable());

        request.sortBuilders().forEach(queryBuilder::withSort);

        NativeQuery nativeQuery = queryBuilder.build();
        SearchHits<TDocument> hits = elasticsearchOperations.search(
                nativeQuery,
                request.documentClass(),
                IndexCoordinates.of(request.indexName()));

        List<TResult> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(request.mapper())
                .toList();

        return new PageImpl<>(content, request.pageable(), hits.getTotalHits());
    }
}

