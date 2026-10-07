package com.apnabazaar;

import com.apnabazaar.service.QueryNormalizer;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class QueryNormalizerTest {
    @Test
    void preservesIndianScriptsAndCombiningMarksWithoutMergingDifferentQueries() {
        var queries = List.of("हिंदी ट्यूशन", "हिंदी संगीत", "తెలుగు పాఠాలు", "தமிழ் வகுப்புகள்",
            "বাংলা শিক্ষা", "മലയാളം ക്ലാസുകൾ", "اردو تعلیم", "ਪੰਜਾਬੀ ਟਿਊਸ਼ਨ", "ગુજરાતી શિક્ષણ", "मराठी शिकवणी");
        var normalized = queries.stream().map(QueryNormalizer::normalize).toList();
        assertEquals(queries, normalized);
        assertEquals(queries.size(), normalized.stream().distinct().count());
    }

    @Test
    void handlesMixedLanguageCasePunctuationAndWhitespace() {
        assertEquals("hindi ट्यूशन चाहिए", QueryNormalizer.normalize("  HINDI ट्यूशन चाहिए?!  "));
        assertEquals("தமிழ் classes", QueryNormalizer.normalize("தமிழ்   CLASSES"));
        assertEquals("hindi tuition", QueryNormalizer.normalize("Hindi—Tuition"));
    }
}
