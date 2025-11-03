package de.umr.nosql.redis.shop;

import com.google.gson.Gson;
import de.umr.nosql.redis.model.Customer;
import de.umr.nosql.redis.model.Product;
import redis.clients.jedis.Jedis;

public class RedisCacheShop implements Shop {

    // Get a product key pointing to a JSON string represents a Product
    // object corresponding to given productId
    // E.g., product:1 -> {"productId": 1, "name": "Washing Machine"}
    private String getProductKey(int productId) {
        return "product:" + productId;
    }

    // Get a key pointing to a customer's purchase history.
    // The key points to a Redis LIST containing the string IDs of the
    // last 5 products purchased by the customer with given customerId
    // E.g., customer:1:purchases -> ["5", "3", "1", "10", "2"]
    // (List of product IDs, most recent is first)
    private String getCustomerPurchasesKey(int customerId) {
        return "customer:" + customerId + ":purchases";
    }

    // The JSON converter
    private static final Gson gson = new  Gson();

    // Store data to the H2 database when a cache is missed
    private final Shop h2Shop;

    // Redis client for persisting and retrieving data to and from Redis
    private final Jedis jedis;

    public  RedisCacheShop(Shop h2Shop) {
        this.h2Shop = h2Shop;
        this.jedis = new Jedis("redis://localhost:6379");
        this.jedis.flushAll();
    }

    @Override
    public Product addProductToShop(String productName) {
       Product newProduct = h2Shop.addProductToShop(productName);

       if (newProduct != null) {
           String key = getProductKey(newProduct.productId());
           String newProductJson = gson.toJson(newProduct);
           jedis.set(key, newProductJson);
           System.out.println("CACHE(addProductToShop): " + newProduct);
       }

       return newProduct;
    }

    @Override
    public Customer registerCustomer(String userName) {
        return h2Shop.registerCustomer(userName);
    }

    @Override
    public void buyProduct(Customer customer, Product product) {
        h2Shop.buyProduct(customer, product);

        String key = getCustomerPurchasesKey(customer.customerId());
        String productId = String.valueOf(product.productId());

        jedis.lpush(key, productId);
        jedis.ltrim(key, 0, 4);
        System.out.println("CACHE(buyProduct): Purchases of " +
                customer  + " - " + jedis.lrange(key, 0, -1));
    }

    @Override
    public boolean hasBought(Customer customer, Product product) {
        String key = getCustomerPurchasesKey(customer.customerId());
        String productId = String.valueOf(product.productId());

        if (jedis.lpos(key, productId) == null) {
            System.out.println("CACHE MISS(hasBought): Did not find " + product +
                    " for " + customer);
            return h2Shop.hasBought(customer, product);
        }

        System.out.println("CACHE HIT(hasBought): Found " + product +
                " for " + customer);
        return true;
    }

    @Override
    public Product getProduct(int productId) {
        String key = getProductKey(productId);

        String productJson = jedis.get(key);

        if (productJson != null) {
            System.out.println("CACHE HIT(getProduct): Found " + productId);
            return gson.fromJson(productJson, Product.class);
        }

        System.out.println("CACHE MISS(getProduct): Did not find " + productId);
        Product foundProduct = h2Shop.getProduct(productId);
        if (foundProduct != null) {
            jedis.set(key, gson.toJson(foundProduct));
        }

        return foundProduct;
    }

    @Override
    public void close() throws Exception {
        h2Shop.close();
        jedis.close();
    }
}
