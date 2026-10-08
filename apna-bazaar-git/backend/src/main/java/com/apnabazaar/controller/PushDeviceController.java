package com.apnabazaar.controller;

import com.apnabazaar.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
public class PushDeviceController {
    private final JdbcTemplate jdbc; private final ResidentService residents;
    private final ChatRateLimiter limiter; private final FirebasePushSender sender; private final boolean proxy;
    public PushDeviceController(JdbcTemplate jdbc, ResidentService residents, ChatRateLimiter limiter,
            FirebasePushSender sender, @Value("${RAILWAY_PROJECT_ID:}") String project) {
        this.jdbc=jdbc;this.residents=residents;this.limiter=limiter;this.sender=sender;proxy=!project.isBlank();
    }
    private void rate(HttpServletRequest request) {
        String ip=request.getRemoteAddr(), forwarded=request.getHeader("X-Forwarded-For");
        if(proxy && forwarded!=null) { var parts=forwarded.split(",");ip=parts[parts.length-1].trim(); }
        if(!limiter.acquire("push:"+ip).allowed())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Try again in a minute");
    }
    private String hash(String auth) {
        if(auth==null || !auth.matches("Bearer [a-f0-9]{64}"))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Device authorization required");
        return residents.hash(auth.substring(7));
    }
    public record Registration(@NotBlank @Size(max=2048) String token, boolean consent) {}
    @GetMapping("/api/push/status") public Map<String,Object> status() {
        return Map.of("configured",sender.configured(),"projectId",sender.project());
    }
    @PutMapping("/api/{slug}/push/installations/{id}")
    public Map<String,Object> register(@PathVariable String slug,@PathVariable UUID id,
            @RequestHeader(value="Authorization",required=false) String auth,@Valid @RequestBody Registration registration,
            HttpServletRequest request) {
        rate(request);String hash=hash(auth);UUID community=residents.community(slug);
        if(!registration.consent() || !registration.token().matches("[A-Za-z0-9_:.-]{20,2048}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Notification consent and a valid token are required");
        try {
            int changed=jdbc.update("""
                INSERT INTO push_installation(id,community_id,secret_hash,fcm_token,enabled) VALUES(?,?,?,?,true)
                ON CONFLICT(id) DO UPDATE SET community_id=excluded.community_id,fcm_token=excluded.fcm_token,
                  enabled=true,updated_at=NOW() WHERE push_installation.secret_hash=excluded.secret_hash
                """,id,community,hash,registration.token());
            if(changed==0)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Device authorization required");
        } catch(DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"This notification token is already registered");
        }
        return Map.of("enabled",true,"sendingConfigured",sender.configured());
    }
    @DeleteMapping("/api/push/installations/{id}")
    public void disable(@PathVariable UUID id,@RequestHeader(value="Authorization",required=false) String auth,HttpServletRequest request) {
        rate(request);String hash=hash(auth);
        // Unknown devices are harmless; an existing device can only be disabled with its private capability.
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM push_installation WHERE id=? AND secret_hash<>?)",Boolean.class,id,hash)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Device authorization required");
        jdbc.update("UPDATE push_installation SET enabled=false,fcm_token=NULL,updated_at=NOW() WHERE id=? AND secret_hash=?",id,hash);
    }
    public record Event(@NotNull UUID deliveryId,@Pattern(regexp="received|opened") @NotNull String type) {}
    @PostMapping("/api/push/installations/{id}/events")
    public void event(@PathVariable UUID id,@RequestHeader(value="Authorization",required=false) String auth,
            @Valid @RequestBody Event event,HttpServletRequest request) {
        rate(request);String hash=hash(auth);
        String column="opened".equals(event.type())?"opened_at":"received_at";
        int changed=jdbc.update("UPDATE push_campaign_delivery d SET "+column+"=COALESCE(d."+column+",NOW()) FROM push_installation i WHERE d.installation_id=i.id AND i.id=? AND i.secret_hash=? AND d.id=?",id,hash,event.deliveryId());
        if(changed==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification unavailable on this device");
    }
}
