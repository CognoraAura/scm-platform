package com.scmcloud.order.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "InventoryService", port = "8082")
class OrderPactTest {

    @Pact(consumer = "OrderService")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        return builder
            .given("inventory exists for SKU-001")
            .uponReceiving("a request to check inventory")
            .path("/api/v1/inventory/check")
            .method("POST")
            .body("{\"skuId\":\"SKU-001\",\"quantity\":10}")
            .willRespondWith()
            .status(200)
            .body("{\"available\":true,\"stock\":100}")
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createPact")
    void testInventoryCheck(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        var result = restTemplate.postForEntity(
            mockServer.getUrl() + "/api/v1/inventory/check",
            Map.of("skuId", "SKU-001", "quantity", 10),
            Map.class
        );
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
    }
}
