package io.gitlab.rxp90.jsymspell;

import java.util.Objects;

public class SuggestItem implements Comparable<SuggestItem> {
  private final String suggestion;
  private final int editDistance;
  private final long frequencyOfSuggestionInDict;

  public SuggestItem() {
    this("", 0, 0);
  }

  public SuggestItem(String suggestion, int editDistance, long frequencyOfSuggestionInDict) {
    this.suggestion = suggestion;
    this.editDistance = editDistance;
    this.frequencyOfSuggestionInDict = frequencyOfSuggestionInDict;
  }

  @Override
  public int compareTo(SuggestItem suggestItem) {
    if (this.editDistance == suggestItem.editDistance) {
      // Descending
      return Long.compare(suggestItem.frequencyOfSuggestionInDict, frequencyOfSuggestionInDict);
    } else {
      // Ascending
      return Integer.compare(editDistance, suggestItem.editDistance);
    }
  }

  public String getSuggestion() {
    return suggestion;
  }

  public int getEditDistance() {
    return editDistance;
  }

  public long getFrequencyOfSuggestionInDict() {
    return frequencyOfSuggestionInDict;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SuggestItem that = (SuggestItem) o;
    return editDistance == that.editDistance
        && frequencyOfSuggestionInDict == that.frequencyOfSuggestionInDict
        && Objects.equals(suggestion, that.suggestion);
  }

  @Override
  public int hashCode() {
    return suggestion.hashCode();
  }

  @Override
  public String toString() {
    return "SuggestItem{"
        + "suggestion='"
        + suggestion
        + '\''
        + ", editDistance="
        + editDistance
        + ", frequencyOfSuggestionInDict="
        + frequencyOfSuggestionInDict
        + '}';
  }
}