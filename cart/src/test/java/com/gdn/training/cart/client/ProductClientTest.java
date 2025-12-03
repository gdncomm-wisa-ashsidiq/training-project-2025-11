package com.gdn.training.cart.client;

import com.gdn.training.cart.config.ServiceClientConfig;
import com.gdn.training.cart.config.ServiceClientsProperties;
import com.gdn.training.cart.dto.ProductDTO;
import com.gdn.training.common.model.BaseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RestTemplateBuilder builder;

    @Mock
    private ServiceClientsProperties serviceClientsProperties;

    private ProductClient productClient;

    @BeforeEach
    void setUp() {
        ServiceClientConfig config = new ServiceClientConfig();
        config.setBaseUrl("http://localhost:8082");
        config.setConnectTimeout(2000);
        config.setReadTimeout(5000);
        config.setEndpoints(Map.of("detail", "/products/{id}"));

        when(serviceClientsProperties.getRequired("product")).thenReturn(config);
        when(builder.connectTimeout(any(Duration.class))).thenReturn(builder);
        when(builder.readTimeout(any(Duration.class))).thenReturn(builder);
        when(builder.build()).thenReturn(restTemplate);

        productClient = new ProductClient(builder, serviceClientsProperties);
    }

    @Test
    void getProductByIdReturnsProductWhenSuccessful() {
        ProductDTO expectedProduct = ProductDTO.builder()
                .id("product-1")
                .name("Test Product")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .build();

        BaseResponse<ProductDTO> responseBody = BaseResponse.success(expectedProduct);
        ResponseEntity<BaseResponse<ProductDTO>> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

        ProductDTO result = productClient.getProductById("product-1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("product-1");
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(99.99));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(
                urlCaptor.capture(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        );
        assertThat(urlCaptor.getValue()).contains("/products/product-1");
    }

    @Test
    void getProductByIdThrowsWhenResponseBodyIsNull() {
        ResponseEntity<BaseResponse<ProductDTO>> response = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

        assertThatThrownBy(() -> productClient.getProductById("product-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void getProductByIdThrowsWhenResponseIsNotSuccessful() {
        BaseResponse<ProductDTO> responseBody = BaseResponse.error("Product not found");
        ResponseEntity<BaseResponse<ProductDTO>> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

        assertThatThrownBy(() -> productClient.getProductById("product-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void getProductByIdThrowsWhenDataIsNull() {
        BaseResponse<ProductDTO> responseBody = BaseResponse.success(null);
        ResponseEntity<BaseResponse<ProductDTO>> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

        assertThatThrownBy(() -> productClient.getProductById("product-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no product data");
    }

    @Test
    void getProductByIdThrowsWhenResponseSuccessFalseWithNoMessage() {
        BaseResponse<ProductDTO> responseBody = new BaseResponse<>();
        responseBody.setSuccess(false);
        responseBody.setMessage(null);
        ResponseEntity<BaseResponse<ProductDTO>> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

        assertThatThrownBy(() -> productClient.getProductById("product-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rejected the request");
    }
}
