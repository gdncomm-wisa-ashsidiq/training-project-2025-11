package com.gdn.training.product.search;

import java.util.function.Function;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.gdn.training.product.config.ProductSearchProperties;
import com.gdn.training.product.entity.Product;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.util.ObjectBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "product.search.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchProductClient implements ProductSearchClient {

    private static final String PRODUCTS_INDEX_ALIAS = "products";

    private final ElasticsearchSearchExecutor searchExecutor;
    private final ProductSearchMapper mapper;
    private final ProductSearchProperties properties;

    @Override
    public Page<Product> search(String query, Pageable pageable) {
        ElasticsearchSearchRequest<ProductSearchDocument, Product> request =
                ElasticsearchSearchRequest.<ProductSearchDocument, Product>builder()
                        .query(query)
                        .pageable(pageable)
                        .documentClass(ProductSearchDocument.class)
                        .indexName(properties.getIndexName(PRODUCTS_INDEX_ALIAS))
                        .queryFactory(this::buildQuery)
                        .mapper(mapper::toEntity)
                        .sortBuilder(s -> s.score(score -> score.order(SortOrder.Desc)))
                        .sortBuilder(s -> s.field(field -> field.field("name.keyword").order(SortOrder.Asc)))
                        .build();

        Page<Product> page = searchExecutor.search(request);
        log.debug("Elasticsearch returned {} hits for query '{}'", page.getTotalElements(), query);
        return page;
    }

    private Function<Query.Builder, ObjectBuilder<Query>> buildQuery(String query) {
        String wildcardValue = query.toLowerCase() + "*";
        return builder -> builder
                .bool(bool -> bool
                        .should(should -> should.multiMatch(multiMatch -> multiMatch
                                .fields("name^4", "description")
                                .query(query)
                                .type(TextQueryType.BoolPrefix)
                                .fuzziness("AUTO")))
                        .should(should -> should.wildcard(wildcard -> wildcard
                                .field("name.keyword")
                                .caseInsensitive(true)
                                .value(wildcardValue)))
                        .should(should -> should.matchPhrase(phrase -> phrase
                                .field("description")
                                .query(query)))
                        .minimumShouldMatch("1"));
    }
}

