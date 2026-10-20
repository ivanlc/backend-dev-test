package com.ivanlc.backenddevtest.product;

import java.net.http.HttpTimeoutException;
import java.net.SocketTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class UpstreamErrorHandler {

    @ExceptionHandler(UpstreamOverloadedException.class)
    ResponseEntity<ProblemDetail> overloaded() {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Detail worker capacity exceeded");
    }

    @ExceptionHandler(UpstreamTimeoutException.class)
    ResponseEntity<ProblemDetail> deadlineExceeded() {
        return problem(HttpStatus.GATEWAY_TIMEOUT, "Upstream request timed out");
    }

    @ExceptionHandler(UpstreamPayloadException.class)
    ResponseEntity<ProblemDetail> invalidPayload() {
        return problem(HttpStatus.BAD_GATEWAY, "Invalid upstream response");
    }

    @ExceptionHandler(RestClientException.class)
    ResponseEntity<ProblemDetail> upstreamFailure(RestClientException error) {
        if (error instanceof RestClientResponseException response
                && response.getStatusCode().value() == 404) {
            return problem(HttpStatus.NOT_FOUND, "Product not found");
        }
        if (error instanceof ResourceAccessException && causedByTimeout(error)) {
            return problem(HttpStatus.GATEWAY_TIMEOUT, "Upstream request timed out");
        }
        return problem(HttpStatus.BAD_GATEWAY, "Upstream request failed");
    }

    private boolean causedByTimeout(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        return ResponseEntity.status(status).body(body);
    }
}
