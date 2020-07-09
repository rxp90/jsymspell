package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.api.DefaultStringHasher;
import io.gitlab.rxp90.jsymspell.api.StringHasher;

public class SymSpellBuilder {

    private int maxDictionaryEditDistance = 2;
    private int prefixLength = 7;
    private int countThreshold = 1;
    private StringHasher stringHasher = new DefaultStringHasher();

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
                stringHasher);
    }
}
