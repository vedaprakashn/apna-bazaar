package com.apnabazaar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderBookService {
  private final JdbcTemplate jdbc;
  private final ResidentService residents;
  private final ResidentMatchingService matching;
  private final ProviderWorkspaceService studio;
  private final ObjectMapper mapper;
  private final String secret;
  private final SecureRandom random = new SecureRandom();
  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

  public OrderBookService(
      JdbcTemplate jdbc,
      ResidentService residents,
      ResidentMatchingService matching,
      ProviderWorkspaceService studio,
      ObjectMapper mapper,
      @Value("${ORDER_OTP_SECRET:}") String secret) {
    this.jdbc = jdbc;
    this.residents = residents;
    this.matching = matching;
    this.studio = studio;
    this.mapper = mapper;
    this.secret = secret;
  }

  public record Line(UUID offeringId, String name, int quantity, BigDecimal price) {}

  public record Request(
      UUID key,
      UUID providerId,
      String name,
      String flat,
      String source,
      String fulfilment,
      LocalDate date,
      LocalTime time,
      String note,
      boolean confirmed,
      List<Line> lines) {}

  public record Action(long revision, String action, String reason, String code) {}

  public record Result(int status, Map<String, Object> data) {}

  private ResponseStatusException error(HttpStatus status, String text) {
    return new ResponseStatusException(status, text);
  }

  private ResponseStatusException bad(String text) {
    return error(HttpStatus.BAD_REQUEST, text);
  }

  private String json(Object v) {
    try {
      return mapper.writeValueAsString(v);
    } catch (Exception e) {
      throw bad("Invalid order data");
    }
  }

  private UUID buyer(String slug, String auth) {
    return matching.member(slug, auth);
  }

  public boolean codesConfigured() {
    return secret.length() >= 32;
  }

  private String crypt(UUID order, String text, boolean encrypt) {
    if (!codesConfigured())
      throw error(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Configure ORDER_OTP_SECRET on the app service before accepting consumer orders");
    try {
      var key =
          new SecretKeySpec(
              MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)),
              "AES");
      byte[] iv = new byte[12], value;
      if (encrypt) {
        random.nextBytes(iv);
        value = text.getBytes(StandardCharsets.UTF_8);
      } else {
        byte[] all = Base64.getDecoder().decode(text);
        iv = Arrays.copyOf(all, 12);
        value = Arrays.copyOfRange(all, 12, all.length);
      }
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(
          encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
      c.updateAAD(order.toString().getBytes(StandardCharsets.UTF_8));
      byte[] out = c.doFinal(value);
      if (!encrypt) return new String(out, StandardCharsets.UTF_8);
      byte[] all = new byte[iv.length + out.length];
      System.arraycopy(iv, 0, all, 0, iv.length);
      System.arraycopy(out, 0, all, iv.length, out.length);
      return Base64.getEncoder().encodeToString(all);
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw error(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Handover code unavailable. Ask the consumer to renew their code");
    }
  }

  private void issueCode(UUID id) {
    String code = String.format(Locale.ROOT, "%06d", random.nextInt(1000000));
    jdbc.update(
        "UPDATE commerce_order SET otp_cipher=?,otp_expires_at=now()+interval '7"
            + " days',otp_issued_at=now(),otp_attempts=0,otp_locked_until=NULL WHERE id=?",
        crypt(id, code, true),
        id);
  }

  private Map<String, Object> order(UUID community, UUID id, boolean lock) {
    var rows =
        jdbc.queryForList(
            "SELECT * FROM commerce_order WHERE id=? AND community_id=?"
                + (lock ? " FOR UPDATE" : ""),
            id,
            community);
    if (rows.isEmpty()) throw error(HttpStatus.NOT_FOUND, "Order not found");
    return rows.getFirst();
  }

  private Map<String, Object> provider(UUID community, UUID id) {
    var rows =
        jdbc.queryForList(
            "SELECT * FROM provider WHERE id=? AND community_id=? AND status='active'",
            id,
            community);
    if (rows.isEmpty()) throw error(HttpStatus.NOT_FOUND, "Shop not found");
    return rows.getFirst();
  }

  private Map<String, Object> safe(Map<String, Object> row) {
    var out = new LinkedHashMap<String, Object>();
    for (String k :
        List.of(
            "id",
            "reference",
            "provider_id",
            "customer_name",
            "customer_flat",
            "pickup_flat",
            "fulfilment",
            "fulfilment_at",
            "total",
            "note",
            "status",
            "revision",
            "source",
            "completion_method",
            "reason",
            "created_at",
            "updated_at")) out.put(k, row.get(k));
    out.put("consumerLinked", row.get("buyer_id") != null);
    try {
      out.put("items", mapper.readValue(row.get("items").toString(), List.class));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return out;
  }

  private void event(Map<String, Object> o, String actor, boolean notifyProvider) {
    UUID id = (UUID) o.get("id"),
        p = (UUID) o.get("provider_id"),
        community = (UUID) o.get("community_id");
    long revision = ((Number) o.get("revision")).longValue();
    String status = o.get("status").toString(), ref = o.get("reference").toString();
    jdbc.update(
        "INSERT INTO commerce_order_event(id,order_id,revision,actor,status) VALUES(?,?,?,?,?) ON"
            + " CONFLICT(order_id,revision) DO NOTHING",
        UUID.randomUUID(),
        id,
        revision,
        actor,
        status);
    String title =
        switch (status) {
          case "requested" -> "A new order just landed";
          case "accepted" -> "Your order is accepted";
          case "ready" -> "Your order is ready";
          case "completed" -> "Order handed over";
          case "rejected" -> "A change of plans";
          case "cancelled" -> "Order cancelled";
          case "expired" -> "Order request expired";
          default -> "Your order is in the making";
        };
    String body =
        ref
            + " · "
            + status
            + (o.get("reason").toString().isBlank() ? "" : " · " + o.get("reason"));
    if (notifyProvider)
      jdbc.update(
          "INSERT INTO provider_order_notification(id,provider_id,order_id,revision,title,body)"
              + " VALUES(?,?,?,?,?,?) ON CONFLICT(order_id,revision) DO NOTHING",
          UUID.randomUUID(),
          p,
          id,
          revision,
          title,
          body);
    if (o.get("buyer_id") != null)
      matching.notify(
          community,
          (UUID) o.get("buyer_id"),
          "order:" + id + ":" + revision,
          title,
          body,
          "/orders/index.html?community="
              + jdbc.queryForObject(
                  "SELECT slug FROM community WHERE id=?", String.class, community)
              + "&order="
              + id);
  }

  private void validate(Request r, boolean manual) {
    if (r == null
        || r.key() == null
        || r.providerId() == null
        || r.lines() == null
        || r.lines().isEmpty()
        || r.lines().size() > 20
        || r.date() == null
        || r.time() == null
        || !r.confirmed()) throw bad("Review and confirm your order details");
    if (r.note() != null && r.note().length() > 500)
      throw bad("Use a note of up to 500 characters");
    if (!Set.of("Pickup", "Delivery").contains(Objects.toString(r.fulfilment(), "")))
      throw bad("Choose pickup or delivery");
    LocalDate today = LocalDate.now(IST);
    if (r.date().isBefore(today.minusDays(manual ? 90 : 0)) || r.date().isAfter(today.plusDays(90)))
      throw bad("Choose a date within the next 90 days (manual backfill: 90 days past)");
    if (!manual
        && r.date()
            .atTime(r.time())
            .atZone(IST)
            .toInstant()
            .isBefore(Instant.now().plusSeconds(900)))
      throw bad("Allow at least 15 minutes for the provider to respond");
  }

  private boolean serves(UUID id, LocalDate date, LocalTime time) {
    var schedules =
        jdbc.queryForList(
            "SELECT day_scope::text,days_of_week,serves_from,serves_to FROM offering_schedule WHERE"
                + " offering_id=? AND is_active",
            id);
    if (schedules.isEmpty()) return true;
    String day = date.getDayOfWeek().name().substring(0, 3);
    int d = date.getDayOfWeek().getValue();
    return schedules.stream()
        .anyMatch(
            s -> {
              boolean dayOk =
                  switch (s.get("day_scope").toString()) {
                    case "weekdays" -> d <= 5;
                    case "weekends" -> d >= 6;
                    case "saturday_only" -> d == 6;
                    case "sunday_only" -> d == 7;
                    case "specific_days" ->
                        Arrays.asList(Objects.toString(s.get("days_of_week"), "").split(","))
                            .contains(day);
                    default -> true;
                  };
              LocalTime from = ((java.sql.Time) s.get("serves_from")).toLocalTime(),
                  to = ((java.sql.Time) s.get("serves_to")).toLocalTime();
              return dayOk && !time.isBefore(from) && !time.isAfter(to);
            });
  }

  @Transactional
  public Map<String, Object> create(
      String slug, String auth, Request r, boolean manual, UUID providerId) {
    UUID community = residents.community(slug), resident = null;
    if (manual) studio.authorize(slug, providerId, auth);
    else resident = buyer(slug, auth);
    if (r == null || r.providerId() == null || r.key() == null)
      throw bad("Review and confirm your order details");
    if (manual && !r.providerId().equals(providerId)) throw bad("Choose your own shop");
    String hash = residents.hash(json(r) + (manual ? "manual" : resident.toString()));
    // Serialise idempotent retries across both entry methods for this provider.
    jdbc.queryForList("SELECT id FROM provider WHERE id=? FOR UPDATE", r.providerId());
    var previous =
        jdbc.queryForList(
            "SELECT * FROM commerce_order WHERE provider_id=? AND idempotency_key=?",
            r.providerId(),
            r.key());
    if (!previous.isEmpty()) {
      var old = previous.getFirst();
      if (!hash.equals(old.get("request_hash")) || !manual && !resident.equals(old.get("buyer_id")))
        throw error(HttpStatus.CONFLICT, "This request key already belongs to a different order");
      return safe(old);
    }
    validate(r, manual);
    if (!manual && !codesConfigured())
      throw error(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Consumer ordering is being set up. Please try again shortly");
    var shop = provider(community, r.providerId());
    if ("Delivery".equals(r.fulfilment()) && !Boolean.TRUE.equals(shop.get("delivery_enabled")))
      throw bad("This provider currently offers pickup only");
    if (manual
        && (!Set.of("whatsapp", "phone", "paper", "excel", "walkin")
            .contains(Objects.toString(r.source(), ""))))
      throw bad("Choose an external order source");
    String name = r.name(), flat = r.flat();
    if (!manual) {
      var profile = residents.profile(resident);
      name = Objects.toString(profile.get("name"), "");
      flat = Objects.toString(profile.get("flat_number"), "");
    }
    if (name == null
        || name.isBlank()
        || name.length() > 100
        || flat == null
        || flat.isBlank()
        || flat.length() > 50) throw bad("Add a customer name and flat");
    List<List<Object>> items = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO;
    Set<UUID> seen = new HashSet<>();
    for (Line line : r.lines()) {
      if (line == null || line.quantity() < 1 || line.quantity() > 100)
        throw bad("Use quantities between 1 and 100");
      String itemName = line.name();
      BigDecimal price = line.price();
      if (!manual) {
        if (line.offeringId() == null || !seen.add(line.offeringId()))
          throw bad("Each item can appear once per order");
        var rows =
            jdbc.queryForList(
                "SELECT *,provider_item_available(id,?) AS enabled_on_date FROM offering WHERE id=?"
                    + " AND provider_id=? FOR SHARE",
                r.date(),
                line.offeringId(),
                r.providerId());
        if (rows.isEmpty()) throw bad("An item no longer belongs to this shop");
        var item = rows.getFirst();
        if (!Boolean.TRUE.equals(item.get("enabled_on_date"))
            || "sold_out".equals(item.get("live_status"))
            || !serves(line.offeringId(), r.date(), r.time()))
          throw error(
              HttpStatus.CONFLICT,
              "An item is unavailable for this date/time. Review the shop menu");
        BigDecimal current = (BigDecimal) item.get("base_price");
        if (current == null || price == null || current.compareTo(price) != 0)
          throw error(
              HttpStatus.CONFLICT,
              "Prices changed. Reload the shop and review your cart before submitting");
        price = current;
        itemName = item.get("name").toString();
      }
      if (itemName == null
          || itemName.isBlank()
          || itemName.length() > 200
          || price == null
          || price.signum() < 0
          || price.scale() > 2
          || price.compareTo(new BigDecimal("999999.99")) > 0)
        throw bad("Check item names and agreed prices");
      items.add(List.of(itemName.trim(), line.quantity(), price));
      total = total.add(price.multiply(BigDecimal.valueOf(line.quantity())));
    }
    UUID id = UUID.randomUUID();
    String reference =
        "HH-" + id.toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    jdbc.update(
        "INSERT INTO"
            + " commerce_order(id,reference,community_id,provider_id,buyer_id,idempotency_key,request_hash,source,customer_name,customer_flat,pickup_flat,fulfilment,fulfilment_at,items,total,note,status)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?,?,?)",
        id,
        reference,
        community,
        r.providerId(),
        resident,
        r.key(),
        hash,
        manual ? r.source() : "heyhood",
        name.trim(),
        flat.trim(),
        Objects.toString(shop.get("flat_number"), ""),
        r.fulfilment(),
        java.sql.Timestamp.from(r.date().atTime(r.time()).atZone(IST).toInstant()),
        json(items),
        total,
        Objects.toString(r.note(), ""),
        manual ? "accepted" : "requested");
    var saved = order(community, id, false);
    event(saved, manual ? "provider" : "consumer", !manual);
    return safe(saved);
  }

  @Transactional
  public List<Map<String, Object>> providerOrders(
      String slug, UUID provider, String auth, String range, String date) {
    studio.authorize(slug, provider, auth);
    LocalDate today = LocalDate.now(IST), from = today, to = today;
    switch (Objects.toString(range, "open")) {
      case "yesterday" -> from = to = today.minusDays(1);
      case "week" -> from = today.minusDays(6);
      case "custom" -> {
        try {
          from = to = LocalDate.parse(date);
        } catch (Exception e) {
          throw bad("Choose a date");
        }
      }
      default -> {}
    }
    String scope = "";
    List<Object> args = new ArrayList<>(List.of(provider));
    if ("open".equals(range)) scope = " AND status IN ('requested','accepted','preparing','ready')";
    else {
      scope = " AND status IN ('completed','rejected','cancelled','expired')";
      if (!"all".equals(range)) {
        scope += " AND (fulfilment_at AT TIME ZONE 'Asia/Kolkata')::date BETWEEN ? AND ?";
        args.add(from);
        args.add(to);
      }
    }
    return jdbc
        .queryForList(
            "SELECT * FROM commerce_order WHERE provider_id=?"
                + scope
                + " ORDER BY created_at DESC LIMIT 200",
            args.toArray())
        .stream()
        .map(this::safe)
        .toList();
  }

  @Transactional
  public Map<String, Object> providerAction(String slug, UUID p, UUID id, String auth, Action a) {
    studio.authorize(slug, p, auth);
    var row = order(residents.community(slug), id, true);
    if (!p.equals(row.get("provider_id"))) throw error(HttpStatus.NOT_FOUND, "Order not found");
    if (a == null || a.revision() != ((Number) row.get("revision")).longValue())
      throw error(HttpStatus.CONFLICT, "Order changed. Refresh before acting");
    String state = row.get("status").toString(), next = a.action();
    boolean linked = row.get("buyer_id") != null;
    boolean allowed =
        switch (Objects.toString(next, "")) {
          case "accepted", "rejected" -> state.equals("requested");
          case "preparing" -> state.equals("accepted");
          case "ready" -> Set.of("accepted", "preparing").contains(state);
          case "cancelled" -> Set.of("requested", "accepted", "preparing", "ready").contains(state);
          case "manual_complete" -> state.equals("ready") && !linked;
          default -> false;
        };
    if (!allowed) throw error(HttpStatus.CONFLICT, "This order can’t make that transition");
    if (Set.of("rejected", "cancelled").contains(next)
        && (a.reason() == null || a.reason().isBlank() || a.reason().length() > 300))
      throw bad("Add a short reason for the customer");
    if ("accepted".equals(next) && linked) issueCode(id);
    String method = "manual_complete".equals(next) ? "provider_recorded" : null;
    String status = "manual_complete".equals(next) ? "completed" : next;
    jdbc.update(
        "UPDATE commerce_order SET"
            + " status=?,revision=revision+1,completion_method=?,reason=?,updated_at=now(),otp_cipher=CASE"
            + " WHEN ? IN ('rejected','cancelled','completed') THEN NULL ELSE otp_cipher END WHERE"
            + " id=?",
        status,
        method,
        Objects.toString(a.reason(), ""),
        status,
        id);
    var saved = order((UUID) row.get("community_id"), id, false);
    event(saved, "provider", status.equals("completed") || status.equals("cancelled"));
    return safe(saved);
  }

  @Transactional
  public Result verify(String slug, UUID p, UUID id, String auth, Action a) {
    studio.authorize(slug, p, auth);
    var o = order(residents.community(slug), id, true);
    if (!p.equals(o.get("provider_id"))) throw error(HttpStatus.NOT_FOUND, "Order not found");
    if (o.get("buyer_id") == null
        || !"ready".equals(o.get("status"))
        || a.revision() != ((Number) o.get("revision")).longValue())
      throw error(
          HttpStatus.CONFLICT, "Refresh the order; a linked consumer’s ready order is required");
    if (o.get("otp_locked_until") != null
        && ((java.sql.Timestamp) o.get("otp_locked_until")).toInstant().isAfter(Instant.now()))
      return new Result(
          429,
          Map.of(
              "error",
              "Too many attempts. Wait 15 minutes or ask the consumer to renew their code"));
    if (o.get("otp_expires_at") == null
        || ((java.sql.Timestamp) o.get("otp_expires_at")).toInstant().isBefore(Instant.now()))
      return new Result(409, Map.of("error", "Ask the consumer to renew their handover code"));
    String expected = crypt(id, Objects.toString(o.get("otp_cipher"), ""), false);
    if (a.code() == null
        || !a.code().matches("[0-9]{6}")
        || !MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8), a.code().getBytes(StandardCharsets.UTF_8))) {
      jdbc.update(
          "UPDATE commerce_order SET otp_attempts=otp_attempts+1,otp_locked_until=CASE WHEN"
              + " otp_attempts+1>=5 THEN now()+interval '15 minutes' ELSE NULL END WHERE id=?",
          id);
      return new Result(
          400,
          Map.of(
              "error",
              "That code doesn’t match. Check with the consumer; five attempts cause a temporary"
                  + " lock"));
    }
    jdbc.update(
        "UPDATE commerce_order SET"
            + " status='completed',completion_method='otp',otp_cipher=NULL,revision=revision+1,updated_at=now()"
            + " WHERE id=?",
        id);
    var saved = order((UUID) o.get("community_id"), id, false);
    event(saved, "provider", true);
    return new Result(200, safe(saved));
  }

  @Transactional
  public Map<String, Object> providerOrder(String slug, UUID p, UUID id, String auth) {
    studio.authorize(slug, p, auth);
    var o = order(residents.community(slug), id, false);
    if (!p.equals(o.get("provider_id"))) throw error(HttpStatus.NOT_FOUND, "Order not found");
    return safe(o);
  }

  @Transactional
  public void readAlert(String slug, UUID p, UUID id, String auth) {
    studio.authorize(slug, p, auth);
    jdbc.update(
        "UPDATE provider_order_notification SET read_at=now() WHERE id=? AND provider_id=?", id, p);
  }

  @Transactional
  public List<Map<String, Object>> mine(String slug, String auth) {
    UUID b = buyer(slug, auth);
    return jdbc
        .queryForList(
            "SELECT * FROM commerce_order WHERE buyer_id=? AND community_id=? ORDER BY created_at"
                + " DESC LIMIT 200",
            b,
            residents.community(slug))
        .stream()
        .map(this::safe)
        .toList();
  }

  @Transactional
  public Map<String, Object> consumer(String slug, UUID id, String auth) {
    UUID b = buyer(slug, auth);
    var o = order(residents.community(slug), id, false);
    if (!b.equals(o.get("buyer_id"))) throw error(HttpStatus.NOT_FOUND, "Order not found");
    return safe(o);
  }

  @Transactional
  public Map<String, Object> code(String slug, UUID id, String auth, boolean renew) {
    UUID b = buyer(slug, auth);
    var o = order(residents.community(slug), id, true);
    if (!b.equals(o.get("buyer_id"))) throw error(HttpStatus.NOT_FOUND, "Order not found");
    if (!Set.of("accepted", "preparing", "ready").contains(o.get("status")))
      throw error(
          HttpStatus.CONFLICT, "A code is available only after acceptance and before completion");
    if (renew) {
      if (o.get("otp_issued_at") != null
          && ((java.sql.Timestamp) o.get("otp_issued_at"))
              .toInstant()
              .isAfter(Instant.now().minusSeconds(60)))
        throw error(HttpStatus.TOO_MANY_REQUESTS, "Wait a minute before renewing the code");
      issueCode(id);
      o = order(residents.community(slug), id, false);
    }
    if (o.get("otp_expires_at") == null
        || ((java.sql.Timestamp) o.get("otp_expires_at")).toInstant().isBefore(Instant.now()))
      throw error(HttpStatus.CONFLICT, "Renew your expired handover code");
    return Map.of("code", crypt(id, o.get("otp_cipher").toString(), false));
  }

  @Transactional
  public Map<String, Object> cancel(String slug, UUID id, String auth) {
    UUID b = buyer(slug, auth);
    var o = order(residents.community(slug), id, true);
    if (!b.equals(o.get("buyer_id"))) throw error(HttpStatus.NOT_FOUND, "Order not found");
    if (!"requested".equals(o.get("status")))
      throw error(
          HttpStatus.CONFLICT,
          "Only unaccepted requests can be cancelled here; contact your provider for an accepted"
              + " order");
    jdbc.update(
        "UPDATE commerce_order SET status='cancelled',reason='Cancelled by"
            + " consumer',revision=revision+1,updated_at=now() WHERE id=?",
        id);
    var saved = order((UUID) o.get("community_id"), id, false);
    event(saved, "consumer", true);
    return safe(saved);
  }

  @Transactional
  public List<Map<String, Object>> alerts(String slug, UUID p, String auth, boolean read) {
    studio.authorize(slug, p, auth);
    if (read)
      jdbc.update(
          "UPDATE provider_order_notification SET read_at=now() WHERE provider_id=? AND read_at IS"
              + " NULL",
          p);
    return jdbc.queryForList(
        "SELECT n.id,n.order_id,n.title,n.body,n.read_at,n.created_at,o.reference FROM"
            + " provider_order_notification n JOIN commerce_order o ON o.id=n.order_id WHERE"
            + " n.provider_id=? ORDER BY n.created_at DESC LIMIT 200",
        p);
  }

  @Scheduled(fixedDelay = 60000)
  @Transactional
  public void expire() {
    var rows =
        jdbc.queryForList(
            "SELECT * FROM commerce_order WHERE status='requested' AND (created_at<now()-interval"
                + " '30 minutes' OR fulfilment_at<now()) FOR UPDATE SKIP LOCKED");
    for (var o : rows) {
      jdbc.update(
          "UPDATE commerce_order SET status='expired',revision=revision+1,reason='Provider did not"
              + " confirm within the response window',updated_at=now() WHERE id=?",
          o.get("id"));
      event(order((UUID) o.get("community_id"), (UUID) o.get("id"), false), "system", true);
    }
  }
}
