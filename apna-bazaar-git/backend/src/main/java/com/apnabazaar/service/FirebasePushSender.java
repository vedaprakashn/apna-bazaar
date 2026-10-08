package com.apnabazaar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Service
public class FirebasePushSender {
    public record Result(String status, boolean invalidToken) {}
    private final ObjectMapper json;
    private final String project;
    private final GoogleCredentials credentials;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    public FirebasePushSender(ObjectMapper json,
            @Value("${FIREBASE_PROJECT_ID:japamala-8284d}") String project,
            @Value("${FIREBASE_SERVICE_ACCOUNT_JSON:}") String serviceAccount) {
        this.json = json; this.project = project;
        GoogleCredentials parsed = null;
        if (!serviceAccount.isBlank()) {
            try {
                var data = json.readTree(serviceAccount);
                if ("service_account".equals(data.path("type").asText())
                        && project.matches("[a-z][a-z0-9-]{4,62}")
                        && project.equals(data.path("project_id").asText())
                        && "https://oauth2.googleapis.com/token".equals(data.path("token_uri").asText())) {
                    parsed = GoogleCredentials.fromStream(new ByteArrayInputStream(serviceAccount.getBytes(StandardCharsets.UTF_8)))
                            .createScoped("https://www.googleapis.com/auth/firebase.messaging");
                }
            } catch (Exception ignored) { /* Never log private credential contents. */ }
        }
        credentials = parsed;
    }
    public boolean configured() { return credentials != null; }
    public String project() { return project; }
    public synchronized Result send(String token, String title, String body, Map<String,String> data, boolean dryRun) {
        if (!configured()) return new Result("failed", false);
        boolean attempted = false;
        try {
            credentials.refreshIfExpired();
            var message = Map.of("token", token, "notification", Map.of("title",title,"body",body), "data", data,
                    "android", Map.of("priority","NORMAL","ttl","3600s","notification",
                            Map.of("channel_id","hood_updates","tag",data.get("deliveryId"),"icon","notification_icon")));
            var request = HttpRequest.newBuilder(URI.create("https://fcm.googleapis.com/v1/projects/"+project+"/messages:send"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization","Bearer "+credentials.getAccessToken().getTokenValue())
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("message",message,"validate_only",dryRun))))
                    .build();
            attempted = true;
            var response = client.send(request,HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) return new Result("accepted",false);
            boolean invalid = false;
            if(response.statusCode()==404) {
                var details=json.readTree(response.body()).path("error").path("details");
                for(var detail:details) if("UNREGISTERED".equals(detail.path("errorCode").asText())) invalid=true;
            }
            return new Result("failed",invalid);
        } catch (Exception ignored) {
            // A timeout may occur after FCM accepted a request; do not automatically retry it.
            return new Result(attempted ? "unknown" : "failed",false);
        }
    }
}
