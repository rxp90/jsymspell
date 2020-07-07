package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.SuggestionStage.Node;

import java.util.Arrays;

public class ChunkArray {

    private static final int CHUNK_SIZE = 4096;
    private static final int DIV_SHIFT = 12;

    private SuggestionStage.Node[][] values;
    private int count;

    ChunkArray(int initialCapacity) {
        int chunks = (initialCapacity + CHUNK_SIZE - 1) / CHUNK_SIZE;
        values = new Node[chunks][];
        Arrays.fill(values, new Node[CHUNK_SIZE]);
    }

    public int add(Node value) {
        if (count == getCapacity()) {
            var newValues = new Node[values.length + 1][];
            System.arraycopy(values, 0, newValues, 0, values.length);
            newValues[values.length] = new Node[CHUNK_SIZE];
            values = newValues;
        }
        values[row(count)][col(count)] = value;
        count++;
        return count - 1;
    }

    public Node get(int index) {
        return this.values[row(index)][col(index)];
    }

    public void set(int index, Node value) {
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
