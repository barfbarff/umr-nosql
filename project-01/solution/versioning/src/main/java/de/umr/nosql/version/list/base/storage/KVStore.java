package de.umr.nosql.version.list.base.storage;

public interface KVStore {
    void put(String storeKey, String storeValue);
    String get(String storeKey);
}
