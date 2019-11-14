package io.gitlab.rxp90.jsymspell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class SymSpellTest {

  @Test
  public void loadDictionary() {
    DefaultStringHasher stringHasher = new DefaultStringHasher();
    SymSpell symSpell =
        new SymSpellBuilder()
            .setStringHasher(stringHasher)
            .setMaxDictionaryEditDistance(2)
            .createSymSpell();
    symSpell.loadDictionary(Set.of("abcde,100", "abcdef,90"), 0, 1);
    LongToStringArrayMap deletes = symSpell.getDeletes();

    String[] suggestions = deletes.get(stringHasher.hash("abcd"));
    assertEquals(
        Set.of("abcde", "abcdef"),
        Set.of(suggestions),
        "abcd == abcde - {e} (distance 1), abcd == abcdef - {ef} (distance 2)");
  }
  @Test
  public void lookCompound() throws IOException {
    DefaultStringHasher stringHasher = new DefaultStringHasher();
    SymSpell symSpell =
        new SymSpellBuilder()
            .setStringHasher(stringHasher)
            .setMaxDictionaryEditDistance(3)
            .createSymSpell();

    Set<String> bigrams = Files.lines(Path.of("src/test/resources/bigrams.txt")).collect(Collectors.toSet());
    Set<String> words = Files.lines(Path.of("src/test/resources/words.txt")).collect(Collectors.toSet());

    symSpell.loadBigramDictionary(bigrams, 0, 2);
    symSpell.loadDictionary(words, 0, 1);

    List<SuggestItem> suggestions = symSpell.lookupCompound("absolutely notmine", 3);

    assertEquals(1, suggestions.size());
    assertEquals("absolutely not mine", suggestions.get(0).getSuggestion());
    assertEquals(1, suggestions.get(0).getEditDistance());
  }

  @Test
  public void editsDistance0() {
    int maxEditDistance = 0;
    SymSpell symSpell =
        new SymSpellBuilder().setMaxDictionaryEditDistance(maxEditDistance).createSymSpell();
    Set<String> edits = symSpell.edits("example", 0, new HashSet<>());
    assertEquals(Collections.emptySet(), edits);
  }

  @Test
  public void editsDistance1() {
    int maxEditDistance = 1;
    SymSpell symSpell =
        new SymSpellBuilder().setMaxDictionaryEditDistance(maxEditDistance).createSymSpell();
    Set<String> edits = symSpell.edits("example", 0, new HashSet<>());
    assertEquals(
        Set.of("xample", "eample", "exmple", "exaple", "examle", "exampe", "exampl"), edits);
  }

  @Test
  public void editsDistance2() {
    int maxEditDistance = 2;
    SymSpell symSpell =
        new SymSpellBuilder().setMaxDictionaryEditDistance(maxEditDistance).createSymSpell();
    Set<String> edits = symSpell.edits("example", 0, new HashSet<>());
    Set<String> expected =
        Set.of(
            "xample", "eample", "exmple", "exaple", "examle", "exampe", "exampl", "exale", "emple",
            "exape", "exmpe", "exapl", "xampe", "exple", "exmpl", "exmle", "xamle", "xmple",
            "exame", "xaple", "xampl", "examl", "eaple", "eampl", "examp", "ample", "eamle",
            "eampe");
    assertEquals(expected, edits);
  }
}
