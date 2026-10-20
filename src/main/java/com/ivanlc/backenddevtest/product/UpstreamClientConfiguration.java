package com.ivanlc.backenddevtest.product;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class UpstreamClientConfiguration {

    @Bean
    RestClient upstreamRestClient(RestClient.Builder builder,
            @Value("${app.upstream.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
