package de.umr.nosql.version.list.frugal;

import de.umr.nosql.version.list.base.serializer.Serializer;
import de.umr.nosql.version.list.base.storage.KVStore;
import de.umr.nosql.version.list.base.storage.VersionList;
import de.umr.nosql.version.map.base.WeavingResult;
import de.umr.nosql.version.util.KVStoreHelper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class BackedWeaverFrugalSkipList<P> implements VersionList<P> {

    @Builder
    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Node {
        String serializedPayload;
        int level;
        long nextKey;
        long ridgyKey;
        long kRidgy;
    }

    private final Serializer<P> payloadSerializer;

    private final KVStoreHelper<Long, Node> kvStoreHelper;

    private long lastVersionNum = 0;

    public BackedWeaverFrugalSkipList(KVStore kvStore, Serializer<P> payloadSerializer) {
        this.payloadSerializer = payloadSerializer;
        this.kvStoreHelper = new KVStoreHelper<>(kvStore, Node.class);
    }

    @Override
    public void append(P p, long timestamp) {
        String serializedPayload = payloadSerializer.serialize(p);

        if (lastVersionNum == 0) {
            initStack(timestamp, serializedPayload);
        } else {
            CoinFace coinFace = CoinFace.toss();

            if (coinFace == CoinFace.HEAD) {
                putOnTopVersionStack(timestamp, serializedPayload);
            } else if (coinFace == CoinFace.TAIL) {
                createNewVersionStack(timestamp, serializedPayload);
            }
        }

        lastVersionNum = timestamp;
    }

    @Override
    public P findVisible(long timestamp) {
        long key = findVisibleKeyFrom(lastVersionNum, timestamp);

        if (key == 0) {
            return null;
        }

        Node node = kvStoreHelper.getValueByKey(key);

        return payloadSerializer.deSerialize(node.getSerializedPayload());
    }

    public long findVisibleKey(long timestamp) {
        return findVisibleKeyFrom(lastVersionNum, timestamp);
    }

    private long findVisibleKeyFrom(long targetVersionNum, long timestamp) {
        if (timestamp <= 0 || lastVersionNum == 0) {
            return 0;
        }

        long tmpKey = targetVersionNum;

        if (tmpKey <= timestamp) {
            return tmpKey;
        }

        while (tmpKey > 0 && tmpKey > timestamp) {
            Node vTmp = kvStoreHelper.getValueByKey(tmpKey);

            if (vTmp.getRidgyKey() > timestamp) {
                tmpKey = vTmp.getRidgyKey();
            } else {
                tmpKey = vTmp.nextKey;
            }
        }

        return (tmpKey > 0) ? tmpKey : 0;
    }

    public WeavingResult<P> findVisibleFrom(long targetVersionNum, long timestamp) {
        long startVersionNum = (targetVersionNum == 0) ? lastVersionNum : targetVersionNum;

        long visibleKey = findVisibleKeyFrom(startVersionNum, timestamp);

        if (visibleKey != 0) {
            Node node = kvStoreHelper.getValueByKey(visibleKey);
            P foundPayload = payloadSerializer.deSerialize(node.getSerializedPayload());

            return WeavingResult.<P>builder()
                    .foundPayload(foundPayload)
                    .kRidgy(node.getKRidgy())
                    .build();
        } else {
            Node node = kvStoreHelper.getValueByKey(startVersionNum);

            return WeavingResult.<P>builder()
                    .foundPayload(null)
                    .kRidgy(node.getKRidgy())
                    .build();
        }
    }

    public void setRidgy(long kRidgy) {
        if (lastVersionNum != 0) {
            Node vSrc = kvStoreHelper.getValueByKey(lastVersionNum);
            vSrc.setKRidgy(kRidgy);
            kvStoreHelper.putObjectIntoKVStore(lastVersionNum, vSrc);
        }
    }

    private void initStack(long timestamp,
                           String serializedPayload) {
        Node vNew = Node.builder()
                .serializedPayload(serializedPayload)
                .level(0)
                .build();

        kvStoreHelper.putObjectIntoKVStore(timestamp, vNew);
    }

    private void putOnTopVersionStack(long timestamp,
                                      String serializedPayload) {
        // Get the vBase node from kvStore using the lastVersionNum key
        Node vBase = kvStoreHelper.getValueByKey(lastVersionNum);

        // Level of the new vNew node
        int vNewLevel = vBase.getLevel() + 1;

        // Temporary key for traversing the kvStore map
        long tmpKey = lastVersionNum;

        // Search for a ridgy processor by traversing ridgy links
        while (tmpKey > 0) {
            // Get the vTmp node from kvStore using the tmpKey key
            Node vTmp = kvStoreHelper.getValueByKey(tmpKey);

            if (vTmp.getLevel() >= vNewLevel) {
                break;
            }

            tmpKey = vTmp.getRidgyKey();
        }

        // Create new vNew node for the new version
        Node vNew = Node.builder()
                .serializedPayload(serializedPayload)
                .level(vNewLevel)
                .nextKey(lastVersionNum)
                .ridgyKey(tmpKey)
                .build();

        kvStoreHelper.putObjectIntoKVStore(timestamp, vNew);
    }

    private void createNewVersionStack(long timestamp,
                                       String serializedPayload) {
        Node vNew = Node.builder()
                .serializedPayload(serializedPayload)
                .level(0)
                .nextKey(lastVersionNum)
                .ridgyKey(lastVersionNum)
                .build();

        kvStoreHelper.putObjectIntoKVStore(timestamp, vNew);
    }
}
