package com.apnabazaar.controller;

import com.apnabazaar.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/{slug}/plans")
public class HoodPlanController {
  private final JdbcTemplate jdbc;
  private final ResidentService residents;
  private final ResidentMatchingService matching;
  private final HoodPlanWorkflow workflow;
  private final MessageModerationService moderation;
  private final ChatRateLimiter limiter;
  private final boolean proxy;

  public HoodPlanController(
      JdbcTemplate jdbc,
      ResidentService residents,
      ResidentMatchingService matching,
      HoodPlanWorkflow workflow,
      MessageModerationService moderation,
      ChatRateLimiter limiter,
      @org.springframework.beans.factory.annotation.Value("${RAILWAY_PROJECT_ID:}")
          String project) {
    this.jdbc = jdbc;
    this.residents = residents;
    this.matching = matching;
    this.workflow = workflow;
    this.moderation = moderation;
    this.limiter = limiter;
    this.proxy = !project.isBlank();
  }

  private void rate(HttpServletRequest req, UUID id) {
    String ip = req.getRemoteAddr();
    if (proxy && req.getHeader("X-Forwarded-For") != null) {
      String[] parts = req.getHeader("X-Forwarded-For").split(",");
      String last = parts[parts.length - 1].trim();
      if (last.matches("[0-9a-fA-F:.]+")) ip = last;
    }
    if (!limiter.acquire("plan-action:" + (id == null ? ip : id)).allowed())
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Try again in a minute");
  }

  private UUID viewer(String slug, UUID visitor, String auth) {
    if (auth != null) return residents.authorize(slug, auth);
    if (visitor != null
        && Boolean.TRUE.equals(
            jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM resident_profile WHERE id=?)",
                Boolean.class,
                visitor))) return null;
    return visitor;
  }

  @GetMapping
  public List<Map<String, Object>> list(
      @PathVariable String slug,
      @RequestParam(required = false) UUID visitorId,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    UUID v = viewer(slug, visitorId, auth);
    return jdbc.queryForList(
        """
SELECT p.*,p.starts_at<=now() AS ended,
 (SELECT count(*) FROM hood_plan_vote a WHERE a.plan_id=p.id AND a.choice='in') AS interested,
 (SELECT count(*) FROM hood_plan_vote a WHERE a.plan_id=p.id AND a.choice='in' AND a.queue_state='attending') AS attending,
 (SELECT count(*) FROM hood_plan_vote a WHERE a.plan_id=p.id AND a.choice='in' AND a.queue_state='waitlisted') AS waitlisted,
 v.choice AS my_vote,v.queue_state AS my_place,COALESCE(v.reminder_enabled,false) AS reminder_enabled,p.organiser_id=? AS mine
FROM hood_plan p LEFT JOIN hood_plan_vote v ON v.plan_id=p.id AND v.visitor_id=?
WHERE p.community_id=? ORDER BY p.starts_at,p.title
""",
        v,
        v,
        residents.community(slug));
  }

  public record Vote(UUID visitorId, String choice) {}

  @PostMapping("/{id}/vote")
  public List<Map<String, Object>> vote(
      @PathVariable String slug,
      @PathVariable UUID id,
      @RequestBody Vote body,
      @RequestHeader(value = "Authorization", required = false) String auth,
      HttpServletRequest request) {
    if (body.visitorId() == null
        || !Set.of("in", "pass", "clear").contains(body.choice() == null ? "" : body.choice()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a response");
    var rows =
        jdbc.queryForList(
            "SELECT is_demo FROM hood_plan WHERE id=? AND community_id=?",
            id,
            residents.community(slug));
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found");
    UUID visitor = body.visitorId();
    if (!Boolean.TRUE.equals(rows.getFirst().get("is_demo"))) {
      UUID authorized =
          body.choice().equals("clear")
              ? residents.authorize(slug, auth)
              : matching.member(slug, auth);
      if (!visitor.equals(authorized))
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Use your own resident profile");
    } else if (Boolean.TRUE.equals(
            jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM resident_profile WHERE id=?)", Boolean.class, visitor))
        && !visitor.equals(residents.authorize(slug, auth)))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Use your own resident profile");
    rate(request, auth == null ? null : visitor);
    workflow.vote(residents.community(slug), id, visitor, body.choice());
    return list(slug, visitor, auth);
  }

  public record NewPlan(
      String title,
      String description,
      String category,
      String location,
      Instant startsAt,
      Integer minimum,
      Integer capacity,
      boolean publish) {}

  public UUID create(UUID community, UUID organiser, NewPlan p) {
    if (!p.publish()
        || p.title() == null
        || p.title().isBlank()
        || p.title().length() > 180
        || p.description() == null
        || p.description().isBlank()
        || p.description().length() > 1000
        || p.category() == null
        || p.category().isBlank()
        || p.category().length() > 60
        || p.location() == null
        || p.location().isBlank()
        || p.location().length() > 160
        || p.minimum() == null
        || p.minimum() < 2
        || p.minimum() > 1000
        || p.capacity() != null && (p.capacity() < p.minimum() || p.capacity() > 1000)
        || p.startsAt() == null
        || !p.startsAt().isAfter(Instant.now())
        || p.startsAt().isAfter(Instant.now().plusSeconds(90 * 86400)))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Check the plan details, future time and sign-up target/capacity");
    var d =
        moderation.check(
            p.title() + " " + p.description() + " " + p.category() + " " + p.location());
    if (d != MessageModerationService.Decision.ALLOW)
      throw new ResponseStatusException(
          d == MessageModerationService.Decision.BLOCK
              ? HttpStatus.UNPROCESSABLE_ENTITY
              : HttpStatus.SERVICE_UNAVAILABLE,
          "Could not publish those plan details");
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO"
            + " hood_plan(id,community_id,title,description,category,starts_at,location,minimum_interested,capacity,organiser_id,is_demo)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,false)",
        id,
        community,
        p.title().trim(),
        p.description().trim(),
        p.category().trim(),
        java.sql.Timestamp.from(p.startsAt()),
        p.location().trim(),
        p.minimum(),
        p.capacity(),
        organiser);
    return id;
  }

  @PostMapping
  public Map<String, Object> post(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody NewPlan body,
      HttpServletRequest request) {
    UUID id = matching.member(slug, auth);
    rate(request, id);
    return Map.of("id", create(residents.community(slug), id, body));
  }

  @PostMapping("/{id}/manage")
  public void manage(
      @PathVariable String slug,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody HoodPlanWorkflow.Manage body,
      HttpServletRequest request) {
    UUID member =
        body.action() != null && body.action().equals("cancel")
            ? residents.authorize(slug, auth)
            : matching.member(slug, auth);
    rate(request, member);
    workflow.manage(residents.community(slug), id, member, body);
  }

  public record Reminder(boolean enabled) {}

  @PostMapping("/{id}/reminder")
  public void remind(
      @PathVariable String slug,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody Reminder body,
      HttpServletRequest request) {
    UUID member = residents.authorize(slug, auth);
    rate(request, member);
    if (jdbc.update(
            "UPDATE hood_plan_vote v SET reminder_enabled=? FROM hood_plan p WHERE p.id=v.plan_id"
                + " AND p.id=? AND p.community_id=? AND v.visitor_id=? AND v.choice='in' AND"
                + " p.status<>'cancelled' AND p.starts_at>now()",
            body.enabled(),
            id,
            residents.community(slug),
            member)
        == 0)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Join an upcoming plan before setting a reminder");
  }
}
