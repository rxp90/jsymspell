package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.api.SuggestItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SuggestItemTest {

  @Test
  void compareToAscendingByDistance() {
    SuggestItem smallDistance = new SuggestItem("test", 5, 10);
    SuggestItem bigDistance = new SuggestItem("test", 10, 10);

    assertEquals(-1, smallDistance.compareTo(bigDistance));
  }

  @Test
  void compareToDescendingByFrequency() {
    SuggestItem sameDistanceSmallerFreq = new SuggestItem("test", 5, 10);
    SuggestItem sameDistanceBiggerFreq = new SuggestItem("test", 5, 20);

    assertEquals(1, sameDistanceSmallerFreq.compareTo(sameDistanceBiggerFreq));
  }


  @Test
  void equalsAndHashCode() {
    SuggestItem si1 = new SuggestItem("test", 5, 10);
    SuggestItem si2 = new SuggestItem("test", 5, 10);

    assertEquals(si1, si2);
    assertEquals(si1.hashCode(), si2.hashCode());
  }
}