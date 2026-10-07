package com.apnabazaar.service;
import com.apnabazaar.dto.*;
import com.apnabazaar.entity.*;
import com.apnabazaar.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {
    private final OpenAiChatModel chatModel;
    private final CommunityRepository communityRepo;
    private final ProviderRepository providerRepo;
    private final DailyPostRepository dailyPostRepo;
    private final AnalyticsService analyticsService;
    private final OrderingWindowService orderingWindowService;
    private final ObjectMapper objectMapper;

    @Transactional
    public SearchResponse search(String communitySlug, String query, UUID sessionId) {
        Community community = communityRepo.findBySlug(communitySlug)
            .orElseThrow(() -> new RuntimeException("Community not found: " + communitySlug));
        List<Provider> providers = providerRepo.findActiveWithOfferings(community.getId());
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        List<DailyPost> posts = dailyPostRepo.findActiveTodayForCommunity(community.getId(), today);
        Map<UUID, List<DailyPost>> postsByProvider = posts.stream()
            .collect(Collectors.groupingBy(dp -> dp.getProvider().getId()));

        QueryIntent interpreted = interpretQuery(query);
        if (interpreted.learningRequest()) {
            providers = providers.stream().filter(p -> p.getOfferings().stream()
                .anyMatch(o -> Boolean.TRUE.equals(o.getIsAvailable()) && o.getOfferingType()==Offering.OfferingType.class_)).toList();
        }
        if (interpreted.teachingLanguage() != null && !interpreted.teachingLanguage().isBlank()) {
            var subject = Pattern.compile("(?iu)(?<![\\p{L}])" + Pattern.quote(interpreted.teachingLanguage()) + "(?![\\p{L}])");
            providers = providers.stream().filter(p -> p.getOfferings().stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsAvailable()))
                .anyMatch(o -> subject.matcher(o.getName() + " " + o.getDescription()).find())).toList();
        }
        String aiResponse = callOpenAi(query, buildCatalog(providers, postsByProvider), interpreted);
        List<MatchedProvider> matched = parseResponse(aiResponse, providers, postsByProvider);

        SearchEvent event = analyticsService.recordSearchEvent(
            community, query, QueryNormalizer.normalize(query), matched.size(), sessionId);
        if (matched.isEmpty()) analyticsService.recordZeroResult(community, query, QueryNormalizer.normalize(query));
        else analyticsService.recordImpressions(event, matched);

        return SearchResponse.builder()
            .intro(Optional.ofNullable(extract(aiResponse, "intro")).orElse(matched.isEmpty() ? aiResponse : "Here are your community matches."))
            .providers(matched)
            .sessionId(event.getId())
            .totalResults(matched.size())
            .build();
    }

    @Transactional
    public void recordClick(UUID searchEventId, UUID providerId, String clickType) {
        analyticsService.recordClick(searchEventId, providerId, clickType);
    }

    private String buildCatalog(List<Provider> providers, Map<UUID, List<DailyPost>> posts) {
        var sb = new StringBuilder();
        for (Provider p : providers) {
            sb.append("SELLER ID:").append(p.getId())
              .append(" | ").append(p.getName())
              .append(" | Shop: ").append(p.getShopName() != null ? p.getShopName() : p.getName())
              .append(" | Flat: ").append(p.getFlatNumber())
              .append("\nPERMANENT CATALOG:\n");
            if (p.getOfferings() != null) {
                p.getOfferings().stream().filter(o -> Boolean.TRUE.equals(o.getIsAvailable())).forEach(o -> {
                    sb.append("  - ").append(o.getName()).append(": ").append(o.getDescription())
                      .append(" | Price: Rs.").append(o.getBasePrice()).append("/").append(o.getUnit()).append("\n");
                    o.getSchedules().stream().filter(sc -> Boolean.TRUE.equals(sc.getIsActive())).forEach(sc ->
                        sb.append("    Schedule: ").append(sc.getDayScope()).append(" ")
                          .append(sc.getDaysOfWeek() == null ? "" : sc.getDaysOfWeek()).append(" ")
                          .append(sc.getServesFrom()).append("–").append(sc.getServesTo()).append(" Asia/Kolkata\n"));
                });
            }
            List<DailyPost> dp = posts.get(p.getId());
            if (dp != null && !dp.isEmpty()) {
                sb.append("TODAY'S MENU:\n");
                dp.forEach(post -> post.getLineItems().forEach(li ->
                    sb.append("  - ").append(li.getItemName())
                      .append(li.getPrice() != null ? " Rs." + li.getPrice() : "")
                      .append(li.getPickupTime() != null ? " | Pickup: " + li.getPickupTime() : "")
                      .append("\n")));
            }
            sb.append("---\n\n");
        }
        return sb.toString();
    }

    private record QueryIntent(String intent, String responseLanguage, String teachingLanguage, boolean learningRequest) {}

    private QueryIntent interpretQuery(String query) {
        // Interpret the short request before the larger catalog can distract from its meaning.
        String interpretation = chatModel.call(new Prompt(List.of(
            new SystemMessage("""
                Translate this community marketplace search into a concise English search intent.
                Understand all Indian languages, native scripts, phonetic transliteration, informal
                speech, code-switching and spelling mistakes. Preserve the exact subject, dates,
                age and constraints. The language spoken by the requester is not necessarily the
                language they want lessons in. Do not answer the question or invent catalog facts.
                Examples:
                "ennaku hindi tuiton venum da" -> "I need Hindi tuition."
                "enakku hindi tuition venum" -> "I need Hindi tuition."
                "హిందీ ట్యూషన్ కావాలి" -> "I need Hindi tuition."
                "இந்தி டியூஷன் வேண்டும்" -> "I need Hindi tuition."
                "hindi tuition kavali" -> "I need Hindi tuition."
                Also identify the language and script used to express the request, not its subject.
                English "Hindi tuition" -> responseLanguage "English".
                Tanglish "ennaku hindi tuiton venum da" -> responseLanguage "Tamil transliterated in Latin script (Tanglish)".
                Telugu "hindi tuition kavali" -> responseLanguage "Telugu transliterated in Latin script".
                If the resident asks to learn a specific language, set teachingLanguage to that
                language's English name (e.g. Hindi, French, Tamil). Otherwise use null.
                This is the language to be taught, never simply the language of the request.
                Set learningRequest=true for requests to learn, tuition, lessons, teachers,
                coaching, classes or workshops; false for buying food, goods or repair services.
                Return only JSON: {"intent":"English meaning","responseLanguage":"language and script","teachingLanguage":null,"learningRequest":false}.
                Do not use Markdown fences.
                """), new UserMessage(query)))).getResult().getOutput().getText();
        String intent = query, responseLanguage = "the original request language and script", teachingLanguage = null;
        boolean learningRequest = false;
        try {
            var interpreted = objectMapper.readTree(interpretation);
            intent = interpreted.path("intent").asText(query);
            responseLanguage = interpreted.path("responseLanguage").asText(responseLanguage);
            teachingLanguage = interpreted.path("teachingLanguage").asText(null);
            learningRequest = interpreted.path("learningRequest").asBoolean(false)
                || Pattern.compile("(?i)\\b(tuition|tuitions|tutor|tutors|teacher|teachers|lessons|classes|workshop|workshops|coaching)\\b")
                    .matcher(intent + " " + query).find();
        } catch (Exception e) { log.warn("Could not parse query interpretation; using original request"); }
        return new QueryIntent(intent, responseLanguage, teachingLanguage, learningRequest);
    }

    private String callOpenAi(String query, String catalog, QueryIntent interpreted) {
        String intent = interpreted.intent(), responseLanguage = interpreted.responseLanguage();
        if (catalog.isBlank()) {
            return chatModel.call(new Prompt(List.of(new SystemMessage(
                "You are Aapta, a friendly community AI guide. No provider in this community catalog offers the requested service. "
                + "Explain that specific missing offering in one helpful sentence, without inventing alternatives. Reply only in "
                + responseLanguage + ". Format: <intro>your explanation</intro><sellers>[]</sellers>."),
                new UserMessage(intent)))).getResult().getOutput().getText();
        }
        String sys = """
            You are Aapta (आप्त, a trusted friend), the warm community guide for Apna Bazaar.
            Speak like a helpful neighbour: friendly, clear, and practical, without sales hype.
            Help residents discover neighbours' shops, food, classes and services. You are an AI
            guide, never pretend to be a resident or claim personal experience with a seller.
            Give useful answers rather than merely repeating the request. Keep your name Aapta
            in every language if introducing yourself; you need not introduce yourself each turn.
            Choose the response language from the wording of the user message, not the product
            or subject requested. "Hindi tuition" and "French tuition" are English requests.
            Return each seller ID once, combining matching offerings in one reason.
            SELLER DATABASE:
            %s
            Rules:
            1. Interpret queries in any Indian language, including native scripts, transliteration,
               and code-switching (Hinglish, Tanglish, Telugu mixed with English, etc.).
               Translate the meaning internally before matching the English catalog.
               Understand synonyms: tuition, tutor, coaching, lessons and classes can express
               the same need; "Hindi tuition", "हिंदी की ट्यूशन", and "Hindi sikhane wale"
               all ask for Hindi teaching. Preserve the requested subject, age and constraints.
               Broad category requests can match several providers. A specific subject must
               be explicitly supported by the listing; never substitute karate or handwriting
               for Hindi tuition. Respect stated days and schedules.
               Reply in the query's language and script, including intro and matchReason.
               The subject being taught does not determine the reply language: an English
               request for Hindi tuition needs an English reply; a Tamil request needs Tamil.
               Keep seller IDs and XML/JSON structural keys unchanged.
               Match only catalog facts; do not infer an unsupported language or skill.
            Only use sellers from this database. Explain when availability is on a different day.
            Listings marked DEMO are fictional; do not invent contacts, ratings, or stock.
            2. Be warm, peppy, use food emojis naturally
            3. Keep intro to ONE punchy sentence max
            4. ALWAYS respond in this exact format when sellers found:
            <intro>Your helpful answer, written in the mandatory response language</intro>
            <sellers>[{"id":"<uuid>","matchReason":"specific item and why, 1 line"}]</sellers>
            If nothing matches, explain the missing offering in the query language using an <intro> tag and <sellers>[]</sellers>.
            """.formatted(catalog);
        sys += "\nMANDATORY RESPONSE LANGUAGE: " + responseLanguage
            + ". Both intro and matchReason must use this language and script. Never copy the format placeholder.";
        var prompt = new Prompt(List.of(new SystemMessage(sys), new UserMessage("Original resident request: " + query
            + "\nEnglish search meaning: " + intent + "\nResponse language: " + responseLanguage
            + "\nMatch the search meaning. For no matches, explain which offering is missing. Use the required tags.")));
        return chatModel.call(prompt).getResult().getOutput().getText();
    }

    private List<MatchedProvider> parseResponse(String raw, List<Provider> providers,
                                                 Map<UUID, List<DailyPost>> posts) {
        var m = Pattern.compile("<sellers>([\\s\\S]*?)</sellers>").matcher(raw);
        if (!m.find()) return List.of();
        Map<UUID, Provider> map = providers.stream().collect(Collectors.toMap(Provider::getId, p -> p));
        try {
            List<Map<String, String>> parsed = objectMapper.readValue(m.group(1).trim(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            Set<UUID> seen = new HashSet<>();
            return parsed.stream().map(r -> {
                try {
                    UUID id = UUID.fromString(r.get("id"));
                    Provider p = map.get(id);
                    if (p == null || !seen.add(id)) return null;
                    List<TodayItemDto> items = buildItems(posts.get(id));
                    return MatchedProvider.builder()
                        .id(p.getId()).name(p.getName())
                        .shopName(p.getShopName() != null ? p.getShopName() : p.getName())
                        .flatNumber(p.getFlatNumber()).whatsappNumber(p.getWhatsappNumber())
                        .matchReason(r.get("matchReason"))
                        .rating(p.getRating()).reviewCount(p.getReviewCount())
                        .todayItems(items)
                        .orderingStatus(items.stream().map(TodayItemDto::orderingStatus)
                            .filter(Objects::nonNull).findFirst().orElse(null))
                        .build();
                } catch (Exception e) { return null; }
            }).filter(Objects::nonNull).toList();
        } catch (Exception e) { log.error("Parse error", e); return List.of(); }
    }

    private List<TodayItemDto> buildItems(List<DailyPost> dp) {
        if (dp == null) return List.of();
        return dp.stream().flatMap(p -> p.getLineItems().stream()).map(li -> {
            String status = null;
            try { status = orderingWindowService.resolve(li).toChatLine(); } catch (Exception ignored) {}
            return new TodayItemDto(li.getItemName(), li.getPrice(), li.getPickupTime(),
                li.getPickupLocation(), li.getDeliveryType() != null ? li.getDeliveryType().name() : null, status);
        }).toList();
    }

    private String extract(String raw, String tag) {
        var m = Pattern.compile("<" + tag + ">([\\s\\S]*?)</" + tag + ">").matcher(raw);
        return m.find() ? m.group(1).trim() : null;
    }


}
