package de.umr.nosql.versioning.list.frugal;

import java.util.Random;

public enum CoinFace {
    HEAD,
    TAIL;

    private static final Random RANDOM = new Random();

    /**
     * Randomly selects a coin face.
     */
    public static CoinFace toss() {
        return RANDOM.nextBoolean() ? HEAD : TAIL;
    }
}
