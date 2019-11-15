package io.gitlab.rxp90.jsymspell.api;

import java.util.HashMap;
import java.util.Map;

public class DefaultLongToStringArrayMap implements LongToStringArrayMap {

  private final Map<Long, String[]> deletes = new HashMap<>();

  public DefaultLongToStringArrayMap() {
  }

  @Override
  public String[] get(long key) {
    return deletes.get(key);
  }

  @Override
  public String[] put(long key, String[] values) {
    return deletes.put(key, values);
  }
}
