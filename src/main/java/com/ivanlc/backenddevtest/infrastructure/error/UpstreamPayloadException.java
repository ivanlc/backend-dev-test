package com.ivanlc.backenddevtest.infrastructure.error;

public class UpstreamPayloadException extends RuntimeException {

    public UpstreamPayloadException(String message) {
        super(message);
    }
}
