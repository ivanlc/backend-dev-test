package com.ivanlc.backenddevtest.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimilarProductsServiceTest {

    private final ExistingProductsGateway gateway = mock(ExistingProductsGateway.class);
    private final SimilarProductsService service = new SimilarProductsService(gateway);

    @Test
    void preservesTheSimilarityOrder() {
        when(gateway.findSimilarIds("source")).thenReturn(List.of("3", "2"));
        when(gateway.findProduct("3"))
                .thenReturn(new ProductDetail("3", "Blazer", new BigDecimal("29.99"), false));
        when(gateway.findProduct("2"))
                .thenReturn(new ProductDetail("2", "Dress", new BigDecimal("19.99"), true));

        assertThat(service.findSimilarProducts("source"))
                .extracting(ProductDetail::id)
                .containsExactly("3", "2");
    }

    @Test
    void returnsAnEmptyArrayWithoutFetchingDetails() {
        when(gateway.findSimilarIds("source")).thenReturn(List.of());

        assertThat(service.findSimilarProducts("source")).isEmpty();
        verify(gateway).findSimilarIds("source");
    }
}
