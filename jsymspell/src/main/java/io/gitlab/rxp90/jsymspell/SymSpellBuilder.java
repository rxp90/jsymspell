package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.api.Bigram;
import io.gitlab.rxp90.jsymspell.api.DamerauLevenshteinOSA;
import io.gitlab.rxp90.jsymspell.api.StringDistance;

import java.util.HashMap;
import java.util.Map;

public class SymSpellBuilder {

    private int maxDictionaryEditDistance = 2;
    private int prefixLength = 7;
    private StringDistance stringDistanceAlgorithm = new DamerauLevenshteinOSA();
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

    public SymSpellBuilder setStringDistanceAlgorithm(StringDistance distanceAlgorithm){
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

    public SymSpellImpl createSymSpell() {
        return new SymSpellImpl(this);
    }
}
