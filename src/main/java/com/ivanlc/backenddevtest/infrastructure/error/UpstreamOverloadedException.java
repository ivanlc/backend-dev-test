package com.ivanlc.backenddevtest.infrastructure.error;

public class UpstreamOverloadedException extends RuntimeException {

    public UpstreamOverloadedException(String message) {
        super(message);
    }
}
