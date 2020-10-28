package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.SymSpell.Verbosity;
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
    private final URL bigramsPath = Objects.requireNonNull(getClass().getClassLoader().getResource("bigrams.txt"));
    private final Map<Bigram, Long> bigrams = Files.lines(Paths.get(bigramsPath.toURI()))
                                                   .map(line -> line.split(" "))
                                                   .collect(Collectors.toMap(tokens -> new Bigram(tokens[0], tokens[1]), tokens -> Long.parseLong(tokens[2])));

    private final URL wordsPath = Objects.requireNonNull(getClass().getClassLoader().getResource("words.txt"));
    private final Map<String, Long> unigrams = Files.lines(Paths.get(wordsPath.toURI()))
                                                    .map(line -> line.split(","))
                                                    .collect(Collectors.toMap(tokens -> tokens[0], tokens -> Long.parseLong(tokens[1])));

    SymSpellTest() throws IOException, URISyntaxException {
    }

    @Test
    void loadDictionary() throws Exception {
        SymSpell symSpell = new SymSpellBuilder().setMaxDictionaryEditDistance(2)
                                                 .setUnigramLexicon(Map.of("abcde", 100L, "abcdef", 90L))
                                                 .createSymSpell();

        Map<String, Collection<String>> deletes = symSpell.getDeletes();

        Collection<String> suggestions = deletes.get("abcd");
        assertTrue(suggestions.containsAll(List.of("abcde", "abcdef")), "abcd == abcde - {e} (distance 1), abcd == abcdef - {ef} (distance 2)");
    }

    @Test
    void lookupCompound() throws Exception {
        SymSpell symSpell = new SymSpellBuilder().setUnigramLexicon(unigrams)
                                                 .setBigramLexicon(bigrams)
                                                 .setMaxDictionaryEditDistance(2)
                                                 .setPrefixLength(10)
                                                 .createSymSpell();

        List<SuggestItem> suggestions = symSpell.lookupCompound("whereis th elove hehad dated forImuch of thepast who couqdn'tread in sixthgrade and ins pired him".toLowerCase(), 2, false);

        assertEquals(1, suggestions.size());
        assertEquals("where is the love he had dated for much of the past who couldn't read in sixth grade and inspired him", suggestions.get(0).getSuggestion());
    }

    @Test
    void lookupCompound2() throws Exception {
        SymSpell symSpell = new SymSpellBuilder().setUnigramLexicon(unigrams)
                                                 .setBigramLexicon(bigrams)
                                                 .setMaxDictionaryEditDistance(2)
                                                 .setPrefixLength(10)
                                                 .createSymSpell();

        List<SuggestItem> suggestions = symSpell.lookupCompound("Can yu readthis messa ge despite thehorible sppelingmsitakes".toLowerCase(), 2, false);

        assertEquals(1, suggestions.size());
        assertEquals("can you read this message despite the horrible spelling mistakes", suggestions.get(0).getSuggestion());
    }

    @Test
    void lookupWordWithNoErrors() throws Exception {
        SymSpell symSpell = new SymSpellBuilder().setUnigramLexicon(unigrams)
                                                 .setMaxDictionaryEditDistance(3)
                                                 .createSymSpell();

        List<SuggestItem> suggestions = symSpell.lookup("questionnaire", Verbosity.CLOSEST);

        assertEquals(1, suggestions.size());
        assertEquals("questionnaire", suggestions.get(0).getSuggestion());
        assertEquals(0, suggestions.get(0).getEditDistance());
    }

    @Test
    void combineWords() throws Exception {
        SymSpell symSpell = new SymSpellBuilder().setUnigramLexicon(unigrams)
                                                 .setBigramLexicon(bigrams)
                                                 .setMaxDictionaryEditDistance(2)
                                                 .setPrefixLength(10)
                                                 .createSymSpell();

        Optional<SuggestItem> newSuggestion = symSpell.combineWords(2, false, "pired", "ins", new SuggestItem("in", 1, 8.46E9), new SuggestItem("tired", 1, 1.1E7));
        assertTrue(newSuggestion.isPresent());
        assertEquals(new SuggestItem("inspired", 0, symSpell.getUnigramLexicon().get("inspired")), newSuggestion.get());
    }

    @Test
    void lookupWithoutLoadingDictThrowsException() throws Exception {
        SymSpell symSpell = new SymSpellBuilder().createSymSpell();
        assertThrows(NotInitializedException.class, () -> symSpell.lookup("boom", Verbosity.CLOSEST));
    }

    @Test
    void lookupAll() throws Exception {
        SymSpell symSpell = new SymSpellBuilder().setMaxDictionaryEditDistance(2)
                                                 .setUnigramLexicon(unigrams)
                                                 .createSymSpell();

        List<SuggestItem> suggestions = symSpell.lookup("sumarized", Verbosity.ALL);
        assertEquals(6, suggestions.size());
        assertEquals("summarized", suggestions.get(0).getSuggestion());
        assertEquals(1, suggestions.get(0).getEditDistance());
    }

    @Test
    void editsDistance0() throws Exception {
        int maxEditDistance = 0;
        SymSpell symSpell = new SymSpellBuilder().setMaxDictionaryEditDistance(maxEditDistance).createSymSpell();
        Set<String> edits = symSpell.edits("example", 0, new HashSet<>());
        assertEquals(Collections.emptySet(), edits);
    }

    @Test
    void editsDistance1() throws Exception {
        int maxEditDistance = 1;
        SymSpell symSpell = new SymSpellBuilder().setMaxDictionaryEditDistance(maxEditDistance).createSymSpell();
        Set<String> edits = symSpell.edits("example", 0, new HashSet<>());
        assertEquals(Set.of("xample", "eample", "exmple", "exaple", "examle", "exampe", "exampl"), edits);
    }

    @Test
    void editsDistance2() throws Exception {
        int maxEditDistance = 2;
        SymSpell symSpell = new SymSpellBuilder().setMaxDictionaryEditDistance(maxEditDistance).createSymSpell();
        Set<String> edits = symSpell.edits("example", 0, new HashSet<>());
        Set<String> expected = Set.of("xample", "eample", "exmple", "exaple", "examle", "exampe", "exampl", "exale", "emple",
                "exape", "exmpe", "exapl", "xampe", "exple", "exmpl", "exmle", "xamle", "xmple",
                "exame", "xaple", "xampl", "examl", "eaple", "eampl", "examp", "ample", "eamle",
                "eampe");
        assertEquals(expected, edits);
    }
}
