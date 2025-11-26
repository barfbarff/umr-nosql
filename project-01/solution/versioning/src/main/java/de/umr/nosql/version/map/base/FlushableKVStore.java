package de.umr.nosql.version.map.base;


import de.umr.nosql.version.list.base.storage.KVStore;

public interface FlushableKVStore extends KVStore, AutoCloseable {
    void flushDB();
}
