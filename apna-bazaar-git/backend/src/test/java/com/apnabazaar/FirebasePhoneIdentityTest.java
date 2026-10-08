package com.apnabazaar;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.apnabazaar.service.FirebasePhoneIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class FirebasePhoneIdentityTest {
  final ObjectMapper json = new ObjectMapper();

  String token(String project, String provider, long expiry) throws Exception {
    var claims =
        Map.of(
            "aud",
            project,
            "iss",
            "https://securetoken.google.com/" + project,
            "sub",
            "test-uid",
            "phone_number",
            "+919740893534",
            "exp",
            expiry,
            "auth_time",
            Instant.now().getEpochSecond(),
            "firebase",
            Map.of("sign_in_provider", provider));
    return "e30."
        + Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsBytes(claims))
        + ".untrusted-signature";
  }

  @Test
  void missingConfigurationNeverPretendsOtpWorked() {
    var service = new FirebasePhoneIdentity(json, "", "japamala-8284d", mock(HttpClient.class));
    assertEquals(
        503,
        assertThrows(ResponseStatusException.class, () -> service.verify("fake"))
            .getStatusCode()
            .value());
  }

  @Test
  void wrongProjectExpiredOrNonPhoneTokensAreRejectedBeforeNetwork() throws Exception {
    var client = mock(HttpClient.class);
    var service = new FirebasePhoneIdentity(json, "public-test-key", "japamala-8284d", client);
    for (String token :
        List.of(
            token("wrong-project", "phone", Instant.now().getEpochSecond() + 100),
            token("japamala-8284d", "password", Instant.now().getEpochSecond() + 100),
            token("japamala-8284d", "phone", Instant.now().getEpochSecond() - 1)))
      assertEquals(
          401,
          assertThrows(ResponseStatusException.class, () -> service.verify(token))
              .getStatusCode()
              .value());
    verifyNoInteractions(client);
  }

  @Test
  @SuppressWarnings("unchecked")
  void forgedPayloadNeedsAuthoritativeFirebaseValidation() throws Exception {
    var client = mock(HttpClient.class);
    var response = (HttpResponse<String>) mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(400);
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(response);
    var service = new FirebasePhoneIdentity(json, "public-test-key", "japamala-8284d", client);
    assertEquals(
        401,
        assertThrows(
                ResponseStatusException.class,
                () ->
                    service.verify(
                        token("japamala-8284d", "phone", Instant.now().getEpochSecond() + 100)))
            .getStatusCode()
            .value());
    verify(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void verifiedUidAndPhoneMustMatchAndOnlyLastFourDigitsAreReturned() throws Exception {
    var client = mock(HttpClient.class);
    var response = (HttpResponse<String>) mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body())
        .thenReturn("{\"users\":[{\"localId\":\"test-uid\",\"phoneNumber\":\"+919740893534\"}]}");
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(response);
    var service = new FirebasePhoneIdentity(json, "public-test-key", "japamala-8284d", client);
    var id = service.verify(token("japamala-8284d", "phone", Instant.now().getEpochSecond() + 100));
    assertEquals("test-uid", id.uid());
    assertEquals("3534", id.last4());
    when(response.body())
        .thenReturn(
            "{\"users\":[{\"localId\":\"another-user\",\"phoneNumber\":\"+919740893534\"}]}");
    assertEquals(
        401,
        assertThrows(
                ResponseStatusException.class,
                () ->
                    service.verify(
                        token("japamala-8284d", "phone", Instant.now().getEpochSecond() + 100)))
            .getStatusCode()
            .value());
  }
}
