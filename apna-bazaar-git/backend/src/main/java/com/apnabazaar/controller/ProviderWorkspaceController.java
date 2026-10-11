package com.apnabazaar.controller;

import com.apnabazaar.service.ProviderWorkspaceService;
import com.apnabazaar.service.ResidentService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/{slug}")
public class ProviderWorkspaceController {
  private final ProviderWorkspaceService service;
  private final ResidentService residents;
  private final JdbcTemplate jdbc;
  private final String key;

  public ProviderWorkspaceController(
      ProviderWorkspaceService service,
      ResidentService residents,
      JdbcTemplate jdbc,
      @Value("${CAMPAIGN_ADMIN_TOKEN:}") String key) {
    this.service = service;
    this.residents = residents;
    this.jdbc = jdbc;
    this.key = key;
  }

  private void operator(String auth) {
    if (key.isBlank())
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Set CAMPAIGN_ADMIN_TOKEN in Railway to issue provider edit links");
    String supplied = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : "";
    if (!MessageDigest.isEqual(
        key.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8)))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Operator key required");
  }

  @GetMapping("/admin/providers")
  public List<Map<String, Object>> providers(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    operator(auth);
    return jdbc.queryForList(
        "SELECT id,name,shop_name AS shop,flat_number AS flat FROM provider WHERE community_id=?"
            + " AND status='active' ORDER BY shop_name",
        residents.community(slug));
  }

  public record Register(String name, String shop, String flat, String whatsapp, String type) {}

  @PostMapping("/admin/providers")
  @Transactional
  public Map<String, Object> register(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody Register r) {
    operator(auth);
    if (r.name() == null
        || r.name().isBlank()
        || r.name().length() > 200
        || r.shop() == null
        || r.shop().isBlank()
        || r.shop().length() > 200
        || r.flat() == null
        || r.flat().isBlank()
        || r.flat().length() > 50
        || r.whatsapp() == null
        || !r.whatsapp().matches("[1-9][0-9]{7,14}")
        || !Set.of("food_seller", "grocery", "service", "tuition", "other")
            .contains(r.type() == null ? "" : r.type()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Check name, shop, flat and WhatsApp number (country code and digits only)");
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO"
            + " provider(id,community_id,name,shop_name,flat_number,whatsapp_number,provider_type,status,is_verified)"
            + " VALUES(?,?,?,?,?,?,?::provider_type_enum,'active',false)",
        id,
        residents.community(slug),
        r.name().trim(),
        r.shop().trim(),
        r.flat().trim(),
        r.whatsapp(),
        r.type());
    return service.issue(slug, id);
  }

  @PostMapping("/admin/providers/{id}/edit-link")
  public Map<String, Object> issue(
      @PathVariable String slug,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    operator(auth);
    return service.issue(slug, id);
  }

  @GetMapping("/provider-workspace/{id}")
  public Map<String, Object> workspace(
      @PathVariable String slug,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    return service.workspace(slug, id, auth);
  }

  @PutMapping("/provider-workspace/{id}/draft")
  public Map<String, Object> save(
      @PathVariable String slug,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody ProviderWorkspaceService.Save save) {
    return service.save(slug, id, auth, save);
  }

  public record Publish(long revision) {}

  @PostMapping("/provider-workspace/{id}/publish")
  public Map<String, Object> publish(
      @PathVariable String slug,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody Publish request) {
    return service.publish(slug, id, auth, request.revision());
  }
}
