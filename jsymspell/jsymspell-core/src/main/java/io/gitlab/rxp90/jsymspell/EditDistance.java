package io.gitlab.rxp90.jsymspell;

public interface EditDistance {

  int distance(String baseString, String string2, int maxDistance);
}
