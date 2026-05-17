package io.gitlab.rxp90.jsymspell.benchmarks;

import io.gitlab.rxp90.jsymspell.api.DamerauLevenshteinOSA;
import io.gitlab.rxp90.jsymspell.api.StringDistance;
import io.gitlab.rxp90.jsymspell.api.SuggestItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Brute-force baseline: scans the entire unigram lexicon, computing
 * Damerau-Levenshtein (OSA) distance against the input. Used only as the
 * comparison point in JMH benchmarks.
 */
public final class NaiveSpellChecker {

    private final Map<String, Long> unigramLexicon;
    private final StringDistance distance = new DamerauLevenshteinOSA();

    public NaiveSpellChecker(Map<String, Long> unigramLexicon) {
        this.unigramLexicon = unigramLexicon;
    }

    /**
     * Returns suggestions whose distance to {@code input} is within
     * {@code maxEditDistance}, sorted by (distance asc, frequency desc).
     */
    public List<SuggestItem> lookup(String input, int maxEditDistance) {
        List<SuggestItem> results = new ArrayList<>();
        for (Map.Entry<String, Long> entry : unigramLexicon.entrySet()) {
            int d = distance.distanceWithEarlyStop(input, entry.getKey(), maxEditDistance);
            if (d >= 0 && d <= maxEditDistance) {
                results.add(new SuggestItem(entry.getKey(), d, entry.getValue()));
            }
        }
        Collections.sort(results);
        return results;
    }
}
