[![codecov](https://codecov.io/gh/rxp90/jsymspell/branch/master/graph/badge.svg)](https://codecov.io/gh/rxp90/jsymspell)
# JSymSpell

JSymSpell is a **zero-dependency** Java 8+ port of [SymSpell](https://github.com/wolfgarbe/SymSpell "SymSpell: 1 million times faster through Symmetric Delete spelling correction algorithm").

The Symmetric Delete spelling correction algorithm speeds up the process up by orders of magnitude.

It achieves this by generating delete-only candidates in advance from a given lexicon.

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

#### Custom String Distance Algorithms
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
#### Custom character comparison
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

#### Frequency dictionaries in other languages
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

## Versioning

We use [SemVer](http://semver.org/) for versioning.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE) file for details

## Acknowledgments

* Wolf Garbe
