package de.umr.nosql.version.list.base.storage;

import java.util.HashMap;
import java.util.Map;

public class MapKVStore implements KVStore {

    private final Map<String, String> map = new HashMap<>();

    @Override
    public void put(String storeKey, String storeValue) {
        map.put(storeKey,storeValue);
    }

    @Override
    public String get(String storeKey) {
        return map.get(storeKey);
    }
}
