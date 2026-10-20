package com.ivanlc.backenddevtest.product;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpExistingProductsGateway implements ExistingProductsGateway {

    private final RestClient restClient;

    public HttpExistingProductsGateway(RestClient upstreamRestClient) {
        this.restClient = upstreamRestClient;
    }

    @Override
    public List<String> findSimilarIds(String productId) {
        JsonNode body = restClient.get()
                .uri("/product/{productId}/similarids", productId)
                .retrieve()
                .body(JsonNode.class);

        if (body == null || !body.isArray()) {
            throw new UpstreamPayloadException("Similar IDs response must be an array");
        }

        LinkedHashSet<String> uniqueIds = new LinkedHashSet<>();
        for (JsonNode id : body) {
            String normalized;
            if (id.isTextual() && !id.asText().isBlank()) {
                normalized = id.asText();
            } else if (id.isIntegralNumber()) {
                // The supplied mock returns numbers although existingApis.yaml says string[].
                normalized = id.asText();
            } else {
                throw new UpstreamPayloadException("Similar ID must be a nonempty string or integer");
            }
            uniqueIds.add(normalized);
        }
        return new ArrayList<>(uniqueIds);
    }

    @Override
    public ProductDetail findProduct(String productId) {
        JsonNode body = restClient.get()
                .uri("/product/{productId}", productId)
                .retrieve()
                .body(JsonNode.class);

        if (body == null || !body.isObject()
                || !isNonemptyText(body.get("id"))
                || !isNonemptyText(body.get("name"))
                || body.get("price") == null || !body.get("price").isNumber()
                || body.get("availability") == null || !body.get("availability").isBoolean()) {
            throw new UpstreamPayloadException("Product detail has missing or invalid fields");
        }
        if (!productId.equals(body.get("id").asText())) {
            throw new UpstreamPayloadException("Product detail ID does not match requested ID");
        }

        return new ProductDetail(
                body.get("id").asText(),
                body.get("name").asText(),
                body.get("price").decimalValue(),
                body.get("availability").booleanValue()
        );
    }

    private boolean isNonemptyText(JsonNode node) {
        return node != null && node.isTextual() && !node.asText().isBlank();
    }
}
