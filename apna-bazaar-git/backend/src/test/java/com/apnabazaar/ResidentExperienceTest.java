package com.apnabazaar;
import com.apnabazaar.controller.*;
import com.apnabazaar.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import java.util.UUID;

class ResidentExperienceTest {
 @Test void profileCreationNeedsExplicitConsent() throws Exception {
  var jdbc=mock(JdbcTemplate.class);var moderation=mock(MessageModerationService.class);
  var mvc=MockMvcBuilders.standaloneSetup(new ResidentController(mock(ResidentService.class),jdbc,moderation,new ChatRateLimiter(10),"")).build();
  mvc.perform(post("/api/tridasa/resident/profile").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Neighbour\",\"flatNumber\":\"T1-101\",\"consent\":false}")).andExpect(status().isBadRequest());
  verifyNoInteractions(jdbc,moderation);
 }
 @Test void postingNeedsConfirmationAndModeration() throws Exception {
  var residents=mock(ResidentService.class);var jdbc=mock(JdbcTemplate.class);var moderation=mock(MessageModerationService.class);
  when(residents.authorize("tridasa","Bearer test")).thenReturn(UUID.randomUUID());
  var mvc=MockMvcBuilders.standaloneSetup(new ResidentController(residents,jdbc,moderation,new ChatRateLimiter(10),"")).build();
  String body="{\"title\":\"Help\",\"body\":\"Request\",\"expiresAt\":\""+java.time.Instant.now().plusSeconds(86400)+"\",\"publish\":false}";
  mvc.perform(post("/api/tridasa/resident/requests").header("Authorization","Bearer test").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
  verifyNoInteractions(jdbc,moderation);
  when(moderation.check(anyString())).thenReturn(MessageModerationService.Decision.BLOCK);
  mvc.perform(post("/api/tridasa/resident/requests").header("Authorization","Bearer test").contentType(MediaType.APPLICATION_JSON).content(body.replace("false","true"))).andExpect(status().isUnprocessableEntity());
  verifyNoInteractions(jdbc);
 }
 @Test void privateActivityCannotBeReadWithoutCapability() throws Exception {
  var residents=mock(ResidentService.class);var jdbc=mock(JdbcTemplate.class);
  when(residents.authorize("tridasa",null)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  var mvc=MockMvcBuilders.standaloneSetup(new ResidentController(residents,jdbc,mock(MessageModerationService.class),new ChatRateLimiter(10),"")).build();
  mvc.perform(get("/api/tridasa/resident/activity")).andExpect(status().isUnauthorized());verifyNoInteractions(jdbc);
 }
 @Test void unavailableModerationNeverPublishes() throws Exception {
  var residents=mock(ResidentService.class);var jdbc=mock(JdbcTemplate.class);var moderation=mock(MessageModerationService.class);
  when(residents.authorize(anyString(),anyString())).thenReturn(UUID.randomUUID());when(moderation.check(anyString())).thenReturn(MessageModerationService.Decision.UNAVAILABLE);
  var mvc=MockMvcBuilders.standaloneSetup(new ResidentController(residents,jdbc,moderation,new ChatRateLimiter(10),"")).build();
  mvc.perform(post("/api/tridasa/resident/requests").header("Authorization","Bearer test").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Help\",\"body\":\"Request\",\"expiresAt\":\""+java.time.Instant.now().plusSeconds(86400)+"\",\"publish\":true}")).andExpect(status().isServiceUnavailable());verifyNoInteractions(jdbc);
 }
 @Test void operatorReviewRequiresConfiguredCredential() throws Exception {
  var jdbc=mock(JdbcTemplate.class);var residents=mock(ResidentService.class);
  var locked=MockMvcBuilders.standaloneSetup(new ResidentOperatorController(jdbc,residents,"")).build();locked.perform(get("/api/tridasa/admin/residents")).andExpect(status().isServiceUnavailable());
  var configured=MockMvcBuilders.standaloneSetup(new ResidentOperatorController(jdbc,residents,"test-operator-key")).build();configured.perform(get("/api/tridasa/admin/residents")).andExpect(status().isUnauthorized());
  verifyNoInteractions(jdbc,residents);
 }
 @Test void followUpHistoryIsBoundedAndScreened() throws Exception {
  var searches=mock(SearchService.class);var moderation=mock(MessageModerationService.class);
  var mvc=MockMvcBuilders.standaloneSetup(new SearchController(searches,new ChatRateLimiter(10),moderation,"")).build();
  mvc.perform(post("/api/tridasa/search").contentType(MediaType.APPLICATION_JSON).content("{\"q\":\"Two seats\",\"history\":[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\"]}")).andExpect(status().isBadRequest());
  verifyNoInteractions(searches,moderation);
  when(moderation.check(anyString())).thenReturn(MessageModerationService.Decision.BLOCK);
  mvc.perform(post("/api/tridasa/search").contentType(MediaType.APPLICATION_JSON).content("{\"q\":\"Two seats\",\"history\":[\"bad old message\"]}")).andExpect(status().isUnprocessableEntity());verifyNoInteractions(searches);
 }
}
