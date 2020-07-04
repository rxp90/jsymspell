package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.api.DefaultStringHasher;
import io.gitlab.rxp90.jsymspell.api.StringHasher;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SymSpellBuilder {

  private int maxDictionaryEditDistance = 2;
  private int prefixLength = 7;
  private int countThreshold = 1;
  private StringHasher stringHasher = new DefaultStringHasher();
  private final Map<Long, String[]> deletes = new HashMap<>();
  private final Collection<String> lexiconWords = new HashSet<>();
  private final Collection<String> lexiconBigrams = new HashSet<>();

  public SymSpellBuilder setDeletesMap( final Map<Long, String[]> map ) {
    assert map != null;
    this.deletes.putAll( map );
    return this;
  }

  public SymSpellBuilder setLexiconWords( final Collection<String> lexicon ) {
    this.lexiconWords.addAll( lexicon );
    return this;
  }

  public SymSpellBuilder setLexiconBigrams( final Collection<String> lexicon ) {
    this.lexiconBigrams.addAll( lexicon );
    return this;
  }

  public SymSpellBuilder setStringHasher( final StringHasher stringHasher ) {
    this.stringHasher = stringHasher;
    return this;
  }

  public SymSpellBuilder setMaxDictionaryEditDistance(
      int maxDictionaryEditDistance ) {
    this.maxDictionaryEditDistance = maxDictionaryEditDistance;
    return this;
  }

  public SymSpellBuilder setPrefixLength( int prefixLength ) {
    this.prefixLength = prefixLength;
    return this;
  }

  public SymSpellBuilder setCountThreshold( int countThreshold ) {
    this.countThreshold = countThreshold;
    return this;
  }

  public SymSpell build() {
    return new SymSpell(
        maxDictionaryEditDistance,
        prefixLength,
        countThreshold,
        stringHasher,
        deletes,
        lexiconWords,
        lexiconBigrams );
  }
}
