package com.apnabazaar;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.apnabazaar.service.*;
import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Opt-in tests against the isolated local PostgreSQL database, never a production host. */
@SpringBootTest(
    properties = {
      "APNA_OPENAI_API_KEY=test-no-network",
      "CAMPAIGN_ADMIN_TOKEN=test-operator",
      "FIREBASE_PUSH_ENABLED=false"
    })
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MATCHING_DB_TESTS", matches = "true")
class MatchingWorkflowDatabaseTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired ResidentService residents;
  @Autowired HoodPlanWorkflow workflow;
  @Autowired ResidentMatchingService matching;
  @Autowired HoodRideService rideSearch;
  @MockBean MessageModerationService moderation;
  @MockBean FirebasePhoneIdentity phone;
  @MockBean ChatRateLimiter limiter;
  UUID community, owner, a, b, c;
  String slug;
  Instant departure;
  Map<UUID, String> tokens;

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
    when(moderation.check(anyString())).thenReturn(MessageModerationService.Decision.ALLOW);
    when(limiter.acquire(anyString())).thenReturn(new ChatRateLimiter.Decision(true, 0));
    community = UUID.randomUUID();
    slug = "matching-test-" + community;
    owner = UUID.randomUUID();
    a = UUID.randomUUID();
    b = UUID.randomUUID();
    c = UUID.randomUUID();
    tokens = new HashMap<>();
    departure = Instant.now().plusSeconds(5400).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    jdbc.update(
        "INSERT INTO community(id,name,city,slug) VALUES(?,?,?,?)",
        community,
        "Test hood",
        "Test",
        slug);
    int flat = 100;
    for (UUID id : List.of(owner, a, b, c)) {
      String token = UUID.randomUUID().toString();
      tokens.put(id, token);
      jdbc.update(
          "INSERT INTO resident_profile(id,community_id,token_hash,name,flat_number,verification)"
              + " VALUES(?,?,?,?,?,'verified')",
          id,
          community,
          residents.hash(token),
          "Test neighbour",
          "T1-" + (++flat));
    }
  }

  @AfterEach
  void cleanup() {
    if (community == null) return;
    jdbc.update("DELETE FROM resident_notification WHERE community_id=?", community);
    jdbc.update(
        "DELETE FROM hood_ride_agreement WHERE ride_id IN(SELECT id FROM hood_ride WHERE"
            + " community_id=?)",
        community);
    jdbc.update("DELETE FROM hood_ride WHERE community_id=?", community);
    jdbc.update(
        "DELETE FROM hood_plan_vote WHERE plan_id IN(SELECT id FROM hood_plan WHERE"
            + " community_id=?)",
        community);
    jdbc.update("DELETE FROM hood_plan WHERE community_id=?", community);
    jdbc.update("UPDATE resident_profile SET invitation_id=NULL WHERE community_id=?", community);
    jdbc.update("DELETE FROM resident_flat_invitation WHERE community_id=?", community);
    jdbc.update("DELETE FROM resident_profile WHERE community_id=?", community);
    jdbc.update("DELETE FROM community WHERE id=?", community);
  }

  JsonNode call(String path, Object body, UUID actor, int status) throws Exception {
    var req =
        post("/api/" + slug + "/" + path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body));
    if (actor != null) req.header("Authorization", "Bearer " + tokens.get(actor));
    var result = mvc.perform(req).andReturn();
    assertEquals(
        status, result.getResponse().getStatus(), result.getResponse().getContentAsString());
    String text = result.getResponse().getContentAsString();
    return text.isBlank() ? json.createObjectNode() : json.readTree(text);
  }

  UUID ride(int seats) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO"
            + " hood_ride(id,community_id,owner_id,kind,direction,destination,departure_at,seats,pickup,name,whatsapp_number,is_demo)"
            + " VALUES(?,?,?,'offer','outbound','Airport',?,?, 'Main gate','Test"
            + " organiser','919740893534',false)",
        id,
        community,
        owner,
        java.sql.Timestamp.from(departure),
        seats);
    return id;
  }

  UUID plan() {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO"
            + " hood_plan(id,community_id,organiser_id,title,description,category,starts_at,location,minimum_interested,capacity,is_demo)"
            + " VALUES(?,?,?,'Test workshop','A workshop','Learning',?,'Clubhouse',2,2,false)",
        id,
        community,
        owner,
        java.sql.Timestamp.from(departure));
    return id;
  }

  JsonNode request(UUID ride, UUID member) throws Exception {
    return call(
        "rides/" + ride + "/agreements", Map.of("departureAt", departure, "seats", 1), member, 200);
  }

  void action(JsonNode req, String action, UUID actor, int revision, int expected)
      throws Exception {
    call(
        "rides/agreements/" + req.get("id").asText() + "/action",
        Map.of(
            "action",
            action,
            "revision",
            revision,
            "terms",
            "Meet at main gate. Share the actual fare equally."),
        actor,
        expected);
  }

  void vote(UUID plan, UUID member, String choice) throws Exception {
    call("plans/" + plan + "/vote", Map.of("visitorId", member, "choice", choice), member, 200);
  }

  @Test
  void invitationsArePrivateSingleUseAndBoundToTheFlat() throws Exception {
    var req =
        post("/api/" + slug + "/admin/residents/invitations")
            .header("Authorization", "Bearer test-operator")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"flats\":[\"T1-101\"],\"privateDelivery\":true,\"validDays\":1}");
    var response = mvc.perform(req).andReturn();
    assertEquals(200, response.getResponse().getStatus());
    assertEquals("no-store", response.getResponse().getHeader("Cache-Control"));
    String code =
        json.readTree(response.getResponse().getContentAsString()).get(0).get("code").asText();
    call("resident/verify-flat", Map.of("code", code), a, 400);
    call("resident/verify-flat", Map.of("code", code), owner, 200);
    call("resident/verify-flat", Map.of("code", code), owner, 400);
    assertNotNull(residents.profile(owner).get("flat_verified_at"));
    assertEquals(code.length(), 6);
    String body =
        mvc.perform(
                get("/api/" + slug + "/admin/residents/invitations")
                    .header("Authorization", "Bearer test-operator"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertFalse(body.contains(code));
    assertFalse(body.contains("code_hash"));
    assertEquals(
        401,
        mvc.perform(get("/api/" + slug + "/admin/residents/invitations"))
            .andReturn()
            .getResponse()
            .getStatus());
  }

  @Test
  void rideHoldsPreventOverbookingAndCancellationReleasesSeats() throws Exception {
    UUID ride = ride(1);
    var first = request(ride, a);
    var second = request(ride, b);
    action(first, "propose", owner, 1, 200);
    assertTrue(
        rideSearch.find(community, "offer", "outbound", "Airport", departure, 1, a).isEmpty());
    action(second, "propose", owner, 1, 409);
    action(first, "confirm", b, 2, 403);
    action(first, "confirm", a, 1, 409);
    action(first, "cancel", a, 2, 200);
    assertEquals(
        1, rideSearch.find(community, "offer", "outbound", "Airport", departure, 1, a).size());
    action(second, "propose", owner, 1, 200);
    action(second, "confirm", b, 2, 200);
    assertEquals(
        "confirmed",
        jdbc.queryForObject(
            "SELECT state FROM hood_ride_agreement WHERE id=?",
            String.class,
            UUID.fromString(second.get("id").asText())));
    call("rides/" + ride + "/close", Map.of("visitorId", owner), b, 403);
    call("rides/" + ride + "/close", Map.of("visitorId", owner), owner, 200);
    assertEquals(
        "cancelled",
        jdbc.queryForObject(
            "SELECT state FROM hood_ride_agreement WHERE id=?",
            String.class,
            UUID.fromString(second.get("id").asText())));
  }

  @Test
  void expiredHoldsCannotBeConfirmed() throws Exception {
    UUID ride = ride(1);
    var first = request(ride, a);
    action(first, "propose", owner, 1, 200);
    jdbc.update(
        "UPDATE hood_ride_agreement SET hold_until=now()-interval '1 second' WHERE ride_id=?",
        ride);
    action(first, "confirm", a, 2, 409);
    assertEquals(
        1, rideSearch.find(community, "offer", "outbound", "Airport", departure, 1, a).size());
  }

  @Test
  void recurringRidesHaveSeparateCapacityForEachDeparture() throws Exception {
    UUID ride = ride(1);
    jdbc.update(
        "UPDATE hood_ride SET recurrence_until=(departure_at AT TIME ZONE"
            + " 'Asia/Kolkata')::date+7,weekdays=ARRAY[1,2,3,4,5,6,7] WHERE id=?",
        ride);
    var first = request(ride, a);
    action(first, "propose", owner, 1, 200);
    action(first, "confirm", a, 2, 200);
    var found = rideSearch.find(community, "offer", "outbound", "Airport", null, 1, b);
    assertEquals(1, found.size());
    assertTrue(
        ((java.sql.Timestamp) found.getFirst().get("departure_at")).toInstant().isAfter(departure));
    call(
        "rides/" + ride + "/agreements",
        Map.of("departureAt", departure.plusSeconds(86400), "seats", 1),
        b,
        200);
  }

  @Test
  void plansRequireAnOrganiserAndPromoteTheOldestWaitlistedNeighbour() throws Exception {
    UUID plan = plan();
    call(
        "plans/" + plan + "/manage",
        Map.of("action", "confirm", "note", "Clubhouse confirmed"),
        owner,
        409);
    vote(plan, a, "in");
    vote(plan, b, "in");
    vote(plan, c, "in");
    call(
        "plans/" + plan + "/manage",
        Map.of("action", "confirm", "note", "Meet at clubhouse. Materials included."),
        a,
        403);
    call(
        "plans/" + plan + "/manage",
        Map.of("action", "confirm", "note", "Meet at clubhouse. Materials included."),
        owner,
        200);
    assertEquals(
        2,
        jdbc.queryForObject(
            "SELECT count(*) FROM hood_plan_vote WHERE plan_id=? AND queue_state='attending'",
            Integer.class,
            plan));
    assertEquals(
        "waitlisted",
        jdbc.queryForObject(
            "SELECT queue_state FROM hood_plan_vote WHERE plan_id=? AND visitor_id=?",
            String.class,
            plan,
            c));
    vote(plan, a, "clear");
    assertEquals(
        "attending",
        jdbc.queryForObject(
            "SELECT queue_state FROM hood_plan_vote WHERE plan_id=? AND visitor_id=?",
            String.class,
            plan,
            c));
    vote(plan, a, "in");
    assertEquals(
        "waitlisted",
        jdbc.queryForObject(
            "SELECT queue_state FROM hood_plan_vote WHERE plan_id=? AND visitor_id=?",
            String.class,
            plan,
            a));
    call("plans/" + plan + "/reminder", Map.of("enabled", true), c, 200);
    workflow.reminders();
    workflow.reminders();
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT count(*) FROM resident_notification WHERE resident_id=? AND event_key LIKE"
                + " 'plan-reminder:%'",
            Integer.class, c));
    call(
        "plans/" + plan + "/manage",
        Map.of("action", "cancel", "note", "Organiser unavailable"),
        owner,
        200);
    call("plans/" + plan + "/vote", Map.of("visitorId", c, "choice", "in"), c, 409);
    vote(plan, c, "clear");
  }

  @Test
  void liveParticipationCannotUseAnUnverifiedOrAnotherResidentIdentity() throws Exception {
    UUID plan = plan();
    call("plans/" + plan + "/vote", Map.of("visitorId", a, "choice", "in"), b, 403);
    call("plans/" + plan + "/vote", Map.of("visitorId", a, "choice", "in"), null, 401);
    jdbc.update("UPDATE resident_profile SET verification='pending' WHERE id=?", a);
    call("plans/" + plan + "/vote", Map.of("visitorId", a, "choice", "in"), a, 403);
    call("rides/" + ride(1) + "/agreements", Map.of("departureAt", departure, "seats", 1), a, 403);
  }

  @Test
  void phoneLinkRequiresFlatCodeAndRestoreRotatesThePrivateDeviceSession() throws Exception {
    when(phone.verify("verified-provider-token"))
        .thenReturn(new FirebasePhoneIdentity.Identity("test-firebase-uid", "3534"));
    call("resident/auth/phone", Map.of("idToken", "verified-provider-token"), owner, 403);
    jdbc.update("UPDATE resident_profile SET flat_verified_at=now() WHERE id=?", owner);
    var link =
        call("resident/auth/phone", Map.of("idToken", "verified-provider-token"), owner, 200);
    assertEquals("3534", link.get("profile").get("phone_last4").asText());
    assertFalse(link.toString().contains("firebase_uid"));
    var restore =
        call("resident/auth/phone", Map.of("idToken", "verified-provider-token"), null, 200);
    assertEquals(owner.toString(), restore.get("profile").get("id").asText());
    assertNotEquals(link.get("token"), restore.get("token"));
    assertEquals(
        401,
        mvc.perform(
                get("/api/" + slug + "/resident/profile")
                    .header("Authorization", "Bearer " + link.get("token").asText()))
            .andReturn()
            .getResponse()
            .getStatus());
  }

  @Test
  void membershipAndPersonalUpdatesStayPrivate() throws Exception {
    UUID ride = ride(1);
    var req = request(ride, a);
    action(req, "propose", owner, 1, 200);
    assertEquals(
        401,
        mvc.perform(get("/api/" + slug + "/rides/agreements/mine"))
            .andReturn()
            .getResponse()
            .getStatus());
    var outsider =
        mvc.perform(
                get("/api/" + slug + "/rides/agreements/mine")
                    .header("Authorization", "Bearer " + tokens.get(c)))
            .andReturn();
    assertEquals("[]", outsider.getResponse().getContentAsString());
    var mine =
        mvc.perform(
                get("/api/" + slug + "/rides/agreements/mine")
                    .header("Authorization", "Bearer " + tokens.get(a)))
            .andReturn();
    assertTrue(mine.getResponse().getContentAsString().contains("A neighbour"));
    assertFalse(mine.getResponse().getContentAsString().contains("T1-101"));
  }

  @Test
  void simultaneousRideProposalsCannotHoldTheSameSeat() throws Exception {
    UUID ride = ride(1);
    var first = request(ride, a);
    var second = request(ride, b);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var go = new java.util.concurrent.CountDownLatch(1);
      var results = new ArrayList<java.util.concurrent.Future<Integer>>();
      for (var req : List.of(first, second))
        results.add(
            pool.submit(
                () -> {
                  go.await();
                  return mvc.perform(
                          post("/api/"
                                  + slug
                                  + "/rides/agreements/"
                                  + req.get("id").asText()
                                  + "/action")
                              .header("Authorization", "Bearer " + tokens.get(owner))
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(
                                  json.writeValueAsString(
                                      Map.of(
                                          "action",
                                          "propose",
                                          "revision",
                                          1,
                                          "terms",
                                          "Meet at main gate; share fare equally."))))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      go.countDown();
      var statuses =
          results.stream()
              .map(
                  f -> {
                    try {
                      return f.get(15, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (Exception e) {
                      throw new RuntimeException(e);
                    }
                  })
              .sorted()
              .toList();
      assertEquals(List.of(200, 409), statuses);
    }
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT sum(seats)::int FROM hood_ride_agreement WHERE ride_id=? AND state='proposed'",
            Integer.class,
            ride));
  }

  @Test
  void expiredRevokedAndReissuedInvitationsCannotBeRedeemed() throws Exception {
    String code = "ABCD72";
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO resident_flat_invitation(id,community_id,flat_number,code_hash,expires_at)"
            + " VALUES(?,?,?,?,now()-interval '1 minute')",
        id,
        community,
        "T1-101",
        residents.hash(slug + ":" + code));
    call("resident/verify-flat", Map.of("code", code), owner, 400);
    jdbc.update(
        "UPDATE resident_flat_invitation SET expires_at=now()+interval '1 day',revoked_at=now()"
            + " WHERE id=?",
        id);
    call("resident/verify-flat", Map.of("code", code), owner, 400);
    jdbc.update("UPDATE resident_flat_invitation SET revoked_at=NULL WHERE id=?", id);
    var r =
        mvc.perform(
                post("/api/" + slug + "/admin/residents/invitations")
                    .header("Authorization", "Bearer test-operator")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"flats\":[\"T1-101\"],\"privateDelivery\":true,\"validDays\":1}"))
            .andReturn();
    assertEquals(200, r.getResponse().getStatus());
    call("resident/verify-flat", Map.of("code", code), owner, 400);
  }

  @Test
  void expiredRideHoldsCreateOnePrivateUpdatePerNeighbour() throws Exception {
    UUID ride = ride(1);
    var req = request(ride, a);
    action(req, "propose", owner, 1, 200);
    jdbc.update(
        "UPDATE hood_ride_agreement SET hold_until=now()-interval '1 second' WHERE ride_id=?",
        ride);
    matching.expireRideHolds();
    matching.expireRideHolds();
    assertEquals(
        "expired",
        jdbc.queryForObject(
            "SELECT state FROM hood_ride_agreement WHERE ride_id=?", String.class, ride));
    assertEquals(
        2,
        jdbc.queryForObject(
            "SELECT count(*) FROM resident_notification WHERE community_id=? AND event_key LIKE"
                + " 'ride-expired:%'",
            Integer.class, community));
  }

  @Test
  void ClosingAPostDoesNotRewritePastConfirmedArrangements() throws Exception {
    UUID ride = ride(1);
    var req = request(ride, a);
    action(req, "propose", owner, 1, 200);
    action(req, "confirm", a, 2, 200);
    jdbc.update(
        "UPDATE hood_ride_agreement SET occurrence_at=now()-interval '1 minute' WHERE ride_id=?",
        ride);
    action(req, "cancel", a, 2, 409);
    call("rides/" + ride + "/close", Map.of("visitorId", owner), owner, 200);
    assertEquals(
        "confirmed",
        jdbc.queryForObject(
            "SELECT state FROM hood_ride_agreement WHERE ride_id=?", String.class, ride));
    var response =
        mvc.perform(
                get("/api/" + slug + "/rides/agreements/mine")
                    .header("Authorization", "Bearer " + tokens.get(a)))
            .andReturn();
    assertTrue(response.getResponse().getContentAsString().contains("\"state\":\"past\""));
  }
}
