package com.apnabazaar;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.apnabazaar.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(
    properties = {
      "APNA_OPENAI_API_KEY=test-no-network",
      "CAMPAIGN_ADMIN_TOKEN=test-operator",
      "FIREBASE_PUSH_ENABLED=false",
      "ORDER_OTP_SECRET=test-orders-secret-at-least-32-characters-long"
    })
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MATCHING_DB_TESTS", matches = "true")
class OrderBookDatabaseTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired OrderBookService book;
  @Autowired ProviderWorkspaceService studio;
  @Autowired ResidentService residents;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  UUID c, p, p2, b, b2, item;
  String slug, pa, p2a, ba, b2a;

  @BeforeEach
  void setup() {
    String url =
        jdbc.execute(
            (org.springframework.jdbc.core.ConnectionCallback<String>)
                cn -> cn.getMetaData().getURL());
    assertTrue(
        url.startsWith("jdbc:postgresql://localhost:")
            || url.startsWith("jdbc:postgresql://127.0.0.1:"),
        "Local DB only");
    c = UUID.randomUUID();
    p = UUID.randomUUID();
    p2 = UUID.randomUUID();
    b = UUID.randomUUID();
    b2 = UUID.randomUUID();
    item = UUID.randomUUID();
    slug = "orders-test-" + c;
    jdbc.update(
        "INSERT INTO community(id,name,city,slug) VALUES(?,'Order tests','Hyderabad',?)", c, slug);
    for (UUID id : List.of(p, p2))
      jdbc.update(
          "INSERT INTO provider(id,community_id,name,shop_name,flat_number,status) VALUES(?,?,'Test"
              + " owner','Test shop','T1-101','active')",
          id,
          c);
    pa = "Bearer " + studio.issue(slug, p).get("token");
    p2a = "Bearer " + studio.issue(slug, p2).get("token");
    ba = "Bearer buyer-" + b;
    b2a = "Bearer buyer-" + b2;
    for (var e : Map.of(b, ba, b2, b2a).entrySet())
      jdbc.update(
          "INSERT INTO resident_profile(id,community_id,token_hash,name,flat_number,verification)"
              + " VALUES(?,?,?,'Test buyer','T2-202','verified')",
          e.getKey(),
          c,
          residents.hash(e.getValue().substring(7)));
    UUID cat = jdbc.queryForObject("SELECT id FROM category WHERE name='Snacks'", UUID.class);
    jdbc.update(
        "INSERT INTO offering(id,provider_id,category_id,name,base_price,unit,is_available)"
            + " VALUES(?,?,?,'Samosa',40,'pair',true)",
        item,
        p,
        cat);
    jdbc.update(
        "INSERT INTO"
            + " offering_schedule(offering_id,day_scope,serves_from,serves_to,accepts_preorder)"
            + " VALUES(?,'daily','00:00','23:59',false)",
        item);
  }

  @AfterEach
  void cleanup() {
    if (c == null) return;
    jdbc.update(
        "DELETE FROM provider_order_notification WHERE provider_id IN (SELECT id FROM provider"
            + " WHERE community_id=?)",
        c);
    jdbc.update(
        "DELETE FROM commerce_order_event WHERE order_id IN (SELECT id FROM commerce_order WHERE"
            + " community_id=?)",
        c);
    jdbc.update("DELETE FROM commerce_order WHERE community_id=?", c);
    jdbc.update("DELETE FROM resident_notification WHERE community_id=?", c);
    jdbc.update("DELETE FROM offering_schedule WHERE offering_id=?", item);
    jdbc.update(
        "DELETE FROM offering WHERE provider_id IN (SELECT id FROM provider WHERE community_id=?)",
        c);
    jdbc.update("DELETE FROM provider WHERE community_id=?", c);
    jdbc.update("DELETE FROM resident_profile WHERE community_id=?", c);
    jdbc.update("DELETE FROM community WHERE id=?", c);
  }

  OrderBookService.Request request(boolean manual) {
    return new OrderBookService.Request(
        UUID.randomUUID(),
        p,
        "External buyer",
        "T3-303",
        manual ? "whatsapp" : null,
        "Pickup",
        LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1),
        LocalTime.NOON,
        "Less spicy",
        true,
        List.of(
            new OrderBookService.Line(manual ? null : item, "Samosa", 2, new BigDecimal("40.00"))));
  }

  Map<String, Object> create(boolean manual) {
    return book.create(slug, manual ? pa : ba, request(manual), manual, manual ? p : null);
  }

  UUID id(Map<String, Object> o) {
    return (UUID) o.get("id");
  }

  OrderBookService.Action action(Map<String, Object> o, String state) {
    return new OrderBookService.Action(
        ((Number) o.get("revision")).longValue(), state, "Provider note", null);
  }

  Map<String, Object> act(Map<String, Object> o, String state) {
    return book.providerAction(slug, p, id(o), pa, action(o, state));
  }

  @Test
  void manualRecordsPersistWithoutConsumerAndWithoutInventedCodes() {
    var o = create(true);
    assertEquals("accepted", o.get("status"));
    assertEquals(false, o.get("consumerLinked"));
    assertEquals("whatsapp", o.get("source"));
    assertEquals(1, book.providerOrders(slug, p, pa, "open", null).size());
    o = act(o, "preparing");
    o = act(o, "ready");
    o = act(o, "manual_complete");
    assertEquals("provider_recorded", o.get("completion_method"));
    assertTrue(book.mine(slug, ba).isEmpty());
    assertEquals(1, book.providerOrders(slug, p, pa, "all", null).size());
  }

  @Test
  void nativeOrderCompletesOnlyWithConsumerCodeAndNotProviderOverride() {
    var o = create(false);
    UUID id = id(o);
    assertEquals("requested", o.get("status"));
    assertFalse(o.containsKey("otp_cipher"));
    assertEquals(1, book.alerts(slug, p, pa, false).size());
    o = act(o, "accepted");
    String code = book.code(slug, id, ba, false).get("code").toString();
    assertEquals(6, code.length());
    assertFalse(book.providerOrder(slug, p, id, pa).containsValue(code));
    o = act(o, "ready");
    var ready = o;
    assertThrows(ResponseStatusException.class, () -> act(ready, "manual_complete"));
    var result =
        book.verify(
            slug,
            p,
            id,
            pa,
            new OrderBookService.Action(
                ((Number) o.get("revision")).longValue(), "verify", null, code));
    assertEquals(200, result.status());
    assertEquals("otp", result.data().get("completion_method"));
    assertThrows(ResponseStatusException.class, () -> book.code(slug, id, ba, false));
    assertEquals(
        4,
        jdbc.queryForObject(
            "SELECT count(*) FROM commerce_order_event WHERE order_id=?", Integer.class, id));
    assertTrue(
        jdbc.queryForObject(
                "SELECT count(*) FROM resident_notification WHERE resident_id=?", Integer.class, b)
            >= 3);
  }

  @Test
  void wrongAttemptsCommitAndLockEvenWithCorrectCode() {
    var o = act(create(false), "accepted");
    String code = book.code(slug, id(o), ba, false).get("code").toString();
    o = act(o, "ready");
    String wrong = code.equals("000000") ? "111111" : "000000";
    for (int i = 0; i < 5; i++)
      assertEquals(
          400,
          book.verify(slug, p, id(o), pa, new OrderBookService.Action(2, "verify", null, wrong))
              .status());
    assertEquals(
        5,
        jdbc.queryForObject(
            "SELECT otp_attempts FROM commerce_order WHERE id=?", Integer.class, id(o)));
    assertEquals(
        429,
        book.verify(slug, p, id(o), pa, new OrderBookService.Action(2, "verify", null, code))
            .status());
    assertEquals("ready", book.consumer(slug, id(o), ba).get("status"));
  }

  @Test
  void buyersProvidersAndCommunitiesCannotReadOthersOrders() throws Exception {
    var o = create(false);
    UUID id = id(o);
    assertThrows(ResponseStatusException.class, () -> book.consumer(slug, id, b2a));
    assertThrows(ResponseStatusException.class, () -> book.providerOrder(slug, p2, id, p2a));
    assertThrows(ResponseStatusException.class, () -> book.providerOrder("sayuk", p, id, pa));
    assertEquals(
        401,
        mvc.perform(get("/api/" + slug + "/orders/mine")).andReturn().getResponse().getStatus());
    assertEquals(
        "no-store",
        mvc.perform(get("/api/" + slug + "/orders/" + id))
            .andReturn()
            .getResponse()
            .getHeader("Cache-Control"));
  }

  @Test
  void retryCreatesOneOrderAndDifferentPayloadConflicts() {
    var r = request(false);
    var a = book.create(slug, ba, r, false, null);
    var again = book.create(slug, ba, r, false, null);
    assertEquals(a.get("id"), again.get("id"));
    var different =
        new OrderBookService.Request(
            r.key(), p, null, null, null, "Pickup", r.date(), r.time(), "changed", true, r.lines());
    assertThrows(
        ResponseStatusException.class, () -> book.create(slug, ba, different, false, null));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT count(*) FROM commerce_order WHERE provider_id=?", Integer.class, p));
  }

  @Test
  void changesToPriceAvailabilityAndTimingAreChecked() {
    var r = request(false);
    jdbc.update("UPDATE offering SET base_price=50 WHERE id=?", item);
    assertThrows(ResponseStatusException.class, () -> book.create(slug, ba, r, false, null));
    jdbc.update("UPDATE offering SET base_price=40 WHERE id=?", item);
    jdbc.update(
        "INSERT INTO offering_day_availability(offering_id,service_date,available)"
            + " VALUES(?,?,false)",
        item,
        r.date());
    assertThrows(ResponseStatusException.class, () -> book.create(slug, ba, r, false, null));
    jdbc.update("DELETE FROM offering_day_availability WHERE offering_id=?", item);
    jdbc.update(
        "UPDATE offering_schedule SET serves_from='16:00',serves_to='18:00' WHERE offering_id=?",
        item);
    assertThrows(ResponseStatusException.class, () -> book.create(slug, ba, r, false, null));
  }

  @Test
  void orderSnapshotsSurviveCatalogEditsAndTerminalTransitionsRejectReplays() {
    var o = create(false);
    jdbc.update("UPDATE offering SET name='Changed',base_price=90,archived=true WHERE id=?", item);
    assertEquals(new BigDecimal("80.00"), book.consumer(slug, id(o), ba).get("total"));
    assertTrue(book.consumer(slug, id(o), ba).get("items").toString().contains("Samosa"));
    o = act(o, "rejected");
    var terminal = o;
    assertThrows(ResponseStatusException.class, () -> act(terminal, "accepted"));
    assertEquals(
        2,
        jdbc.queryForObject(
            "SELECT count(*) FROM commerce_order_event WHERE order_id=?", Integer.class, id(o)));
  }

  @Test
  void consumerCanCancelOnlyBeforeAcceptanceAndCancellationInvalidatesCode() {
    var o = create(false);
    assertEquals("cancelled", book.cancel(slug, id(o), ba).get("status"));
    var other = act(create(false), "accepted");
    var accepted = other;
    assertThrows(ResponseStatusException.class, () -> book.cancel(slug, id(accepted), ba));
    other = act(other, "cancelled");
    assertNull(
        jdbc.queryForObject(
            "SELECT otp_cipher FROM commerce_order WHERE id=?", String.class, id(other)));
  }

  @Test
  void notificationReadStateAndHistoryFiltersPersist() {
    var o = create(false);
    var notes = book.alerts(slug, p, pa, false);
    book.readAlert(slug, p, (UUID) notes.getFirst().get("id"), pa);
    assertNotNull(book.alerts(slug, p, pa, false).getFirst().get("read_at"));
    act(o, "rejected");
    assertTrue(book.providerOrders(slug, p, pa, "open", null).isEmpty());
    assertEquals(
        1,
        book.providerOrders(
                slug,
                p,
                pa,
                "custom",
                LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1).toString())
            .size());
  }

  @Test
  void expiryUpdatesBothInboxesWithoutAutoAccepting() {
    var o = create(false);
    jdbc.update(
        "UPDATE commerce_order SET created_at=now()-interval '31 minutes' WHERE id=?", id(o));
    book.expire();
    assertEquals("expired", book.consumer(slug, id(o), ba).get("status"));
    assertEquals(2, book.alerts(slug, p, pa, false).size());
  }

  @Test
  void deliveryRequiresProviderOptInAndUnverifiedBuyerCannotSubmit() {
    var r = request(false);
    var delivery =
        new OrderBookService.Request(
            r.key(), p, null, null, null, "Delivery", r.date(), r.time(), "", true, r.lines());
    assertThrows(ResponseStatusException.class, () -> book.create(slug, ba, delivery, false, null));
    jdbc.update("UPDATE resident_profile SET verification='pending' WHERE id=?", b);
    assertThrows(ResponseStatusException.class, () -> book.create(slug, ba, r, false, null));
  }
}
