package de.umr.nosql.versioning.list.base.storage;

public interface KVStore {
    void put(String storeKey, String storeValue);
    String get(String storeKey);
}
