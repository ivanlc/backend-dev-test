package com.ivanlc.backenddevtest.product;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SimilarProductsControllerTest {

    private final SimilarProductsService service = mock(SimilarProductsService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new SimilarProductsController(service)).build();

    @Test
    void exposesTheContractedGetRouteAndJsonArray() throws Exception {
        when(service.findSimilarProducts("source")).thenReturn(List.of(
                new ProductDetail("2", "Dress", new BigDecimal("19.99"), true)));

        mvc.perform(get("/product/source/similar"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("2"))
                .andExpect(jsonPath("$[0].name").value("Dress"))
                .andExpect(jsonPath("$[0].price").value(19.99))
                .andExpect(jsonPath("$[0].availability").value(true));
    }
}
