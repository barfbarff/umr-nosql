package de.umr.nosql.version.list.frugal;

import de.umr.nosql.version.list.base.serializer.Serializer;
import de.umr.nosql.version.list.base.storage.KVStore;
import de.umr.nosql.version.list.base.storage.VersionList;
import de.umr.nosql.version.util.KVStoreHelper;
import lombok.Builder;

public class BackedFrugalSkipList<P> implements VersionList<P> {

    @Builder
    private record Node(
            String serializedPayload,
            int level,
            long nextKey,
            long ridgyKey
    ) {}

    private final Serializer<P> payloadSerializer;

    private final KVStoreHelper<Long, Node> kvStoreHelper;

    private long lastVersionNum = 0;

    public BackedFrugalSkipList(KVStore kvStore, Serializer<P> payloadSerializer) {
        this.payloadSerializer = payloadSerializer;
        this.kvStoreHelper = new KVStoreHelper<>(kvStore, Node.class);
    }

    @Override
    public void append(P p, long timestamp) {
        String serializedPayload = payloadSerializer.serialize(p);

        if (lastVersionNum == 0) {
            initStack(timestamp, serializedPayload);
            lastVersionNum = timestamp;
            return;
        }

        CoinFace coinFace = CoinFace.toss();

        if (coinFace == CoinFace.HEAD) {
            putOnTopVersionStack(timestamp, serializedPayload);
        } else if (coinFace == CoinFace.TAIL) {
            createNewVersionStack(timestamp, serializedPayload);
        }

        lastVersionNum = timestamp;
    }

    @Override
    public P findVisible(long timestamp) {
        if (timestamp <= 0 || lastVersionNum == 0) {
            return null;
        }

        long tmpKey = lastVersionNum;

        Node vTmp = kvStoreHelper.getValueByKey(tmpKey);

        if (tmpKey <= timestamp) {
            return payloadSerializer.deSerialize(vTmp.serializedPayload());
        }

        while (tmpKey > 0 && tmpKey > timestamp) {
            vTmp = kvStoreHelper.getValueByKey(tmpKey);

            if (vTmp.ridgyKey() > timestamp) {
                tmpKey = vTmp.ridgyKey();
            } else {
                tmpKey = vTmp.nextKey();
            }
        }

        if (tmpKey > 0) {
            vTmp = kvStoreHelper.getValueByKey(tmpKey);
            return payloadSerializer.deSerialize(vTmp.serializedPayload());
        }

        return null;
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
        int vNewLevel = vBase.level() + 1;

        // Temporary key for traversing the kvStore map
        long tmpKey = lastVersionNum;

        // Search for a ridgy processor by traversing ridgy links
        while (tmpKey > 0) {
            // Get the vTmp node from kvStore using the tmpKey key
            Node vTmp = kvStoreHelper.getValueByKey(tmpKey);

            if (vTmp.level() >= vNewLevel) {
                break;
            }

            tmpKey = vTmp.ridgyKey();
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
