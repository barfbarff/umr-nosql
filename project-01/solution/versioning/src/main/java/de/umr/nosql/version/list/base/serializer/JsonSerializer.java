package de.umr.nosql.version.list.base.serializer;

import tools.jackson.databind.ObjectMapper;

public class JsonSerializer<T> implements Serializer<T> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Class<T> type;

    public JsonSerializer(Class<T> type) {
        this.type = type;
    }

    @Override
    public String serialize(T t) {
        return objectMapper.writeValueAsString(t);
    }

    @Override
    public T deSerialize(String serializedT) {
        return objectMapper.readValue(serializedT, this.type);
    }
}
