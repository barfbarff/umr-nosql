package de.umr.nosql.version.map.weaver;

import de.umr.nosql.version.list.base.serializer.JsonSerializer;
import de.umr.nosql.version.list.base.serializer.Serializer;
import de.umr.nosql.version.list.base.storage.KVStore;
import de.umr.nosql.version.list.base.storage.VersionList;
import de.umr.nosql.version.list.frugal.BackedWeaverFrugalSkipList;
import de.umr.nosql.version.map.base.MultiVersionMap;
import de.umr.nosql.version.map.base.WeaverSkipListVersionFactory;
import de.umr.nosql.version.map.base.WeavingResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class BackedVWeaverMVM<K extends Comparable<? super K>, P>
        implements MultiVersionMap<K, P> {

    private final TreeMap<K, BackedWeaverFrugalSkipList<P>> keyToVersionListMap = new TreeMap<>();

    private final WeaverSkipListVersionFactory<P> versionListFactory;

    private final Map<Long, K> ridgyLookupMap = new HashMap<>();

    private final Class<P> payloadType;

    private final KVStore kvStore;

    private long versionNumCounter = 1;

    public BackedVWeaverMVM(WeaverSkipListVersionFactory<P> versionListFactory,
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

        BackedWeaverFrugalSkipList<P> versionList = keyToVersionListMap.computeIfAbsent(k, key -> {
            Serializer<P> jsonSerializer = new JsonSerializer<>(payloadType);
            return versionListFactory.create(kvStore, jsonSerializer);
        });

        K nextKey = keyToVersionListMap.higherKey(k);

        if (nextKey != null) {
            BackedWeaverFrugalSkipList<P> nextVersionList = keyToVersionListMap
                    .get(keyToVersionListMap.higherKey(k));

            long kRidgy = nextVersionList.findVisibleKey(newVersionNum);

            if (kRidgy != 0) {
                ridgyLookupMap.put(kRidgy, nextKey);
                versionList.setRidgy(kRidgy);
            }
        }

        versionList.append(p, newVersionNum);

        return newVersionNum;
    }

    @Override
    public Iterator<Map.Entry<K, P>> rangeSnapshot(K fromKey,
                                                   boolean fromInclusive,
                                                   K toKey,
                                                   boolean toInclusive,
                                                   long timestamp) {
        if (keyToVersionListMap.isEmpty()) {
            return null;
        }

        Map<K, P> result = new HashMap<>();

        NavigableMap<K, BackedWeaverFrugalSkipList<P>> subMap;
        if (fromKey == null && toKey == null) {
            subMap = keyToVersionListMap;
        } else if (fromKey != null && toKey != null) {
            subMap = keyToVersionListMap.subMap(fromKey, fromInclusive, toKey, toInclusive);
        } else if (toKey == null) {
            subMap = keyToVersionListMap.tailMap(fromKey, fromInclusive);
        } else {
            subMap = keyToVersionListMap.headMap(toKey, toInclusive);
        }

        K kCurr = (fromKey == null) ? keyToVersionListMap.firstKey() : fromKey;

        long kRidgy = 0;

        while (kCurr != null) {
            if (!subMap.containsKey(kCurr)) {
                break;
            }

            BackedWeaverFrugalSkipList<P> currVersionList = subMap.get(kCurr);
            WeavingResult<P> weavingResult = currVersionList.findVisibleFrom(kRidgy, timestamp);

            if (weavingResult.foundPayload() != null) {
                result.put(kCurr, weavingResult.foundPayload());
            }

            kRidgy = weavingResult.kRidgy();

            if (kRidgy != 0) {
                kCurr = ridgyLookupMap.get(kRidgy);
            } else {
                kCurr = subMap.higherKey(kCurr);
            }
        }

        return result.entrySet().iterator();
    }

    @Override
    public Iterator<Map.Entry<K, P>> snapshot(long timestamp) {
        return rangeSnapshot(null, true, null, true, timestamp);
    }
}
