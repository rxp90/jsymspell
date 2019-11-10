package io.gitlab.rxp90.jsymspell;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefaultJavaMap implements Map<Long, String[]> {

  private final Map<Long, String[]> deletes = new HashMap<>();

  @Override
  public int size() {
    return deletes.size();
  }

  @Override
  public boolean isEmpty() {
    return deletes.isEmpty();
  }

  @Override
  public boolean containsKey(Object o) {
    return deletes.containsKey(o);
  }

  @Override
  public boolean containsValue(Object o) {
    return deletes.containsValue(o);
  }

  @Override
  public String[] get(Object o) {
    return deletes.get(o);
  }

  @Override
  public String[] put(Long aLong, String[] strings) {
    return deletes.put(aLong, strings);
  }

  @Override
  public String[] remove(Object o) {
    return deletes.remove(o);
  }

  @Override
  public void putAll(Map<? extends Long, ? extends String[]> map) {
    deletes.putAll(map);
  }

  @Override
  public void clear() {
    deletes.clear();
  }

  @Override
  public Set<Long> keySet() {
    return deletes.keySet();
  }

  @Override
  public Collection<String[]> values() {
    return deletes.values();
  }

  @Override
  public Set<Entry<Long, String[]>> entrySet() {
    return deletes.entrySet();
  }
}
