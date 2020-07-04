package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.SymSpell.Verbosity;
import io.gitlab.rxp90.jsymspell.api.DefaultStringHasher;
import io.gitlab.rxp90.jsymspell.exceptions.NotInitializedException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static io.gitlab.rxp90.jsymspell.SymSpell.LEXICON_DELIMITER;
import static org.junit.jupiter.api.Assertions.*;

class SymSpellTest {

  private static final String FILE_LEXICON_WORDS = "words.txt";
  private static final String FILE_LEXICON_BIGRAMS = "bigrams.txt";

  @Test
  void test_Lexicon_EntriesAdded_SuggestsReturned() {
    final DefaultStringHasher stringHasher = new DefaultStringHasher();
    final Set<String> lexicon = Set.of(
        entry( "abcde", 100 ),
        entry( "abcdef", 90 ) );

    final SymSpell symSpell = new SymSpellBuilder()
        .setStringHasher( stringHasher )
        .setMaxDictionaryEditDistance( 2 )
        .setLexiconWords( lexicon )
        .build();
    final Map<Long, String[]> deletes = symSpell.getDeletes();

    final String[] suggestions = deletes.get( stringHasher.hash( "abcd" ) );
    assertEquals(
        Set.of( "abcde", "abcdef" ),
        Set.of( suggestions ),
        "abcd == abcde - {e} (distance 1), abcd == abcdef - {ef} (distance 2)"
    );
  }

  @Test
  void loadDictionaryIfWordIsRepeatedFrequenciesAreTotaledUp() {
    final Set<String> lexicon = Set.of(
        entry( "I_am_repeated", 100 ),
        entry( "I_am_repeated", 90 ) );

    final SymSpell symSpell = new SymSpellBuilder()
        .setLexiconWords( lexicon )
        .build();

    assertEquals( 190, symSpell.getWords().get( "I_am_repeated" ) );
  }

  @Test
  void loadDictionaryFrequenciesBelowThresholdAreIgnored() {
    final Set<String> lexicon = Set.of(
        entry( "above_threshold", 200 ),
        entry( "below_threshold", 50 ) );

    final SymSpell symSpell = new SymSpellBuilder()
        .setCountThreshold( 100 )
        .setLexiconWords( lexicon )
        .build();

    assertFalse( symSpell.getWords().containsKey( "below_threshold" ) );
    assertTrue( symSpell.getWords().containsKey( "above_threshold" ) );
  }

  @Test
  void lookupCompound()
      throws IOException, NotInitializedException, URISyntaxException {
    final DefaultStringHasher stringHasher = new DefaultStringHasher();
    final SymSpell symSpell = new SymSpellBuilder()
        .setStringHasher( stringHasher )
        .setMaxDictionaryEditDistance( 3 )
        .setLexiconBigrams( readLexiconLines( FILE_LEXICON_BIGRAMS ) )
        .setLexiconWords( readLexiconLines( FILE_LEXICON_WORDS ) )
        .build();

    final List<SuggestItem> suggestions = symSpell.lookupCompound(
        "absolutely notmine",
        3 );

    assertEquals( 1, suggestions.size() );
    assertEquals( "absolutely not mine", suggestions.get( 0 ).getSuggestion() );
    assertEquals( 1, suggestions.get( 0 ).getEditDistance() );
  }

  @Test
  void lookupWordWithNoErrors()
      throws IOException, NotInitializedException, URISyntaxException {
    final SymSpell symSpell = new SymSpellBuilder()
        .setMaxDictionaryEditDistance( 3 )
        .setLexiconWords( readLexiconLines( FILE_LEXICON_WORDS ) )
        .build();

    final List<SuggestItem> suggestions =
        symSpell.lookup( "questionnaire", Verbosity.CLOSEST );

    assertEquals( 1, suggestions.size() );
    assertEquals( "questionnaire", suggestions.get( 0 ).getSuggestion() );
    assertEquals( 0, suggestions.get( 0 ).getEditDistance() );
  }

  @Test()
  void lookupWithoutLoadingDictThrowsException()
      throws NotInitializedException {
    final SymSpell symSpell = new SymSpellBuilder().build();
    assertThrows( NotInitializedException.class,
                  () -> symSpell.lookup( "boom", Verbosity.CLOSEST ) );
  }

  @Test
  void lookupAll()
      throws IOException, NotInitializedException, URISyntaxException {
    final SymSpell symSpell = new SymSpellBuilder()
        .setMaxDictionaryEditDistance( 2 )
        .setLexiconWords( readLexiconLines( FILE_LEXICON_WORDS ) )
        .build();

    final List<SuggestItem> suggestions = symSpell.lookup( "sumarized",
                                                           Verbosity.ALL );
    assertEquals( 6, suggestions.size() );
    assertEquals( "summarized", suggestions.get( 0 ).getSuggestion() );
    assertEquals( 1, suggestions.get( 0 ).getEditDistance() );
  }

  @Test
  void editsDistance0() {
    final SymSpell symSpell = new SymSpellBuilder()
        .setMaxDictionaryEditDistance( 0 )
        .build();
    final Set<String> edits = symSpell.edits( "example", 0, new HashSet<>() );
    assertEquals( Collections.emptySet(), edits );
  }

  @Test
  void editsDistance1() {
    final SymSpell symSpell = new SymSpellBuilder()
        .setMaxDictionaryEditDistance( 1 )
        .build();
    final Set<String> edits = symSpell.edits( "example", 0, new HashSet<>() );
    assertEquals( Set.of(
        "xample",
        "eample",
        "exmple",
        "exaple",
        "examle",
        "exampe",
        "exampl" ), edits );
  }

  @Test
  void editsDistance2() {
    final SymSpell symSpell = new SymSpellBuilder()
        .setMaxDictionaryEditDistance( 2 )
        .build();
    Set<String> edits = symSpell.edits( "example", 0, new HashSet<>() );
    Set<String> expected = Set.of(
        "xample",
        "eample",
        "exmple",
        "exaple",
        "examle",
        "exampe",
        "exampl",
        "exale",
        "emple",
        "exape",
        "exmpe",
        "exapl",
        "xampe",
        "exple",
        "exmpl",
        "exmle",
        "xamle",
        "xmple",
        "exame",
        "xaple",
        "xampl",
        "examl",
        "eaple",
        "eampl",
        "examp",
        "ample",
        "eamle",
        "eampe" );
    assertEquals( expected, edits );
  }

  /**
   * Opens the given lexicon file and reads the lines into memory.
   *
   * @param filename File containing words and heuristics.
   * @return The complete set of lines from the file.
   * @throws URISyntaxException Could find the file.
   * @throws IOException        Could not read the file.
   */
  private Set<String> readLexiconLines( final String filename )
      throws URISyntaxException, IOException {
    final URL path = Objects.requireNonNull(
        getClass().getClassLoader().getResource( filename ) );
    return Files.lines( Paths.get( path.toURI() ) )
                .collect( Collectors.toSet() );
  }

  /**
   * Creates a lexicon entry using the given key/value pairs.
   *
   * @param key   The lexicon's word.
   * @param value The frequency of the word in the lexicon.
   * @return The key and value as a string, separated by a delimiter.
   */
  private String entry( final String key, final long value ) {
    return key + LEXICON_DELIMITER + value;
  }

  /**
   * Creates a lexicon entry using the given key/value pairs.
   *
   * @param key   The lexicon's word.
   * @param value The frequency of the word in the lexicon.
   * @return The key and value as a string, separated by a delimiter.
   */
  private String entry( final String key, final int value ) {
    return entry( key, (long) value );
  }
}
