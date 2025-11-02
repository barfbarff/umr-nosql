package de.umr.nosql.redis.jedis;

import java.io.Serializable;

/**
 * @param productId productId should be unique for every new Product!
 */
public record Product(int productId, String name) implements Serializable {

}
