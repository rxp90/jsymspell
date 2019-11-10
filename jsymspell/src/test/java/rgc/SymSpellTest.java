package rgc;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class SymSpellTest {

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
