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
Map <String, Long> unigrams = Files.lines(Paths.get("src/test/resources/words.txt"))
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
List<SuggestItem> suggestions = symSpell.lookupCompound("Nostalgiais truly one of th greatests human weakneses", 2, false);
System.out.println(suggestions.get(0).getSuggestion());
// Output: nostalgia is truly one of the greatest human weaknesses
// ... only second to the neck!
```

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## Versioning

We use [SemVer](http://semver.org/) for versioning.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE) file for details

## Acknowledgments

* Wolf Garbe
