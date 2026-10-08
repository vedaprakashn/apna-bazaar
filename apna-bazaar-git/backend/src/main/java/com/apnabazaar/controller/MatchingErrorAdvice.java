package com.apnabazaar.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(
    assignableTypes = {
      RideAgreementController.class,
      ResidentTrustController.class,
      HoodPlanController.class,
      PlanOperatorController.class,
      ResidentOperatorController.class,
      HoodRideController.class
    })
public class MatchingErrorAdvice {
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<?> error(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(
            Map.of(
                "error", e.getReason() == null ? "Could not complete that action" : e.getReason()));
  }
}
