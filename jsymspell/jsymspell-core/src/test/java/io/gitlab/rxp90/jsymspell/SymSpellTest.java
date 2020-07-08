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

import static org.junit.jupiter.api.Assertions.*;

class SymSpellTest {

    @Test
    void loadDictionary() {
        DefaultStringHasher stringHasher = new DefaultStringHasher();
        SymSpell symSpell =
                new SymSpellBuilder()
                        .setStringHasher(stringHasher)
                        .setMaxDictionaryEditDistance(2)
                        .createSymSpell();
        symSpell.loadDictionary(Set.of("abcde,100", "abcdef,90"), 0, 1);
        Map<Long, String[]> deletes = symSpell.getDeletes();

        String[] suggestions = deletes.get(stringHasher.hash("abcd"));
        assertEquals(
                Set.of("abcde", "abcdef"),
                Set.of(suggestions),
                "abcd == abcde - {e} (distance 1), abcd == abcdef - {ef} (distance 2)");
    }

    @Test
    void loadDictionaryIfWordIsRepeatedFrequenciesAreTotaledUp() {
        SymSpell symSpell = new SymSpellBuilder().createSymSpell();
        symSpell.loadDictionary(Set.of("I_am_repeated,100", "I_am_repeated,90"), 0, 1);

        assertEquals(190, symSpell.getWords().get("I_am_repeated"));
    }

    @Test
    void loadDictionaryFrequenciesBelowThresholdAreIgnored() {
        SymSpell symSpell =
                new SymSpellBuilder()
                        .setCountThreshold(100)
                        .createSymSpell();
        symSpell.loadDictionary(Set.of("above_threshold,200", "below_threshold,50"), 0, 1);

        assertFalse(symSpell.getWords().containsKey("below_threshold"));
        assertTrue(symSpell.getWords().containsKey("above_threshold"));
    }

    @Test
    void lookupCompound() throws IOException, NotInitializedException, URISyntaxException {
        DefaultStringHasher stringHasher = new DefaultStringHasher();
        SymSpell symSpell =
                new SymSpellBuilder()
                        .setStringHasher(stringHasher)
                        .setMaxDictionaryEditDistance(2)
                        .setPrefixLength(10)
                        .createSymSpell();

        URL bigramsPath = Objects.requireNonNull(getClass().getClassLoader().getResource("bigrams.txt"));
        URL wordsPath = Objects.requireNonNull(getClass().getClassLoader().getResource("words.txt"));
        Set<String> bigrams =
                Files.lines(Paths.get(bigramsPath.toURI())).collect(Collectors.toSet());
        Set<String> words =
                Files.lines(Paths.get(wordsPath.toURI())).collect(Collectors.toSet());

        symSpell.loadBigramDictionary(bigrams, 0, 2);
        symSpell.loadDictionary(words, 0, 1);

        List<SuggestItem> suggestions = symSpell.lookupCompound("whereis th elove hehad dated forImuch of thepast who couqdn'tread in sixthgrade and ins pired him".toLowerCase(), 2, false);

        assertEquals(1, suggestions.size());
        assertEquals("where is the love he had dated for much of the past who couldn't read in sixth grade and inspired him", suggestions.get(0).getSuggestion());
    }

    @Test
    void lookupWordWithNoErrors() throws IOException, NotInitializedException, URISyntaxException {
        SymSpell symSpell = new SymSpellBuilder().setMaxDictionaryEditDistance(3).createSymSpell();

        URL wordsPath = Objects.requireNonNull(getClass().getClassLoader().getResource("words.txt"));
        Set<String> words =
                Files.lines(Paths.get(wordsPath.toURI())).collect(Collectors.toSet());
        symSpell.loadDictionary(words, 0, 1);

        List<SuggestItem> suggestions = symSpell.lookup("questionnaire", Verbosity.CLOSEST);

        assertEquals(1, suggestions.size());
        assertEquals("questionnaire", suggestions.get(0).getSuggestion());
        assertEquals(0, suggestions.get(0).getEditDistance());
    }

    @Test
    void combineWords() throws URISyntaxException, IOException, NotInitializedException {
        DefaultStringHasher stringHasher = new DefaultStringHasher();
        SymSpell symSpell =
                new SymSpellBuilder()
                        .setStringHasher(stringHasher)
                        .setMaxDictionaryEditDistance(2)
                        .setPrefixLength(10)
                        .createSymSpell();

        URL bigramsPath = Objects.requireNonNull(getClass().getClassLoader().getResource("bigrams.txt"));
        URL wordsPath = Objects.requireNonNull(getClass().getClassLoader().getResource("words.txt"));
        Set<String> bigrams =
                Files.lines(Paths.get(bigramsPath.toURI())).collect(Collectors.toSet());
        Set<String> words =
                Files.lines(Paths.get(wordsPath.toURI())).collect(Collectors.toSet());

        symSpell.loadBigramDictionary(bigrams, 0, 2);
        symSpell.loadDictionary(words, 0, 1);

        Optional<SuggestItem> newSuggestion = symSpell.combineWords(2, false, "pired", "ins", new SuggestItem("in", 1, 8.46E9), new SuggestItem("tired", 1, 1.1E7));
        assertTrue(newSuggestion.isPresent());
        assertEquals(new SuggestItem("inspired", 0, symSpell.getWords().get("inspired")), newSuggestion.get());
    }

    @Test
    void lookupWithoutLoadingDictThrowsException() throws IOException, NotInitializedException {
        SymSpell symSpell = new SymSpellBuilder().createSymSpell();
        assertThrows(NotInitializedException.class, () -> symSpell.lookup("boom", Verbosity.CLOSEST));
    }

    @Test
    void lookupAll() throws IOException, NotInitializedException, URISyntaxException {
        SymSpell symSpell = new SymSpellBuilder().setMaxDictionaryEditDistance(2).createSymSpell();

        URL wordsPath = Objects.requireNonNull(getClass().getClassLoader().getResource("words.txt"));
        Set<String> words =
                Files.lines(Paths.get(wordsPath.toURI())).collect(Collectors.toSet());
        symSpell.loadDictionary(words, 0, 1);

        List<SuggestItem> suggestions = symSpell.lookup("sumarized", Verbosity.ALL);
        assertEquals(6, suggestions.size());
        assertEquals("summarized", suggestions.get(0).getSuggestion());
        assertEquals(1, suggestions.get(0).getEditDistance());
    }

    @Test
    void editsDistance0() {
        int maxEditDistance = 0;
        SymSpell symSpell =
                new SymSpellBuilder().setMaxDictionaryEditDistance(maxEditDistance).createSymSpell();
        Set<String> edits = symSpell.edits("example", 0, new HashSet<>());
        assertEquals(Collections.emptySet(), edits);
    }

    @Test
    void editsDistance1() {
        int maxEditDistance = 1;
        SymSpell symSpell =
                new SymSpellBuilder().setMaxDictionaryEditDistance(maxEditDistance).createSymSpell();
        Set<String> edits = symSpell.edits("example", 0, new HashSet<>());
        assertEquals(
                Set.of("xample", "eample", "exmple", "exaple", "examle", "exampe", "exampl"), edits);
    }

    @Test
    void editsDistance2() {
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
