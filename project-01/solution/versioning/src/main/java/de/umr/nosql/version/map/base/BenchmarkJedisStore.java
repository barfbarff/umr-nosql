package de.umr.nosql.version.map.base;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.HashMap;
import java.util.Map;

// (+) This class is specifically designed for testing MultiversionMap on bench_mark.csv
// (+) Any other uses are not recommended.
// (+) This class is designed to facilitate bulk insertion into Redis, as bench_mark.csv
//     contains 500,000 records, and it is extremely slow to make a single request to
//     Redis for every record
public class BenchmarkJedisStore implements FlushableKVStore{

    // The number of records to be inserted into Redis at a time. Bulk insertion
    // avoids making multiple requests to Redis, and thus reduces RTT
    private static final int BATCH_SIZE = 5000;

    private final Jedis jedis = new Jedis("redis://localhost:6379");

    // (+) This Map is dedicated to BackedFrugalSkipList as it needs to know previous nodes
    //     to traverse ridgy links and find a ridgy processor.
    // (+) Without this, we may face errors in certain cases where the function get() is
    //     executed while no records have been flushed into Redis yet.
    // (+) This also serves as a quick in-memory lookup for construction of a
    //     BackedFrugalSkipList. Without this, a search for a ridgy processor may involve
    //     numerous calls to Redis, and thus increases RTT tremendously.
    private final Map<String, String> cacheForSkipList = new HashMap<>();

    private final Map<String, String> writeBuffer = new HashMap<>();

    // Track the number of requests for put data into Redis. The requests are only
    // executed when the number of requests equals BATCH_SIZE
    private long putRequestNum = 0;

    boolean queryMode = false;

    public BenchmarkJedisStore() {}

    @Override
    public void flushDB() {
        this.jedis.flushAll();
        this.putRequestNum = 0;
        this.queryMode = false;
        this.writeBuffer.clear();
        this.cacheForSkipList.clear();
    }

    @Override
    public void put(String storeKey, String storeValue) {
        writeBuffer.put(storeKey, storeValue);

        // Cache storeKey and storeValue for BackedFrugalSkipList
        cacheForSkipList.put(storeKey, storeValue);

        // Increment the number of requests for putting data into Redis
        // If it divides by BATCH_SIZE, executes pipeline.sync() to insert
        // all records into Redis at once
        putRequestNum++;
        if (putRequestNum % BATCH_SIZE == 0) {
            flushWriteBuffer();
        }
    }

    @Override
    public String get(String storeKey) {
        // Once all records in benchmark_data.csv are inserted to Redis, we always
        // get data from Redis. This ensures that the queries made after insertions
        // are always retrieved from Redis
        if (queryMode) {
            return jedis.get(storeKey);
        }

        // Otherwise, we check the cache of most recent values. This is necessary as
        // BackedFrugalSkipList needs to get values associated with keys. However,
        // the problem is data may not have been inserted into Redis as the number of
        // requests has not yet divided by BATCH_SIZE
        if (cacheForSkipList.containsKey(storeKey)) {
            return cacheForSkipList.get(storeKey);
        }

        return null;
    }

    public void enableQueryMode() {
        if (!writeBuffer.isEmpty()) {
            flushWriteBuffer();
        }

        this.queryMode = true;
        this.cacheForSkipList.clear();
    }

    private void flushWriteBuffer() {
        try (Pipeline pipeline = jedis.pipelined()) {
            writeBuffer.forEach(pipeline::set);
            pipeline.sync();
        }
        writeBuffer.clear();
    }

    @Override
    public void close() {
        jedis.close();
    }
}
