package de.umr.nosql.version.map.base;

import redis.clients.jedis.Jedis;

public class SimpleJedisStore implements FlushableKVStore{

    private final Jedis jedis = new Jedis("redis://localhost:6379");

    public SimpleJedisStore() {}

    @Override
    public void flushDB() {
        this.jedis.flushAll();
    }

    @Override
    public void put(String storeKey, String storeValue) {
        jedis.set(storeKey, storeValue);
    }

    @Override
    public String get(String storeKey) {
        return jedis.get(storeKey);
    }

    @Override
    public void close() {
        jedis.close();
    }
}
