package rgc;

import java.util.HashMap;
import java.util.Map;

public class DefaultDeletes implements Deletes {

  private final Map<Long, String[]> deletes = new HashMap<>();

  @Override
  public String[] get(long key) {
    return deletes.get(key);
  }

  @Override
  public String[] put(long key, String[] values) {
    return deletes.put(key, values);
  }
}
