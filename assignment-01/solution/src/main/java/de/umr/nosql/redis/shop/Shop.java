package de.umr.nosql.redis.shop;

import de.umr.nosql.redis.model.Customer;
import de.umr.nosql.redis.model.Product;

public interface Shop extends AutoCloseable {

    /**
     * Adds a new product to the Shop inventory
     * @param productName The name of the product to add
     */
    Product addProductToShop(String productName);

    /**
     * Registers a new customer in the shop with the given username
     * @param userName The desired userName of the Customer
     * @return An Object containing information about the Customer
     */
    Customer registerCustomer(String userName);

    /**
     * Registers that a customer has bought a product
     * @param customer The customer who bought something
     * @param product The product he bought
     */
    void buyProduct(Customer customer, Product product);

    /**
     * Checks whether the customer has bought the given product
     * @param customer The customer who may have bought the product
     * @param product The product he may have bought
     * @return true if the customer has bought the product; false otherwise
     */
    boolean hasBought(Customer customer, Product product);

    /**
     * Returns the desired product
     * @param productId The id of the desired product
     * @return An object containing information about the product
     */
    Product getProduct(int productId);

}
