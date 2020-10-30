package io.gitlab.rxp90.jsymspell;

import java.util.HashMap;
import java.util.Map;

public class SymSpellBuilder {

    private int maxDictionaryEditDistance = 2;
    private int prefixLength = 7;
    private Map<String, Long> unigramLexicon = new HashMap<>();
    private Map<Bigram, Long> bigramLexicon = new HashMap<>();

    public SymSpellBuilder setMaxDictionaryEditDistance(int maxDictionaryEditDistance) {
        this.maxDictionaryEditDistance = maxDictionaryEditDistance;
        return this;
    }

    public SymSpellBuilder setPrefixLength(int prefixLength) {
        this.prefixLength = prefixLength;
        return this;
    }

    public SymSpellBuilder setUnigramLexicon(Map<String, Long> unigramLexicon) {
        this.unigramLexicon = unigramLexicon;
        return this;
    }

    public SymSpellBuilder setBigramLexicon(Map<Bigram, Long> bigramLexicon) {
        this.bigramLexicon = bigramLexicon;
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

    public SymSpell createSymSpell() {
        return new SymSpell(this);
    }
}
