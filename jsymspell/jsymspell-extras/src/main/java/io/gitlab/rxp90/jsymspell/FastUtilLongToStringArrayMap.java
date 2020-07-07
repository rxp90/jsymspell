package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.api.LongToStringArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class FastUtilLongToStringArrayMap implements LongToStringArrayMap {

    private final Long2ObjectMap<String[]> deletes = new Long2ObjectOpenHashMap<>();

    @Override
    public String[] get(long key) {
        return deletes.get(key);
    }

    @Override
    public String[] put(long key, String[] values) {
        return deletes.put(key, values);
    }
}
