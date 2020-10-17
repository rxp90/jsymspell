package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.api.DamerauLevenshteinOSA;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamerauLevenshteinOSATest {

  private static final DamerauLevenshteinOSA DAMERAU_LEVENSHTEIN_OSA = new DamerauLevenshteinOSA();

  @Test
  void distance() {
    int distance = DAMERAU_LEVENSHTEIN_OSA.distanceWithEarlyStop("CA", "ABC", 3);
    assertEquals(3, distance);
  }
}