package de.umr.nosql.redis.model;

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
public record Customer(int customerId,
                       String userName,
                       String address,
                       List<Order> orders) implements Serializable {
    public Customer(int customerId, String userName, List<Order> orders) {
        this(customerId, userName, "N/A", orders);
    }

    public Customer(int customerId, String userName) {
        this(customerId, userName, "N/A", new ArrayList<>());
    }

    public void addOrder(Order order) {
        if (order != null) {
            orders.add(order);
        }
    }

    @Override
    public String toString() {
        return "Customer[" +
                "customerId=" + customerId + ", " +
                "userName=" + userName + ", " +
                "address=" + address + ", " +
                "ordersIds=" + getOrderIds() +
                "]";
    }

    private String getOrderIds() {
        StringBuilder orderIds = new StringBuilder();

        orderIds.append("[");

        for (int i = 0; i < orders.size(); i++) {
            orderIds.append(orders.get(i).orderId());

            if (i < orders.size() - 1) {
                orderIds.append(", ");
            }
        }

        orderIds.append("]");

        return orderIds.toString();
    }

    public String getSimplifiedOrders() {
        StringBuilder result = new StringBuilder();

        result.append("All orders of " + userName + " =[");

        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);

            result.append(order.orderId());
            result.append(" - ");

            for (int j = 0; j < order.items().size(); j++) {
                Product item = order.items().get(j);
                result.append("(").append(item.productId()).append(": ").append(item.name()).append(")");

                if (j < order.items().size() - 1) {
                    result.append(", ");
                }
            }

            if (i < orders.size() - 1) {
                result.append(", ");
            }
        }

        result.append("]");

        return result.toString();
    }
}

