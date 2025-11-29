package de.umr.nosql.version.map.base;

import lombok.Builder;

@Builder
public record WeavingResult<P>(
        P foundPayload,
        long kRidgy
) {}
