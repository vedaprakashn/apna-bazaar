package com.apnabazaar.service;
import com.apnabazaar.dto.*;
import com.apnabazaar.entity.*;
import com.apnabazaar.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
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
    private final HelpDirectoryService helpDirectory;
    private final HoodRideService hoodRides;

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
        if (interpreted.rideRequest()) {
            Instant at = null;
            boolean badTime = false;
            if (interpreted.rideAt() != null && !interpreted.rideAt().isBlank()) {
                try { at = OffsetDateTime.parse(interpreted.rideAt()).toInstant(); }
                catch (Exception ex) { try { at = LocalDateTime.parse(interpreted.rideAt()).atZone(ZoneId.of("Asia/Kolkata")).toInstant(); } catch(Exception ignored) { badTime=true; } }
            }
            boolean past = at != null && !at.isAfter(Instant.now());
            var rides = badTime || past ? List.<Map<String,Object>>of() : hoodRides.find(community.getId(),interpreted.rideKind(),interpreted.rideDirection(),interpreted.rideDestination(),at,interpreted.rideSeats(),null);
            String facts = past ? "The requested departure time is already past. Ask the resident for a future date/time."
                : badTime ? "The requested time could not be understood. Ask for the date and time."
                : rides.isEmpty() ? "No matching upcoming rides. The resident can open Hood Rides to post a request explicitly; their search has not been published."
                : "Found " + rides.size() + " upcoming pilot ride posts, matching direction, destination and (when supplied) within one hour of the requested departure. These are not confirmed rides. Invite the resident to discuss exact timing and seats through WhatsApp.";
            String introJson=callJson(AAPTA_VOICE + " Reply in " + interpreted.responseLanguage() + ". Return JSON with intro only. Explain these facts without inventing posts, transport bookings or availability: " + facts,query);
            var event=analyticsService.recordSearchEvent(community,query,QueryNormalizer.normalize(query),rides.size(),sessionId,interpreted.intent());
            if(rides.isEmpty())analyticsService.recordZeroResult(community,query,QueryNormalizer.normalize(query));
            return SearchResponse.builder().intro(Optional.ofNullable(extract(introJson,"intro")).orElse(facts)).providers(List.of()).rides(rides).rideQuery(Map.of("kind",interpreted.rideKind(),"direction",interpreted.rideDirection(),"destination",interpreted.rideDestination(),"at",at==null?"":at.toString(),"seats",interpreted.rideSeats())).sessionId(event.getId()).totalResults(rides.size()).searchIntent(interpreted.intent()).build();
        }
        if (interpreted.contactRequest()) {
            var contacts = helpDirectory.contacts(community.getId(), interpreted.contactCategories(), interpreted.doctorSpecialty());
            boolean urgent = contacts.stream().anyMatch(c -> "urgent".equals(c.get("section")))
                || interpreted.contactCategories().stream().anyMatch(c -> Set.of("First aid","Police","Fire services","Snake rescue","Ambulance").contains(c));
            String fallback = urgent ? "For a real emergency in India, call 112 now. These local contacts are demo listings only." : "Here are demo contacts in and around your hood. Their details aren’t verified yet.";
            String introJson = callJson(AAPTA_VOICE + "\nReply in " + interpreted.responseLanguage()
                + ". Return only JSON with intro. Explain these exact facts: " + fallback
                + " There are " + contacts.size() + " matching contacts. Do not invent phone numbers, medical advice, availability, distance or bookings.", query);
            var event = analyticsService.recordSearchEvent(community,query,QueryNormalizer.normalize(query),contacts.size(),sessionId,interpreted.intent());
            if (contacts.isEmpty()) analyticsService.recordZeroResult(community,query,QueryNormalizer.normalize(query));
            return SearchResponse.builder().intro(Optional.ofNullable(extract(introJson,"intro")).orElse(fallback))
                .providers(List.of()).contacts(contacts).urgentHelp(urgent).sessionId(event.getId())
                .totalResults(contacts.size()).searchIntent(interpreted.intent()).build();
        }
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
        // A cooked-idli request must not return a seller who only sells the ingredient batter.
        String foodIntent = interpreted.intent().toLowerCase(Locale.ROOT);
        if (Pattern.compile("\\b(idli|idly)\\b").matcher(foodIntent).find() && !foodIntent.contains("batter")) {
            providers = providers.stream().filter(p -> p.getOfferings().stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsAvailable()))
                .anyMatch(o -> Pattern.compile("(?i)\\b(idli|idly)\\b").matcher(o.getName()).find()
                    && !o.getName().toLowerCase(Locale.ROOT).contains("batter"))).toList();
        }
        String aiResponse = callOpenAi(query, buildCatalog(providers, postsByProvider), interpreted);
        List<MatchedProvider> matched = parseResponse(aiResponse, providers, postsByProvider);

        SearchEvent event = analyticsService.recordSearchEvent(
            community, query, QueryNormalizer.normalize(query), matched.size(), sessionId, interpreted.intent());
        if (matched.isEmpty()) analyticsService.recordZeroResult(community, query, QueryNormalizer.normalize(query));
        else analyticsService.recordImpressions(event, matched);

        return SearchResponse.builder()
            .intro(Optional.ofNullable(extract(aiResponse, "intro")).orElse(matched.isEmpty() ? aiResponse : matched.getFirst().matchReason()))
            .providers(matched)
            .sessionId(event.getId())
            .totalResults(matched.size())
            .searchIntent(interpreted.intent())
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

    private record QueryIntent(String intent, String responseLanguage, String teachingLanguage, boolean learningRequest, boolean contactRequest, List<String> contactCategories, String doctorSpecialty, boolean rideRequest, String rideKind, String rideDirection, String rideDestination, String rideAt, int rideSeats) {}

    private QueryIntent interpretQuery(String query) {
        // Interpret the short request before the larger catalog can distract from its meaning.
        String interpretation = callJson("""
                Translate this community marketplace search into a concise English search intent.
                Use a noun phrase suitable after "I was looking for", such as "weekend karate classes for kids".
                Do not include first-person requests like "I need", or trailing sentence punctuation.
                Supported languages: English, Hindi, Telugu, Tamil, Kannada, Malayalam,
                Marathi, Punjabi, Bengali and Assamese. Distinguish Bengali from Assamese
                even though they share a script. Preserve the user's language and script.
                Support Hinglish (Hindi-English), Tenglish (Telugu-English), Tanglish/Tamglish
                (Tamil-English), and English mixed with Kannada, Malayalam, Marathi, Punjabi,
                Bengali or Assamese. Detect meaning, not fixed keywords or spelling.
                "ইডলি পোৱা যাব নে?" is Assamese, not Bengali; reply using Assamese vocabulary.
                "idli dya please" is Marathi-English; "idli kittumo please" is Malayalam-English.
                "idli sigutta please" is Kannada-English (sigutta means is it available), not Telugu.
                "idli mil jayegi kya" is Hinglish; "idli mil sakdi aa" is Punjabi-English.
                "idli pawa jabe?" is Bengali-English, not Assamese (pawa jabe means can I get it).
                "idli pua jabo ne" is Assamese-English, not Bengali.
                For any Latin-script input, responseLanguage must specify Latin-script replies;
                never switch to native script unless the request itself uses native script.
                Understand native scripts, phonetic transliteration, informal
                speech, code-switching and spelling mistakes. Preserve the exact subject, dates,
                age and constraints. The language spoken by the requester is not necessarily the
                language they want lessons in. Do not answer the question or invent catalog facts.
                Examples:
                "ennaku hindi tuiton venum da" -> "I need Hindi tuition."
                "enakku hindi tuition venum" -> "I need Hindi tuition."
                "హిందీ ట్యూషన్ కావాలి" -> "I need Hindi tuition."
                "இந்தி டியூஷன் வேண்டும்" -> "I need Hindi tuition."
                "hindi tuition kavali" -> "I need Hindi tuition."
                "idly ivvu bey" -> "I want ready-to-eat idli." (Telugu, Latin script)
                "idly unda ra" -> "Is ready-to-eat idli available?" (Telugu, Latin script)
                Telugu: ivvu=give, unda=is there, kavali=want, ra/bey=casual address.
                Tamil: venum=want, irukka=is there. Never confuse Telugu with Tamil.
                Idly and idli mean the same food. Ready-to-eat idli is different from batter.
                Also identify the language and script used to express the request, not its subject.
                English "Hindi tuition" -> responseLanguage "English".
                Tanglish "ennaku hindi tuiton venum da" -> responseLanguage "Tamil transliterated in Latin script (Tanglish)".
                Telugu "hindi tuition kavali" -> responseLanguage "Telugu transliterated in Latin script".
                If the resident asks to learn a specific language, set teachingLanguage to that
                language's English name (e.g. Hindi, French, Tamil). Otherwise use null.
                This is the language to be taught, never simply the language of the request.
                Set learningRequest=true for requests to learn, tuition, lessons, teachers,
                coaching, classes or workshops; false for buying food, goods or repair services.
                Set contactRequest=true for requests to find/contact doctors, nurses, lawyers, physiotherapists, ambulance,
                first aid, local police, fire services, snake rescue, or the community help directory.
                Set contactCategories to an array of relevant exact names from:
                Doctors, Nurses, Lawyers, Physiotherapists, First aid, Police, Fire services, Snake rescue, Ambulance.
                If a doctor specialty is requested, set doctorSpecialty to its English medical specialty.
                Canonical values: General medicine (general physician), Paediatrics (child doctor),
                Cardiology (heart doctor), Dermatology (skin doctor), Orthopaedics (bone/joint doctor).
                For other specialties preserve the English specialty (do not replace with one of these).
                For generic doctor requests or non-doctor requests, doctorSpecialty=null.
                For all help contacts use an empty array and contactRequest=true.
                Understand this across all supported languages and romanized speech.
                Do not route food, classes (including first-aid classes), or a blood-test booking into contacts.
                Do not invent nearest distances or appointment slots.
                Set rideRequest=true for carpool/ride-sharing requests or offers, e.g. "anyone going to airport tonight at 11pm".
                rideKind is the type of OTHER PEOPLE'S posts to search, not the user's own type.
                Set rideKind="offer" for passengers looking for a driver: "anyone going to airport tonight at 11pm", "need a ride", "can I join someone", "airport lift chahiye" all search offers.
                Set rideKind="request" ONLY when the user clearly has a ride to offer and wants passengers: "I am driving to airport, anyone need a lift?", "I have two seats available".
                Questions about anyone going somewhere default to offer; they do NOT mean the user is a driver.
                Set rideDirection="outbound" for community-to-destination, "inbound" for returning to the community.
                Set rideDestination to a concise English place name; RGIA/Shamshabad airport means Airport.
                Set rideAt to the requested departure datetime in ISO-8601 with +05:30 offset, resolving tonight/tomorrow against current India time.
                Set rideSeats to the number of seats requested, default 1 (maximum 6).
                If no time requested set rideAt=null. Preserve past times rather than silently changing tonight to tomorrow.
                Never classify blood-test booking, bicycle repair or general service requests as carpooling.
                Return only JSON: {"intent":"English meaning","responseLanguage":"language and script","teachingLanguage":null,"learningRequest":false,"contactRequest":false,"contactCategories":[],"doctorSpecialty":null,"rideRequest":false,"rideKind":"offer","rideDirection":"outbound","rideDestination":"","rideAt":null,"rideSeats":1}.
                Do not use Markdown fences.
                """ + "\nCurrent India datetime (Asia/Kolkata): " + LocalDateTime.now(ZoneId.of("Asia/Kolkata")), query);
        String intent = query, responseLanguage = "the original request language and script", teachingLanguage = null, doctorSpecialty = null;
        boolean learningRequest = false, contactRequest = false, rideRequest = false;
        String rideKind="offer", rideDirection="outbound", rideDestination="", rideAt=null;
        int rideSeats=1;
        List<String> contactCategories = new ArrayList<>();
        try {
            var interpreted = objectMapper.readTree(interpretation);
            intent = interpreted.path("intent").asText(query);
            contactRequest = interpreted.path("contactRequest").asBoolean(false);
            rideRequest=interpreted.path("rideRequest").asBoolean(false);
            rideKind="request".equals(interpreted.path("rideKind").asText())?"request":"offer";
            rideDirection="inbound".equals(interpreted.path("rideDirection").asText())?"inbound":"outbound";
            rideDestination=interpreted.path("rideDestination").asText("");
            rideAt=interpreted.path("rideAt").asText(null);
            rideSeats=Math.max(1,Math.min(6,interpreted.path("rideSeats").asInt(1)));
            doctorSpecialty = interpreted.path("doctorSpecialty").asText(null);
            for (var category : interpreted.path("contactCategories")) {
                if (HelpDirectoryService.CATEGORIES.contains(category.asText())) contactCategories.add(category.asText());
            }
            responseLanguage = interpreted.path("responseLanguage").asText(responseLanguage);
            teachingLanguage = interpreted.path("teachingLanguage").asText(null);
            learningRequest = interpreted.path("learningRequest").asBoolean(false)
                || Pattern.compile("(?i)\\b(tuition|tuitions|tutor|tutors|teacher|teachers|lessons|classes|workshop|workshops|coaching)\\b")
                    .matcher(intent + " " + query).find();
        } catch (Exception e) { log.warn("Could not parse query interpretation; using original request"); }
        var nativeScript = query.codePoints().mapToObj(Character.UnicodeScript::of)
            .filter(script -> script != Character.UnicodeScript.LATIN
                && script != Character.UnicodeScript.COMMON && script != Character.UnicodeScript.INHERITED)
            .findFirst();
        if (nativeScript.isPresent()) {
            String alphabet = nativeScript.get() == Character.UnicodeScript.BENGALI
                ? "the shared Bengali-Assamese alphabet; keep the detected language (Assamese is not Bengali)"
                : nativeScript.get().toString();
            responseLanguage += "; preserve the original alphabet: " + alphabet + "; never Latin transliteration";
        }
        if (nativeScript.isEmpty()) responseLanguage += "; write in Latin script only, matching the romanized input";
        return new QueryIntent(intent, responseLanguage, teachingLanguage, learningRequest, contactRequest, contactCategories, doctorSpecialty, rideRequest, rideKind, rideDirection, rideDestination, rideAt, rideSeats);
    }

    private static final String AAPTA_VOICE = """
        Use a relaxed, respectful neighbour-to-neighbour voice: warm and conversational,
        neither a formal customer-service report nor exaggerated slang. Use short everyday
        words and natural contractions. No "bro", "bestie", forced jokes, sales hype or excessive
        apologies/emojis. Apply this voice naturally in the user's language and script.
        Keep interface/technical terms such as "community catalog", "providers", "requested
        service" and "search results" out of resident-facing intro and matchReason.
        For no matches, say you couldn't find the specific thing around here or in their hood
        yet, rather than declaring that nobody offers it. One warm, concise sentence is enough;
        do not promise future stock, personal follow-up or onboarding. Do not add a question
        that assumes conversational follow-up memory, and never invent alternatives.
        English example for no match: "I couldn’t find Bharatanatyam lessons in your hood yet."
        English example for a matching Hindi tutor: "I found Hindi tuition in your hood."
        Examples illustrate tone only; adapt the actual subject and language to the request.
        "Hood" is optional where natural; do not force English slang into other languages.
        """;

    private String callOpenAi(String query, String catalog, QueryIntent interpreted) {
        String intent = interpreted.intent(), responseLanguage = interpreted.responseLanguage();
        if (catalog.isBlank()) {
            return callJson(AAPTA_VOICE + "You are Aapta, a friendly community AI guide. No provider in this community catalog offers the requested service. "
                + "Explain that specific missing offering in one helpful sentence, without inventing alternatives. Reply only in "
                + responseLanguage + ". Return JSON: {\"intro\":\"your explanation\",\"sellers\":[]}.", intent);
        }
        String sys = """
            You are Aapta (आप्त, a trusted friend), the warm community guide for HeyHood.
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
            1. Support English, Hindi, Telugu, Tamil, Kannada, Malayalam, Marathi, Punjabi,
               Bengali and Assamese, including native scripts, transliteration,
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
            2. Be warm, grounded and conversational. An occasional relevant emoji is fine;
               never force food emojis into lessons or service enquiries.
            3. Keep intro to ONE useful sentence identifying the matching offering or seller.
               Do not quote serving hours or prices in the intro. Never invent times or stock.
               A permanent catalog entry does not prove an item is available right now.
               Use the storefront for schedules; only mention today's availability if supported
               by TODAY'S MENU. A missing daily post must not be described as live stock.
            4. Return a JSON object with intro and sellers fields:
               {"intro":"Specific answer in the mandatory response language",
                "sellers":[{"id":"seller UUID","matchReason":"specific matching offering and why it fits the request"}]}
               Every matching seller needs a card. If none match, use sellers:[] and explain
               exactly what is missing. Never give a generic greeting or merely promise help.
               Idly/idli are equivalent. Ready-to-eat idli must match the idli plate, not batter.
            """.formatted(catalog);
        sys += "\n" + AAPTA_VOICE;
        sys += "\nMANDATORY RESPONSE LANGUAGE: " + responseLanguage
            + ". Both intro and matchReason must use this language and script. Never copy the format placeholder.";
        return callJson(sys, "Original resident request: " + query
            + "\nEnglish search meaning: " + intent + "\nResponse language: " + responseLanguage
            + "\nMatch the search meaning. For no matches, explain which offering is missing. Return the required JSON object.");
    }

    private String callJson(String system, String user) {
        var options = OpenAiChatOptions.builder().temperature(0.1)
            .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_OBJECT, null)).build();
        return chatModel.call(new Prompt(List.of(new SystemMessage(system), new UserMessage(user)), options))
            .getResult().getOutput().getText();
    }

    private List<MatchedProvider> parseResponse(String raw, List<Provider> providers,
                                                 Map<UUID, List<DailyPost>> posts) {
        try {
            var json = objectMapper.readTree(raw);
            if (json.has("sellers")) raw = "<sellers>" + json.get("sellers") + "</sellers>";
        } catch (Exception ignored) { /* Accept legacy tagged replies too. */ }
        var m = Pattern.compile("<sellers>([\\s\\S]*?)</sellers>", Pattern.CASE_INSENSITIVE).matcher(raw);
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
                        .flatNumber(p.getFlatNumber()).whatsappNumber(p.getWhatsappNumber()).whatsappGroupUrl(p.getWhatsappGroupUrl())
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
        try {
            var value = objectMapper.readTree(raw).path(tag);
            if (value.isTextual() && !value.asText().isBlank()) return value.asText();
        } catch (Exception ignored) {}

        var m = Pattern.compile("<" + tag + ">([\\s\\S]*?)</" + tag + ">", Pattern.CASE_INSENSITIVE).matcher(raw);
        return m.find() ? m.group(1).trim() : null;
    }


}
