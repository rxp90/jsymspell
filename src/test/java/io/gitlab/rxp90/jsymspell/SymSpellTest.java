package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.api.Bigram;
import io.gitlab.rxp90.jsymspell.api.StringDistance;
import io.gitlab.rxp90.jsymspell.api.SuggestItem;
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
        SymSpellImpl symSpell = new SymSpellBuilder().setMaxDictionaryEditDistance(2)
                                                     .setUnigramLexicon(mapOf("abcde", 100L, "abcdef", 90L))
                                                     .createSymSpell();

        Map<String, Collection<String>> deletes = symSpell.getDeletes();

        Collection<String> suggestions = deletes.get("abcd");
        assertTrue(suggestions.containsAll(Arrays.asList("abcde", "abcdef")), "abcd == abcde - {e} (distance 1), abcd == abcdef - {ef} (distance 2)");
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
    void lookupCompoundWithUnknownWords() throws Exception {
        SymSpell symSpell = new SymSpellBuilder().setUnigramLexicon(unigrams)
                                                 .setBigramLexicon(bigrams)
                                                 .setMaxDictionaryEditDistance(2)
                                                 .setPrefixLength(7)
                                                 .createSymSpell();

        List<SuggestItem> suggestions = symSpell.lookupCompound("Atrociraptor wasassigned to the Velociraptorinae within a larger Dromaeosauridae", 1, false);

        assertEquals("Atrociraptor was assigned to the Velociraptorinae within a larger Dromaeosauridae", suggestions.get(0).getSuggestion());
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
    void noSuggestionFound() throws Exception {
        SymSpell symSpell = new SymSpellBuilder().setUnigramLexicon(unigrams)
                                                 .setMaxDictionaryEditDistance(2)
                                                 .createSymSpell();

        List<SuggestItem> suggestions = symSpell.lookup("qwertyuiop", Verbosity.ALL, false);

        assertTrue(suggestions.isEmpty());
    }

    @Test
    void noSuggestionFoundIncludeUnknown() throws Exception {
        SymSpell symSpell = new SymSpellBuilder().setUnigramLexicon(unigrams)
                                                 .setMaxDictionaryEditDistance(2)
                                                 .createSymSpell();

        String input = "qwertyuiop";
        List<SuggestItem> suggestions = symSpell.lookup(input, Verbosity.ALL, true);

        assertFalse(suggestions.isEmpty());
        assertEquals(input, suggestions.get(0).getSuggestion());
    }

    @Test
    void combineWords() throws Exception {
        SymSpellImpl symSpell = new SymSpellBuilder().setUnigramLexicon(unigrams)
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
        SymSpellImpl symSpell = new SymSpellBuilder().setMaxDictionaryEditDistance(maxEditDistance).createSymSpell();
        Set<String> edits = symSpell.edits("example", 0, new HashSet<>());
        assertEquals(Collections.emptySet(), edits);
    }

    @Test
    void editsDistance1() throws Exception {
        int maxEditDistance = 1;
        SymSpellImpl symSpell = new SymSpellBuilder().setMaxDictionaryEditDistance(maxEditDistance).createSymSpell();
        Set<String> edits = symSpell.edits("example", 0, new HashSet<>());
        assertEquals(setOf("xample", "eample", "exmple", "exaple", "examle", "exampe", "exampl"), edits);
    }

    @Test
    void editsDistance2() throws Exception {
        int maxEditDistance = 2;
        SymSpellImpl symSpell = new SymSpellBuilder().setMaxDictionaryEditDistance(maxEditDistance).createSymSpell();
        Set<String> edits = symSpell.edits("example", 0, new HashSet<>());
        Set<String> expected = setOf("xample", "eample", "exmple", "exaple", "examle", "exampe", "exampl", "exale", "emple",
                "exape", "exmpe", "exapl", "xampe", "exple", "exmpl", "exmle", "xamle", "xmple",
                "exame", "xaple", "xampl", "examl", "eaple", "eampl", "examp", "ample", "eamle",
                "eampe");
        assertEquals(expected, edits);
    }

    @Test
    void customStringDistanceAlgorithm() throws NotInitializedException {
        StringDistance hammingDistance = (string1, string2, maxDistance) -> {
            if (string1.length() != string2.length()){
                return -1;
            }
            char[] chars1 = string1.toCharArray();
            char[] chars2 = string2.toCharArray();
            int distance = 0;
            for (int i = 0; i < chars1.length; i++) {
                char c1 = chars1[i];
                char c2 = chars2[i];
                if (c1 != c2) {
                    distance += 1;
                }
            }
            return distance;
        };
        SymSpell symSpell = new SymSpellBuilder().setUnigramLexicon(mapOf("1001001", 1L))
                                                 .setStringDistanceAlgorithm(hammingDistance)
                                                 .setMaxDictionaryEditDistance(1)
                                                 .createSymSpell();

        List<SuggestItem> suggestions = symSpell.lookup("1000001", Verbosity.CLOSEST);

        assertEquals(1, suggestions.get(0).getEditDistance());
    }


    public static <T> Map<String, T> mapOf(Object... objects){
        Map<String, T> map = new HashMap<>();
        for (int i = 0; i < objects.length; i+=2){
            map.put((String) objects[i], (T) objects[i+1]);
        }
        return map;
    }

    public static <T> Set<T> setOf(T ... values){
        return Arrays.stream(values).collect(Collectors.toSet());
    }
}
