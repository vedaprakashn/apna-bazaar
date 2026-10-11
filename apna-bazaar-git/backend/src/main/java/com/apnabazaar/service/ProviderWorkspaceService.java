package com.apnabazaar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProviderWorkspaceService {
  private final JdbcTemplate jdbc;
  private final ResidentService residents;
  private final ObjectMapper mapper;
  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

  public record Item(
      UUID id,
      UUID categoryId,
      String name,
      String description,
      String type,
      BigDecimal price,
      String unit,
      boolean enabled,
      LocalTime starts,
      LocalTime ends,
      List<String> days,
      Map<LocalDate, Boolean> dates) {}

  public record Draft(List<Item> items) {}

  public record Save(long revision, Draft draft) {}

  private ResponseStatusException bad(String msg) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
  }

  private Map<String, Object> access(String slug, UUID id, String auth) {
    if (auth == null || !auth.startsWith("Bearer ") || auth.length() > 200)
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Open your private provider link to edit this shop");
    var rows =
        jdbc.queryForList(
            "SELECT w.* FROM provider_workspace w JOIN provider p ON p.id=w.provider_id WHERE"
                + " p.id=? AND p.community_id=? AND p.status='active' AND w.token_hash=? AND"
                + " w.expires_at>now() FOR UPDATE OF w",
            id,
            residents.community(slug),
            residents.hash(auth.substring(7)));
    if (rows.isEmpty())
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "This edit link has expired or was replaced. Ask your operator for a new link");
    return rows.getFirst();
  }

  public List<Map<String, Object>> categories() {
    return jdbc.queryForList(
        "SELECT id,name,icon_emoji AS emoji,domain::text FROM category ORDER BY sort_order,name");
  }

  @Transactional
  public Map<String, Object> issue(String slug, UUID id) {
    var rows =
        jdbc.queryForList(
            "SELECT id FROM provider WHERE id=? AND community_id=? AND status='active'",
            id,
            residents.community(slug));
    if (rows.isEmpty())
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Choose an active provider in this community");
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    jdbc.update(
        "INSERT INTO provider_workspace(provider_id,token_hash,expires_at)"
            + " VALUES(?,?,now()+interval '90 days') ON CONFLICT(provider_id) DO UPDATE SET"
            + " token_hash=excluded.token_hash,expires_at=excluded.expires_at",
        id,
        residents.hash(token));
    return Map.of("token", token, "providerId", id, "validDays", 90);
  }

  private Draft current(UUID id) {
    var items = new ArrayList<Item>();
    for (var row :
        jdbc.queryForList(
            "SELECT * FROM offering WHERE provider_id=? AND NOT archived ORDER BY sort_order,name",
            id)) {
      UUID item = (UUID) row.get("id");
      var schedules =
          jdbc.queryForList(
              "SELECT serves_from,serves_to,days_of_week,day_scope::text FROM offering_schedule"
                  + " WHERE offering_id=? AND is_active ORDER BY serves_from LIMIT 1",
              item);
      var schedule = schedules.isEmpty() ? Map.<String, Object>of() : schedules.getFirst();
      var dates = new TreeMap<LocalDate, Boolean>();
      for (var d :
          jdbc.queryForList(
              "SELECT service_date,available FROM offering_day_availability WHERE offering_id=? AND"
                  + " service_date>=? ORDER BY service_date",
              item,
              LocalDate.now(IST)))
        dates.put(
            ((java.sql.Date) d.get("service_date")).toLocalDate(), (Boolean) d.get("available"));
      String days = (String) schedule.get("days_of_week");
      items.add(
          new Item(
              item,
              (UUID) row.get("category_id"),
              (String) row.get("name"),
              Objects.toString(row.get("description"), ""),
              row.get("offering_type").toString(),
              (BigDecimal) row.get("base_price"),
              (String) row.get("unit"),
              Boolean.TRUE.equals(row.get("is_available")),
              schedule.get("serves_from") == null
                  ? LocalTime.of(9, 0)
                  : ((java.sql.Time) schedule.get("serves_from")).toLocalTime(),
              schedule.get("serves_to") == null
                  ? LocalTime.of(18, 0)
                  : ((java.sql.Time) schedule.get("serves_to")).toLocalTime(),
              days == null
                  ? switch (Objects.toString(schedule.get("day_scope"), "daily")) {
                    case "weekdays" -> List.of("MON", "TUE", "WED", "THU", "FRI");
                    case "weekends" -> List.of("SAT", "SUN");
                    case "saturday_only" -> List.of("SAT");
                    case "sunday_only" -> List.of("SUN");
                    default -> List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");
                  }
                  : List.of(days.split(",")),
              dates));
    }
    return new Draft(items);
  }

  private Draft decode(Object value) {
    try {
      return mapper.readValue(value.toString(), Draft.class);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to read shop draft", e);
    }
  }

  private String encode(Draft draft) {
    try {
      return mapper.writeValueAsString(draft);
    } catch (Exception e) {
      throw bad("Unable to save draft");
    }
  }

  @Transactional
  public Map<String, Object> workspace(String slug, UUID id, String auth) {
    var w = access(slug, id, auth);
    var result = new LinkedHashMap<String, Object>();
    result.put(
        "provider",
        jdbc.queryForMap(
            "SELECT id,name,shop_name AS shop,flat_number AS flat,whatsapp_number AS whatsapp FROM"
                + " provider WHERE id=?",
            id));
    result.put("draft", w.get("draft") == null ? current(id) : decode(w.get("draft")));
    result.put("revision", w.get("revision"));
    result.put("publishedRevision", w.get("published_revision"));
    result.put("publishedAt", w.get("published_at"));
    result.put("categories", categories());
    return result;
  }

  private void validate(UUID provider, Draft draft) {
    if (draft == null || draft.items() == null || draft.items().size() > 100)
      throw bad("Add up to 100 items");
    var ids = new HashSet<UUID>();
    var cats = new HashSet<>(jdbc.queryForList("SELECT id FROM category", UUID.class));
    LocalDate today = LocalDate.now(IST);
    for (Item i : draft.items()) {
      if (i == null || i.id() == null || !ids.add(i.id()))
        throw bad("Every item needs a unique ID");
      if (!cats.contains(i.categoryId())) throw bad("Choose an existing category");
      if (i.name() == null
          || i.name().isBlank()
          || i.name().length() > 200
          || i.description() == null
          || i.description().length() > 2000
          || i.unit() == null
          || i.unit().isBlank()
          || i.unit().length() > 50) throw bad("Check item name, description and price unit");
      if (i.price() == null
          || i.price().signum() < 0
          || i.price().compareTo(new BigDecimal("999999.99")) > 0
          || i.price().scale() > 2)
        throw bad("Use a price between ₹0 and ₹999999.99, with up to two decimals");
      if (!Set.of("food_item", "grocery_item", "service_package", "class", "other")
          .contains(i.type() == null ? "" : i.type())) throw bad("Choose an item type");
      if (i.starts() == null || i.ends() == null || !i.ends().isAfter(i.starts()))
        throw bad("Choose a serving end time after its start time (same day)");
      if (i.days() == null
          || i.days().isEmpty()
          || i.days().size() > 7
          || new HashSet<>(i.days()).size() != i.days().size()
          || !Set.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").containsAll(i.days()))
        throw bad("Choose at least one serving day");
      if (i.dates() == null
          || i.dates().size() > 90
          || i.dates().entrySet().stream()
              .anyMatch(
                  d ->
                      d.getKey() == null
                          || d.getValue() == null
                          || d.getKey().isBefore(today.minusDays(90))
                          || d.getKey().isAfter(today.plusDays(90))))
        throw bad("Date overrides must be within 90 days");
      var owners =
          jdbc.queryForList("SELECT provider_id FROM offering WHERE id=?", UUID.class, i.id());
      if (!owners.isEmpty() && !owners.getFirst().equals(provider))
        throw bad("That item belongs to a different provider");
    }
  }

  @Transactional
  public Map<String, Object> save(String slug, UUID id, String auth, Save save) {
    var w = access(slug, id, auth);
    if (save == null || save.revision() != ((Number) w.get("revision")).longValue())
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A newer draft exists. Reload before editing");
    validate(id, save.draft());
    jdbc.update(
        "UPDATE provider_workspace SET draft=?::jsonb,revision=revision+1,updated_at=now() WHERE"
            + " provider_id=?",
        encode(save.draft()),
        id);
    return workspace(slug, id, auth);
  }

  @Transactional
  public Map<String, Object> publish(String slug, UUID id, String auth, long revision) {
    var w = access(slug, id, auth);
    if (revision != ((Number) w.get("revision")).longValue())
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A newer draft exists. Reload before publishing");
    if (w.get("draft") == null) throw bad("Save your draft before publishing");
    Draft draft = decode(w.get("draft"));
    validate(id, draft);
    jdbc.update("UPDATE offering SET archived=true WHERE provider_id=?", id);
    int index = 0;
    for (Item i : draft.items()) {
      int written =
          jdbc.update(
              "INSERT INTO"
                  + " offering(id,provider_id,category_id,name,description,offering_type,base_price,unit,is_available,sort_order,archived)"
                  + " VALUES(?,?,?,?,?,?::offering_type_enum,?,?,?,?,false) ON CONFLICT(id) DO"
                  + " UPDATE SET"
                  + " category_id=excluded.category_id,name=excluded.name,description=excluded.description,offering_type=excluded.offering_type,base_price=excluded.base_price,unit=excluded.unit,is_available=excluded.is_available,sort_order=excluded.sort_order,live_status='unconfirmed',availability_updated_at=NULL,archived=false"
                  + " WHERE offering.provider_id=excluded.provider_id",
              i.id(),
              id,
              i.categoryId(),
              i.name().trim(),
              i.description().trim(),
              i.type(),
              i.price(),
              i.unit().trim(),
              i.enabled(),
              index++);
      if (written != 1) throw bad("That item belongs to a different provider");
      jdbc.update("DELETE FROM offering_schedule WHERE offering_id=?", i.id());
      jdbc.update(
          "INSERT INTO"
              + " offering_schedule(offering_id,day_scope,days_of_week,serves_from,serves_to,accepts_preorder,accepts_realtime)"
              + " VALUES(?,'specific_days',?,?,?,false,true)",
          i.id(),
          String.join(",", i.days()),
          i.starts(),
          i.ends());
      jdbc.update("DELETE FROM offering_day_availability WHERE offering_id=?", i.id());
      for (var d : i.dates().entrySet())
        jdbc.update(
            "INSERT INTO offering_day_availability(offering_id,service_date,available)"
                + " VALUES(?,?,?)",
            i.id(),
            d.getKey(),
            d.getValue());
    }
    jdbc.update(
        "UPDATE provider_workspace SET published_revision=revision,published_at=now() WHERE"
            + " provider_id=?",
        id);
    return workspace(slug, id, auth);
  }
}
