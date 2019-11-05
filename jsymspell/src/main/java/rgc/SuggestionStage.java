package rgc;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SuggestionStage {

  static class Node {
    private final String suggestion;
    private final int next;

    Node(String suggestion, int next) {
      this.suggestion = suggestion;
      this.next = next;
    }
  }

  private static class Entry {
    private int count;
    private int first;

    private Entry(int count, int first) {
      this.count = count;
      this.first = first;
    }

    private void incrCount() {
      this.count++;
    }

    void setFirst(int first) {
      this.first = first;
    }
  }

  private final Map<Long, Entry> deletes;
  private final ChunkArray nodes;

  public SuggestionStage(int initialCapacity) {
    this.deletes = new HashMap<>(initialCapacity);
    this.nodes = new ChunkArray(initialCapacity * 2);
  }

  public int deleteCount() {
    return deletes.size();
  }

  public int nodeCount() {
    return nodes.getCount();
  }

  void add(long deleteHash, String suggestion) {
    Entry entry = deletes.getOrDefault(deleteHash, new Entry(0, -1));
    int next = entry.first;
    entry.incrCount();
    entry.setFirst(nodes.getCount());
    deletes.put(deleteHash, entry);
    nodes.add(new Node(suggestion, next));
  }

  void commitTo(Deletes permanentDeletes) {
    for (Map.Entry<Long, Entry> entry : deletes.entrySet()) {
      int i = 0;
      String[] suggestions = permanentDeletes.get(entry.getKey().longValue());
      if (suggestions != null) {
        i = suggestions.length;
        var newSuggestions = new String[suggestions.length + entry.getValue().count];
        System.arraycopy(suggestions, 0, newSuggestions, 0, suggestions.length);
        permanentDeletes.put(entry.getKey(), newSuggestions);
      } else {
        suggestions = new String[entry.getValue().count];
        permanentDeletes.put(entry.getKey(), suggestions);
      }

      int next = entry.getValue().first;
      while (next >= 0) {
        var node = nodes.get(next);
        suggestions[i] = node.suggestion;
        next = node.next;
        i++;
      }
    }
  }
}
