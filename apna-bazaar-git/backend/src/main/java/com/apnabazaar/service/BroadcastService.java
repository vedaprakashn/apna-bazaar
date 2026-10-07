package com.apnabazaar.service;
import com.apnabazaar.dto.*;
import com.apnabazaar.entity.*;
import com.apnabazaar.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BroadcastService {
    private final BroadcastRepository broadcastRepo;
    private final CommunityRepository communityRepo;
    private final ZeroResultLogRepository zeroResultRepo;
    private final OpenAiChatModel chatModel;

    @Transactional
    public Broadcast create(String communitySlug, BroadcastRequest req) {
        Community community = communityRepo.findBySlug(communitySlug).orElseThrow();
        return broadcastRepo.save(Broadcast.builder()
            .community(community).message(req.message())
            .broadcastType(Broadcast.BroadcastType.valueOf(req.type() != null ? req.type() : "organic"))
            .channel(Broadcast.BroadcastChannel.chatbot_digest)
            .scheduledAt(req.scheduledAt())
            .isRecurring(req.recurrencePattern() != null)
            .recurrencePattern(req.recurrencePattern())
            .status(req.scheduledAt() == null ? Broadcast.BroadcastStatus.draft : Broadcast.BroadcastStatus.scheduled)
            .build());
    }

    @Transactional
    public Broadcast sendNow(UUID id) {
        Broadcast b = broadcastRepo.findById(id).orElseThrow();
        b.setStatus(Broadcast.BroadcastStatus.sent);
        b.setSentAt(Instant.now());
        log.info("Broadcast sent [{}]: {}", b.getCommunity().getSlug(), b.getMessage());
        return broadcastRepo.save(b);
    }

    @Scheduled(fixedDelayString = "${apna.broadcast.scheduler-interval-ms:60000}")
    @Transactional
    public void processScheduled() {
        broadcastRepo.findDueForSending(LocalDateTime.now(ZoneId.of("Asia/Kolkata"))).forEach(b -> {
            try {
                sendNow(b.getId());
                if (Boolean.TRUE.equals(b.getIsRecurring())) {
                    b.setStatus(Broadcast.BroadcastStatus.recurring_active);
                    b.setScheduledAt(nextOccurrence(b.getRecurrencePattern(), LocalDateTime.now()));
                    broadcastRepo.save(b);
                }
            } catch (Exception e) { log.error("Failed broadcast {}", b.getId(), e); }
        });
    }

    public List<AISuggestedNudge> generateAISuggestions(String communitySlug) {
        Community community = communityRepo.findBySlug(communitySlug).orElseThrow();
        List<ZeroResultLog> unmet = zeroResultRepo.findTopUnmetDemand(community.getId());
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        String context = "Community: " + community.getName()
            + "\nTime: " + now.toLocalTime() + " " + now.getDayOfWeek()
            + "\nTop zero-result queries: " + unmet.stream().limit(3)
                .map(z -> z.getRawQuery() + "(" + z.getOccurrenceCount() + "x)").reduce("", (a,b) -> a + ", " + b);
        String prompt = "Generate 4 short broadcast nudges for a gated community marketplace.\n"
            + "Context: " + context + "\n"
            + "Rules: max 200 chars each, Hinglish, food emojis, end with chatbot search prompt.\n"
            + "Respond ONLY with JSON array: [{\"reason\":\"...\",\"message\":\"...\"}]";
        try {
            String raw = chatModel.call(new Prompt(List.of(new UserMessage(prompt))))
                .getResult().getOutput().getText().replaceAll("```json|```","").trim();
            return parseNudges(raw);
        } catch (Exception e) { log.error("AI nudge generation failed", e); return defaultNudges(); }
    }

    public Optional<String> getActiveDailyDigest(String communitySlug) {
        return broadcastRepo.findLatestSentForCommunity(communitySlug, LocalDate.now()).map(Broadcast::getMessage);
    }
    public List<Broadcast> getHistory(String slug, int limit) { return broadcastRepo.findSentHistory(slug, limit); }
    public List<Broadcast> getScheduled(String slug) { return broadcastRepo.findScheduled(slug); }

    @Transactional
    public void recordBroadcastSearch(UUID id) {
        broadcastRepo.findById(id).ifPresent(b -> { b.setSearchesTriggered(b.getSearchesTriggered()+1); broadcastRepo.save(b); });
    }

    private List<AISuggestedNudge> parseNudges(String json) {
        try {
            var rp = java.util.regex.Pattern.compile("\"reason\"\\s*:\\s*\"([^\"]+)\"");
            var mp = java.util.regex.Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
            var rm = rp.matcher(json); var mm = mp.matcher(json);
            List<AISuggestedNudge> r = new ArrayList<>();
            while (rm.find() && mm.find()) r.add(new AISuggestedNudge(rm.group(1), mm.group(1)));
            return r.isEmpty() ? defaultNudges() : r;
        } catch (Exception e) { return defaultNudges(); }
    }

    private List<AISuggestedNudge> defaultNudges() {
        return List.of(
            new AISuggestedNudge("Morning", "☀️ Good morning! Fresh home food available today. Ask me 'what\'s cooking' 🍱"),
            new AISuggestedNudge("Evening", "🌙 Dinner time! Home kitchens are ready tonight. Ask me what\'s available 🍽️"),
            new AISuggestedNudge("Weekend", "🛌 Order your batter tonight for tomorrow\'s lazy Sunday breakfast 🫓"),
            new AISuggestedNudge("Discovery", "🏡 10+ neighbours sell things in Tridasa! Ask Apna Bazaar to discover!")
        );
    }

    private LocalDateTime nextOccurrence(String pattern, LocalDateTime from) {
        if (pattern == null) return from.plusDays(1);
        return switch (pattern) {
            case "daily" -> from.plusDays(1);
            case "weekdays" -> { var n=from.plusDays(1); while(n.getDayOfWeek()==DayOfWeek.SATURDAY||n.getDayOfWeek()==DayOfWeek.SUNDAY) n=n.plusDays(1); yield n; }
            case "weekly" -> from.plusWeeks(1);
            default -> from.plusDays(1);
        };
    }
}
