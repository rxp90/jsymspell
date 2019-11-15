package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.api.LongToStringArrayMap;
import java.util.HashMap;
import java.util.Map;

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

  SuggestionStage(int initialCapacity) {
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

  void commitTo(LongToStringArrayMap permanentLongToStringArrayMap) {
    for (Map.Entry<Long, Entry> entry : deletes.entrySet()) {
      int i = 0;
      String[] suggestions = permanentLongToStringArrayMap.get(entry.getKey());
      if (suggestions != null) {
        i = suggestions.length;
        var newSuggestions = new String[suggestions.length + entry.getValue().count];
        System.arraycopy(suggestions, 0, newSuggestions, 0, suggestions.length);
        permanentLongToStringArrayMap.put(entry.getKey(), newSuggestions);
      } else {
        suggestions = new String[entry.getValue().count];
        permanentLongToStringArrayMap.put(entry.getKey(), suggestions);
      }

      int next = entry.getValue().first;
      while (next >= 0 && i < suggestions.length) {
        var node = nodes.get(next);
        suggestions[i] = node.suggestion;
        next = node.next;
        i++;
      }
    }
  }
}
