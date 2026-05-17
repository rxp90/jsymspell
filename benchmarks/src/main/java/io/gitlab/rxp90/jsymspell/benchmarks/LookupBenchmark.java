package io.gitlab.rxp90.jsymspell.benchmarks;

import io.gitlab.rxp90.jsymspell.SymSpell;
import io.gitlab.rxp90.jsymspell.SymSpellBuilder;
import io.gitlab.rxp90.jsymspell.Verbosity;
import io.gitlab.rxp90.jsymspell.api.SuggestItem;
import io.gitlab.rxp90.jsymspell.exceptions.NotInitializedException;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(2)
@OperationsPerInvocation(LookupBenchmark.INPUTS_COUNT)
public class LookupBenchmark {

    // Must match INPUTS.size(). Annotation values require a compile-time constant.
    static final int INPUTS_COUNT = 11;

    @Param({"1", "2", "3"})
    public int maxEditDistance;

    @Param({"10000", "80000"})
    public int lexiconSize;

    private SymSpell symSpell;
    private NaiveSpellChecker naive;

    // Mix of short/medium/long typos, plus correctly-spelled words and an
    // out-of-vocabulary input. Kept fixed so results are reproducible.
    private static final List<String> INPUTS = Collections.unmodifiableList(Arrays.asList(
            "hapy",            // short typo
            "thier",           // short typo
            "recieve",         // short typo
            "sumarized",       // medium typo
            "acommodate",      // medium typo
            "questionaire",    // medium typo
            "misunderstanded", // long typo
            "responsability",  // long typo
            "hello",           // correctly spelled (fast path)
            "example",         // correctly spelled (fast path)
            "qwertyuiop"       // no suggestions within distance
    ));

    @Setup(Level.Trial)
    public void setup() throws IOException {
        if (INPUTS.size() != INPUTS_COUNT) {
            throw new IllegalStateException("INPUTS_COUNT (" + INPUTS_COUNT + ") must match INPUTS.size() (" + INPUTS.size() + ")");
        }
        Map<String, Long> fullLexicon = loadWords();
        Map<String, Long> truncated = topN(fullLexicon, lexiconSize);

        symSpell = new SymSpellBuilder()
                .setUnigramLexicon(truncated)
                .setMaxDictionaryEditDistance(maxEditDistance)
                .createSymSpell();

        naive = new NaiveSpellChecker(truncated);
    }

    @Benchmark
    public void symspellLookup(Blackhole bh) throws NotInitializedException {
        for (String input : INPUTS) {
            List<SuggestItem> result = symSpell.lookup(input, Verbosity.CLOSEST);
            bh.consume(result);
        }
    }

    @Benchmark
    public void naiveLookup(Blackhole bh) {
        for (String input : INPUTS) {
            List<SuggestItem> result = naive.lookup(input, maxEditDistance);
            bh.consume(result);
        }
    }

    private static Map<String, Long> loadWords() throws IOException {
        Map<String, Long> map = new HashMap<>();
        try (InputStream in = LookupBenchmark.class.getResourceAsStream("/words.txt")) {
            if (in == null) {
                throw new IOException("words.txt not found on classpath");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int comma = line.indexOf(',');
                    if (comma <= 0) continue;
                    String word = line.substring(0, comma);
                    long freq = Long.parseLong(line.substring(comma + 1));
                    map.put(word, freq);
                }
            }
        }
        return map;
    }

    private static Map<String, Long> topN(Map<String, Long> source, int n) {
        if (source.size() <= n) {
            return source;
        }
        List<Map.Entry<String, Long>> entries = new ArrayList<>(source.entrySet());
        entries.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        Map<String, Long> result = new HashMap<>(n * 2);
        for (int i = 0; i < n; i++) {
            Map.Entry<String, Long> e = entries.get(i);
            result.put(e.getKey(), e.getValue());
        }
        return result;
    }
}
