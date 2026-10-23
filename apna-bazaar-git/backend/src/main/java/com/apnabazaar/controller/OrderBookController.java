package com.apnabazaar.controller;

import com.apnabazaar.service.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/{slug}")
public class OrderBookController {
  private final OrderBookService orders;
  private final ChatRateLimiter limiter;
  private final ResidentService residents;
  private final ChatRateLimiter providerLimiter;

  public OrderBookController(
      OrderBookService orders,
      ChatRateLimiter limiter,
      ResidentService residents,
      @Value("${ORDER_PROVIDER_WRITES_PER_MINUTE:60}") int providerLimit) {
    this.orders = orders;
    this.limiter = limiter;
    this.residents = residents;
    this.providerLimiter =
        new ChatRateLimiter(
            providerLimit,
            () -> java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime()));
  }

  private void rate(String auth) {
    if (!limiter.acquire("orders:" + residents.hash(Objects.toString(auth, "anonymous"))).allowed())
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "A little breather—try again in a minute");
  }

  private void providerRate(String auth) {
    if (!providerLimiter
        .acquire("provider-orders:" + residents.hash(Objects.toString(auth, "anonymous")))
        .allowed())
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "A little breather—try again in a minute");
  }

  @PostMapping("/orders")
  public Object create(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody OrderBookService.Request r) {
    rate(auth);
    return orders.create(slug, auth, r, false, null);
  }

  @GetMapping("/orders/mine")
  public Object mine(
      @PathVariable String slug,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    return orders.mine(slug, auth);
  }

  @GetMapping("/orders/{id}")
  public Object get(
      @PathVariable String slug,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    return orders.consumer(slug, id, auth);
  }

  @GetMapping("/orders/{id}/code")
  public Object code(
      @PathVariable String slug,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    return orders.code(slug, id, auth, false);
  }

  @PostMapping("/orders/{id}/code")
  public Object renew(
      @PathVariable String slug,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    rate(auth);
    return orders.code(slug, id, auth, true);
  }

  @PostMapping("/orders/{id}/cancel")
  public Object cancel(
      @PathVariable String slug,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    rate(auth);
    return orders.cancel(slug, id, auth);
  }

  @GetMapping("/provider-workspace/{p}/orders")
  public Object list(
      @PathVariable String slug,
      @PathVariable UUID p,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestParam(defaultValue = "open") String range,
      @RequestParam(required = false) String date) {
    return Map.of(
        "orders",
        orders.providerOrders(slug, p, auth, range, date),
        "codesConfigured",
        orders.codesConfigured());
  }

  @PostMapping("/provider-workspace/{p}/orders")
  public Object manual(
      @PathVariable String slug,
      @PathVariable UUID p,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody OrderBookService.Request r) {
    providerRate(auth);
    return orders.create(slug, auth, r, true, p);
  }

  @GetMapping("/provider-workspace/{p}/orders/{id}")
  public Object single(
      @PathVariable String slug,
      @PathVariable UUID p,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    return orders.providerOrder(slug, p, id, auth);
  }

  @PostMapping("/provider-workspace/{p}/notifications/{id}/read")
  public void readOne(
      @PathVariable String slug,
      @PathVariable UUID p,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    orders.readAlert(slug, p, id, auth);
  }

  @PostMapping("/provider-workspace/{p}/orders/{id}")
  public ResponseEntity<?> action(
      @PathVariable String slug,
      @PathVariable UUID p,
      @PathVariable UUID id,
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody OrderBookService.Action a) {
    providerRate(auth);
    if (a == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an order action");
    if ("verify".equals(a.action())) {
      var result = orders.verify(slug, p, id, auth, a);
      return ResponseEntity.status(result.status()).body(result.data());
    }
    return ResponseEntity.ok(orders.providerAction(slug, p, id, auth, a));
  }

  @GetMapping("/provider-workspace/{p}/notifications")
  public Object alerts(
      @PathVariable String slug,
      @PathVariable UUID p,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    return orders.alerts(slug, p, auth, false);
  }

  @PostMapping("/provider-workspace/{p}/notifications/read")
  public Object read(
      @PathVariable String slug,
      @PathVariable UUID p,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    return orders.alerts(slug, p, auth, true);
  }
}
