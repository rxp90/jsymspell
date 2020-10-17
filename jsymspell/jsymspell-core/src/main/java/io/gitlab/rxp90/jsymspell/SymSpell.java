package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.api.DamerauLevenshteinOSA;
import io.gitlab.rxp90.jsymspell.api.StringDistance;
import io.gitlab.rxp90.jsymspell.api.StringHasher;
import io.gitlab.rxp90.jsymspell.exceptions.JSymSpellException;
import io.gitlab.rxp90.jsymspell.exceptions.NotInitializedException;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.gitlab.rxp90.jsymspell.SymSpell.Verbosity.ALL;

public class SymSpell {

    private static final Logger logger = Logger.getLogger(SymSpell.class.getName());

    private final int maxDictionaryEditDistance;
    private final int prefixLength;
    private final int countThreshold;

    private final Map<Long, String[]> deletes = new HashMap<>();
    private final Map<String, Long> words = new HashMap<>();
    private final Map<String, Long> bigrams = new HashMap<>();
    private final Map<String, Long> belowThresholdWords = new HashMap<>();
    private final StringDistance stringDistance;

    private final StringHasher stringHasher;

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

    SymSpell(
            int maxDictionaryEditDistance,
            int prefixLength,
            int countThreshold,
            StringHasher stringHasher) {
        this.maxDictionaryEditDistance = maxDictionaryEditDistance;
        this.prefixLength = prefixLength;
        this.countThreshold = countThreshold;
        this.stringHasher = stringHasher;;
        stringDistance = new DamerauLevenshteinOSA();
    }

    private boolean deleteSuggestionPrefix(
            String delete, int deleteLen, String suggestion, int suggestionLen) {
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
                if (deleteWords.add(delete)) {
                    if (editDistance < maxDictionaryEditDistance) {
                        edits(delete, editDistance, deleteWords);
                    }
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

    public void loadDictionary(Collection<String> corpus, int termIndex, int countIndex) throws JSymSpellException {
        SuggestionStage staging = new SuggestionStage(16384);
        for (String line : corpus) {
            String[] parts = line.split(",");
            String key = parts[termIndex];
            String count = parts[countIndex];
            try {
                if (key == null) {
                    throw new JSymSpellException("Key is null in the following line: " + line);
                }
                Long countAsLong = Long.parseLong(count);
                createDictionaryEntry(key, countAsLong, staging);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Something went wrong loading the dictionary", e);
                throw new JSymSpellException("Couldn't load dictionary", e);
            }
        }
        commitStaged(staging);
        logger.log(Level.INFO, "Word dictionary loaded");
    }

    public void loadBigramDictionary(Collection<String> corpus, int termIndex, int countIndex) throws JSymSpellException {
        for (String line : corpus) {
            String[] parts = line.split(" ");
            String key = parts[termIndex] + " " + parts[termIndex + 1];
            String count = parts[countIndex];
            try {
                long countAsLong = Long.parseLong(count);
                bigrams.put(key, countAsLong);
                if (countAsLong < bigramCountMin) bigramCountMin = countAsLong;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Something went wrong loading the bigram dictionary", e);
                throw new JSymSpellException("Couldn't load bigram dictionary", e);
            }
        }
        logger.log(Level.INFO, "Bigram dictionary loaded");
    }

    private void commitStaged(SuggestionStage staging) {
        staging.commitTo(deletes);
    }

    private void createDictionaryEntry(String key, Long count, SuggestionStage staging) {
        if (count <= 0) {
            if (countThreshold > 0) {
                return;
            }
            count = 0L;
        }

        Long countPrevious = belowThresholdWords.get(key);

        if (countThreshold > 1 && countPrevious != null) {

            count = (Long.MAX_VALUE - countPrevious > count) ? countPrevious + count : Long.MAX_VALUE;

            if (count >= countThreshold) {
                belowThresholdWords.remove(key);
            } else {
                belowThresholdWords.put(key, count);
            }
        } else {
            if (words.containsKey(key)) {
                countPrevious = words.get(key);
                count = (Long.MAX_VALUE - countPrevious > count) ? countPrevious + count : Long.MAX_VALUE;
                words.put(key, count);
                return;
            } else if (count < countThreshold) {
                belowThresholdWords.put(key, count);
                return;
            }
            words.put(key, count);

            if (key.length() > maxDictionaryWordLength) {
                maxDictionaryWordLength = key.length();
            }
            generateDeletes(key, staging);
        }
    }

    private void generateDeletes(String key, SuggestionStage staging) {
        Set<String> edits = editsPrefix(key);

        if (staging != null) {
            edits.forEach(delete -> staging.add(stringHasher.hash(delete), key));
        } else {
            edits.forEach(
                    delete -> {
                        long deleteHash = stringHasher.hash(delete);
                        String[] suggestions = deletes.get(deleteHash);
                        if (suggestions != null) {
                            var newSuggestions = Arrays.copyOf(suggestions, suggestions.length + 1);
                            deletes.put(deleteHash, newSuggestions);
                            suggestions = newSuggestions;
                        } else {
                            suggestions = new String[1];
                            deletes.put(deleteHash, suggestions);
                        }
                        suggestions[suggestions.length - 1] = key;
                    });
        }
    }

    public List<SuggestItem> lookup(String input, Verbosity verbosity) throws NotInitializedException {
        return lookup(input, verbosity, this.maxDictionaryEditDistance, false);
    }

    private List<SuggestItem> lookup(
            String input, Verbosity verbosity, int maxEditDistance, boolean includeUnknown) throws NotInitializedException {
        if (maxEditDistance > maxDictionaryEditDistance) {
            throw new IllegalArgumentException("maxEditDistance > maxDictionaryEditDistance");
        }

        if (words.isEmpty()) {
            throw new NotInitializedException(
                    "There are no words in the dictionary. Please, call `loadDictionary` to add words.");
        }

        List<SuggestItem> suggestions = new ArrayList<>();
        int inputLen = input.length();
        boolean wordIsTooLong = inputLen - maxEditDistance > maxDictionaryWordLength;
        if (wordIsTooLong) {
            if (includeUnknown) {
                return List.of(new SuggestItem(input, maxEditDistance + 1, 0));
            }
        }

        long suggestionCount;
        if (words.containsKey(input)) {
            SuggestItem suggestSameWord = new SuggestItem(input, 0, words.get(input));
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
        int candidatePointer = 0;

        List<String> candidates = new ArrayList<>();

        int inputPrefixLen;
        if (inputLen > prefixLength) {
            inputPrefixLen = prefixLength;
            candidates.add(input.substring(0, inputPrefixLen));
        } else {
            inputPrefixLen = inputLen;
        }
        candidates.add(input);

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

            String[] dictSuggestions = deletes.get(stringHasher.hash(candidate));
            if (dictSuggestions != null) {
                for (String suggestion : dictSuggestions) {
                    if (suggestion != null) {
                        if (suggestion.equals(input)) continue;

                        int suggestionLen = suggestion.length();
                        // Filter on equivalence
                        if ((Math.abs(suggestionLen - inputLen) > maxEditDistance2)
                                || (suggestionLen < candidateLength)
                                || (suggestionLen == candidateLength && !suggestion.equals(candidate))) {
                            continue;
                        }
                        // Filter on prefix len
                        int suggestionPrefixLen = Math.min(suggestionLen, prefixLength);
                        if (suggestionPrefixLen > inputPrefixLen
                                && (suggestionPrefixLen - candidateLength) > maxEditDistance2) {
                            continue;
                        }

                        int distance;
                        if (candidateLength == 0) {
                            distance = Math.max(inputLen, suggestionLen);
                            if (distance <= maxEditDistance2) {
                                suggestionsAlreadyConsidered.add(suggestion);
                            }
                        } else if (suggestionLen == 1) {
                            if (input.indexOf(suggestion.charAt(0)) < 0) {
                                distance = inputLen;
                            } else {
                                distance = inputLen - 1;
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
                                suggestionCount = words.get(suggestion);
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

    Map<Long, String[]> getDeletes() {
        return deletes;
    }

    public List<SuggestItem> lookupCompound(String input, int editDistanceMax, boolean includeUnknown) throws NotInitializedException {
        String[] termList = input.split(" ");
        List<SuggestItem> suggestionParts = new ArrayList<>();
        StringDistance stringDistance = new DamerauLevenshteinOSA();

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
        SuggestItem suggestion =
                new SuggestItem(term, stringDistance.distanceWithEarlyStop(input, term, Integer.MAX_VALUE), freq);
        List<SuggestItem> suggestionsLine = new ArrayList<>();
        suggestionsLine.add(suggestion);
        return suggestionsLine;
    }

    private void splitWords(
            int editDistanceMax,
            String[] termList,
            List<SuggestItem> suggestions,
            List<SuggestItem> suggestionParts,
            int i) throws NotInitializedException {
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

                        String splitTerm =
                                suggestions1.get(0).getSuggestion() + " " + suggestions2.get(0).getSuggestion();
                        int splitDistance = stringDistance.distanceWithEarlyStop(word, splitTerm, editDistanceMax);

                        if (splitDistance < 0) splitDistance = editDistanceMax + 1;

                        if (suggestionSplitBest != null) {
                            if (splitDistance > suggestionSplitBest.getEditDistance()) continue;
                            if (splitDistance < suggestionSplitBest.getEditDistance()) suggestionSplitBest = null;
                        }
                        double freq;
                        if (bigrams.containsKey(splitTerm)) {
                            freq = bigrams.get(splitTerm);

                            if (!suggestions.isEmpty()) {
                                if ((suggestions1.get(0).getSuggestion() + suggestions2.get(0).getSuggestion())
                                        .equals(word)) {
                                    freq = Math.max(freq, suggestions.get(0).getFrequencyOfSuggestionInDict() + 2);
                                } else if ((suggestions1
                                        .get(0)
                                        .getSuggestion()
                                        .equals(suggestions.get(0).getSuggestion())
                                        || suggestions2
                                        .get(0)
                                        .getSuggestion()
                                        .equals(suggestions.get(0).getSuggestion()))) {
                                    freq = Math.max(freq, suggestions.get(0).getFrequencyOfSuggestionInDict() + 1);
                                }

                            } else if ((suggestions1.get(0).getSuggestion() + suggestions2.get(0).getSuggestion())
                                    .equals(word)) {
                                freq =
                                        Math.max(
                                                freq,
                                                Math.max(
                                                        suggestions1.get(0).getFrequencyOfSuggestionInDict(),
                                                        suggestions2.get(0).getFrequencyOfSuggestionInDict()));
                            }
                        } else {
                            // The Naive Bayes probability of the word combination is the product of the two
                            // word probabilities: P(AB) = P(A) * P(B)
                            // use it to estimate the frequency count of the combination, which then is used
                            // to rank/select the best splitting variant
                            freq =
                                    Math.min(
                                            bigramCountMin,
                                            (long)
                                                    ((suggestions1.get(0).getFrequencyOfSuggestionInDict()
                                                            / (double) SymSpell.N)
                                                            * suggestions2.get(0).getFrequencyOfSuggestionInDict()));
                        }
                        suggestionSplit = new SuggestItem(splitTerm, splitDistance, freq);

                        if (suggestionSplitBest == null
                                || suggestionSplit.getFrequencyOfSuggestionInDict()
                                > suggestionSplitBest.getFrequencyOfSuggestionInDict())
                            suggestionSplitBest = suggestionSplit;
                    }
                }
            }
            if (suggestionSplitBest != null) {
                suggestionParts.add(suggestionSplitBest);
            } else {
                SuggestItem suggestItem =
                        new SuggestItem(
                                word,
                                editDistanceMax + 1,
                                (long) ((double) 10 / Math.pow(10, word.length()))); // estimated word occurrence probability P=10 / (N * 10^word length l)

                suggestionParts.add(suggestItem);
            }
        } else {
            SuggestItem suggestItem =
                    new SuggestItem(
                            word,
                            editDistanceMax + 1,
                            (long) ((double) 10 / Math.pow(10, word.length())));
            suggestionParts.add(suggestItem);
        }
    }

    Optional<SuggestItem> combineWords(
            int editDistanceMax,
            boolean includeUnknown,
            String token,
            String previousToken,
            SuggestItem suggestItem,
            SuggestItem secondBestSuggestion) throws NotInitializedException {

        List<SuggestItem> suggestionsCombination =
                lookup(previousToken + token, Verbosity.TOP, editDistanceMax, includeUnknown);
        if (!suggestionsCombination.isEmpty()) {
            SuggestItem best2;
            if (secondBestSuggestion != null) {
                best2 = secondBestSuggestion;
            } else {
                long estimatedWordOccurrenceProbability = // TODO fixme
                        (long) ((double) 10 / Math.pow(10, token.length())); // P=10 / (N * 10^word length l)
                best2 = new SuggestItem(token, editDistanceMax + 1, estimatedWordOccurrenceProbability);
            }

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

    Map<String, Long> getWords() {
        return words;
    }
}
