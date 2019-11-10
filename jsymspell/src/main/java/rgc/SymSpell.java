package rgc;

import static rgc.SymSpell.Verbosity.ALL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SymSpell {

  private final int initialCapacity;
  private final int maxDictionaryEditDistance;
  private final int prefixLength;
  private final int countThreshold;
  private final byte compactLevel;

  private final LongToStringArrayMap deletes;
  private final StringToLongMap words;
  private final Map<String, Long> belowThresholdWords = new HashMap<>();
  private final EditDistance damerauLevenshteinOSA;

  private final StringHasher stringHasher;

  private Pattern wordPattern = Pattern.compile("['’\\w-[_]]+");
  private int maxDictionaryWordLength;

  public enum Verbosity {
    TOP,
    CLOSEST,
    ALL
  }

  SymSpell(
      int initialCapacity,
      int maxDictionaryEditDistance,
      int prefixLength,
      int countThreshold,
      byte compactLevel,
      StringHasher stringHasher,
      LongToStringArrayMap deletes,
      StringToLongMap words) {
    this.initialCapacity = initialCapacity;
    this.maxDictionaryEditDistance = maxDictionaryEditDistance;
    this.prefixLength = prefixLength;
    this.countThreshold = countThreshold;
    this.compactLevel = compactLevel;
    this.stringHasher = stringHasher;
    this.deletes = deletes;
    this.words = words;
    damerauLevenshteinOSA = new DamerauLevenshteinOSA();
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

  private List<String> parseWords(String text) {
    Matcher matcher = wordPattern.matcher(text.toLowerCase());
    List<String> words = new ArrayList<>();
    if (matcher.find()) {
      for (int group = 0; group < matcher.groupCount(); group++) {
        String word = matcher.group(group);
        words.add(word);
      }
    }
    return words;
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

  public boolean loadDictionary(Collection<String> corpus, int termIndex, int countIndex) {
    SuggestionStage staging = new SuggestionStage(16384);
    corpus.forEach(
        line -> {
          String[] parts = line.split(",");
          String key = parts[termIndex];
          String count = parts[countIndex];
          try {
            if (key == null) {
              throw new IllegalArgumentException("Key is null in the following line: " + line);
            }
            Long countAsLong = Long.parseLong(count);
            createDictionaryEntry(key, countAsLong, staging);
          } catch (Exception e) {
            System.err.println(e.getMessage());
          }
        });
    commitStaged(staging);
    return true;
  }

  private void commitStaged(SuggestionStage staging) {
    staging.commitTo(deletes);
  }

  private boolean createDictionaryEntry(String key, Long count, SuggestionStage staging) {
    if (count <= 0) {
      if (countThreshold > 0) {
        return false;
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
        return false;
      }
    } else {
      if (words.contains(key)) {
        countPrevious = words.get(key);
        count = (Long.MAX_VALUE - countPrevious > count) ? countPrevious + count : Long.MAX_VALUE;
        words.put(key, count);
        return false;
      } else if (count < countThreshold) {
        belowThresholdWords.put(key, count);
        return false;
      }
      words.put(key, count);

      if (key.length() > maxDictionaryWordLength) {
        maxDictionaryWordLength = key.length();
      }

      // Create deletes

      var edits = editsPrefix(key);

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
    return true;
  }

  public List<SuggestItem> lookup(String input, Verbosity verbosity) {
    return lookup(input, verbosity, this.maxDictionaryEditDistance, false);
  }

  private List<SuggestItem> lookup(
      String input, Verbosity verbosity, int maxEditDistance, boolean includeUnknown) {
    if (maxEditDistance > maxDictionaryEditDistance) {
      throw new IllegalArgumentException("maxEditDistance > maxDictionaryEditDistance");
    }

    List<SuggestItem> suggestions = new ArrayList<>();
    int inputLen = input.length();
    if (inputLen - maxEditDistance > maxDictionaryWordLength) {
      return Collections.emptyList();
    }

    long suggestionCount;
    if (words.contains(input)) {
      suggestions.add(new SuggestItem(input, 0,  words.get(input)));
      if (!Verbosity.ALL.equals(verbosity)) {
        return suggestions;
      }
    }

    if (maxEditDistance == 0) {
      return suggestions;
    }

    Set<String> deletesAlreadyConsidered = new HashSet<>();
    Set<String> suggestionsAlreadyConsidered = new HashSet<>();

    suggestionsAlreadyConsidered.add(input);

    int maxEditDistance2 = maxEditDistance;
    int candidatePointer = 0;
    var singleSuggestion = new String[] {""};
    List<String> candidates = new ArrayList<>();

    int inputPrefixLen = inputLen;
    if (inputPrefixLen > prefixLength) {
      inputPrefixLen = prefixLength;
      candidates.add(input.substring(0, inputPrefixLen));
    } else {
      candidates.add(input);
    }

    while (candidatePointer < candidates.size()) {
      String candidate = candidates.get(candidatePointer++);
      int candidateLength = candidate.length();
      int lengthDiff = inputPrefixLen - candidateLength;

      if (lengthDiff > maxEditDistance2) {
        if (verbosity.equals(Verbosity.ALL)) {
          continue;
        } else {
          break;
        }
      }

      String[] dictSuggestions = deletes.get(stringHasher.hash(candidate));
      if (dictSuggestions != null) {
        for (String suggestion : dictSuggestions) {
          if (suggestion.equals(input)) continue;

          int suggestionLen = suggestion.length();

          if ((Math.abs(suggestionLen - inputLen) > maxEditDistance2)
              || (suggestionLen < candidateLength)
              || (suggestionLen == candidateLength && !suggestion.equals(candidate))) {
            continue;
          }
          int suggestionPrefixLen = Math.min(suggestionLen, prefixLength);
          if (suggestionPrefixLen > inputPrefixLen
              && (suggestionPrefixLen - candidateLength) > maxEditDistance2) {
            continue;
          }

          int distance;
          int min = 0;
          if (candidateLength == 0) {
            distance = Math.max(inputLen, suggestionLen);
            if (distance > maxEditDistance2 || !suggestionsAlreadyConsidered.add(suggestion)) {
              continue;
            }
          } else if (suggestionLen == 1) {
            if (input.indexOf(suggestion.charAt(0)) < 0) {
              distance = inputLen;
            } else {
              distance = inputLen - 1;
            }
            if (distance > maxEditDistance2 || !suggestionsAlreadyConsidered.add(suggestion))
              continue;
          } else {

            if ((prefixLength - maxEditDistance == candidateLength)
                    && (((min = Math.min(inputLen, suggestionLen) - prefixLength) > 1)
                        && (!input
                            .substring(inputLen + 1 - min)
                            .equals(suggestion.substring(suggestionLen + 1 - min))))
                || ((min > 0)
                    && (input.charAt(inputLen - min) != suggestion.charAt(suggestionLen - min))
                    && ((input.charAt(inputLen - min - 1) != suggestion.charAt(suggestionLen - min))
                        || (input.charAt(inputLen - min)
                            != suggestion.charAt(suggestionLen - min - 1))))) {
              continue;
            } else {
              if ((!verbosity.equals(Verbosity.ALL)
                      && deleteSuggestionPrefix(
                          candidate, candidateLength, suggestion, suggestionLen))
                  || !suggestionsAlreadyConsidered.add(suggestion)) continue;
              distance = damerauLevenshteinOSA.distance(input, suggestion, maxEditDistance2);
              if (distance < 0) continue;
            }

            if (distance <= maxEditDistance2) {
              suggestionCount = words.get(suggestion);
              SuggestItem suggestItem = new SuggestItem(suggestion, distance, suggestionCount);
              if (suggestions.size() > 0) {
                switch (verbosity) {
                  case CLOSEST:
                    if (distance < maxEditDistance2) {
                      suggestions.clear();
                      break;
                    }
                  case TOP:
                    if (distance < maxEditDistance2
                        || suggestionCount > suggestions.get(0).getFrequencyOfSuggestionInDict()) {
                      maxEditDistance2 = distance;
                      suggestions.set(0, suggestItem);
                    }
                    continue;
                }
              }
              if (!verbosity.equals(ALL)) maxEditDistance2 = distance;
              suggestions.add(suggestItem);
            }
          }
        }
      }

      // add edits
      if (lengthDiff < maxEditDistance && candidateLength <= prefixLength) {
        if (!verbosity.equals(ALL) && lengthDiff >= maxEditDistance2) continue;
        for (int i = 0; i < candidateLength; i++) {
          StringBuilder editableString = new StringBuilder(candidate);
          String delete = editableString.deleteCharAt(i).toString();
          if (deletesAlreadyConsidered.add(delete)) {
            candidates.add(delete);
          }
        }
      }
    }
    if (suggestions.size() > 1) {
      Collections.sort(suggestions);
    }
    if (includeUnknown && (suggestions.size() == 0)) {
      SuggestItem noSuggestionsFound = new SuggestItem(input, maxEditDistance + 1, 0);
      suggestions.add(noSuggestionsFound);
    }
    return suggestions;
  }
}
