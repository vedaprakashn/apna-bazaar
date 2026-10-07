package com.apnabazaar.service;

import java.text.Normalizer;
import java.util.Locale;

public final class QueryNormalizer {
    private QueryNormalizer() {}

    public static String normalize(String query) {
        return Normalizer.normalize(query, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{M}\\p{N}\\s]", " ").trim().replaceAll("\\s+", " ");
    }
}
