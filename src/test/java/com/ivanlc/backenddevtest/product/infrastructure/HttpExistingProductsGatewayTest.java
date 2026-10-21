package com.ivanlc.backenddevtest.product.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ivanlc.backenddevtest.infrastructure.error.UpstreamPayloadException;
import com.ivanlc.backenddevtest.product.domain.ProductDetail;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpExistingProductsGatewayTest {

    private MockRestServiceServer server;
    private HttpExistingProductsGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://mock:3001");
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new HttpExistingProductsGateway(builder.build());
    }

    @Test
    void normalizesNumericIdsAndRemovesDuplicatesInFirstSeenOrder() {
        server.expect(requestTo("http://mock:3001/product/source/similarids"))
                .andRespond(withSuccess("[3,2,3,\"4\"]", MediaType.APPLICATION_JSON));

        assertThat(gateway.findSimilarIds("source")).containsExactly("3", "2", "4");
        server.verify();
    }

    @Test
    void mapsACompleteProductDetail() {
        server.expect(requestTo("http://mock:3001/product/2"))
                .andRespond(withSuccess("""
                        {"id":"2","name":"Dress","price":19.99,"availability":true}
                        """, MediaType.APPLICATION_JSON));

        ProductDetail detail = gateway.findProduct("2");
        assertThat(detail.id()).isEqualTo("2");
        assertThat(detail.name()).isEqualTo("Dress");
        assertThat(detail.price()).isEqualByComparingTo(new BigDecimal("19.99"));
        assertThat(detail.availability()).isTrue();
        server.verify();
    }

    @Test
    void rejectsMissingFieldsRatherThanReturningAFalseDefault() {
        server.expect(requestTo("http://mock:3001/product/2"))
                .andRespond(withSuccess("""
                        {"id":"2","name":"Dress","price":19.99}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> gateway.findProduct("2"))
                .isInstanceOf(UpstreamPayloadException.class);
        server.verify();
    }
}
