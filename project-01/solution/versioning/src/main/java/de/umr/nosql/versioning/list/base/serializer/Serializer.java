package de.umr.nosql.versioning.list.base.serializer;

public interface Serializer<T> {

    String serialize(T t);
    T deSerialize(String serializedT);
}
