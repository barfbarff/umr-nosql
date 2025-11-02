package de.umr.nosql.redis.jedis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/*
 * customerId should be unique for every new customer!
 */

/*
 * List<Order> orders is the list containing all orders the customer ever made.
 * For this exercise there should be at least one order for every randomly generated customer!
 */
public record Customer(int customerId, String userName, String address, List<Order> orders) implements Serializable {
    public Customer(int customerId, String userName, List<Order> orders) {
        this(customerId, userName, "N/A", orders);
    }

    public Customer(int customerId, String userName) {
        this(customerId, userName, "N/A", new ArrayList<>());
    }

    public void addOrder(Order order) {
        orders.add(order);
    }
}

