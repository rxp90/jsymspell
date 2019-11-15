package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.api.DefaultLongToStringArrayMap;
import io.gitlab.rxp90.jsymspell.api.DefaultStringHasher;
import io.gitlab.rxp90.jsymspell.api.DefaultStringToLongMap;
import io.gitlab.rxp90.jsymspell.api.LongToStringArrayMap;
import io.gitlab.rxp90.jsymspell.api.StringHasher;
import io.gitlab.rxp90.jsymspell.api.StringToLongMap;

public class SymSpellBuilder {

  private int maxDictionaryEditDistance = 2;
  private int prefixLength = 7;
  private int countThreshold = 1;
  private StringHasher stringHasher = new DefaultStringHasher();
  private LongToStringArrayMap deletes = new DefaultLongToStringArrayMap();
  private StringToLongMap words = new DefaultStringToLongMap();
  private StringToLongMap bigrams = new DefaultStringToLongMap();

  public SymSpellBuilder setDeletesMapWrapper(LongToStringArrayMap mapWrapper) {
    this.deletes = mapWrapper;
    return this;
  }

  public SymSpellBuilder setWordsMapWrapper(StringToLongMap mapWrapper) {
    this.words = mapWrapper;
    return this;
  }

  public SymSpellBuilder setBigramsMapWrapper(StringToLongMap mapWrapper) {
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
