package com.gdn.training.product.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ProductSearchProperties.class)
public class ProductSearchConfig {

    @Bean
    @ConditionalOnProperty(prefix = "product.search.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RestClientBuilderCustomizer productRestClientCustomizer(ProductSearchProperties properties) {
        return builder -> builder
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(toIntMillis(properties.getConnectTimeout()))
                        .setSocketTimeout(toIntMillis(properties.getSocketTimeout())))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setMaxConnTotal(properties.getMaxConnections())
                        .setMaxConnPerRoute(properties.getMaxConnectionsPerRoute()));
    }

    private static int toIntMillis(Duration duration) {
        long millis = duration.toMillis();
        return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millis;
    }
}

