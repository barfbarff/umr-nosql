package de.umr.nosql.version.util;

import de.umr.nosql.version.list.base.storage.KVStore;
import tools.jackson.databind.ObjectMapper;

public class KVStoreHelper<K, V> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final KVStore kvStore;

    private final Class<V> typeRef;

    public KVStoreHelper(KVStore kvStore, Class<V> typeRef) {
        this.kvStore = kvStore;
        this.typeRef = typeRef;
    }

    public V getValueByKey(K key) {
        String serializedNode = kvStore.get(String.valueOf(key));

        if (serializedNode == null) {
            return null;
        }

        return objectMapper.readValue(serializedNode, typeRef);
    }

    public void putObjectIntoKVStore(K key, Object object) {
        String serializedNode = objectMapper.writeValueAsString(object);
        kvStore.put(String.valueOf(key), serializedNode);
    }
}
