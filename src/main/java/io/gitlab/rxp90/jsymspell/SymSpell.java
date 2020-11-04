package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.api.Bigram;
import io.gitlab.rxp90.jsymspell.api.SuggestItem;
import io.gitlab.rxp90.jsymspell.exceptions.NotInitializedException;

import java.util.List;
import java.util.Map;

/**
 * Port of SymSpell: 1 million times faster spelling correction and fuzzy search through Symmetric Delete spelling correction algorithm
 *
 * @see <a href="https://github.com/wolfgarbe/SymSpell">https://github.com/wolfgarbe/SymSpell</a>
 */
public interface SymSpell {
    /**
     * Returns a sorted {@code List} of {@code SuggestItem} for a given {@code input}
     * @param input string to apply spelling correction to
     * @param verbosity see {@link Verbosity}
     * @param includeUnknown controls whether non-lexicon words should be considered
     * @return sorted {@code List} of {@code SuggestItem} for a given {@code input}
     * @throws NotInitializedException if no unigram lexicon has been provided, i.e. {@link SymSpell#getUnigramLexicon} is empty
     */
    List<SuggestItem> lookup(String input, Verbosity verbosity, boolean includeUnknown) throws NotInitializedException;

    /**
     * Same as {@link SymSpell#lookup(String, Verbosity, boolean)} where {@code includeUnknown} is false
     * @see SymSpell#lookup(String, Verbosity, boolean)
     * @param input string to apply spelling correction to
     * @param verbosity see {@link Verbosity}
     * @return sorted {@code List} of {@code SuggestItem} for a given {@code input}
     * @throws NotInitializedException if no unigram lexicon has been provided, i.e. {@link SymSpell#getUnigramLexicon} is empty
     */
    List<SuggestItem> lookup(String input, Verbosity verbosity) throws NotInitializedException;

    /**
     * Performs spelling correction of multiple space separated words.
     * @param input string to apply spelling correction to, where words are separated by spaces
     * @param editDistanceMax limit up to which lexicon words can be considered suggestions, must be lower or equal than {@link SymSpell#getMaxDictionaryEditDistance()}
     * @param includeUnknown controls whether non-lexicon words should be considered
     * @return sorted {@code List} of {@code SuggestItem} for a given {@code input}
     * @throws NotInitializedException if no unigram, and/or bigram lexicon has been provided, i.e. {@link SymSpell#getUnigramLexicon} is empty, and/or {@link SymSpell#getBigramLexicon()} is empty
     */
    List<SuggestItem> lookupCompound(String input, int editDistanceMax, boolean includeUnknown) throws NotInitializedException;

    /**
     * Map where the key is a word of the lexicon and the value is the frequency.
     * @return map where the key is a word of the lexicon and the value is the frequency
     */
    Map<String, Long> getUnigramLexicon();

    /**
     * Map where the key is a pair of words of the lexicon and the value is the frequency.
     * @see Bigram
     * @return map where the key is a pair of words of the lexicon and the value is the frequency
     */
    Map<Bigram, Long> getBigramLexicon();

    /**
     * Limit up to which lexicon words can be considered suggestions.
     * @return limit up to which lexicon words can be considered suggestions
     */
    int getMaxDictionaryEditDistance();
}
