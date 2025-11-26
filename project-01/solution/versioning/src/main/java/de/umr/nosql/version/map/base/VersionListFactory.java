package de.umr.nosql.version.map.base;

import de.umr.nosql.version.list.base.serializer.Serializer;
import de.umr.nosql.version.list.base.storage.KVStore;
import de.umr.nosql.version.list.base.storage.VersionList;

public interface VersionListFactory<P> {
    VersionList<P> create(KVStore store, Serializer<P> serializer);
}
