package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.api.CharComparator;
import io.gitlab.rxp90.jsymspell.api.DamerauLevenshteinOSA;
import io.gitlab.rxp90.jsymspell.api.StringDistance;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamerauLevenshteinOSATest {

    private static final DamerauLevenshteinOSA DAMERAU_LEVENSHTEIN_OSA = new DamerauLevenshteinOSA();

    @Test
    void distanceWithEarlyStop() {
        int distance = DAMERAU_LEVENSHTEIN_OSA.distanceWithEarlyStop("CA", "ABC", 3);
        assertEquals(3, distance);
    }

    @Test
    void distanceLargerThanMax() {
        int distance = DAMERAU_LEVENSHTEIN_OSA.distanceWithEarlyStop("abcdef", "ghijkl", 3);
        assertEquals(-1, distance);
    }

    @Test
    void maxDistance() {
        int distance = DAMERAU_LEVENSHTEIN_OSA.distance("abcdef", "ghijkl");
        assertEquals(6, distance);
    }

    @Nested
    class CustomCharComparator {

        @Test
        void similarChars() {
            CharComparator customCharComparator = new CharComparator() {
                @Override
                public boolean areEqual(char ch1, char ch2) {
                    if (ch1 == 'ñ' || ch2 == 'ñ') {
                        return ch1 == 'n' || ch2 == 'n';
                    }
                    return ch1 == ch2;
                }
            };
            StringDistance damerauLevenshteinOSA = new DamerauLevenshteinOSA(customCharComparator);
            int distance = damerauLevenshteinOSA.distance("Espana", "España");
            assertEquals(0, distance);
        }

        @Test
        void ignoreCase() {
            CharComparator ignoreCaseCharComparator = new CharComparator() {
                @Override
                public boolean areEqual(char ch1, char ch2) {
                    return Character.toLowerCase(ch1) == Character.toLowerCase(ch2);
                }
            };
            StringDistance damerauLevenshteinOSA = new DamerauLevenshteinOSA(ignoreCaseCharComparator);
            int distance = damerauLevenshteinOSA.distance("JSYMSPELL", "jsymspell");
            assertEquals(0, distance);
        }
    }


}