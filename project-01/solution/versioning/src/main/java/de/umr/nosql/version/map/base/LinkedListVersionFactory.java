package de.umr.nosql.version.map.base;

import de.umr.nosql.version.list.base.serializer.Serializer;
import de.umr.nosql.version.list.base.storage.KVStore;
import de.umr.nosql.version.list.base.storage.VersionList;
import de.umr.nosql.version.list.linked.BackedVLinkedList;

public class LinkedListVersionFactory<P> implements VersionListFactory<P> {

    @Override
    public VersionList<P> create(KVStore store, Serializer<P> serializer) {
        return new BackedVLinkedList<>(store, serializer);
    }
}
