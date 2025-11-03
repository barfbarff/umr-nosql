package de.umr.nosql.redis.model;

import java.io.Serializable;

/**
 * @param productId productId should be unique for every new Product!
 */
public record Product(int productId,
                      String name) implements Serializable {

}
