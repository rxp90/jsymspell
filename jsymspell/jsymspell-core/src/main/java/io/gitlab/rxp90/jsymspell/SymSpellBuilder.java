package io.gitlab.rxp90.jsymspell;

public class SymSpellBuilder {

  private int initialCapacity = 256;
  private int maxDictionaryEditDistance = 2;
  private int prefixLength = 7;
  private int countThreshold = 1;
  private byte compactLevel = 5;
  private StringHasher stringHasher = new DefaultStringHasher();
  private LongToStringArrayMap deletes = new DefaultLongToStringArrayMap();
  private StringToLongMap words = new DefaultStringToLongMap();
  private StringToLongMap bigrams = new DefaultStringToLongMap();

  public SymSpellBuilder setInitialCapacity(int initialCapacity) {
    this.initialCapacity = initialCapacity;
    return this;
  }

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

  public SymSpellBuilder setCompactLevel(byte compactLevel) {
    this.compactLevel = compactLevel;
    return this;
  }

  public SymSpell createSymSpell() {
    return new SymSpell(
        initialCapacity,
        maxDictionaryEditDistance,
        prefixLength,
        countThreshold,
        compactLevel,
        stringHasher,
        deletes,
        words,
        bigrams);
  }
}
