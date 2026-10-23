package com.apnabazaar.controller;

import com.apnabazaar.service.*;
import java.time.LocalTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/{slug}/provider-workspace/{p}/profile")
public class ProviderProfileController {
  private final JdbcTemplate jdbc;
  private final ProviderWorkspaceService studio;

  public ProviderProfileController(JdbcTemplate jdbc, ProviderWorkspaceService studio) {
    this.jdbc = jdbc;
    this.studio = studio;
  }

  @GetMapping
  @Transactional
  public Map<String, Object> get(
      @PathVariable String slug,
      @PathVariable UUID p,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    studio.authorize(slug, p, auth);
    return jdbc.queryForMap(
        "SELECT shop_name AS shop,name,flat_number AS flat,public_bio AS"
            + " bio,coalesce(serves_from,'09:00'::time) AS starts,coalesce(serves_to,'18:00'::time)"
            + " AS ends,delivery_enabled AS delivery FROM provider WHERE id=?",
        p);
  }

  public record Profile(
      String shop,
      String name,
      String flat,
      String bio,
      LocalTime starts,
      LocalTime ends,
      boolean delivery) {}

  @PutMapping
  @Transactional
  public Map<String, Object> put(
      @PathVariable String slug,
      @PathVariable UUID p,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody Profile profile) {
    studio.authorize(slug, p, auth);
    if (profile.shop() == null
        || profile.shop().isBlank()
        || profile.shop().length() > 100
        || profile.name() == null
        || profile.name().isBlank()
        || profile.name().length() > 100
        || profile.flat() == null
        || profile.flat().isBlank()
        || profile.flat().length() > 40
        || profile.bio() == null
        || profile.bio().length() > 500
        || profile.starts() == null
        || profile.ends() == null
        || !profile.ends().isAfter(profile.starts()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Check shop details and opening hours");
    jdbc.update(
        "UPDATE provider SET is_verified=CASE WHEN name<>? OR flat_number IS DISTINCT FROM ? THEN"
            + " false ELSE is_verified END,verification_checked_at=CASE WHEN name<>? OR flat_number"
            + " IS DISTINCT FROM ? THEN NULL ELSE verification_checked_at"
            + " END,shop_name=?,name=?,flat_number=?,public_bio=?,serves_from=?,serves_to=?,delivery_enabled=?,updated_at=now()"
            + " WHERE id=?",
        profile.name().trim(),
        profile.flat().trim(),
        profile.name().trim(),
        profile.flat().trim(),
        profile.shop().trim(),
        profile.name().trim(),
        profile.flat().trim(),
        profile.bio().trim(),
        profile.starts(),
        profile.ends(),
        profile.delivery(),
        p);
    return get(slug, p, auth);
  }
}
