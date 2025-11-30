package de.umr.nosql.version.map.weaver;

import de.umr.nosql.version.map.base.BenchmarkJedisStore;
import de.umr.nosql.version.map.base.FlushableKVStore;
import de.umr.nosql.version.map.base.MultiVersionMap;
import de.umr.nosql.version.map.base.SimpleJedisStore;
import de.umr.nosql.version.map.base.WeaverSkipListVersionFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Test {
    public record Payload(String title, String comment, String timestamp){}

    public static void main(String[] args) {


        /* -- Test -- */
        /* You can use the provided method to read the data like
        List<Map.Entry<String, Payload>> data = readData("path/to/test_data.csv");

        Correct output for range ["KEY002", "KEY004"] and timestamp 20 is
        KEY002=Payload[title=Some Title for KEY002, comment=Change 3 for key KEY002, timestamp=19]
        KEY003=Payload[title=Some Title for KEY003, comment=Change 4 for key KEY003, timestamp=20]
        KEY004=Payload[title=Some Title for KEY004, comment=Change 3 for key KEY004, timestamp=13]
         */

        /* -- Benchmark -- */
        /* CPU: Intel(R) Core(TM) i7-8750H CPU @ 2.20GHz (2.20 GHz)

        We are going to provide benchmarks for two implementations of FlushableKVStore
        (+) BenchmarkJedisStore: For the sake of constructing a MultiVersionMap when we test
                                 bench_mark.csv, as the file contains 500,000 records. In short,
                                 this class mitigates network latency when it comes to insertion
                                 by doing multiple bulk insertions into Redis instead of making a
                                 single request to Redis for every record. Additionally, as
                                 BackFrugalSkipList needs to keep track of previous nodes to
                                 traverse ridgy links and find a ridgy processor whenever a new data
                                 item is added to the SkipList, we also cache entries that have been
                                 added to Redis to reduce multiple requests to Redis while
                                 traversing ridgy links. However, the queries made after all 500,000
                                 records are inserted into Redis are retrieved directly from Redis.
                                 You can find more details in the implementation of the class!
        (+) SimpleJedisStore: Plain and generic implementation for Jedis. For testing
                              MultiVersionMap on test_data.csv, using SimpleJedisStore is totally
                              okay, asit contains only 100 records. We also include the benchmark
                              for testing MultiVersionMap on benchmark_data.csv with
                              SimpleJedisStore as we are unsure whether this is a hard requirement
                              of the task.

        ~~~ BackedWeaverFrugalSkipList with BenchmarkJedisStore ~~~
        Elapsed time of iteration 0
        (+) Insertion time: 8.6346444s
        (+) Query time: 0.2355769s
        Elapsed time of iteration 1
        (+) Insertion time: 7.5080826s
        (+) Query time: 0.1475895s
        Elapsed time of iteration 2
        (+) Insertion time: 7.6857282s
        (+) Query time: 0.0958363s
        Average Elapsed Time (in seconds): 8.102485966666666s
         */

        InputStream testDataStream = Test.class.getResourceAsStream("/test_data.csv");
        InputStream benchmarkDataStream = Test.class.getResourceAsStream("/benchmark_data.csv");

        try {
            List<Map.Entry<String, Payload>> testData = readData(testDataStream);
            testPartB(testData);

            List<Map.Entry<String, Payload>> benchmarkData = readData(benchmarkDataStream);
            testPartCWithBenchmarkJedis(benchmarkData);
//            testPartCWithSimpleJedis(benchmarkData);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Map.Entry<String, Payload>> readData(InputStream inputStream) throws IOException {
        List<Map.Entry<String, Payload>> l = new ArrayList<>();

        String line;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            // Skip header
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",", 4); // key,title,comment,"version"
                String key = values[0];
                String title = values[1];
                String comment = values[2];
                String timestamp = values[3];

                l.add(new AbstractMap.SimpleEntry<>(key, new Payload(title, comment, timestamp)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return l;
    }

    private static void testPartB(List<Map.Entry<String, Payload>> data) {
        try (SimpleJedisStore simpleJedisStore = new SimpleJedisStore()) {
            System.out.println("+".repeat(40) + " TASK 1.3 (PART B) " + "+".repeat(40));

            System.out.println("~".repeat(35) + " BackedWeaverFrugalSkipList " + "~".repeat(35));
            WeaverSkipListVersionFactory<Payload> versionListFactory = new WeaverSkipListVersionFactory<>();
            runTestPartB(data, versionListFactory, simpleJedisStore);

            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runTestPartB(List<Map.Entry<String, Payload>> data,
                                     WeaverSkipListVersionFactory<Payload> versionListFactory,
                                     FlushableKVStore kvStore) {
        kvStore.flushDB();

        MultiVersionMap<String, Payload> multiVersionMap = new BackedVWeaverMVM<>(
                versionListFactory,
                Payload.class,
                kvStore
        );

        data.forEach(entry -> {
           String key = entry.getKey();
           Payload payload = entry.getValue();

           multiVersionMap.append(key, payload);
        });

        Iterator<Map.Entry<String, Payload>> snapshots = multiVersionMap
                .rangeSnapshot("KEY002", true, "KEY004", true, 20);

        while (snapshots.hasNext()) {
            Map.Entry<String, Payload> entry = snapshots.next();
            System.out.println(entry);
        }
    }

    private static void testPartCWithBenchmarkJedis(List<Map.Entry<String, Payload>> data) {
        try (FlushableKVStore benchmarkJedisStore = new BenchmarkJedisStore()) {
            testPartC(data, benchmarkJedisStore, benchmarkJedisStore.getClass().getSimpleName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void testPartCWithSimpleJedis(List<Map.Entry<String, Payload>> data) {
        try (FlushableKVStore simpleJedisStore = new SimpleJedisStore()) {
            testPartC(data, simpleJedisStore, simpleJedisStore.getClass().getSimpleName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void testPartC(List<Map.Entry<String, Payload>> data,
                                  FlushableKVStore kvStore,
                                  String kvStoreUsed) {
        System.out.println("+".repeat(40) + " TASK 1.3 (PART C) " + "+".repeat(40));

        System.out.println("~".repeat(35) + " BackedWeaverFrugalSkipList with " + kvStoreUsed + " " +
                "~".repeat(35));
        WeaverSkipListVersionFactory<Payload> versionListFactory = new WeaverSkipListVersionFactory<>();
        runTestPartC(data, versionListFactory, kvStore, 5);

        System.out.println();
    }

    private static void runTestPartC(List<Map.Entry<String, Payload>> data,
                                     WeaverSkipListVersionFactory<Payload> versionListFactory,
                                     FlushableKVStore kvStore,
                                     int iterations) {
        int[] testTimestamps = new int[] {10, 100, 500, 1000, 5000, 10000, 50000, 100000, 500000};
        double[] elapsedTimes = new double[iterations];

        for (int i = 0; i < iterations; i++) {
            kvStore.flushDB();

            System.out.println("Elapsed time of iteration " + i);

            MultiVersionMap<String, Payload> multiVersionMap = new BackedVWeaverMVM<>(
                    versionListFactory,
                    Payload.class,
                    kvStore
            );

            long startInsertionTime = System.nanoTime();

            data.forEach(entry -> {
                String key = entry.getKey();
                Payload payload = entry.getValue();

                multiVersionMap.append(key, payload);
            });

            long endInsertionTime = System.nanoTime();
            double insertionTime = (endInsertionTime - startInsertionTime)/ 1_000_000_000.0;

            System.out.println("(+) Insertion time: " + insertionTime + "s");

            if (kvStore instanceof BenchmarkJedisStore benchmarkJedisStore) {
                benchmarkJedisStore.enableQueryMode();
            }

            long startQueryTime  = System.nanoTime();

            for (int testTimestamp : testTimestamps) {
                var it = multiVersionMap.snapshot(testTimestamp);

                while(it.hasNext()) {
                    it.next();
                }
            }

            long endQueryTime = System.nanoTime();
            double queryTime = (endQueryTime - startQueryTime)/ 1_000_000_000.0;

            elapsedTimes[i] = insertionTime + queryTime;

            System.out.println("(+) Query time: " + queryTime + "s");
        }

        double averageElapsedTime =  Arrays.stream(elapsedTimes).sum() / iterations;

        System.out.println("Average Elapsed Time (in seconds): " + averageElapsedTime + "s");
    }
}
