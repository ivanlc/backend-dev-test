package com.ivanlc.backenddevtest.product;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class UpstreamErrorHandlerTest {

    private final SimilarProductsService service = mock(SimilarProductsService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SimilarProductsController(service))
            .setControllerAdvice(new UpstreamErrorHandler()).build();

    @Test
    void returnsDocumented404ForMissingProduct() throws Exception {
        when(service.findSimilarProducts("missing"))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        mvc.perform(get("/product/missing/similar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void returns502ForUpstreamFailureOrInvalidPayload() throws Exception {
        when(service.findSimilarProducts("failed"))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
        when(service.findSimilarProducts("invalid"))
                .thenThrow(new UpstreamPayloadException("missing field"));

        mvc.perform(get("/product/failed/similar")).andExpect(status().isBadGateway());
        mvc.perform(get("/product/invalid/similar")).andExpect(status().isBadGateway());
    }

    @Test
    void returns504ForUpstreamOrOverallTimeout() throws Exception {
        when(service.findSimilarProducts("slow"))
                .thenThrow(new ResourceAccessException("timed out", new SocketTimeoutException()));
        when(service.findSimilarProducts("deadline"))
                .thenThrow(new UpstreamTimeoutException("deadline"));

        mvc.perform(get("/product/slow/similar")).andExpect(status().isGatewayTimeout());
        mvc.perform(get("/product/deadline/similar")).andExpect(status().isGatewayTimeout());
    }

    @Test
    void returns503WhenDetailCapacityIsExhausted() throws Exception {
        when(service.findSimilarProducts("busy"))
                .thenThrow(new UpstreamOverloadedException("busy"));

        mvc.perform(get("/product/busy/similar"))
                .andExpect(status().isServiceUnavailable());
    }
}
