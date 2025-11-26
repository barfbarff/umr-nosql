package de.umr.nosql.version.map.simple;

import de.umr.nosql.version.list.base.serializer.JsonSerializer;
import de.umr.nosql.version.list.base.serializer.Serializer;
import de.umr.nosql.version.list.base.storage.KVStore;
import de.umr.nosql.version.list.base.storage.VersionList;
import de.umr.nosql.version.map.base.MultiVersionMap;
import de.umr.nosql.version.map.base.VersionListFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

public class BackedSimpleMVM<K extends Comparable<? super K>, P>
        implements MultiVersionMap<K, P> {

    private final TreeMap<K, VersionList<P>> keyToVersionListMap = new TreeMap<>();

    private final VersionListFactory<P> versionListFactory;

    private final Class<P> payloadType;

    private final KVStore kvStore;

    private long versionNumCounter = 1;


    public BackedSimpleMVM(VersionListFactory<P> versionListFactory,
                           Class<P> payloadType,
                           KVStore kvStore) {
        this.versionListFactory = versionListFactory;
        this.payloadType = payloadType;
        this.kvStore = kvStore;
    }

    @Override
    public Map.Entry<K, P> get(K k, long t) {
        VersionList<P> versionList = keyToVersionListMap.get(k);

        if (versionList == null) {
            return null;
        }

        P visiblePayload = versionList.findVisible(t);

        if (visiblePayload == null) {
            return null;
        }

        return Map.entry(k, visiblePayload);
    }

    @Override
    public long append(K k, P p) {
        long newVersionNum = versionNumCounter++;

        VersionList<P> versionList = keyToVersionListMap.computeIfAbsent(k, key -> {
            Serializer<P> jsonSerializer = new JsonSerializer<>(payloadType);
            return versionListFactory.create(kvStore, jsonSerializer);
        });

        versionList.append(p, newVersionNum);

        return newVersionNum;
    }

    @Override
    public Iterator<Map.Entry<K, P>> rangeSnapshot(K fromKey,
                                                   boolean fromInclusive,
                                                   K toKey,
                                                   boolean toInclusive,
                                                   long timestamp) {
        // Returns the view of the map whose keys are within the specified range
        NavigableMap<K, VersionList<P>> subMap = keyToVersionListMap
                .subMap(fromKey, fromInclusive, toKey, toInclusive);

        // Map each key to its visible payload at the given timestamp
        Stream<Map.Entry<K, P>> entries = streamVisibleEntries(subMap, timestamp);

        // Filter null entries and turn the Stream into an iterator
        return entries.filter(Objects::nonNull).iterator();
    }

    @Override
    public Iterator<Map.Entry<K, P>> snapshot(long timestamp) {
        // Map each key to its visible payload at the given timestamp
        Stream<Map.Entry<K, P>> entries = streamVisibleEntries(keyToVersionListMap, timestamp);

        // Filter null entries and turn the Stream into an iterator
        return entries.filter(Objects::nonNull).iterator();
    }

    private Stream<Map.Entry<K, P>> streamVisibleEntries(Map<K, VersionList<P>> keyToVersionListMap,
                                                         long timestamp) {
        return keyToVersionListMap.entrySet().stream()
                .map(entry -> {
                    K key = entry.getKey();
                    VersionList<P> versionList = entry.getValue();

                    P payload = versionList.findVisible(timestamp);

                    if (payload == null) {
                        return null;
                    }

                    return Map.entry(key, payload);
                });
    }
}
