package com.ivanlc.backenddevtest.product;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.upstream")
public record UpstreamProperties(String baseUrl, Duration connectTimeout, Duration readTimeout,
        Duration requestTimeout, int maxDetailConcurrency, int queueCapacity, int perRequestParallelism) {

    public UpstreamProperties {
        if (baseUrl == null || baseUrl.isBlank() || connectTimeout == null || readTimeout == null
                || requestTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()
                || readTimeout.isNegative() || readTimeout.isZero()
                || requestTimeout.isNegative() || requestTimeout.isZero()
                || maxDetailConcurrency < 1 || queueCapacity < 1
                || perRequestParallelism < 1) {
            throw new IllegalArgumentException("Invalid upstream configuration");
        }
    }
}
