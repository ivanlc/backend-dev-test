package com.ivanlc.backenddevtest.product;

public class UpstreamOverloadedException extends RuntimeException {

    public UpstreamOverloadedException(String message) {
        super(message);
    }
}
