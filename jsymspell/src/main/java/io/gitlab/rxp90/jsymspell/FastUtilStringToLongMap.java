package io.gitlab.rxp90.jsymspell;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

public class FastUtilStringToLongMap implements StringToLongMap {

  private final Object2LongOpenHashMap<String> map = new Object2LongOpenHashMap<>();

  @Override
  public long get(String key) {
    return map.getLong(key);
  }

  @Override
  public void put(String key, long value) {
    map.put(key, value);
  }

  @Override
  public boolean contains(String key) {
    return map.containsKey(key);
  }
}
