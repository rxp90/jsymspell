package io.gitlab.rxp90.jsymspell.api;

import java.util.Objects;

public final class SuggestItem implements Comparable<SuggestItem> {
    private final String suggestion;
    private final int editDistance;
    private final double frequencyOfSuggestionInDict;

    public SuggestItem(String suggestion, int editDistance, double frequencyOfSuggestionInDict) {
        this.suggestion = suggestion;
        this.editDistance = editDistance;
        this.frequencyOfSuggestionInDict = frequencyOfSuggestionInDict;
    }

    /**
     * Compares this {@code SuggestItem} with the specified {@code SuggestItem}.
     * It will first sort by {@link SuggestItem#getEditDistance()}, and then by {@link SuggestItem#getFrequencyOfSuggestionInDict()}
     * @param suggestItem {@code SuggestItem} to which this {@code SuggestItem} is to be compared.
     * @return 0 if this {@code SuggestItem}'s edit distance, and frequency of suggestion are the same as {@code suggestItem}'s
     *         1 if this {@code SuggestItem}'s edit distance is greater than {@code suggestItem}'s, or if they are equal, this {@code SuggestItem}'s frequency of suggestion is lower
     *         -1 if this {@code SuggestItem}'s edit distance is lower than {@code suggestItem}'s, or if it's equal and the frequency is greater
     */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SuggestItem that = (SuggestItem) o;
        return editDistance == that.editDistance &&
                Double.compare(that.frequencyOfSuggestionInDict, frequencyOfSuggestionInDict) == 0 &&
                Objects.equals(suggestion, that.suggestion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(suggestion, editDistance, frequencyOfSuggestionInDict);
    }

    public String getSuggestion() {
        return suggestion;
    }

    public int getEditDistance() {
        return editDistance;
    }

    public double getFrequencyOfSuggestionInDict() {
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
