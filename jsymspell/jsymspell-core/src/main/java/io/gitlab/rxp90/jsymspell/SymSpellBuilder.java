package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.api.DefaultStringHasher;
import io.gitlab.rxp90.jsymspell.api.StringHasher;

import java.util.HashMap;
import java.util.Map;

public class SymSpellBuilder {

    private int maxDictionaryEditDistance = 2;
    private int prefixLength = 7;
    private int countThreshold = 1;
    private StringHasher stringHasher = new DefaultStringHasher();
    private Map<String, Long> unigramLexicon = new HashMap<>();
    private Map<Bigram, Long> bigramLexicon = new HashMap<>();

    public SymSpellBuilder setStringHasher(StringHasher stringHasher) {
        this.stringHasher = stringHasher;
        return this;
    }

    public SymSpellBuilder setMaxDictionaryEditDistance(int maxDictionaryEditDistance) {
        this.maxDictionaryEditDistance = maxDictionaryEditDistance;
        return this;
    }

    public SymSpellBuilder setPrefixLength(int prefixLength) {
        this.prefixLength = prefixLength;
        return this;
    }

    public SymSpellBuilder setCountThreshold(int countThreshold) {
        this.countThreshold = countThreshold;
        return this;
    }

    public SymSpellBuilder setUnigramLexicon(Map<String, Long> unigramLexicon) {
        this.unigramLexicon = new HashMap<>(unigramLexicon);
        return this;
    }

    public SymSpellBuilder setBigramLexicon(Map<Bigram, Long> bigramLexicon) {
        this.bigramLexicon = new HashMap<>(bigramLexicon);
        return this;
    }

    public SymSpell createSymSpell() {
        return new SymSpell(maxDictionaryEditDistance, prefixLength, stringHasher, unigramLexicon, bigramLexicon);
    }
}
