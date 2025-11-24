package de.umr.nosql.versioning.list.frugal;

import de.umr.nosql.versioning.list.base.serializer.Serializer;
import de.umr.nosql.versioning.list.base.storage.KVStore;
import de.umr.nosql.versioning.list.base.storage.VersionList;
import lombok.Builder;
import tools.jackson.databind.ObjectMapper;

public class BackedFrugalSkipList<P> implements VersionList<P> {

    @Builder
    private record Node(
            String serializedPayload,
            long timestamp,
            int level,
            int nextKey,
            int ridgyKey
    ) {}

    private final Serializer<P> payloadSerializer;

    private final KVStore kvStore;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private int lastVersionNum = 0;

    public BackedFrugalSkipList(Serializer<P> payloadSerializer, KVStore kvStore) {
        this.payloadSerializer = payloadSerializer;
        this.kvStore = kvStore;
    }

    @Override
    public void append(P p, long timestamp) {
        int newVersionNum = lastVersionNum + 1;

        String serializedPayload = payloadSerializer.serialize(p);

        if (lastVersionNum == 0) {
            initStack(newVersionNum, serializedPayload, timestamp);
            lastVersionNum = newVersionNum;
            return;
        }

        CoinFace coinFace = CoinFace.toss();

        if (coinFace == CoinFace.HEAD) {
            putOnTopVersionStack(newVersionNum, serializedPayload, timestamp);
        } else if (coinFace == CoinFace.TAIL) {
            createNewVersionStack(newVersionNum, serializedPayload, timestamp);
        }

        lastVersionNum = newVersionNum;
    }

    @Override
    public P findVisible(long timestamp) {
        // Temporary node for traversing the Frugal Skip List
        Node vTmp = getNodeByKey(lastVersionNum);

        while (vTmp != null && vTmp.timestamp() > timestamp) {
            Node vRidgy = getNodeByKey(vTmp.ridgyKey());

            if (vRidgy != null && vRidgy.timestamp() > timestamp) {
                vTmp = vRidgy;
            } else {
                vTmp = getNodeByKey(vTmp.nextKey());
            }
        }

        if (vTmp != null) {
            String serializedPayload = vTmp.serializedPayload();
            return payloadSerializer.deSerialize(serializedPayload);
        }

        return null;
    }

    private void initStack(int newVersionNum,
                           String serializedPayload,
                           long timestamp) {
        Node vNew = Node.builder()
                .serializedPayload(serializedPayload)
                .timestamp(timestamp)
                .level(0)
                .build();

        putNodeIntoKVStore(newVersionNum, vNew);
    }

    private void putOnTopVersionStack(int newVersionNum,
                                      String serializedPayload,
                                      long timestamp) {
        // Get the vBase node from kvStore using the lastVersionNum key
        Node vBase = getNodeByKey(lastVersionNum);

        // Level of the new vNew node
        int vNewLevel = vBase.level() + 1;

        // Temporary key for traversing the kvStore map
        int tmpKey = lastVersionNum;

        // Search for a ridgy processor by traversing ridgy links
        while (tmpKey > 0) {
            // Get the vTmp node from kvStore using the tmpKey key
            Node vTmp = getNodeByKey(tmpKey);

            if (vTmp.level() >= vNewLevel) {
                break;
            }

            tmpKey = vTmp.ridgyKey();
        }

        // Create new vNew node for the new version
        Node vNew = Node.builder()
                .serializedPayload(serializedPayload)
                .timestamp(timestamp)
                .level(vNewLevel)
                .nextKey(lastVersionNum)
                .ridgyKey(tmpKey)
                .build();

        putNodeIntoKVStore(newVersionNum, vNew);
    }

    private void createNewVersionStack(int newVersionNum,
                                       String serializedPayload,
                                       long timestamp) {
        Node vNew = Node.builder()
                .serializedPayload(serializedPayload)
                .timestamp(timestamp)
                .level(0)
                .nextKey(lastVersionNum)
                .ridgyKey(lastVersionNum)
                .build();

        putNodeIntoKVStore(newVersionNum, vNew);
    }

    private Node getNodeByKey(int key) {
        if (key == 0) {
            return null;
        }

        if (key < 0 || key > lastVersionNum) {
            throw new IllegalArgumentException("Invalid key request: " + key +
                    ". Valid keys should be integers from 0 to " + lastVersionNum);
        }

        // Get serialized string of node from kvStore
        String serializedNode = kvStore.get(String.valueOf(key));

        if (serializedNode == null) {
            throw new IllegalStateException("Data Corruption: Version " + key +
                    " is missing from KVStore");
        }

        // Deserialize the string into node
        return objectMapper.readValue(serializedNode, Node.class);
    }

    private void putNodeIntoKVStore(long newVersionNum, Node node) {
        String serializedNode = objectMapper.writeValueAsString(node);
        kvStore.put(String.valueOf(newVersionNum), serializedNode);
    }
}
