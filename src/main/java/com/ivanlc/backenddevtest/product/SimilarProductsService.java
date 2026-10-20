package com.ivanlc.backenddevtest.product;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class SimilarProductsService {

    private final ExistingProductsGateway gateway;
    private final ThreadPoolExecutor executor;
    private final UpstreamProperties properties;

    public SimilarProductsService(ExistingProductsGateway gateway, ThreadPoolExecutor executor,
            UpstreamProperties properties) {
        this.gateway = gateway;
        this.executor = executor;
        this.properties = properties;
    }

    public List<ProductDetail> findSimilarProducts(String productId) {
        long deadline = System.nanoTime() + properties.requestTimeout().toNanos();
        List<String> ids = gateway.findSimilarIds(productId);
        ProductDetail[] ordered = new ProductDetail[ids.size()];

        for (int start = 0; start < ids.size(); start += properties.perRequestParallelism()) {
            int end = Math.min(start + properties.perRequestParallelism(), ids.size());
            fetchBatch(ids, ordered, start, end, deadline);
        }
        return List.of(ordered);
    }

    private void fetchBatch(List<String> ids, ProductDetail[] ordered, int start, int end,
            long deadline) {
        ExecutorCompletionService<IndexedDetail> completion = new ExecutorCompletionService<>(executor);
        List<Future<IndexedDetail>> submitted = new ArrayList<>(end - start);
        try {
            for (int index = start; index < end; index++) {
                if (deadline - System.nanoTime() <= 0) {
                    throw new UpstreamTimeoutException("Overall request deadline exceeded");
                }
                final int position = index;
                submitted.add(completion.submit(() ->
                        new IndexedDetail(position, gateway.findProduct(ids.get(position)))));
            }
            for (int remaining = end - start; remaining > 0; remaining--) {
                long waitNanos = deadline - System.nanoTime();
                if (waitNanos <= 0) {
                    throw new UpstreamTimeoutException("Overall request deadline exceeded");
                }
                Future<IndexedDetail> completed = completion.poll(waitNanos, TimeUnit.NANOSECONDS);
                if (completed == null) {
                    throw new UpstreamTimeoutException("Overall request deadline exceeded");
                }
                IndexedDetail result = completed.get();
                ordered[result.index()] = result.detail();
            }
        } catch (RejectedExecutionException error) {
            throw new UpstreamOverloadedException("Detail worker capacity exceeded");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new UpstreamTimeoutException("Request interrupted");
        } catch (ExecutionException error) {
            if (error.getCause() instanceof RuntimeException failure) {
                throw failure;
            }
            throw new IllegalStateException("Unexpected detail worker failure", error.getCause());
        } finally {
            for (Future<IndexedDetail> future : submitted) {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            }
        }
    }

    private record IndexedDetail(int index, ProductDetail detail) { }
}
