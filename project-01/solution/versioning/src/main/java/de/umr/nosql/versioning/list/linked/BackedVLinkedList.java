package de.umr.nosql.versioning.list.linked;

import de.umr.nosql.versioning.list.base.serializer.Serializer;
import de.umr.nosql.versioning.list.base.storage.KVStore;
import de.umr.nosql.versioning.list.base.storage.VersionList;
import lombok.Builder;
import tools.jackson.databind.ObjectMapper;

public class BackedVLinkedList<P> implements VersionList<P> {

    @Builder
    private record Node(String serializedPayload, long timestamp, long nextVersionNum) {}

    private final Serializer<P> payloadSerializer;

    private final KVStore kvStore;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private int lastVersionNum = 0;

    public BackedVLinkedList(Serializer<P> payloadSerializer, KVStore kvStore) {
        this.payloadSerializer = payloadSerializer;
        this.kvStore = kvStore;
    }

    @Override
    public void append(P p, long timestamp) {
        int newVersionNum = lastVersionNum + 1;

        String serializedPayload = payloadSerializer.serialize(p);

        Node newNode = Node.builder()
                .serializedPayload(serializedPayload)
                .timestamp(timestamp)
                .nextVersionNum(lastVersionNum)
                .build();

        String serializedNode = objectMapper.writeValueAsString(newNode);

        kvStore.put(String.valueOf(newVersionNum), serializedNode);

        lastVersionNum = newVersionNum;
    }

    @Override
    public P findVisible(long timestamp) {
        if (lastVersionNum == 0) {
            return null;
        }

        String serializedNode = kvStore.get(String.valueOf(lastVersionNum));

        while (serializedNode != null) {
            Node node = objectMapper.readValue(serializedNode, Node.class);

            if (node.timestamp() <= timestamp) {
                String serializedPayload = node.serializedPayload();
                return payloadSerializer.deSerialize(serializedPayload);
            }

            serializedNode = kvStore.get(String.valueOf(node.nextVersionNum()));
        }

        return null;
    }
}
