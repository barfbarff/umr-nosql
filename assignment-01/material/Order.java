package de.umr.nosql.redis.jedis;

import java.io.Serializable;
import java.util.List;

/**
 * @param orderId orderId should be unique for every new Order!
 * @param items   List containing all items (Products) of an order.
 *                For this exercise there should be at least one product in every order!
 */
public record Order(int orderId, String shippingAddress, List<Product> items) implements Serializable {

}
