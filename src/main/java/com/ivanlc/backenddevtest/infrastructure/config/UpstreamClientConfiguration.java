package com.ivanlc.backenddevtest.infrastructure.config;

import java.net.http.HttpClient;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class UpstreamClientConfiguration {

    @Bean
    RestClient upstreamRestClient(RestClient.Builder builder, UpstreamProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.readTimeout());
        return builder.baseUrl(properties.baseUrl()).requestFactory(factory).build();
    }

    @Bean(destroyMethod = "shutdownNow")
    ThreadPoolExecutor detailExecutor(UpstreamProperties properties) {
        return new ThreadPoolExecutor(properties.maxDetailConcurrency(),
                properties.maxDetailConcurrency(), 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(properties.queueCapacity()),
                new ThreadPoolExecutor.AbortPolicy());
    }
}
