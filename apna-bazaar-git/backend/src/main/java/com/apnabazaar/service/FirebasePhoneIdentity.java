package com.apnabazaar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Firebase performs signature validation; a phone number supplied by the client is never trusted.
 */
@Service
public class FirebasePhoneIdentity {
  public record Identity(String uid, String last4) {}

  private final ObjectMapper json;
  private final String key, project;
  private final HttpClient client;

  @org.springframework.beans.factory.annotation.Autowired
  public FirebasePhoneIdentity(
      ObjectMapper json,
      @Value("${FIREBASE_WEB_API_KEY:}") String key,
      @Value("${FIREBASE_PROJECT_ID:japamala-8284d}") String project) {
    this(
        json, key, project, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
  }

  public FirebasePhoneIdentity(ObjectMapper json, String key, String project, HttpClient client) {
    this.json = json;
    this.key = key;
    this.project = project;
    this.client = client;
  }

  public Map<String, Object> config() {
    return Map.of(
        "configured",
        !key.isBlank(),
        "apiKey",
        key,
        "projectId",
        project,
        "authDomain",
        project + ".firebaseapp.com");
  }

  public Identity verify(String token) {
    if (key.isBlank())
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Configure Firebase Phone Authentication and FIREBASE_WEB_API_KEY on the app service");
    if (token == null || token.length() > 6000)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Verify your phone again");
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) throw new IllegalArgumentException();
      var claims = json.readTree(Base64.getUrlDecoder().decode(parts[1]));
      long now = Instant.now().getEpochSecond();
      if (!project.equals(claims.path("aud").asText())
          || !("https://securetoken.google.com/" + project).equals(claims.path("iss").asText())
          || !"phone".equals(claims.path("firebase").path("sign_in_provider").asText())
          || claims.path("exp").asLong() <= now
          || claims.path("auth_time").asLong() < now - 600
          || claims.path("auth_time").asLong() > now + 30) throw new IllegalArgumentException();
      var req =
          HttpRequest.newBuilder(
                  URI.create(
                      "https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=" + key))
              .timeout(Duration.ofSeconds(15))
              .header("Content-Type", "application/json")
              .POST(
                  HttpRequest.BodyPublishers.ofString(
                      json.writeValueAsString(Map.of("idToken", token))))
              .build();
      var response = client.send(req, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 500 || response.statusCode() == 429)
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE, "Phone verification is taking a moment. Retry shortly");
      if (response.statusCode() != 200) throw new IllegalArgumentException();
      var user = json.readTree(response.body()).path("users").path(0);
      String uid = user.path("localId").asText(), phone = user.path("phoneNumber").asText();
      if (user.path("disabled").asBoolean()
          || uid.isBlank()
          || uid.length() > 128
          || !uid.equals(claims.path("sub").asText())
          || !phone.matches("\\+[1-9][0-9]{7,14}")
          || !phone.equals(claims.path("phone_number").asText()))
        throw new IllegalArgumentException();
      return new Identity(uid, phone.substring(phone.length() - 4));
    } catch (ResponseStatusException e) {
      throw e;
    } catch (java.io.IOException e) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Could not verify the phone right now");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Could not verify the phone right now");
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Verify your phone again");
    }
  }
}
