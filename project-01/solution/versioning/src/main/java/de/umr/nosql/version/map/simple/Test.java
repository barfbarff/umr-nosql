package de.umr.nosql.version.map.simple;

import de.umr.nosql.version.map.base.BenchmarkJedisStore;
import de.umr.nosql.version.map.base.FlushableKVStore;
import de.umr.nosql.version.map.base.LinkedListVersionFactory;
import de.umr.nosql.version.map.base.MultiVersionMap;
import de.umr.nosql.version.map.base.SimpleJedisStore;
import de.umr.nosql.version.map.base.SkipListVersionFactory;
import de.umr.nosql.version.map.base.VersionListFactory;

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

        ~~~ BackedVLinkedList with BenchmarkJedisStore ~~~
        Elapsed time of iteration 0 (in seconds): 4.739177300000001s
        (+) Insertion time: 4.7378501s
        (+) Query time: 0.0013272s
        Elapsed time of iteration 1 (in seconds): 3.683895s
        (+) Insertion time: 3.6837894s
        (+) Query time: 1.056E-4s
        Elapsed time of iteration 2 (in seconds): 3.5809002999999997s
        (+) Insertion time: 3.580837s
        (+) Query time: 6.33E-5s
        Average Elapsed Time (in seconds): 4.0013242s

        ~~~ BackedFrugalSkipList with BenchmarkJedisStore ~~~
        Elapsed time of iteration 0 (in seconds): 6.2811196s
        (+) Insertion time: 6.2810556s
        (+) Query time: 6.4E-5s
        Elapsed time of iteration 1 (in seconds): 5.7125164s
        (+) Insertion time: 5.7124616s
        (+) Query time: 5.48E-5s
        Elapsed time of iteration 2 (in seconds): 5.544826100000001s
        (+) Insertion time: 5.5447547s
        (+) Query time: 7.14E-5s
        Average Elapsed Time (in seconds): 5.846154033333334s

        ~~~ BackedVLinkedList with SimpleJedisStore ~~~
        Elapsed time of iteration 0 (in seconds): 276.7862449s
        (+) Insertion time: 276.7861541s
        (+) Query time: 9.08E-5s
        Elapsed time of iteration 1 (in seconds): 259.98836750000004s
        (+) Insertion time: 259.988282s
        (+) Query time: 8.55E-5s
        Elapsed time of iteration 2 (in seconds): 274.225065s
        (+) Insertion time: 274.2250003s
        (+) Query time: 6.47E-5s
        Average Elapsed Time (in seconds): 270.3332258s

        ~~~ BackedFrugalSkipList with SimpleJedisStore ~~~
        Elapsed time of iteration 0 (in seconds): 767.8726054s
        (+) Insertion time: 767.8724951s
        (+) Query time: 1.103E-4s
        Elapsed time of iteration 1 (in seconds): 772.6001374s
        (+) Insertion time: 772.600005s
        (+) Query time: 1.324E-4s
        Elapsed time of iteration 2 (in seconds): 776.2818867999999s
        (+) Insertion time: 776.2818163s
        (+) Query time: 7.05E-5s
        Average Elapsed Time (in seconds): 772.2515432s
         */

        InputStream testDataStream = Test.class.getResourceAsStream("/test_data.csv");
        InputStream benchmarkDataStream = Test.class.getResourceAsStream("/benchmark_data.csv");

        try {
            List<Map.Entry<String, Payload>> testData = readData(testDataStream);
            testPartB(testData);

            List<Map.Entry<String, Payload>> benchmarkData = readData(benchmarkDataStream);
            testPartCWithBenchmarkJedis(benchmarkData);
            testPartCWithSimpleJedis(benchmarkData);

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

            System.out.println("~".repeat(35) + " BackedVLinkedList " + "~".repeat(35));
            LinkedListVersionFactory<Payload> linkedListVersionFactory =
                    new LinkedListVersionFactory<>();
            runTestPartB(data, linkedListVersionFactory, simpleJedisStore);

            System.out.println();

            System.out.println("~".repeat(35) + " BackedFrugalSkipList " + "~".repeat(35));
            SkipListVersionFactory<Payload> skipListVersionFactory = new SkipListVersionFactory<>();
            runTestPartB(data, skipListVersionFactory, simpleJedisStore);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runTestPartB(List<Map.Entry<String, Payload>> data,
                                     VersionListFactory<Payload> versionListFactory,
                                     FlushableKVStore kvStore) {
        kvStore.flushDB();

        MultiVersionMap<String, Payload> multiVersionMap = new BackedSimpleMVM<>(
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
        System.out.println();

        System.out.println("+".repeat(40) + " TASK 1.3 (PART C) " + "+".repeat(40));

        System.out.println("~".repeat(35) + " BackedVLinkedList with " + kvStoreUsed +
                " ~".repeat(35));
        LinkedListVersionFactory<Payload> linkedListVersionFactory =
                new LinkedListVersionFactory<>();
        runTestPartC(data, linkedListVersionFactory, kvStore, 3);

        System.out.println();

        System.out.println("~".repeat(35) + " BackedFrugalSkipList with " + kvStoreUsed +
                " ~".repeat(35));
        SkipListVersionFactory<Payload> skipListVersionFactory = new SkipListVersionFactory<>();
        runTestPartC(data, skipListVersionFactory, kvStore, 3);
    }

    private static void runTestPartC(List<Map.Entry<String, Payload>> data,
                                     VersionListFactory<Payload> versionListFactory,
                                     FlushableKVStore kvStore,
                                     int iterations) {
        kvStore.flushDB();

        int[] testTimestamps = new int[] {10, 100, 500, 1000, 5000, 10000, 50000, 100000, 500000};
        double[] elapsedTimes = new double[iterations];

        for (int i = 0; i < iterations; i++) {
            MultiVersionMap<String, Payload> multiVersionMap = new BackedSimpleMVM<>(
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

            long startQueryTime  = System.nanoTime();

            for (int testTimestamp : testTimestamps) {
                multiVersionMap.snapshot(testTimestamp);
            }

            long endQueryTime = System.nanoTime();
            double queryTime = (endQueryTime - startQueryTime)/ 1_000_000_000.0;

            elapsedTimes[i] = insertionTime + queryTime;

            System.out.println("Elapsed time of iteration " + i + " (in seconds): " +
                    elapsedTimes[i] + "s");
            System.out.println("(+) Insertion time: " + insertionTime + "s");
            System.out.println("(+) Query time: " + queryTime + "s");
        }

        double averageElapsedTime =  Arrays.stream(elapsedTimes).sum() / iterations;

        System.out.println("Average Elapsed Time (in seconds): " + averageElapsedTime + "s");
    }
}
