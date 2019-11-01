package rgc;

import java.lang.reflect.Array;
import java.util.Arrays;

public class ChunkArray<T> {

  private static final int CHUNK_SIZE = 4096;
  private static final int DIV_SHIFT = 12;

  private T[][] values;
  private final Class<T> clazz;
  private int count;

  @SuppressWarnings("unchecked")
  public ChunkArray(Class<T> clazz, int initialCapacity) {
    this.clazz = clazz;
    int chunks = (initialCapacity + CHUNK_SIZE - 1) / CHUNK_SIZE;
    values = (T[][]) Array.newInstance(clazz, chunks);
    Arrays.fill(values, Array.newInstance(clazz, CHUNK_SIZE));
  }

  @SuppressWarnings("unchecked")
  public int add(T value) {
    if (count == getCapacity()) {
      var newValues = (T[][]) Array.newInstance(clazz, values.length + 1);
      System.arraycopy(values, 0, newValues, 0, values.length);
      newValues[values.length] = (T[]) Array.newInstance(clazz, CHUNK_SIZE);
      values = newValues;
    }
    values[row(count)][col(count)] = value;
    count++;
    return count - 1;
  }

  public T get(int index) {
    return this.values[row(index)][col(index)];
  }

  public void set(int index, T value) {
    this.values[row(index)][col(index)] = value;
  }

  private int row(int index) {
    return index >> DIV_SHIFT;
  }

  private int col(int index) {
    return index & (CHUNK_SIZE - 1);
  }

  private int getCapacity() {
    return values.length * CHUNK_SIZE;
  }

  public int getCount() {
    return count;
  }
}
