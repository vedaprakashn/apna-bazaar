package com.apnabazaar;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.apnabazaar.service.*;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "APNA_OPENAI_API_KEY=test-no-network",
      "CAMPAIGN_ADMIN_TOKEN=test-operator",
      "FIREBASE_PUSH_ENABLED=false"
    })
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MATCHING_DB_TESTS", matches = "true")
class ProviderWorkspaceDatabaseTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired ProviderWorkspaceService studio;
  @Autowired LiveDashboardService dashboard;
  @Autowired com.apnabazaar.repository.ProviderRepository providers;
  UUID community, provider, other, category, item;
  String slug, auth;

  @BeforeEach
  void fixture() {
    String url =
        jdbc.execute(
            (org.springframework.jdbc.core.ConnectionCallback<String>)
                cn -> cn.getMetaData().getURL());
    assertTrue(
        url.startsWith("jdbc:postgresql://localhost:")
            || url.startsWith("jdbc:postgresql://127.0.0.1:"),
        "Local database only");
    community = UUID.randomUUID();
    provider = UUID.randomUUID();
    other = UUID.randomUUID();
    item = UUID.randomUUID();
    slug = "studio-test-" + community;
    jdbc.update(
        "INSERT INTO community(id,name,city,slug) VALUES(?,'Studio test','Hyderabad',?)",
        community,
        slug);
    for (UUID id : List.of(provider, other))
      jdbc.update(
          "INSERT INTO provider(id,community_id,name,shop_name,status) VALUES(?,?,'Test"
              + " owner','Test shop','active')",
          id,
          community);
    category = jdbc.queryForObject("SELECT id FROM category WHERE name='Snacks'", UUID.class);
    auth = "Bearer " + studio.issue(slug, provider).get("token");
  }

  @AfterEach
  void cleanup() {
    if (community == null) return;
    jdbc.update(
        "DELETE FROM offering_schedule WHERE offering_id IN (SELECT id FROM offering WHERE"
            + " provider_id IN (SELECT id FROM provider WHERE community_id=?))",
        community);
    jdbc.update(
        "DELETE FROM offering WHERE provider_id IN (SELECT id FROM provider WHERE community_id=?)",
        community);
    jdbc.update("DELETE FROM provider WHERE community_id=?", community);
    jdbc.update("DELETE FROM community WHERE id=?", community);
  }

  ProviderWorkspaceService.Item item(Map<LocalDate, Boolean> dates) {
    return new ProviderWorkspaceService.Item(
        item,
        category,
        "Samosa",
        "Fresh samosa",
        "food_item",
        new BigDecimal("40.00"),
        "pair",
        true,
        LocalTime.of(16, 0),
        LocalTime.of(20, 0),
        List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
        dates);
  }

  Map<String, Object> save(long rev, ProviderWorkspaceService.Item i) {
    return studio.save(
        slug,
        provider,
        auth,
        new ProviderWorkspaceService.Save(rev, new ProviderWorkspaceService.Draft(List.of(i))));
  }

  String base() {
    return "/api/" + slug + "/provider-workspace/" + provider;
  }

  @Test
  void publicIdsCannotEditAndTokensCannotCrossShops() throws Exception {
    assertEquals(401, mvc.perform(get(base())).andReturn().getResponse().getStatus());
    assertEquals(
        401,
        mvc.perform(
                get("/api/" + slug + "/provider-workspace/" + other).header("Authorization", auth))
            .andReturn()
            .getResponse()
            .getStatus());
    assertEquals(
        401,
        mvc.perform(
                get("/api/tridasa/provider-workspace/" + provider).header("Authorization", auth))
            .andReturn()
            .getResponse()
            .getStatus());
    assertEquals(
        "no-store",
        mvc.perform(get(base()).header("Authorization", auth))
            .andReturn()
            .getResponse()
            .getHeader("Cache-Control"));
  }

  @Test
  void draftsArePrivateUntilPublishAndPricesAndSchedulesGoLive() {
    save(0, item(Map.of()));
    assertTrue(dashboard.catalog(slug).isEmpty());
    studio.publish(slug, provider, auth, 1);
    var catalog = dashboard.catalog(slug);
    assertEquals(1, catalog.size());
    assertEquals(new BigDecimal("40.00"), catalog.getFirst().get("price"));
    var page = dashboard.storefront(slug, provider);
    assertTrue(page.toString().contains("16:00"));
    assertFalse(page.toString().contains(auth));
  }

  @Test
  void dateSwitchesApplyToTodayWithoutDisablingTomorrow() {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
    save(0, item(Map.of(today, false, today.plusDays(1), true)));
    studio.publish(slug, provider, auth, 1);
    assertTrue(dashboard.catalog(slug).isEmpty());
    assertEquals(
        false,
        jdbc.queryForObject("SELECT provider_item_available(?,?)", Boolean.class, item, today));
    assertEquals(
        true,
        jdbc.queryForObject(
            "SELECT provider_item_available(?,?)", Boolean.class, item, today.plusDays(1)));
    assertEquals(
        false,
        providers.findActiveWithOfferings(community).stream()
            .filter(p -> p.getId().equals(provider))
            .findFirst()
            .orElseThrow()
            .getOfferings()
            .getFirst()
            .getIsAvailable());
  }

  @Test
  void staleRevisionsCannotOverwriteNewerDraft() throws Exception {
    save(0, item(Map.of()));
    var payload =
        new ProviderWorkspaceService.Save(
            0, new ProviderWorkspaceService.Draft(List.of(item(Map.of()))));
    assertEquals(
        409,
        mvc.perform(
                put(base() + "/draft")
                    .header("Authorization", auth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(payload)))
            .andReturn()
            .getResponse()
            .getStatus());
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> studio.publish(slug, provider, auth, 0));
  }

  @Test
  void rotatingLinkRevokesPreviousAccessAndKeepsDraft() throws Exception {
    save(0, item(Map.of()));
    String fresh = "Bearer " + studio.issue(slug, provider).get("token");
    assertEquals(
        401,
        mvc.perform(get(base()).header("Authorization", auth))
            .andReturn()
            .getResponse()
            .getStatus());
    assertEquals(
        1L, ((Number) studio.workspace(slug, provider, fresh).get("revision")).longValue());
  }

  @Test
  void removingItemArchivesItWithoutAffectingOtherShops() {
    save(0, item(Map.of()));
    studio.publish(slug, provider, auth, 1);
    UUID second = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO offering(id,provider_id,category_id,name,is_available) VALUES(?,?,?,'Other"
            + " shop item',true)",
        second,
        other,
        category);
    studio.save(
        slug,
        provider,
        auth,
        new ProviderWorkspaceService.Save(1, new ProviderWorkspaceService.Draft(List.of())));
    studio.publish(slug, provider, auth, 2);
    assertEquals(1, dashboard.catalog(slug).size());
    assertEquals(
        true, jdbc.queryForObject("SELECT archived FROM offering WHERE id=?", Boolean.class, item));
    assertTrue(((List<?>) dashboard.storefront(slug, provider).get("offerings")).isEmpty());
  }

  @Test
  void rejectsForeignItemAndInvalidPriceOrTiming() {
    jdbc.update(
        "INSERT INTO offering(id,provider_id,category_id,name) VALUES(?,?,?,'Foreign item')",
        item,
        other,
        category);
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> save(0, item(Map.of())));
    jdbc.update("DELETE FROM offering WHERE id=?", item);
    var i = item(Map.of());
    var invalid =
        new ProviderWorkspaceService.Item(
            i.id(),
            i.categoryId(),
            i.name(),
            i.description(),
            i.type(),
            new BigDecimal("-1"),
            i.unit(),
            true,
            i.ends(),
            i.starts(),
            i.days(),
            i.dates());
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class, () -> save(0, invalid));
    assertTrue(dashboard.catalog(slug).isEmpty());
  }

  @Test
  void operatorProvisioningRequiresKeyAndNewProvidersGetNoVerifiedBadge() throws Exception {
    assertEquals(
        401,
        mvc.perform(post("/api/" + slug + "/admin/providers/" + provider + "/edit-link"))
            .andReturn()
            .getResponse()
            .getStatus());
    var result =
        mvc.perform(
                post("/api/" + slug + "/admin/providers")
                    .header("Authorization", "Bearer test-operator")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Riya\",\"shop\":\"Riya"
                            + " bites\",\"flat\":\"T1-105\",\"whatsapp\":\"919740893534\",\"type\":\"food_seller\"}"))
            .andReturn()
            .getResponse();
    assertEquals(200, result.getStatus());
    UUID created =
        UUID.fromString(json.readTree(result.getContentAsString()).path("providerId").asText());
    assertFalse(
        jdbc.queryForObject("SELECT is_verified FROM provider WHERE id=?", Boolean.class, created));
  }
}
