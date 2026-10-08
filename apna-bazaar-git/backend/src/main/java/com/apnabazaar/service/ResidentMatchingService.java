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
public class ResidentMatchingService {
  private final JdbcTemplate jdbc;
  private final ResidentService residents;

  public UUID member(String slug, String auth) {
    UUID id = residents.authorize(slug, auth);
    if (!Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT verification='verified' FROM resident_profile WHERE id=?", Boolean.class, id)))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Verify your flat in My profile to arrange rides or join live plans");
    return id;
  }

  public void notify(
      UUID community, UUID resident, String key, String title, String body, String path) {
    if (resident == null) return;
    jdbc.update(
        "INSERT INTO resident_notification(id,community_id,resident_id,event_key,title,body,path)"
            + " VALUES(?,?,?,?,?,?,?) ON CONFLICT(resident_id,event_key) DO NOTHING",
        UUID.randomUUID(),
        community,
        resident,
        key,
        title,
        body.length() > 600 ? body.substring(0, 599) + "…" : body,
        path);
  }

  public void promote(UUID plan) {
    var rows =
        jdbc.queryForList(
            "SELECT community_id,title,status,capacity,starts_at>now() AS upcoming FROM hood_plan"
                + " WHERE id=?",
            plan);
    if (rows.isEmpty()
        || !"confirmed".equals(rows.getFirst().get("status"))
        || !Boolean.TRUE.equals(rows.getFirst().get("upcoming"))) return;
    var p = rows.getFirst();
    int limit = p.get("capacity") == null ? 1000 : ((Number) p.get("capacity")).intValue();
    // Called under the plan row lock. Only current verified members can hold a place.
    jdbc.update(
        "UPDATE hood_plan_vote v SET queue_state='waitlisted' WHERE plan_id=? AND choice='in' AND"
            + " NOT EXISTS(SELECT 1 FROM resident_profile r WHERE r.id=v.visitor_id AND"
            + " r.community_id=? AND r.verification='verified')",
        plan,
        p.get("community_id"));
    jdbc.update(
        "UPDATE hood_plan_vote SET queue_state='waitlisted' WHERE plan_id=? AND choice='in' AND"
            + " queue_state<>'attending'",
        plan);
    int taken =
        jdbc.queryForObject(
            "SELECT count(*) FROM hood_plan_vote WHERE plan_id=? AND choice='in' AND"
                + " queue_state='attending'",
            Integer.class,
            plan);
    if (taken >= limit) return;
    var next =
        jdbc.queryForList(
            "SELECT v.visitor_id FROM hood_plan_vote v JOIN resident_profile r ON r.id=v.visitor_id"
                + " WHERE v.plan_id=? AND v.choice='in' AND v.queue_state<>'attending' AND"
                + " r.community_id=? AND r.verification='verified' ORDER BY"
                + " v.joined_at,v.visitor_id LIMIT ?",
            plan,
            p.get("community_id"),
            limit - taken);
    for (var n : next) {
      UUID id = (UUID) n.get("visitor_id");
      jdbc.update(
          "UPDATE hood_plan_vote SET queue_state='attending',reminder_sent_at=NULL WHERE plan_id=?"
              + " AND visitor_id=?",
          plan,
          id);
      notify(
          (UUID) p.get("community_id"),
          id,
          "plan-place:"
              + plan
              + ":"
              + jdbc.queryForObject(
                  "SELECT joined_at::text FROM hood_plan_vote WHERE plan_id=? AND visitor_id=?",
                  String.class,
                  plan,
                  id),
          "Your place is confirmed",
          p.get("title") + " · Check the organiser’s details before you go.",
          "/plans/index.html?id=" + plan);
    }
  }

  @Scheduled(fixedDelay = 60000, initialDelay = 90000)
  @Transactional
  public void expireRideHolds() {
    var rides =
        jdbc.queryForList(
            "SELECT r.id,r.community_id,r.owner_id FROM hood_ride r WHERE EXISTS(SELECT 1 FROM"
                + " hood_ride_agreement a WHERE a.ride_id=r.id AND (a.state='proposed' AND"
                + " a.hold_until<=now() OR a.state IN ('requested','proposed') AND"
                + " a.occurrence_at<=now())) LIMIT 100 FOR UPDATE SKIP LOCKED");
    for (var r : rides) {
      var expired =
          jdbc.queryForList(
              "UPDATE hood_ride_agreement SET state='expired',updated_at=now() WHERE ride_id=? AND"
                  + " (state='proposed' AND hold_until<=now() OR state IN ('requested','proposed')"
                  + " AND occurrence_at<=now()) RETURNING id,resident_id,revision",
              r.get("id"));
      for (var a : expired) {
        String key = "ride-expired:" + a.get("id") + ":" + a.get("revision");
        notify(
            (UUID) r.get("community_id"),
            (UUID) a.get("resident_id"),
            key,
            "Ride request timed out",
            "The confirmation window passed. Seats are available again; send a new request if the"
                + " trip is still upcoming.",
            "/activity/index.html");
        notify(
            (UUID) r.get("community_id"),
            (UUID) r.get("owner_id"),
            key,
            "Ride request timed out",
            "An arrangement wasn’t confirmed in time. Any held seats were released.",
            "/activity/index.html");
      }
    }
  }
}
