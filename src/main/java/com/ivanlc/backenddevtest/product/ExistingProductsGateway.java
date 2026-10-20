package com.ivanlc.backenddevtest.product;

import java.util.List;

public interface ExistingProductsGateway {

    List<String> findSimilarIds(String productId);

    ProductDetail findProduct(String productId);
}
