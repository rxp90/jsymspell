package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.api.Bigram;
import io.gitlab.rxp90.jsymspell.api.DamerauLevenshteinOSA;
import io.gitlab.rxp90.jsymspell.api.StringDistance;
import io.gitlab.rxp90.jsymspell.exceptions.NotInitializedException;

import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for constructing an instance of {@link SymSpell}. By default,
 * the unigram lexicon is empty and must be filled with one more or entries
 * added using {@link #setUnigramLexicon(Map)}.
 */
public class SymSpellBuilder {

    private int maxDictionaryEditDistance = 2;
    private int prefixLength = 7;
    private StringDistance stringDistanceAlgorithm = new DamerauLevenshteinOSA();
    private final Map<String, Long> unigramLexicon = new HashMap<>();
    private final Map<Bigram, Long> bigramLexicon = new HashMap<>();

    public SymSpellBuilder setMaxDictionaryEditDistance(int maxDictionaryEditDistance) {
        this.maxDictionaryEditDistance = maxDictionaryEditDistance;
        return this;
    }

    public SymSpellBuilder setPrefixLength(int prefixLength) {
        this.prefixLength = prefixLength;
        return this;
    }

    /**
     * Appends the given set of unigrams to the lexicon. This must be called
     * at least once with one or more values in the {@code unigramLexicon}.
     *
     * @param unigramLexicon The list of words to add to the lexicon.
     * @return {@code this}
     */
    public SymSpellBuilder setUnigramLexicon(Map<String, Long> unigramLexicon) {
        assert unigramLexicon != null;
        assert !unigramLexicon.isEmpty();

        this.unigramLexicon.putAll(unigramLexicon);
        return this;
    }

    public SymSpellBuilder setBigramLexicon(Map<Bigram, Long> bigramLexicon) {
        assert bigramLexicon != null;
        assert !bigramLexicon.isEmpty();

        this.bigramLexicon.putAll(bigramLexicon);
        return this;
    }

    public SymSpellBuilder setStringDistanceAlgorithm(StringDistance distanceAlgorithm){
        assert distanceAlgorithm != null;

        this.stringDistanceAlgorithm = distanceAlgorithm;
        return this;
    }

    public int getMaxDictionaryEditDistance() {
        return maxDictionaryEditDistance;
    }

    public int getPrefixLength() {
        return prefixLength;
    }

    public Map<String, Long> getUnigramLexicon() {
        return unigramLexicon;
    }

    public Map<Bigram, Long> getBigramLexicon() {
        return bigramLexicon;
    }

    public StringDistance getStringDistanceAlgorithm() {
        return stringDistanceAlgorithm;
    }

    /**
     * Responsible for creating an object that implements the {@link SymSpell}
     * contract.
     *
     * @return A new spelling instance for alternative spelling suggestions.
     * @throws NotInitializedException The unigram lexicon is missing.
     */
    public SymSpellImpl createSymSpell() throws NotInitializedException {
        // Prevent creation of the speller instance if there's no lexicon.
        if( unigramLexicon.isEmpty() ) {
            throw new NotInitializedException( "Missing unigram lexicon" );
        }

        return new SymSpellImpl(this);
    }

    /**
     * Alias.
     *
     * @see #createSymSpell()
     */
    public SymSpellImpl build() throws NotInitializedException {
        return createSymSpell();
    }
}
