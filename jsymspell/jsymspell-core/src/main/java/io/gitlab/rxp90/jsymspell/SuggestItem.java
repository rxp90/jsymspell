package io.gitlab.rxp90.jsymspell;

public final class SuggestItem implements Comparable<SuggestItem> {
  private final String suggestion;
  private final int editDistance;
  private final double frequencyOfSuggestionInDict;

  SuggestItem(String suggestion, int editDistance, double frequencyOfSuggestionInDict) {
    this.suggestion = suggestion;
    this.editDistance = editDistance;
    this.frequencyOfSuggestionInDict = frequencyOfSuggestionInDict;
  }

  @Override
  public int compareTo(SuggestItem suggestItem) {
    if (this.editDistance == suggestItem.editDistance) {
      // Descending
      return Double.compare(suggestItem.frequencyOfSuggestionInDict, frequencyOfSuggestionInDict);
    } else {
      // Ascending
      return Integer.compare(editDistance, suggestItem.editDistance);
    }
  }

  public String getSuggestion() {
    return suggestion;
  }

  int getEditDistance() {
    return editDistance;
  }

  double getFrequencyOfSuggestionInDict() {
    return frequencyOfSuggestionInDict;
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
