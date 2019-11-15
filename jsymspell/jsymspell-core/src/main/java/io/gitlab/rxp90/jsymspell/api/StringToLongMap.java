package io.gitlab.rxp90.jsymspell.api;

public interface StringToLongMap {
  long get(String key);
  void put(String key, long value);
  boolean contains(String key);
  boolean isEmpty();
}
