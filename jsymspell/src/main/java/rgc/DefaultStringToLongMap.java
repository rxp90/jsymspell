package rgc;

import java.util.HashMap;
import java.util.Map;

public class DefaultStringToLongMap implements StringToLongMap {

  private final Map<String, Long> map = new HashMap<>();

  @Override
  public long get(String key) {
    return map.get(key);
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
