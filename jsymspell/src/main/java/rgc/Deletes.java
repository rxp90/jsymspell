package rgc;

public interface Deletes {

  String[] get(long key);
  String[] put(long key, String[] values);
}
