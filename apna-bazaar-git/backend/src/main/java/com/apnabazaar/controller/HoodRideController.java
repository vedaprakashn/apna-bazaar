package com.apnabazaar.controller;

import com.apnabazaar.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/{slug}/rides")
public class HoodRideController {
  private final ResidentService residents;
  private final JdbcTemplate jdbc;
  private final HoodRideService rides;
  private final MessageModerationService moderation;
  private final ChatRateLimiter limiter;
  private final boolean proxy;

  public HoodRideController(
      JdbcTemplate jdbc,
      HoodRideService rides,
      MessageModerationService moderation,
      ChatRateLimiter limiter,
      @Value("${RAILWAY_PROJECT_ID:}") String project,
      ResidentService residents) {
    this.residents = residents;
    this.jdbc = jdbc;
    this.rides = rides;
    this.moderation = moderation;
    this.limiter = limiter;
    proxy = !project.isBlank();
  }

  private UUID community(String slug) {
    var rows = jdbc.queryForList("SELECT id FROM community WHERE slug=?", slug);
    return rows.isEmpty() ? null : (UUID) rows.getFirst().get("id");
  }

  private boolean choices(String kind, String direction) {
    return Set.of("offer", "request").contains(kind)
        && Set.of("outbound", "inbound").contains(direction);
  }

  @GetMapping
  public ResponseEntity<?> list(
      @PathVariable String slug,
      @RequestParam(defaultValue = "offer") String kind,
      @RequestParam(defaultValue = "outbound") String direction,
      @RequestParam(defaultValue = "") String destination,
      @RequestParam(required = false) Instant at,
      @RequestParam(defaultValue = "1") int seats,
      @RequestParam(required = false) UUID visitorId,
      @RequestParam(defaultValue = "") String arrangement,
      @RequestParam(required = false) Instant until) {
    var c = community(slug);
    if (c == null) return ResponseEntity.notFound().build();
    if (!choices(kind, direction) || destination.length() > 120 || seats < 1 || seats > 6)
      return ResponseEntity.badRequest().build();
    if (!Set.of("", "lift", "shared_cab", "school_run").contains(arrangement)
        || until != null
            && (at == null || until.isBefore(at) || until.isAfter(at.plusSeconds(30 * 86400))))
      return ResponseEntity.badRequest().build();
    return ResponseEntity.ok(
        rides.find(c, kind, direction, destination, at, seats, visitorId, arrangement, until));
  }

  public record Post(
      UUID id,
      UUID visitorId,
      String kind,
      String direction,
      String destination,
      Instant departureAt,
      Integer seats,
      String pickup,
      String name,
      String flatNumber,
      String notes,
      boolean publish,
      String arrangement,
      LocalDate recurrenceUntil,
      List<Integer> weekdays,
      String exchangeTerms) {
    public Post {
      arrangement = arrangement == null ? "lift" : arrangement;
      exchangeTerms = exchangeTerms == null ? "" : exchangeTerms;
      flatNumber = flatNumber == null ? "" : flatNumber;
      notes = notes == null ? "" : notes;
    }
  }

  private ResponseEntity<?> rate(HttpServletRequest r) {
    String ip = r.getRemoteAddr();
    if (proxy && r.getHeader("X-Forwarded-For") != null) {
      var hops = r.getHeader("X-Forwarded-For").split(",");
      var last = hops[hops.length - 1].trim();
      if (last.matches("[0-9a-fA-F:.]+")) ip = last;
    }
    var d = limiter.acquire("rides:" + ip);
    return d.allowed()
        ? null
        : ResponseEntity.status(429)
            .header("Retry-After", Long.toString(d.retryAfterSeconds()))
            .body(Map.of("error", "Give it a moment before trying again."));
  }

  private boolean text(String s, int max, boolean required) {
    return s != null && s.length() <= max && (!required || !s.isBlank());
  }

  @PostMapping
  public ResponseEntity<?> post(
      @PathVariable String slug,
      @RequestBody Post p,
      HttpServletRequest request,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    var limited = rate(request);
    if (limited != null) return limited;
    var c = community(slug);
    if (c == null) return ResponseEntity.notFound().build();
    if (!Set.of("lift", "shared_cab", "school_run").contains(p.arrangement())
        || p.exchangeTerms().length() > 240
        || p.id() == null
        || p.visitorId() == null
        || !p.publish()
        || p.kind() == null
        || p.direction() == null
        || !choices(p.kind(), p.direction())
        || !text(p.destination(), 120, true)
        || !text(p.pickup(), 120, true)
        || !text(p.name(), 80, true)
        || !text(p.flatNumber(), 30, false)
        || !text(p.notes(), 240, false)
        || p.seats() == null
        || p.seats() < 1
        || p.seats() > 6
        || p.departureAt() == null
        || !p.departureAt().isAfter(Instant.now())
        || p.departureAt().isAfter(Instant.now().plusSeconds(30 * 86400)))
      return ResponseEntity.badRequest()
          .body(
              Map.of(
                  "error",
                  "Add a destination, a future time within 30 days and 1–6 seats. Confirm"
                      + " publishing to your community."));
    checkOwner(slug, p.visitorId(), auth);
    if (!"verified".equals(residents.profile(p.visitorId()).get("verification")))
      return ResponseEntity.status(403)
          .body(Map.of("error", "Verify your flat in My profile before posting a ride."));
    LocalDate start = p.departureAt().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
    if ((p.recurrenceUntil() == null) != (p.weekdays() == null)
        || p.recurrenceUntil() != null
            && (p.recurrenceUntil().isBefore(start)
                || p.recurrenceUntil().isAfter(start.plusDays(30))
                || p.weekdays().isEmpty()
                || p.weekdays().size() > 7
                || p.weekdays().stream().anyMatch(d -> d == null || d < 1 || d > 7)))
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Choose repeat weekdays and an end date within 30 days."));
    var decision =
        moderation.check(
            p.exchangeTerms()
                + " "
                + p.name()
                + " "
                + p.flatNumber()
                + " "
                + p.destination()
                + " "
                + p.pickup()
                + " "
                + p.notes());
    if (decision == MessageModerationService.Decision.BLOCK)
      return ResponseEntity.unprocessableEntity()
          .body(Map.of("error", MessageModerationService.BLOCK_REPLY));
    if (decision != MessageModerationService.Decision.ALLOW)
      return ResponseEntity.status(503)
          .body(Map.of("error", "Can’t check that post right now. Please retry."));
    int added =
        jdbc.update(
            connection -> {
              var statement =
                  connection.prepareStatement(
                      """
INSERT INTO hood_ride(id,community_id,owner_id,kind,direction,destination,departure_at,seats,pickup,name,flat_number,notes,whatsapp_number,arrangement,recurrence_until,weekdays,exchange_terms,is_demo)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?,'919740893534',?,?,?,?,false) ON CONFLICT(id) DO NOTHING
""");
              Object[] values = {
                p.id(),
                c,
                p.visitorId(),
                p.kind(),
                p.direction(),
                p.destination().trim(),
                java.sql.Timestamp.from(p.departureAt()),
                p.seats(),
                p.pickup().trim(),
                p.name().trim(),
                p.flatNumber().trim(),
                p.notes().trim(),
                p.arrangement(),
                p.recurrenceUntil(),
                p.weekdays() == null
                    ? null
                    : connection.createArrayOf("integer", p.weekdays().toArray()),
                p.exchangeTerms().trim()
              };
              for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
              return statement;
            });
    if (added == 0) {
      var own =
          jdbc.queryForList(
              "SELECT id FROM hood_ride WHERE id=? AND owner_id=? AND community_id=?",
              p.id(),
              p.visitorId(),
              c);
      if (own.isEmpty())
        return ResponseEntity.status(409).body(Map.of("error", "Please create a fresh post."));
    }
    return ResponseEntity.ok(
        Map.of(
            "id",
            p.id(),
            "message",
            "Posted to Hood Rides. This is interest only, not a confirmed ride."));
  }

  private void checkOwner(String slug, UUID owner, String auth) {
    if (owner == null || !owner.equals(residents.authorize(slug, auth)))
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "Use your own resident profile");
  }

  public record Close(UUID visitorId) {}

  @PostMapping("/{id}/close")
  @Transactional
  public ResponseEntity<?> close(
      @PathVariable String slug,
      @PathVariable UUID id,
      @RequestBody Close close,
      HttpServletRequest request,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    checkOwner(slug, close.visitorId(), auth);
    var limited = rate(request);
    if (limited != null) return limited;
    var c = community(slug);
    if (c == null || close.visitorId() == null) return ResponseEntity.notFound().build();
    int updated =
        jdbc.update(
            "UPDATE hood_ride SET status='closed' WHERE id=? AND community_id=? AND owner_id=?",
            id,
            c,
            close.visitorId());
    if (updated == 0) return ResponseEntity.notFound().build();
    var cancelled =
        jdbc.queryForList(
            "UPDATE hood_ride_agreement SET state='cancelled',updated_at=now() WHERE ride_id=? AND"
                + " state IN ('requested','proposed','confirmed') AND occurrence_at>now() RETURNING"
                + " id,resident_id,revision",
            id);
    for (var a : cancelled)
      jdbc.update(
          "INSERT INTO resident_notification(id,community_id,resident_id,event_key,title,body,path)"
              + " VALUES(?,?,?,?,?,?,?) ON CONFLICT(resident_id,event_key) DO NOTHING",
          UUID.randomUUID(),
          c,
          a.get("resident_id"),
          "ride:" + a.get("id") + ":cancelled:" + a.get("revision"),
          "Ride post cancelled",
          "The organiser closed this ride. Check My stuff before travelling.",
          "/activity/index.html");
    return ResponseEntity.ok().build();
  }
}
