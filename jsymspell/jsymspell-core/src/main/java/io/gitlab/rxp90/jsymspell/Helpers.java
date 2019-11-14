package io.gitlab.rxp90.jsymspell;

public class Helpers {

  public static PrefixSuffix prefixSuffixPrep(String string1, String string2) {
    int len2 = string2.length();
    int len1 = string1.length();

    assert len1 <= len2;

    // Suffix
    while (len1 != 0 && string1.charAt(len1 - 1) == string2.charAt(len2 - 1)) {
      len1--;
      len2--;
    }

    int start = 0;

    // Prefix
    while (start != len1 && string1.charAt(start) == string2.charAt(start)) {
      start++;
    }
    if (start != 0) {
      len2 -= start; // length of the part excluding common prefix and suffix
      len1 -= start;
    }

    return new PrefixSuffix(len1, len2, start);
  }

  public static class PrefixSuffix {
    private final int len1;
    private final int len2;
    private final int start;

    public PrefixSuffix(int len1, int len2, int start) {
      this.len1 = len1;
      this.len2 = len2;
      this.start = start;
    }

    public int getLen1() {
      return len1;
    }

    public int getLen2() {
      return len2;
    }

    public int getStart() {
      return start;
    }
  }
}
