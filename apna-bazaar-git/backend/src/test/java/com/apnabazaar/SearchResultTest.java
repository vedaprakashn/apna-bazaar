package com.apnabazaar;

import com.apnabazaar.entity.Provider;
import com.apnabazaar.service.SearchService;
import com.apnabazaar.dto.MatchedProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SearchResultTest {
    @Test
    void multipleMatchingOfferingsProduceOneSellerCard() {
        var service = new SearchService(null, null, null, null, null, null, new ObjectMapper());
        UUID id = UUID.randomUUID();
        var provider = Provider.builder().id(id).name("Hindi tutor").rating(java.math.BigDecimal.ZERO).reviewCount(0).build();
        String entry = "{\"id\":\"" + id + "\",\"matchReason\":\"Hindi lessons\"}";
        List<MatchedProvider> results = ReflectionTestUtils.invokeMethod(service, "parseResponse",
            "<sellers>[" + entry + "," + entry + "]</sellers>", List.of(provider), Map.of());
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(id, results.getFirst().id());
    }
    @Test
    void structuredJsonPreservesTeluguIntroAndSellerCard() {
        var service = new SearchService(null, null, null, null, null, null, new ObjectMapper());
        UUID id = UUID.randomUUID();
        var provider = Provider.builder().id(id).name("Idli seller")
            .rating(java.math.BigDecimal.ZERO).reviewCount(0).build();
        String raw = "{\"intro\":\"Idli ikkada dorukutundi\",\"sellers\":[{\"id\":\"" + id
            + "\",\"matchReason\":\"Steamed idli plate\"}]}";
        List<MatchedProvider> results = ReflectionTestUtils.invokeMethod(service, "parseResponse", raw, List.of(provider), Map.of());
        assertNotNull(results);
        assertEquals(id, results.getFirst().id());
        assertEquals("Idli ikkada dorukutundi", ReflectionTestUtils.invokeMethod(service, "extract", raw, "intro"));
    }
}
