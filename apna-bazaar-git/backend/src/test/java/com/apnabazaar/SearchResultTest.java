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
        var service = new SearchService(null, null, null, null, null, null, new ObjectMapper(), null, null, null);
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
        var service = new SearchService(null, null, null, null, null, null, new ObjectMapper(), null, null, null);
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
    @Test void availabilityRequiresFreshConsistentEvidence() {
        var service=new SearchService(null,null,null,null,null,null,new ObjectMapper(),null,null,null);
        UUID providerId=UUID.randomUUID();
        var a=com.apnabazaar.entity.Offering.builder().id(UUID.randomUUID()).name("Idli batter").isAvailable(true).liveStatus("available").availabilityUpdatedAt(java.time.Instant.now()).build();
        var b=com.apnabazaar.entity.Offering.builder().id(UUID.randomUUID()).name("Dosa batter").isAvailable(true).liveStatus("available").availabilityUpdatedAt(java.time.Instant.now()).build();
        var provider=Provider.builder().id(providerId).name("Batter shop").rating(java.math.BigDecimal.ZERO).reviewCount(0).offerings(List.of(a,b)).build();
        String raw="{\"sellers\":[{\"id\":\""+providerId+"\",\"matchReason\":\"Batter\"}]}";
        List<MatchedProvider> confirmed=ReflectionTestUtils.invokeMethod(service,"parseResponse",raw,List.of(provider),Map.of());
        assertEquals("available",confirmed.getFirst().availabilityStatus());
        b.setAvailabilityUpdatedAt(java.time.Instant.now().minusSeconds(2*86400));
        List<MatchedProvider> stale=ReflectionTestUtils.invokeMethod(service,"parseResponse",raw,List.of(provider),Map.of());
        assertEquals("unconfirmed",stale.getFirst().availabilityStatus());
        b.setLiveStatus("preorder");b.setAvailabilityUpdatedAt(java.time.Instant.now());
        List<MatchedProvider> mixed=ReflectionTestUtils.invokeMethod(service,"parseResponse",raw,List.of(provider),Map.of());
        assertEquals("unconfirmed",mixed.getFirst().availabilityStatus());
    }
    @Test void soldOutOfferingCannotBeReturnedByItsIdentifier() {
        var service=new SearchService(null,null,null,null,null,null,new ObjectMapper(),null,null,null);
        UUID providerId=UUID.randomUUID(),offeringId=UUID.randomUUID();
        var item=com.apnabazaar.entity.Offering.builder().id(offeringId).name("Idli").isAvailable(true).liveStatus("sold_out").build();
        var provider=Provider.builder().id(providerId).rating(java.math.BigDecimal.ZERO).reviewCount(0).offerings(List.of(item)).build();
        String raw="{\"sellers\":[{\"id\":\""+providerId+"\",\"offeringId\":\""+offeringId+"\",\"matchReason\":\"Idli\"}]}";
        List<MatchedProvider> result=ReflectionTestUtils.invokeMethod(service,"parseResponse",raw,List.of(provider),Map.of());
        assertTrue(result.isEmpty());
    }
}
