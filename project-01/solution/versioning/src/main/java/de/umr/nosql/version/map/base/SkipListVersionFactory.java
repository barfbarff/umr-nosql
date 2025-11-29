package de.umr.nosql.version.map.base;

import de.umr.nosql.version.list.base.serializer.Serializer;
import de.umr.nosql.version.list.base.storage.KVStore;
import de.umr.nosql.version.list.frugal.BackedFrugalSkipList;

public class SkipListVersionFactory<P> implements VersionListFactory<P> {

    @Override
    public BackedFrugalSkipList<P> create(KVStore store, Serializer<P> serializer) {
        return new BackedFrugalSkipList<>(store, serializer);
    }
}
