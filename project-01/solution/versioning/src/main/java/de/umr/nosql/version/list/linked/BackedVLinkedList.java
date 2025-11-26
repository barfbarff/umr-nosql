package de.umr.nosql.version.list.linked;

import de.umr.nosql.version.list.base.serializer.Serializer;
import de.umr.nosql.version.list.base.storage.KVStore;
import de.umr.nosql.version.list.base.storage.VersionList;
import de.umr.nosql.version.util.KVStoreHelper;
import lombok.Builder;

public class BackedVLinkedList<P> implements VersionList<P> {

    @Builder
    private record Node(String serializedPayload, long nextVersionNum) {}

    private final Serializer<P> payloadSerializer;

    private final KVStoreHelper<Long, Node> kvStoreHelper;

    private long lastVersionNum = 0;

    public BackedVLinkedList(Serializer<P> payloadSerializer, KVStore kvStore) {
        this.payloadSerializer = payloadSerializer;
        this.kvStoreHelper = new KVStoreHelper<>(kvStore, Node.class);
    }

    @Override
    public void append(P p, long timestamp) {
        String serializedPayload = payloadSerializer.serialize(p);

        Node newNode = Node.builder()
                .serializedPayload(serializedPayload)
                .nextVersionNum(lastVersionNum)
                .build();

        kvStoreHelper.putObjectIntoKVStore(timestamp, newNode);

        lastVersionNum = timestamp;
    }

    @Override
    public P findVisible(long timestamp) {
        if (timestamp <= 0 || lastVersionNum == 0) {
            return null;
        }

        long tmpKey = lastVersionNum;

        Node vTmp = kvStoreHelper.getValueByKey(tmpKey);

        if (lastVersionNum <= timestamp) {
            return payloadSerializer.deSerialize(vTmp.serializedPayload());
        }

        while (tmpKey > 0 && tmpKey > timestamp) {
            vTmp = kvStoreHelper.getValueByKey(tmpKey);
            tmpKey = vTmp.nextVersionNum();
        }

        if (tmpKey > 0) {
            vTmp = kvStoreHelper.getValueByKey(tmpKey);
            return payloadSerializer.deSerialize(vTmp.serializedPayload());
        }

        return null;
    }
}
