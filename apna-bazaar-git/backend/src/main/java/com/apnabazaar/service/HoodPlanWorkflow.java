package com.apnabazaar.service;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class HoodPlanWorkflow {
  private final JdbcTemplate jdbc;
  private final ResidentMatchingService matching;
  private final MessageModerationService moderation;

  private Map<String, Object> locked(UUID community, UUID id) {
    var rows =
        jdbc.queryForList(
            "SELECT *,starts_at>now() AS upcoming FROM hood_plan WHERE id=? AND community_id=? FOR"
                + " UPDATE",
            id,
            community);
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found");
    return rows.getFirst();
  }

  public record Manage(String action, String note) {}

  @Transactional
  public void manage(UUID community, UUID id, UUID actor, Manage body) {
    var p = locked(community, id);
    if (actor != null && !actor.equals(p.get("organiser_id")))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Only the organiser can manage this plan");
    if (Boolean.TRUE.equals(p.get("is_demo")))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Demo plans stay fictional. Create a real plan first");
    if (!Boolean.TRUE.equals(p.get("upcoming")))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "This plan has already started");
    String action = body.action() == null ? "" : body.action();
    if (!Set.of("confirm", "cancel").contains(action))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose confirm or cancel");
    if (body.note() == null || body.note().isBlank() || body.note().length() > 500)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Add the organiser’s details or cancellation reason (up to 500 characters)");
    var screened = moderation.check(body.note());
    if (screened != MessageModerationService.Decision.ALLOW)
      throw new ResponseStatusException(
          screened == MessageModerationService.Decision.BLOCK
              ? HttpStatus.UNPROCESSABLE_ENTITY
              : HttpStatus.SERVICE_UNAVAILABLE,
          "Could not publish those organiser details");
    if (action.equals("confirm")) {
      if (!"gathering".equals(p.get("status")))
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "This plan is no longer collecting interest");
      int count =
          jdbc.queryForObject(
              "SELECT count(*) FROM hood_plan_vote v JOIN resident_profile r ON r.id=v.visitor_id"
                  + " WHERE v.plan_id=? AND v.choice='in' AND r.community_id=? AND"
                  + " r.verification='verified'",
              Integer.class,
              id,
              community);
      if (count < ((Number) p.get("minimum_interested")).intValue())
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "The verified resident sign-up target has not been reached yet");
      jdbc.update(
          "UPDATE hood_plan SET"
              + " status='confirmed',confirmed_at=now(),confirmation_note=?,updated_at=now() WHERE"
              + " id=?",
          body.note().trim(),
          id);
      matching.promote(id);
    } else {
      if ("cancelled".equals(p.get("status"))) return;
      jdbc.update(
          "UPDATE hood_plan SET status='cancelled',confirmation_note=?,updated_at=now() WHERE id=?",
          body.note().trim(),
          id);
    }
    var people =
        jdbc.queryForList(
            "SELECT v.visitor_id,v.queue_state FROM hood_plan_vote v JOIN resident_profile r ON"
                + " r.id=v.visitor_id WHERE v.plan_id=? AND v.choice='in' AND r.community_id=?",
            id,
            community);
    for (var person : people) {
      if (action.equals("confirm") && person.get("queue_state").equals("attending")) continue;
      matching.notify(
          community,
          (UUID) person.get("visitor_id"),
          "plan:" + id + ":" + action,
          action.equals("cancel")
              ? "Plan cancelled"
              : person.get("queue_state").equals("attending")
                  ? "The plan is on"
                  : "You’re on the waitlist",
          p.get("title") + " · " + body.note().trim(),
          "/plans/index.html?id=" + id);
    }
  }

  @Transactional
  public void vote(UUID community, UUID id, UUID visitor, String choice) {
    var p = locked(community, id);
    if (choice.equals("clear")) {
      jdbc.update("DELETE FROM hood_plan_vote WHERE plan_id=? AND visitor_id=?", id, visitor);
      matching.promote(id);
      return;
    }
    if (!Boolean.TRUE.equals(p.get("upcoming")) || "cancelled".equals(p.get("status")))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "This plan is no longer taking sign-ups");
    jdbc.update(
        """
INSERT INTO hood_plan_vote(plan_id,visitor_id,choice) VALUES(?,?,?)
ON CONFLICT(plan_id,visitor_id) DO UPDATE SET choice=excluded.choice,updated_at=now(),
 joined_at=CASE WHEN hood_plan_vote.choice<>excluded.choice THEN now() ELSE hood_plan_vote.joined_at END,
 queue_state=CASE WHEN hood_plan_vote.choice<>excluded.choice THEN 'interested' ELSE hood_plan_vote.queue_state END,
 reminder_enabled=CASE WHEN excluded.choice='pass' THEN false ELSE hood_plan_vote.reminder_enabled END,
 reminder_sent_at=CASE WHEN hood_plan_vote.choice<>excluded.choice THEN NULL ELSE hood_plan_vote.reminder_sent_at END
""",
        id,
        visitor,
        choice);
    if ("confirmed".equals(p.get("status"))) matching.promote(id);
  }

  @Scheduled(fixedDelay = 60000, initialDelay = 90000)
  @Transactional
  public void reminders() {
    // One durable in-app reminder per opt-in; never for cancelled plans, demos or waitlisted
    // neighbours.
    var due =
        jdbc.queryForList(
            "SELECT id FROM hood_plan WHERE NOT is_demo AND status='confirmed' AND starts_at>now()"
                + " AND starts_at<=now()+interval '2 hours' ORDER BY starts_at LIMIT 100 FOR UPDATE"
                + " SKIP LOCKED");
    for (var plan : due) {
      var rows =
          jdbc.queryForList(
              """
UPDATE hood_plan_vote v SET reminder_sent_at=now() FROM hood_plan p,resident_profile r
WHERE v.plan_id=p.id AND r.id=v.visitor_id AND r.community_id=p.community_id AND r.verification='verified'
 AND p.id=? AND NOT p.is_demo AND p.status='confirmed' AND v.choice='in' AND v.queue_state='attending'
 AND v.reminder_enabled AND v.reminder_sent_at IS NULL AND p.starts_at>now() AND p.starts_at<=now()+interval '2 hours'
RETURNING v.visitor_id,p.id,p.community_id,p.title,p.starts_at
""",
              plan.get("id"));
      for (var p : rows)
        matching.notify(
            (UUID) p.get("community_id"),
            (UUID) p.get("visitor_id"),
            "plan-reminder:" + p.get("id") + ":" + p.get("starts_at"),
            "Your hood plan is coming up",
            p.get("title") + " starts within two hours. Check the time and location.",
            "/plans/index.html?id=" + p.get("id"));
    }
  }
}
