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
    private Map<Long, String[]> deletes = new HashMap<>();
    private Map<String, Long> words = new HashMap<>();
    private Map<String, Long> bigrams = new HashMap<>();

    public SymSpellBuilder setDeletesMapWrapper(Map<Long, String[]> mapWrapper) {
        this.deletes = mapWrapper;
        return this;
    }

    public SymSpellBuilder setWordsMapWrapper(Map<String, Long> mapWrapper) {
        this.words = mapWrapper;
        return this;
    }

    public SymSpellBuilder setBigramsMapWrapper(Map<String, Long> mapWrapper) {
        this.bigrams = mapWrapper;
        return this;
    }


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


    public SymSpell createSymSpell() {
        return new SymSpell(
                maxDictionaryEditDistance,
                prefixLength,
                countThreshold,
                stringHasher,
                deletes,
                words,
                bigrams);
    }
}
