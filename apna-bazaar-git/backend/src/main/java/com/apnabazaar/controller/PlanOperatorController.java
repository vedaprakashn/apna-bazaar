package com.apnabazaar.controller;

import com.apnabazaar.service.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/{slug}/admin/residents/plans")
public class PlanOperatorController {
  private final ResidentService residents;
  private final HoodPlanWorkflow workflow;
  private final HoodPlanController plans;
  private final String key;

  public PlanOperatorController(
      ResidentService residents,
      HoodPlanWorkflow workflow,
      HoodPlanController plans,
      @Value("${CAMPAIGN_ADMIN_TOKEN:}") String key) {
    this.residents = residents;
    this.workflow = workflow;
    this.plans = plans;
    this.key = key;
  }

  private void auth(String header) {
    if (key.isBlank())
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Configure CAMPAIGN_ADMIN_TOKEN in Railway");
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
    return plans.list(slug, null, null);
  }

  @PostMapping
  public Map<String, Object> create(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String header,
      @RequestBody HoodPlanController.NewPlan body) {
    auth(header);
    return Map.of("id", plans.create(residents.community(slug), null, body));
  }

  @PostMapping("/{id}/manage")
  public void manage(
      @PathVariable String slug,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String header,
      @RequestBody HoodPlanWorkflow.Manage body) {
    auth(header);
    workflow.manage(residents.community(slug), id, null, body);
  }
}
