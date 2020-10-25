package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.api.DamerauLevenshteinOSA;
import io.gitlab.rxp90.jsymspell.api.StringDistance;
import io.gitlab.rxp90.jsymspell.exceptions.NotInitializedException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static io.gitlab.rxp90.jsymspell.SymSpell.Verbosity.ALL;

public class SymSpell {

    private static final Logger logger = Logger.getLogger(SymSpell.class.getName());

    private final int maxDictionaryEditDistance;
    private final int prefixLength;

    private final Map<String, Collection<String>> deletes = new ConcurrentHashMap<>();
    private final Map<Bigram, Long> bigramLexicon;
    private final Map<String, Long> unigramLexicon;
    private final StringDistance stringDistance;

    private int maxDictionaryWordLength;

    // number of all words in the corpus used to generate the frequency dictionary
    // this is used to calculate the word occurrence probability p from word counts c : p=c/N
    // N equals the sum of all counts c in the dictionary only if the dictionary is complete, but not
    // if the dictionary is truncated or filtered
    private static long N = 1024908267229L; //
    private long bigramCountMin = Long.MAX_VALUE;

    public enum Verbosity {
        TOP,
        CLOSEST,
        ALL
    }

    SymSpell(int maxDictionaryEditDistance, int prefixLength, Map<String, Long> unigramLexicon, Map<Bigram, Long> bigramLexicon) {
        this.unigramLexicon = unigramLexicon;
        this.maxDictionaryEditDistance = maxDictionaryEditDistance;
        this.prefixLength = prefixLength;
        this.bigramLexicon = bigramLexicon;
        stringDistance = new DamerauLevenshteinOSA();
        init();
    }

    private boolean deleteSuggestionPrefix(String delete, int deleteLen, String suggestion, int suggestionLen) {
        if (deleteLen == 0) return true;

        int adjustedSuggestionLen = Math.min(prefixLength, suggestionLen);

        int j = 0;

        for (int i = 0; i < deleteLen; i++) {
            char delChar = delete.charAt(i);
            while (j < adjustedSuggestionLen && delChar != suggestion.charAt(j)) {
                j++;
            }
            if (j == adjustedSuggestionLen) return false;
        }
        return true;
    }

    Set<String> edits(String word, int editDistance, Set<String> deleteWords) {
        editDistance++;
        if (word.length() > 1 && editDistance <= maxDictionaryEditDistance) {
            for (int i = 0; i < word.length(); i++) {
                StringBuilder editableWord = new StringBuilder(word);
                String delete = editableWord.deleteCharAt(i).toString();
                if (deleteWords.add(delete) && editDistance < maxDictionaryEditDistance) {
                    edits(delete, editDistance, deleteWords);
                }
            }
        }
        return deleteWords;
    }

    private Set<String> editsPrefix(String key) {
        Set<String> set = new HashSet<>();
        if (key.length() <= maxDictionaryEditDistance) {
            set.add("");
        }
        if (key.length() > prefixLength) {
            key = key.substring(0, prefixLength);
        }
        set.add(key);
        return edits(key, 0, set);
    }

    private void init() {
        this.maxDictionaryWordLength = 0;
        this.unigramLexicon.keySet().forEach(word -> {
            this.maxDictionaryWordLength = Math.max(this.maxDictionaryWordLength, word.length());
            Map<String, Collection<String>> edits = generateEdits(word);
            edits.forEach((string, suggestions) -> this.deletes.computeIfAbsent(string, ignored -> new ArrayList<>()).addAll(suggestions));
        });
    }

    private Map<String, Collection<String>> generateEdits(String key) {
        Set<String> edits = editsPrefix(key);
        Map<String, Collection<String>> generatedDeletes = new HashMap<>();
        edits.forEach(delete -> {
            generatedDeletes.computeIfAbsent(delete, ignored -> new ArrayList<>()).add(key);
        });
        return generatedDeletes;
    }

    public List<SuggestItem> lookup(String input, Verbosity verbosity) throws NotInitializedException {
        return lookup(input, verbosity, this.maxDictionaryEditDistance, false);
    }

    private List<SuggestItem> lookup(String input, Verbosity verbosity, int maxEditDistance, boolean includeUnknown) throws NotInitializedException {
        if (maxEditDistance > maxDictionaryEditDistance) {
            throw new IllegalArgumentException("maxEditDistance > maxDictionaryEditDistance");
        }

        if (unigramLexicon.isEmpty()) {
            throw new NotInitializedException("There are no words in the lexicon.");
        }

        List<SuggestItem> suggestions = new ArrayList<>();
        int inputLen = input.length();
        boolean wordIsTooLong = inputLen - maxEditDistance > maxDictionaryWordLength;
        if (wordIsTooLong && includeUnknown) {
            return List.of(new SuggestItem(input, maxEditDistance + 1, 0));
        }

        if (unigramLexicon.containsKey(input)) {
            SuggestItem suggestSameWord = new SuggestItem(input, 0, unigramLexicon.get(input));
            suggestions.add(suggestSameWord);

            if (!verbosity.equals(ALL)) {
                return suggestions;
            }
        }

        if (maxEditDistance == 0 && includeUnknown && suggestions.isEmpty()) {
            return List.of(new SuggestItem(input, maxEditDistance + 1, 0));
        }

        Set<String> suggestionsAlreadyConsidered = new HashSet<>();

        suggestionsAlreadyConsidered.add(input);

        int maxEditDistance2 = maxEditDistance;

        List<String> candidates = new ArrayList<>();

        int inputPrefixLen;
        if (inputLen > prefixLength) {
            inputPrefixLen = prefixLength;
            candidates.add(input.substring(0, inputPrefixLen));
        } else {
            inputPrefixLen = inputLen;
        }
        candidates.add(input);

        int candidatePointer = 0;
        while (candidatePointer < candidates.size()) {
            String candidate = candidates.get(candidatePointer++);
            int candidateLength = candidate.length();
            int lengthDiffBetweenInputAndCandidate = inputPrefixLen - candidateLength;

            boolean candidateDistanceHigherThanSuggestionDistance = lengthDiffBetweenInputAndCandidate > maxEditDistance2;
            if (candidateDistanceHigherThanSuggestionDistance) {
                if (verbosity.equals(Verbosity.ALL)) {
                    continue;
                } else {
                    break;
                }
            }

            Collection<String> dictSuggestions = deletes.get(candidate);
            if (dictSuggestions != null) {
                for (String suggestion : dictSuggestions) {
                    if (suggestion.equals(input) || ((Math.abs(suggestion.length() - inputLen) > maxEditDistance2)
                            || (suggestion.length() < candidateLength)
                            || (suggestion.length() == candidateLength && !suggestion.equals(candidate))) || (Math.min(suggestion.length(), prefixLength) > inputPrefixLen
                            && (Math.min(suggestion.length(), prefixLength) - candidateLength) > maxEditDistance2)){
                        continue;
                    }

                    int suggestionLen = suggestion.length();

                    int distance;
                    if (candidateLength == 0) {
                        distance = Math.max(inputLen, suggestionLen);
                        if (distance <= maxEditDistance2) {
                            suggestionsAlreadyConsidered.add(suggestion);
                        }
                    } else if (suggestionLen == 1) {
                        if (input.contains(suggestion)) {
                            distance = inputLen - 1;
                        } else {
                            distance = inputLen;
                        }
                        if (distance <= maxEditDistance2) {
                            suggestionsAlreadyConsidered.add(suggestion);
                        }
                    } else {
          /*
          handles the shortcircuit of min_distance assignment when first boolean expression
          evaluates to False
         */
                        int minDistance = Math.min(inputLen, suggestionLen) - prefixLength;
                        // Is distance calculation required
                        if (prefixLength - maxEditDistance == candidateLength
                                && (minDistance > 1
                                && (!input.substring(inputLen + 1 - minDistance).equals(suggestion.substring(suggestionLen + 1 - minDistance))))
                                || (minDistance > 0
                                && input.charAt(inputLen - minDistance) != suggestion.charAt(suggestionLen - minDistance)
                                && input.charAt(inputLen - minDistance - 1) != suggestion.charAt(suggestionLen - minDistance)
                                && input.charAt(inputLen - minDistance) != suggestion.charAt(suggestionLen - minDistance - 1))) {
                            continue;
                        } else {
                            if (!verbosity.equals(Verbosity.ALL)
                                    && !deleteSuggestionPrefix(candidate, candidateLength, suggestion, suggestionLen)
                                    || !suggestionsAlreadyConsidered.add(suggestion)) {
                                continue;
                            }
                            distance = stringDistance.distanceWithEarlyStop(input, suggestion, maxEditDistance2);
                            if (distance < 0) continue;
                        }

                        if (distance <= maxEditDistance2) {
                            long suggestionCount = unigramLexicon.get(suggestion);
                            SuggestItem suggestItem = new SuggestItem(suggestion, distance, suggestionCount);
                            if (!suggestions.isEmpty()) {
                                switch (verbosity) {
                                    case CLOSEST:
                                        if (distance < maxEditDistance2) {
                                            suggestions.clear();
                                            break;
                                        }
                                    case TOP:
                                        if (distance < maxEditDistance2
                                                || suggestionCount
                                                > suggestions.get(0).getFrequencyOfSuggestionInDict()) {
                                            maxEditDistance2 = distance;
                                            suggestions.set(0, suggestItem);
                                        }
                                        continue;
                                    case ALL:
                                        break;
                                }
                            }
                            if (!verbosity.equals(ALL)) maxEditDistance2 = distance;
                            suggestions.add(suggestItem);
                        }
                    }
                }
            }

            // add edits
            if (lengthDiffBetweenInputAndCandidate < maxEditDistance && candidateLength <= prefixLength) {
                if (!verbosity.equals(ALL) && lengthDiffBetweenInputAndCandidate >= maxEditDistance2) continue;
                Set<String> newDeletes = generateDeletes(candidate);
                candidates.addAll(newDeletes);
            }
        }
        if (suggestions.size() > 1) {
            Collections.sort(suggestions);
        }
        if (includeUnknown && (suggestions.isEmpty())) {
            SuggestItem noSuggestionsFound = new SuggestItem(input, maxEditDistance + 1, 0);
            suggestions.add(noSuggestionsFound);
        }
        return suggestions;
    }

    private Set<String> generateDeletes(String candidate) {
        Set<String> newDeletes = new HashSet<>();
        for (int i = 0; i < candidate.length(); i++) {
            StringBuilder editableString = new StringBuilder(candidate);
            String delete = editableString.deleteCharAt(i).toString();
            newDeletes.add(delete);
        }
        return newDeletes;
    }

    public List<SuggestItem> lookupCompound(String input, int editDistanceMax, boolean includeUnknown) throws NotInitializedException {
        String[] termList = input.split(" ");
        List<SuggestItem> suggestionParts = new ArrayList<>();

        boolean lastCombination = false;

        for (int i = 0; i < termList.length; i++) {
            String currentToken = termList[i];
            List<SuggestItem> suggestionsForCurrentToken = lookup(currentToken, Verbosity.TOP, editDistanceMax, includeUnknown);

            if (i > 0 && !lastCombination) {
                SuggestItem bestSuggestion = suggestionParts.get(suggestionParts.size() - 1);
                Optional<SuggestItem> newSuggestion = combineWords(editDistanceMax, includeUnknown, currentToken, termList[i - 1], bestSuggestion, suggestionsForCurrentToken.isEmpty() ? null : suggestionsForCurrentToken.get(0));

                if (newSuggestion.isPresent()) {
                    suggestionParts.set(suggestionParts.size() - 1, newSuggestion.get());
                    lastCombination = true;
                    continue;
                }
            }

            lastCombination = false;

            if (!suggestionsForCurrentToken.isEmpty()) {
                boolean firstSuggestionIsPerfect = suggestionsForCurrentToken.get(0).getEditDistance() == 0;
                if (firstSuggestionIsPerfect || currentToken.length() == 1) {
                    suggestionParts.add(suggestionsForCurrentToken.get(0));
                } else {
                    splitWords(editDistanceMax, termList, suggestionsForCurrentToken, suggestionParts, i);
                }
            } else {
                splitWords(editDistanceMax, termList, suggestionsForCurrentToken, suggestionParts, i);
            }
        }
        double freq = N;
        StringBuilder stringBuilder = new StringBuilder();
        for (SuggestItem suggestItem : suggestionParts) {
            stringBuilder.append(suggestItem.getSuggestion()).append(" ");
            freq *= suggestItem.getFrequencyOfSuggestionInDict() / N;
        }

        String term = stringBuilder.toString().stripTrailing();
        SuggestItem suggestion = new SuggestItem(term, stringDistance.distanceWithEarlyStop(input, term, Integer.MAX_VALUE), freq);
        List<SuggestItem> suggestionsLine = new ArrayList<>();
        suggestionsLine.add(suggestion);
        return suggestionsLine;
    }

    private void splitWords(int editDistanceMax, String[] termList, List<SuggestItem> suggestions, List<SuggestItem> suggestionParts, int i) throws NotInitializedException {
        SuggestItem suggestionSplitBest = null;
        if (!suggestions.isEmpty()) suggestionSplitBest = suggestions.get(0);

        String word = termList[i];
        if (word.length() > 1) {
            for (int j = 1; j < word.length(); j++) {
                String part1 = word.substring(0, j);
                String part2 = word.substring(j);
                SuggestItem suggestionSplit;
                List<SuggestItem> suggestions1 = lookup(part1, Verbosity.TOP, editDistanceMax, false);
                if (!suggestions1.isEmpty()) {
                    List<SuggestItem> suggestions2 = lookup(part2, Verbosity.TOP, editDistanceMax, false);
                    if (!suggestions2.isEmpty()) {

                        Bigram splitTerm = new Bigram(suggestions1.get(0).getSuggestion(), suggestions2.get(0).getSuggestion());
                        int splitDistance = stringDistance.distanceWithEarlyStop(word, splitTerm.toString(), editDistanceMax);

                        if (splitDistance < 0) splitDistance = editDistanceMax + 1;

                        if (suggestionSplitBest != null) {
                            if (splitDistance > suggestionSplitBest.getEditDistance()) continue;
                            if (splitDistance < suggestionSplitBest.getEditDistance()) suggestionSplitBest = null;
                        }
                        double freq;
                        if (bigramLexicon.containsKey(splitTerm)) {
                            freq = bigramLexicon.get(splitTerm);

                            if (!suggestions.isEmpty()) {
                                if ((suggestions1.get(0).getSuggestion() + suggestions2.get(0).getSuggestion()).equals(word)) {
                                    freq = Math.max(freq, suggestions.get(0).getFrequencyOfSuggestionInDict() + 2);
                                } else if ((suggestions1.get(0)
                                                        .getSuggestion()
                                                        .equals(suggestions.get(0).getSuggestion())
                                        || suggestions2.get(0)
                                                       .getSuggestion()
                                                       .equals(suggestions.get(0).getSuggestion()))) {
                                    freq = Math.max(freq, suggestions.get(0).getFrequencyOfSuggestionInDict() + 1);
                                }

                            } else if ((suggestions1.get(0).getSuggestion() + suggestions2.get(0).getSuggestion()).equals(word)) {
                                freq = Math.max(freq, Math.max(suggestions1.get(0).getFrequencyOfSuggestionInDict(), suggestions2.get(0).getFrequencyOfSuggestionInDict()));
                            }
                        } else {
                            // The Naive Bayes probability of the word combination is the product of the two
                            // word probabilities: P(AB) = P(A) * P(B)
                            // use it to estimate the frequency count of the combination, which then is used
                            // to rank/select the best splitting variant
                            freq = Math.min(bigramCountMin, (long) ((suggestions1.get(0).getFrequencyOfSuggestionInDict() / (double) SymSpell.N) * suggestions2.get(0).getFrequencyOfSuggestionInDict()));
                        }
                        suggestionSplit = new SuggestItem(splitTerm.toString(), splitDistance, freq);

                        if (suggestionSplitBest == null || suggestionSplit.getFrequencyOfSuggestionInDict() > suggestionSplitBest.getFrequencyOfSuggestionInDict()){
                            suggestionSplitBest = suggestionSplit;
                        }
                    }
                }
            }
            if (suggestionSplitBest != null) {
                suggestionParts.add(suggestionSplitBest);
            } else {
                SuggestItem suggestItem = new SuggestItem(word, editDistanceMax + 1, estimatedWordOccurrenceProbability(word)); // estimated word occurrence probability P=10 / (N * 10^word length l)

                suggestionParts.add(suggestItem);
            }
        } else {
            SuggestItem suggestItem = new SuggestItem(word, editDistanceMax + 1, estimatedWordOccurrenceProbability(word));
            suggestionParts.add(suggestItem);
        }
    }

    private long estimatedWordOccurrenceProbability(String word) {
        return (long) ((double) 10 / Math.pow(10, word.length()));
    }

    Optional<SuggestItem> combineWords(int editDistanceMax, boolean includeUnknown, String token, String previousToken, SuggestItem suggestItem, SuggestItem secondBestSuggestion) throws NotInitializedException {
        List<SuggestItem> suggestionsCombination = lookup(previousToken + token, Verbosity.TOP, editDistanceMax, includeUnknown);
        if (!suggestionsCombination.isEmpty()) {
            SuggestItem best2;
            // TODO fixme
            best2 = Objects.requireNonNullElseGet(secondBestSuggestion, () -> new SuggestItem(token, editDistanceMax + 1, estimatedWordOccurrenceProbability(token)));

            int distance = suggestItem.getEditDistance() + best2.getEditDistance();

            SuggestItem firstSuggestion = suggestionsCombination.get(0);

            if (distance >= 0 && (firstSuggestion.getEditDistance() + 1 < distance)
                    || (firstSuggestion.getEditDistance() + 1 == distance
                    && firstSuggestion.getFrequencyOfSuggestionInDict()
                    > suggestItem.getFrequencyOfSuggestionInDict()
                    / N
                    * best2.getFrequencyOfSuggestionInDict())) {

                return Optional.of(new SuggestItem(
                        firstSuggestion.getSuggestion(),
                        firstSuggestion.getEditDistance(),
                        firstSuggestion.getFrequencyOfSuggestionInDict()));
            }
        }
        return Optional.empty();
    }

    public Map<String, Long> getUnigramLexicon() {
        return unigramLexicon;
    }

    Map<String, Collection<String>> getDeletes() {
        return deletes;
    }
}
