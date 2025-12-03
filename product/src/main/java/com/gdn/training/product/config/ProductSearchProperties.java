package com.gdn.training.product.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "product.search.elasticsearch")
public class ProductSearchProperties {

    /**
     * Toggle for enabling Elasticsearch-based product search.
     */
    private boolean enabled = true;

    /**
     * Registered Elasticsearch indices keyed by a domain alias (e.g. "products").
     */
    private Map<String, IndexProperties> indices = new HashMap<>();

    /**
     * Maximum time allowed to establish a TCP connection.
     */
    private Duration connectTimeout = Duration.ofSeconds(2);

    /**
     * Maximum time allowed to wait for the response body.
     */
    private Duration socketTimeout = Duration.ofSeconds(5);

    /**
     * Hard cap for pooled HTTP connections to Elasticsearch.
     */
    private int maxConnections = 200;

    /**
     * Max connections that can be established towards a single Elasticsearch node.
     */
    private int maxConnectionsPerRoute = 50;

    @PostConstruct
    void ensureDefaults() {
        indices.computeIfAbsent("products", key -> {
            IndexProperties props = new IndexProperties();
            props.setName("products");
            return props;
        });
    }

    public String getIndexName(String alias) {
        IndexProperties props = indices.get(alias);
        if (props == null || !StringUtils.hasText(props.getName())) {
            throw new IllegalArgumentException("No Elasticsearch index configured for alias '" + alias + "'");
        }
        return props.getName();
    }

    @Getter
    @Setter
    public static class IndexProperties {
        /**
         * Physical index name inside the Elasticsearch cluster.
         */
        private String name;
    }
}

