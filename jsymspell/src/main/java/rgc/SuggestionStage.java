package rgc;

import java.util.HashMap;
import java.util.Map;

public class SuggestionStage {

  public class Node {
    private final String suggestion;
    private final int next;

    public Node(String suggestion, int next) {
      this.suggestion = suggestion;
      this.next = next;
    }
  }

  private class Entry {
    private int count;
    private int first;

    private Entry(int count, int first) {
      this.count = count;
      this.first = first;
    }

    private int incrCount() {
      this.count++;
      return count;
    }

    public void setFirst(int first) {
      this.first = first;
    }
  }

  private final Map<Number, Entry> deletes;
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

  void commitTo(Map<Number, String[]> permanentDeletes) {
    for (Map.Entry<Number, Entry> entry : deletes.entrySet()) {
      int i = 0;
      String[] suggestions = permanentDeletes.get(entry.getKey());
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
