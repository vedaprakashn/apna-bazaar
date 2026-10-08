package com.apnabazaar.controller;

import com.apnabazaar.service.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/{slug}/rides")
public class RideAgreementController {
  private final JdbcTemplate jdbc;
  private final ResidentService residents;
  private final ResidentMatchingService matching;
  private final MessageModerationService moderation;
  private final ChatRateLimiter limiter;

  public RideAgreementController(
      JdbcTemplate jdbc,
      ResidentService residents,
      ResidentMatchingService matching,
      MessageModerationService moderation,
      ChatRateLimiter limiter) {
    this.jdbc = jdbc;
    this.residents = residents;
    this.matching = matching;
    this.moderation = moderation;
    this.limiter = limiter;
  }

  private void rate(UUID id) {
    if (!limiter.acquire("agreement:" + id).allowed())
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Try again in a minute");
  }

  private ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  private Map<String, Object> ride(String slug, UUID id) {
    var rows =
        jdbc.queryForList(
            "SELECT * FROM hood_ride WHERE id=? AND community_id=? FOR UPDATE",
            id,
            residents.community(slug));
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found");
    return rows.getFirst();
  }

  private void live(Map<String, Object> r) {
    if (Boolean.TRUE.equals(r.get("is_demo"))
        || r.get("owner_id") == null
        || !Boolean.TRUE.equals(
            jdbc.queryForObject(
                "SELECT verification='verified' FROM resident_profile WHERE id=?",
                Boolean.class,
                r.get("owner_id"))))
      throw conflict("This is not a verified resident ride. Demo trips cannot be arranged");
    if (!"open".equals(r.get("status"))) throw conflict("This ride post is closed");
  }

  private void occurrence(Map<String, Object> r, Instant at) {
    if (at == null || !at.isAfter(Instant.now())) throw conflict("Choose an upcoming departure");
    Instant start = ((Timestamp) r.get("departure_at")).toInstant();
    if (r.get("recurrence_until") == null) {
      if (!start.equals(at)) throw conflict("The departure has changed. Refresh this ride");
      return;
    }
    var local = at.atZone(ZoneId.of("Asia/Kolkata"));
    var first = start.atZone(ZoneId.of("Asia/Kolkata"));
    try {
      var weekdays = (Integer[]) ((java.sql.Array) r.get("weekdays")).getArray();
      var end = ((java.sql.Date) r.get("recurrence_until")).toLocalDate();
      if (local.toLocalDate().isBefore(first.toLocalDate())
          || local.toLocalDate().isAfter(end)
          || !local.toLocalTime().equals(first.toLocalTime())
          || !Arrays.asList(weekdays).contains(local.getDayOfWeek().getValue()))
        throw conflict("Choose one of the listed recurring departures");
    } catch (java.sql.SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  private int held(UUID ride, Instant at, UUID excluding) {
    return jdbc.queryForObject(
        "SELECT COALESCE(sum(seats),0)::int FROM hood_ride_agreement WHERE ride_id=? AND"
            + " occurrence_at=? AND id<>? AND (state='confirmed' OR state='proposed' AND"
            + " hold_until>now())",
        Integer.class,
        ride,
        Timestamp.from(at),
        excluding);
  }

  private void expire(UUID ride) {
    jdbc.update(
        "UPDATE hood_ride_agreement SET state='expired',updated_at=now() WHERE ride_id=? AND"
            + " (state='proposed' AND hold_until<=now() OR state IN ('requested','proposed') AND"
            + " occurrence_at<=now())",
        ride);
  }

  public record Request(Instant departureAt, Integer seats) {}

  @PostMapping("/{id}/agreements")
  @Transactional
  public Map<String, Object> request(
      @PathVariable String slug,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody Request body) {
    UUID member = matching.member(slug, auth);
    rate(member);
    var r = ride(slug, id);
    live(r);
    occurrence(r, body.departureAt());
    expire(id);
    if (member.equals(r.get("owner_id"))) throw conflict("This is your own ride");
    int seats = body.seats() == null ? 1 : body.seats();
    if (seats < 1
        || seats > 6
        || seats > ((Number) r.get("seats")).intValue()
        || "request".equals(r.get("kind")) && seats != ((Number) r.get("seats")).intValue())
      throw conflict("Check the number of seats for this arrangement");
    UUID agreement = UUID.randomUUID();
    int changed =
        jdbc.update(
            """
INSERT INTO hood_ride_agreement(id,ride_id,resident_id,occurrence_at,seats) VALUES(?,?,?,?,?)
ON CONFLICT(ride_id,resident_id,occurrence_at) DO UPDATE SET state='requested',seats=excluded.seats,terms='',hold_until=NULL,revision=hood_ride_agreement.revision+1,updated_at=now()
WHERE hood_ride_agreement.state IN ('cancelled','declined','expired')
""",
            agreement,
            id,
            member,
            Timestamp.from(body.departureAt()),
            seats);
    var row =
        jdbc.queryForMap(
            "SELECT id,state,revision FROM hood_ride_agreement WHERE ride_id=? AND resident_id=?"
                + " AND occurrence_at=?",
            id,
            member,
            Timestamp.from(body.departureAt()));
    if (changed > 0)
      matching.notify(
          (UUID) r.get("community_id"),
          (UUID) r.get("owner_id"),
          "ride-request:" + row.get("id") + ":" + row.get("revision"),
          "Someone’s heading your way",
          r.get("destination") + " · Review the request in My stuff.",
          "/activity/index.html");
    return row;
  }

  @GetMapping("/agreements/mine")
  public List<Map<String, Object>> mine(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    UUID id = residents.authorize(slug, auth);
    return jdbc.queryForList(
        """
SELECT a.id,a.ride_id,a.occurrence_at,a.seats,a.terms,a.revision,a.hold_until,
 CASE WHEN a.state='confirmed' AND a.occurrence_at<=now() THEN 'past' WHEN a.state='proposed' AND a.hold_until<=now() OR a.state IN ('requested','proposed') AND a.occurrence_at<=now() THEN 'expired' ELSE a.state END AS state,
 r.destination,r.pickup,r.arrangement,r.exchange_terms,r.kind,r.owner_id=? AS organiser,
 CASE WHEN other.share_name THEN other.name ELSE 'A neighbour' END AS neighbour,
 CASE WHEN other.share_flat THEN other.flat_number ELSE NULL END AS flat_number
FROM hood_ride_agreement a JOIN hood_ride r ON r.id=a.ride_id
JOIN resident_profile other ON other.id=CASE WHEN r.owner_id=? THEN a.resident_id ELSE r.owner_id END
WHERE r.community_id=? AND (a.resident_id=? OR r.owner_id=?) ORDER BY a.updated_at DESC LIMIT 100
""",
        id,
        id,
        residents.community(slug),
        id,
        id);
  }

  public record Action(String action, Integer revision, String terms) {}

  @PostMapping("/agreements/{agreement}/action")
  @Transactional
  public void action(
      @PathVariable String slug,
      @PathVariable UUID agreement,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody Action body) {
    UUID member = residents.authorize(slug, auth);
    rate(member);
    var link =
        jdbc.queryForList(
            "SELECT a.ride_id FROM hood_ride_agreement a JOIN hood_ride r ON r.id=a.ride_id WHERE"
                + " a.id=? AND r.community_id=?",
            agreement,
            residents.community(slug));
    if (link.isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arrangement not found");
    UUID rideId = (UUID) link.getFirst().get("ride_id");
    var r = ride(slug, rideId);
    expire(rideId);
    var a = jdbc.queryForMap("SELECT * FROM hood_ride_agreement WHERE id=? FOR UPDATE", agreement);
    boolean owner = member.equals(r.get("owner_id")),
        participant = member.equals(a.get("resident_id"));
    if (!owner && !participant)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This arrangement is private");
    String action = body.action() == null ? "" : body.action(), state = (String) a.get("state");
    if (!Set.of("propose", "confirm", "decline", "cancel").contains(action))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a ride action");
    if (Set.of("declined", "cancelled", "expired").contains(state)) {
      if (action.equals("cancel") && state.equals("cancelled")) return;
      throw conflict("That arrangement is already closed");
    }
    String next;
    if ("cancel".equals(action)) {
      if (state.equals("confirmed")
          && !((Timestamp) a.get("occurrence_at")).toInstant().isAfter(Instant.now()))
        throw conflict("That ride time has already passed");
      next = "cancelled";
    } else {
      matching.member(slug, auth);
      live(r);
      occurrence(r, ((Timestamp) a.get("occurrence_at")).toInstant());
      if (body.revision() == null || body.revision() != ((Number) a.get("revision")).intValue())
        throw conflict("The arrangement changed. Refresh it before agreeing");
      if ("decline".equals(action)) {
        if (!owner || !Set.of("requested", "proposed").contains(state))
          throw conflict("This request cannot be declined now");
        next = "declined";
      } else if ("propose".equals(action)) {
        if (!owner || !"requested".equals(state))
          throw conflict("Only the ride organiser can propose this request");
        if (body.terms() == null || body.terms().isBlank() || body.terms().length() > 500)
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Add the pickup, sharing and handover details (up to 500 characters)");
        var screened = moderation.check(body.terms());
        if (screened != MessageModerationService.Decision.ALLOW)
          throw new ResponseStatusException(
              screened == MessageModerationService.Decision.BLOCK
                  ? HttpStatus.UNPROCESSABLE_ENTITY
                  : HttpStatus.SERVICE_UNAVAILABLE,
              "Could not publish those arrangement details");
        Instant at = ((Timestamp) a.get("occurrence_at")).toInstant();
        int reserved = held(rideId, at, agreement);
        if (reserved + ((Number) a.get("seats")).intValue() > ((Number) r.get("seats")).intValue())
          throw conflict("Those seats are already held or confirmed");
        Instant until = Instant.now().plusSeconds(7200);
        if (until.isAfter(at)) until = at;
        jdbc.update(
            "UPDATE hood_ride_agreement SET terms=?,hold_until=?,revision=revision+1 WHERE id=?",
            body.terms().trim(),
            Timestamp.from(until),
            agreement);
        next = "proposed";
      } else {
        if (!participant
            || !"proposed".equals(state)
            || a.get("hold_until") == null
            || !((Timestamp) a.get("hold_until")).toInstant().isAfter(Instant.now()))
          throw conflict("Only the joining neighbour can confirm an active proposal");
        if (!Boolean.TRUE.equals(
            jdbc.queryForObject(
                "SELECT verification='verified' FROM resident_profile WHERE id=?",
                Boolean.class,
                r.get("owner_id")))) throw conflict("The organiser’s membership needs review");
        next = "confirmed";
      }
    }
    jdbc.update(
        "UPDATE hood_ride_agreement SET state=?,updated_at=now() WHERE id=?", next, agreement);
    UUID recipient = owner ? (UUID) a.get("resident_id") : (UUID) r.get("owner_id");
    int revision =
        jdbc.queryForObject(
            "SELECT revision FROM hood_ride_agreement WHERE id=?", Integer.class, agreement);
    matching.notify(
        (UUID) r.get("community_id"),
        recipient,
        "ride:" + agreement + ":" + next + ":" + revision,
        "Ride arrangement · " + next,
        r.get("destination") + " · Open My stuff for timing and details.",
        "/activity/index.html");
  }
}
