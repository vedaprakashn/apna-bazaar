package com.apnabazaar;
import com.apnabazaar.controller.ProviderReputationController;
import com.apnabazaar.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import java.util.UUID;

class ProviderReputationTest {
 private final ResidentService residents=mock(ResidentService.class);
 private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
 private final MessageModerationService moderation=mock(MessageModerationService.class);
 private final UUID id=UUID.randomUUID();
 private org.springframework.test.web.servlet.MockMvc mvc(String key){return MockMvcBuilders.standaloneSetup(new ProviderReputationController(jdbc,residents,moderation,new ChatRateLimiter(10),key,"")).build();}
 @Test void reviewRequiresActualInteractionAndValidStars() throws Exception {
  when(residents.authorize("tridasa","Bearer test")).thenReturn(UUID.randomUUID());
  for(String body:new String[]{"{\"stars\":5,\"interacted\":false}","{\"stars\":6,\"interacted\":true}","{\"interacted\":true}"})mvc("").perform(put("/api/tridasa/providers/"+id+"/review").header("Authorization","Bearer test").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());verifyNoInteractions(jdbc,moderation);
 }
 @Test void privateFeedbackRequiresResidentCapability() throws Exception {
  when(residents.authorize("tridasa",null)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  mvc("").perform(get("/api/tridasa/providers/"+id+"/reputation/mine")).andExpect(status().isUnauthorized());
  mvc("").perform(post("/api/tridasa/providers/"+id+"/contact")).andExpect(status().isUnauthorized());verifyNoInteractions(jdbc);
 }
 @Test void abusiveOrUncheckedReviewCannotPublish() throws Exception {
  when(residents.authorize("tridasa","Bearer test")).thenReturn(UUID.randomUUID());
  for(var d:new MessageModerationService.Decision[]{MessageModerationService.Decision.BLOCK,MessageModerationService.Decision.UNAVAILABLE}){
   when(moderation.check("text")).thenReturn(d);
   mvc("").perform(put("/api/tridasa/providers/"+id+"/review").header("Authorization","Bearer test").contentType(MediaType.APPLICATION_JSON).content("{\"stars\":1,\"body\":\"text\",\"interacted\":true}")).andExpect(status().is(d==MessageModerationService.Decision.BLOCK?422:503));
  }verifyNoInteractions(jdbc);
 }
 @Test void verificationNeedsThreeChecksAndEvidence() throws Exception {
  mvc("operator-test").perform(post("/api/tridasa/admin/reputation/"+id+"/verify").header("Authorization","Bearer operator-test").contentType(MediaType.APPLICATION_JSON).content("{\"verified\":true,\"identityChecked\":true,\"flatChecked\":true,\"contactChecked\":false,\"evidence\":\"records\"}")).andExpect(status().isBadRequest());verifyNoInteractions(jdbc,residents);
 }
 @Test void operatorWorkspaceCannotBeReadWithoutKey() throws Exception {
  mvc("").perform(get("/api/tridasa/admin/reputation")).andExpect(status().isServiceUnavailable());
  mvc("operator-test").perform(get("/api/tridasa/admin/reputation")).andExpect(status().isUnauthorized());verifyNoInteractions(jdbc,residents);
 }
}
