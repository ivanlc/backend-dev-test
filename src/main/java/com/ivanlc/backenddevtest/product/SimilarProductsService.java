package com.ivanlc.backenddevtest.product;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SimilarProductsService {

    private final ExistingProductsGateway gateway;

    public SimilarProductsService(ExistingProductsGateway gateway) {
        this.gateway = gateway;
    }

    public List<ProductDetail> findSimilarProducts(String productId) {
        return gateway.findSimilarIds(productId).stream()
                .map(gateway::findProduct)
                .toList();
    }
}
