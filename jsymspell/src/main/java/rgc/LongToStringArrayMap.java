package rgc;

public interface LongToStringArrayMap {
  String[] get(long key);
  String[] put(long key, String[] values);
}
