package dev.rizwan.stockroom.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.rizwan.stockroom.error.InsufficientStockException;
import dev.rizwan.stockroom.error.StockContentionException;
import dev.rizwan.stockroom.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web layer only - no database. These assert the HTTP contract: status codes
 * and the shape of the problem document, which is what clients actually couple
 * to.
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private OrderService orders;

    private static final String ONE_LINE = """
            {"lines":[{"sku":"DESK-001","quantity":2}]}
            """;

    @Test
    void insufficientStockIsA409CarryingTheNumbers() throws Exception {
        given(orders.place(any())).willThrow(new InsufficientStockException("DESK-001", 2, 1));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(ONE_LINE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient stock"))
                .andExpect(jsonPath("$.sku").value("DESK-001"))
                .andExpect(jsonPath("$.requested").value(2))
                .andExpect(jsonPath("$.available").value(1));
    }

    @Test
    void contentionIsA503MarkedRetryableNotA409() throws Exception {
        given(orders.place(any())).willThrow(new StockContentionException("too busy"));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(ONE_LINE))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.retryable").value(true));
    }

    @Test
    void anEmptyOrderIsRejectedBeforeItReachesTheService() throws Exception {
        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.fields.lines").exists());
    }

    @Test
    void aNegativeQuantityIsRejected() throws Exception {
        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"sku\":\"DESK-001\",\"quantity\":-3}]}"))
                .andExpect(status().isBadRequest());
    }
}
