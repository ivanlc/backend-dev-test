package com.ivanlc.backenddevtest.infrastructure.error;

public class UpstreamTimeoutException extends RuntimeException {

    public UpstreamTimeoutException(String message) {
        super(message);
    }
}
