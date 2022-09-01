package io.gitlab.rxp90.jsymspell.api;

import java.util.AbstractMap;

/**
 * Holds a pair of words.
 */
public class Bigram extends AbstractMap.SimpleImmutableEntry<String, String> {
    /**
     * Constructs a bigram with the specified words.
     *
     * @param word1 first word
     * @param word2 second word
     */
    public Bigram(final String word1, final String word2) {
        super(word1, word2);
    }

    @Override
    public String toString() {
        return getKey() + ' ' + getValue();
    }
}
