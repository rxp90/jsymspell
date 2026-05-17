<h1 align="center">JSymSpell</h1>

<div align="center">
<strong>
  JSymSpell is a <code>zero-dependency</code> Java 8+ port of <a href="https://github.com/wolfgarbe/SymSpell">SymSpell</a>
</strong>
</div>

<br />

[![forthebadge](https://forthebadge.com/images/badges/made-with-java.svg)](https://forthebadge.com)
<br />
<br />
[![codecov](https://codecov.io/gh/rxp90/jsymspell/branch/master/graph/badge.svg)](https://codecov.io/gh/rxp90/jsymspell)
[![Maven Central](https://img.shields.io/maven-central/v/io.gitlab.rxp90/jsymspell.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.gitlab.rxp90%22%20AND%20a:%22jsymspell%22)
[![License MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Open Source Love svg1](https://badges.frapsoft.com/os/v1/open-source.svg?v=103)](https://github.com/ellerbrock/open-source-badges/)

## Overview

The Symmetric Delete spelling correction algorithm speeds up the process up by orders of magnitude.

It achieves this by generating delete-only candidates in advance from a given lexicon.

## Setup

Add the latest [JSymSpell dependency](https://search.maven.org/artifact/io.gitlab.rxp90/jsymspell) to your project

## Getting Started

To start, we'll load the data sets of unigrams and bigrams:

```java
Map<Bigram, Long> bigrams = Files.lines(Paths.get("src/test/resources/bigrams.txt"))
                                 .map(line -> line.split(" "))
                                 .collect(Collectors.toMap(tokens -> new Bigram(tokens[0], tokens[1]), tokens -> Long.parseLong(tokens[2])));
Map<String, Long> unigrams = Files.lines(Paths.get("src/test/resources/words.txt"))
                                  .map(line -> line.split(","))
                                  .collect(Collectors.toMap(tokens -> tokens[0], tokens -> Long.parseLong(tokens[1])));
```

Let's now create an instance of `SymSpell` by using the builder and load these maps. For this example we'll limit the max edit distance to 2:
```java
SymSpell symSpell = new SymSpellBuilder().setUnigramLexicon(unigrams)
                                         .setBigramLexicon(bigrams)
                                         .setMaxDictionaryEditDistance(2)
                                         .createSymSpell();
```

And we are ready!
```java
int maxEditDistance = 2;
boolean includeUnknowns = false;
List<SuggestItem> suggestions = symSpell.lookupCompound("Nostalgiais truly one of th greatests human weakneses", maxEditDistance, includeUnknowns);
System.out.println(suggestions.get(0).getSuggestion());
// Output: nostalgia is truly one of the greatest human weaknesses
// ... only second to the neck!
```

### Custom String Distance Algorithms
By default, JSymSpell calculates [Damerau-Levenshtein](https://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance) distance. Depending on your use case, you may want to use a different one.

Other algorithms to calculate String Distance that might result of interest are:
* [Hamming Distance](https://en.wikipedia.org/wiki/Hamming_distance)
* [Jaro Distance](https://en.wikipedia.org/wiki/Jaro%E2%80%93Winkler_distance)
* [Keyboard distance](https://metacpan.org/pod/release/KRBURTON/String-KeyboardDistance-1.01/KeyboardDistance.pm)

Here's an example using [Hamming Distance](https://en.wikipedia.org/wiki/Hamming_distance):
```java
SymSpell symSpell = new SymSpellBuilder().setUnigramLexicon(unigrams)
                                         .setStringDistanceAlgorithm((string1, string2, maxDistance) -> {
                                             if (string1.length() != string2.length()){
                                                 return -1;
                                             }
                                             char[] chars1 = string1.toCharArray();
                                             char[] chars2 = string2.toCharArray();
                                             int distance = 0;
                                             for (int i = 0; i < chars1.length; i++) {
                                                 if (chars1[i] != chars2[i]) {
                                                     distance += 1;
                                                 }
                                             }
                                             return distance;
                                         })
                                         .createSymSpell();
```
### Custom character comparison
Let's say you are building a query engine for country names where the input form allows Unicode characters, but the database is all ASCII.
You might want searches for `Espana` to return `España` entries with distance 0:
```java
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
SymSpell symSpell = new SymSpellBuilder().setUnigramLexicon(Map.of("España", 10L))
                                         .setStringDistanceAlgorithm(damerauLevenshteinOSA)
                                         .createSymSpell();
List<SuggestItem> suggestions = symSpell.lookup("Espana", Verbosity.ALL);
assertEquals(0, suggestions.get(0).getEditDistance());
```

### Frequency dictionaries in other languages
As in the original [SymSpell](https://github.com/wolfgarbe/SymSpell/blob/master/SymSpell/frequency_dictionary_en_82_765.txt) project, this port contains an English frequency dictionary that you can find at `src/test/resources/words.txt`
If you need a different one, you just need to compute a `Map<String, Long>` where the key is the word and the value is the frequency in the corpus.

```java
Map<String, Long> unigrams = Arrays.stream("A B A B C A B A C A".split(" "))
                                   .collect(Collectors.groupingBy(String::toLowerCase, Collectors.counting()));
System.out.println(unigrams);
// Output: {a=5, b=3, c=2}
```
## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## Benchmarks

JSymSpell's `lookup` is dramatically faster than a brute-force scan of the lexicon that computes Damerau-Levenshtein-OSA distance against every entry. The gap widens with dictionary size — on an 80k-word lexicon, SymSpell is between **~1,900× and ~3,900× faster**.

| Max edit distance | Lexicon size | SymSpell (µs/lookup) | Naive scan (µs/lookup) | Speedup |
|------------------:|-------------:|---------------------:|-----------------------:|--------:|
|                 1 |       10,000 |                 1.02 |                 666.78 |    654× |
|                 1 |       80,000 |                 2.13 |               8,398.61 |  3,943× |
|                 2 |       10,000 |                 2.49 |                 844.58 |    339× |
|                 2 |       80,000 |                 3.92 |               9,415.02 |  2,402× |
|                 3 |       10,000 |                 8.42 |               1,178.25 |    140× |
|                 3 |       80,000 |                 6.27 |              11,801.68 |  1,882× |

*JMH 1.37 · 2 forks × (5 × 1 s warmup + 5 × 2 s measurement) · Intel Core i7-4900MQ @ 2.80 GHz · OpenJDK 25. Inputs: a fixed list of 11 words (8 typos of varying length, 2 correctly-spelled words, 1 out-of-vocabulary string) — see [`LookupBenchmark.java`](benchmarks/src/main/java/io/gitlab/rxp90/jsymspell/benchmarks/LookupBenchmark.java).*

The benchmarks live in `benchmarks/` as a standalone Maven module (not published). To run:

```bash
mvn install -DskipTests
cd benchmarks
mvn package
java -jar target/benchmarks.jar
```

Vary the parameters with JMH flags, e.g.:

```bash
java -jar target/benchmarks.jar LookupBenchmark -p maxEditDistance=2 -p lexiconSize=80000
```

## Versioning

We use [SemVer](http://semver.org/) for versioning.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE) file for details

## Acknowledgments

* Wolf Garbe
