package com.ivanlc.backenddevtest.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SimilarProductsServiceTest {

    private final ExistingProductsGateway gateway = mock(ExistingProductsGateway.class);
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(8));
    private final UpstreamProperties properties = new UpstreamProperties("http://localhost:3001",
            Duration.ofMillis(500), Duration.ofSeconds(6), Duration.ofSeconds(2), 2, 8, 2);
    private final SimilarProductsService service = new SimilarProductsService(gateway, executor, properties);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

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

    @Test
    void preservesOrderWhenSecondDetailFinishesFirst() {
        CountDownLatch secondFinished = new CountDownLatch(1);
        when(gateway.findSimilarIds("source")).thenReturn(List.of("slow", "fast"));
        when(gateway.findProduct("slow")).thenAnswer(invocation -> {
            if (!secondFinished.await(1, TimeUnit.SECONDS)) {
                throw new AssertionError("Second detail did not run concurrently");
            }
            return new ProductDetail("slow", "Slow", BigDecimal.ONE, true);
        });
        when(gateway.findProduct("fast")).thenAnswer(invocation -> {
            secondFinished.countDown();
            return new ProductDetail("fast", "Fast", BigDecimal.TEN, true);
        });

        assertThat(service.findSimilarProducts("source"))
                .extracting(ProductDetail::id).containsExactly("slow", "fast");
    }

    @Test
    void failsEntireRequestWhenOneDetailFails() {
        when(gateway.findSimilarIds("source")).thenReturn(List.of("1", "2"));
        when(gateway.findProduct("1")).thenThrow(new UpstreamPayloadException("bad detail"));

        assertThatThrownBy(() -> service.findSimilarProducts("source"))
                .isInstanceOf(UpstreamPayloadException.class);
    }

    @Test
    void enforcesOverallDeadline() {
        UpstreamProperties shortDeadline = new UpstreamProperties("http://localhost:3001",
                Duration.ofMillis(500), Duration.ofSeconds(6), Duration.ofMillis(20), 2, 8, 2);
        SimilarProductsService timedService = new SimilarProductsService(gateway, executor, shortDeadline);
        when(gateway.findSimilarIds("source")).thenReturn(List.of("slow"));
        when(gateway.findProduct("slow")).thenAnswer(invocation -> {
            Thread.sleep(200);
            return new ProductDetail("slow", "Slow", BigDecimal.ONE, true);
        });

        assertThatThrownBy(() -> timedService.findSimilarProducts("source"))
                .isInstanceOf(UpstreamTimeoutException.class);
    }
}
