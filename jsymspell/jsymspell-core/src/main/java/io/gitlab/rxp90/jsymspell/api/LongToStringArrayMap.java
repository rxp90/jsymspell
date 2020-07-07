package io.gitlab.rxp90.jsymspell.api;

public interface LongToStringArrayMap {
    String[] get(long key);

    String[] put(long key, String[] values);
}
