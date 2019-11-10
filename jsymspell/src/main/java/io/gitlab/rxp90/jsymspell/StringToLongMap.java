package io.gitlab.rxp90.jsymspell;

public interface StringToLongMap {
  long get(String key);
  void put(String key, long value);
  boolean contains(String key);
}
