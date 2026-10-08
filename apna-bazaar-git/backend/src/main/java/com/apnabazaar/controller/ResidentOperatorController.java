package com.apnabazaar.controller;

import com.apnabazaar.service.ResidentService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/{slug}/admin/residents")
public class ResidentOperatorController {
  private final JdbcTemplate jdbc;
  private final ResidentService residents;
  private final String key;

  public ResidentOperatorController(
      JdbcTemplate jdbc, ResidentService residents, @Value("${CAMPAIGN_ADMIN_TOKEN:}") String key) {
    this.jdbc = jdbc;
    this.residents = residents;
    this.key = key;
  }

  private void auth(String header) {
    if (key.isBlank())
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Configure CAMPAIGN_ADMIN_TOKEN in Railway for operator approval and availability"
              + " updates");
    String supplied = header != null && header.startsWith("Bearer ") ? header.substring(7) : "";
    if (!MessageDigest.isEqual(
        key.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8)))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Operator key required");
  }

  @GetMapping
  public List<Map<String, Object>> list(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String header) {
    auth(header);
    return jdbc.queryForList(
        "SELECT id,name,flat_number,verification,created_at FROM resident_profile WHERE"
            + " community_id=? ORDER BY created_at DESC",
        residents.community(slug));
  }

  public record Review(String status) {}

  @PostMapping("/{id}/review")
  public void review(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String header,
      @PathVariable UUID id,
      @RequestBody Review review) {
    auth(header);
    if (!Set.of("verified", "rejected", "pending")
        .contains(review.status() == null ? "" : review.status()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid review status");
    if (jdbc.update(
            "UPDATE resident_profile SET verification=?,reviewed_at=NOW() WHERE id=? AND"
                + " community_id=?",
            review.status(),
            id,
            residents.community(slug))
        == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resident not found");
  }

  @GetMapping("/availability")
  public List<Map<String, Object>> offerings(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String header) {
    auth(header);
    return jdbc.queryForList(
        "SELECT o.id,o.name,o.live_status,o.availability_updated_at,p.shop_name FROM offering o"
            + " JOIN provider p ON p.id=o.provider_id WHERE p.community_id=? AND p.status='active'"
            + " ORDER BY p.shop_name,o.name",
        residents.community(slug));
  }

  @PostMapping("/availability/{id}")
  public void availability(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String header,
      @PathVariable UUID id,
      @RequestBody Review review) {
    auth(header);
    if (!Set.of("available", "sold_out", "preorder", "unconfirmed")
        .contains(review.status() == null ? "" : review.status()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid availability");
    if (jdbc.update(
            "UPDATE offering o SET live_status=?,availability_updated_at=NOW() FROM provider p"
                + " WHERE o.provider_id=p.id AND o.id=? AND p.community_id=?",
            review.status(),
            id,
            residents.community(slug))
        == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Offering not found");
  }

  public record Invitations(List<String> flats, Integer validDays, boolean privateDelivery) {}

  @PostMapping("/invitations")
  @Transactional
  public List<Map<String, Object>> invitations(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String header,
      @RequestBody Invitations batch) {
    auth(header);
    int days = batch.validDays() == null ? 14 : batch.validDays();
    if (!batch.privateDelivery()
        || batch.flats() == null
        || batch.flats().isEmpty()
        || batch.flats().size() > 100
        || days < 1
        || days > 30
        || batch.flats().stream()
            .anyMatch(f -> f == null || f.isBlank() || f.length() > 30 || f.contains("\n")))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Add up to 100 flats and confirm private delivery; expiry is 1–30 days");
    UUID c = residents.community(slug);
    var random = new SecureRandom();
    String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    var result = new ArrayList<Map<String, Object>>();
    var seen = new HashSet<String>();
    for (String raw : batch.flats()) {
      String flat = raw.trim();
      if (!seen.add(flat.toLowerCase(Locale.ROOT))) continue;
      jdbc.queryForList(
          "SELECT pg_advisory_xact_lock(hashtextextended(?,0))",
          slug + ":flat:" + flat.toLowerCase(Locale.ROOT));
      jdbc.update(
          "UPDATE resident_flat_invitation SET revoked_at=now() WHERE community_id=? AND"
              + " lower(flat_number)=lower(?) AND consumed_at IS NULL AND revoked_at IS NULL",
          c,
          flat);
      String code;
      do {
        var b = new StringBuilder();
        for (int i = 0; i < 6; i++) b.append(alphabet.charAt(random.nextInt(alphabet.length())));
        code = b.toString();
      } while (Boolean.TRUE.equals(
          jdbc.queryForObject(
              "SELECT EXISTS(SELECT 1 FROM resident_flat_invitation WHERE code_hash=?)",
              Boolean.class,
              residents.hash(slug + ":" + code))));
      UUID id = UUID.randomUUID();
      Instant expiry = Instant.now().plusSeconds(days * 86400L);
      jdbc.update(
          "INSERT INTO resident_flat_invitation(id,community_id,flat_number,code_hash,expires_at)"
              + " VALUES(?,?,?,?,?)",
          id,
          c,
          flat,
          residents.hash(slug + ":" + code),
          java.sql.Timestamp.from(expiry));
      result.add(Map.of("id", id, "flat", flat, "code", code, "expiresAt", expiry));
    }
    return result;
  }

  @GetMapping("/invitations")
  public List<Map<String, Object>> invitationList(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String header) {
    auth(header);
    return jdbc.queryForList(
        "SELECT id,flat_number,expires_at,consumed_at,revoked_at FROM resident_flat_invitation"
            + " WHERE community_id=? ORDER BY created_at DESC LIMIT 200",
        residents.community(slug));
  }

  @PostMapping("/invitations/{id}/revoke")
  public void revoke(
      @PathVariable String slug,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String header) {
    auth(header);
    jdbc.update(
        "UPDATE resident_flat_invitation SET revoked_at=now() WHERE id=? AND community_id=?",
        id,
        residents.community(slug));
  }
}
