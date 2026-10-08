package com.apnabazaar.controller;

import com.apnabazaar.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ResidentTrustController {
  private final JdbcTemplate jdbc;
  private final ResidentService residents;
  private final FirebasePhoneIdentity phone;
  private final ChatRateLimiter limiter;
  private final boolean proxy;

  public ResidentTrustController(
      JdbcTemplate jdbc,
      ResidentService residents,
      FirebasePhoneIdentity phone,
      ChatRateLimiter limiter,
      @org.springframework.beans.factory.annotation.Value("${RAILWAY_PROJECT_ID:}")
          String project) {
    this.jdbc = jdbc;
    this.residents = residents;
    this.phone = phone;
    this.limiter = limiter;
    this.proxy = !project.isBlank();
  }

  private void rate(String key) {
    if (!limiter.acquire("trust:" + key).allowed())
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Wait a minute before trying again");
  }

  private String ip(HttpServletRequest request) {
    String ip = request.getRemoteAddr();
    if (proxy && request.getHeader("X-Forwarded-For") != null) {
      var parts = request.getHeader("X-Forwarded-For").split(",");
      String last = parts[parts.length - 1].trim();
      if (last.matches("[0-9a-fA-F:.]+")) ip = last;
    }
    return ip;
  }

  @GetMapping("/api/resident/auth/config")
  public Map<String, Object> config() {
    return phone.config();
  }

  public record Code(String code) {}

  @PostMapping("/api/{slug}/resident/verify-flat")
  @Transactional
  public Map<String, Object> redeem(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody Code code,
      HttpServletRequest request) {
    rate(ip(request));
    UUID id = residents.authorize(slug, auth);
    rate(id.toString());
    String input = code.code() == null ? "" : code.code().trim().toUpperCase(Locale.ROOT);
    if (!input.matches("[A-Z2-9]{6}"))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Enter your private six-character flat code");
    var invitations =
        jdbc.queryForList(
            "SELECT * FROM resident_flat_invitation WHERE community_id=? AND code_hash=? AND"
                + " revoked_at IS NULL AND consumed_at IS NULL AND expires_at>now() FOR UPDATE",
            residents.community(slug),
            residents.hash(slug + ":" + input));
    if (invitations.isEmpty())
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "That code is unavailable. Ask your RWA for a fresh private code");
    var inv = invitations.getFirst();
    var p = jdbc.queryForMap("SELECT flat_number FROM resident_profile WHERE id=? FOR UPDATE", id);
    if (!inv.get("flat_number").toString().equalsIgnoreCase(p.get("flat_number").toString().trim()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "That code is unavailable. Ask your RWA for a fresh private code");
    jdbc.update(
        "UPDATE resident_flat_invitation SET consumed_by=?,consumed_at=now() WHERE id=?",
        id,
        inv.get("id"));
    jdbc.update(
        "UPDATE resident_profile SET"
            + " verification='verified',flat_verified_at=now(),invitation_id=?,reviewed_at=now()"
            + " WHERE id=?",
        inv.get("id"),
        id);
    return residents.profile(id);
  }

  public record Phone(String idToken) {}

  @PostMapping("/api/{slug}/resident/auth/phone")
  @Transactional
  public Map<String, Object> login(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody Phone body,
      HttpServletRequest request) {
    rate(ip(request));
    UUID c = residents.community(slug);
    var identity = phone.verify(body.idToken());
    // Serialise first-link and restore operations for this Firebase identity across app instances.
    jdbc.queryForList(
        "SELECT pg_advisory_xact_lock(hashtextextended(?,0))", slug + ":" + identity.uid());
    var existing =
        jdbc.queryForList(
            "SELECT id FROM resident_profile WHERE community_id=? AND firebase_uid=? FOR UPDATE",
            UUID.class,
            c,
            identity.uid());
    UUID id;
    if (!existing.isEmpty()) id = existing.getFirst();
    else {
      id = residents.authorize(slug, auth);
      var profile =
          jdbc.queryForMap(
              "SELECT verification,firebase_uid,flat_verified_at FROM resident_profile WHERE id=?"
                  + " FOR UPDATE",
              id);
      if (!"verified".equals(profile.get("verification"))
          || profile.get("flat_verified_at") == null)
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Verify your flat with its private invitation code before linking a phone");
      if (profile.get("firebase_uid") != null
          && !identity.uid().equals(profile.get("firebase_uid")))
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "A different phone account is already linked. Ask your community operator for help");
    }
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String token = HexFormat.of().formatHex(bytes);
    jdbc.update(
        "UPDATE resident_profile SET"
            + " firebase_uid=?,phone_last4=?,phone_verified_at=now(),token_hash=? WHERE id=?",
        identity.uid(),
        identity.last4(),
        residents.hash(token),
        id);
    return Map.of("profile", residents.profile(id), "token", token);
  }

  @PostMapping("/api/{slug}/resident/notifications/{id}/read")
  public void read(
      @PathVariable String slug,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    UUID resident = residents.authorize(slug, auth);
    jdbc.update(
        "UPDATE resident_notification SET read_at=COALESCE(read_at,now()) WHERE id=? AND"
            + " resident_id=? AND community_id=?",
        id,
        resident,
        residents.community(slug));
  }

  @GetMapping("/api/{slug}/resident/notifications")
  public List<Map<String, Object>> notifications(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    UUID id = residents.authorize(slug, auth);
    return jdbc.queryForList(
        "SELECT id,title,body,path,created_at,read_at FROM resident_notification WHERE"
            + " resident_id=? AND community_id=? ORDER BY created_at DESC LIMIT 50",
        id,
        residents.community(slug));
  }

  @PostMapping("/api/{slug}/resident/auth/logout")
  public void logout(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    UUID id = residents.authorize(slug, auth);
    jdbc.update(
        "UPDATE resident_profile SET token_hash=? WHERE id=?",
        residents.hash(UUID.randomUUID() + ":" + UUID.randomUUID()),
        id);
  }
}
