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
           System.out.println("[DEBUG]<Redis>(addProductToShop): Added " + newProduct + " to the cache");
       }

       return newProduct;
    }

    @Override
    public Customer registerCustomer(String userName) {
        return h2Shop.registerCustomer(userName);
    }

    @Override
    public void buyProduct(Customer customer, Product product) {
        System.out.println("~".repeat(35) + " TRANSACTION (RedisCache) " + "~".repeat(35));
        System.out.println("[DEBUG]<Redis>(Input): " + customer);
        System.out.println("[DEBUG]<Redis>(Input): " + product);

        if (getProduct(product.productId()) == null) {
            System.out.println("[INFO]<Redis>(buyProduct): Transaction was cancelled as " +
                            "the Product was not found in the H2 DB");

            System.out.println("[DEBUG]<Redis>(buyProduct): The product does not exist in the H2 DB. " +
                    "Skip caching this product");
            return;
        }

        h2Shop.buyProduct(customer, product);


        String key = getCustomerPurchasesKey(customer.customerId());
        String productId = String.valueOf(product.productId());

        jedis.lpush(key, productId);
        jedis.ltrim(key, 0, 4);
        System.out.println("[DEBUG]<Redis>(buyProduct): {CACHE} Purchases of Customer with ID = " +
                customer.customerId()  + " - " + jedis.lrange(key, 0, -1));
    }

    @Override
    public boolean hasBought(Customer customer, Product product) {
        String key = getCustomerPurchasesKey(customer.customerId());
        String productId = String.valueOf(product.productId());

        if (jedis.lpos(key, productId) == null) {
            System.out.print("[DEBUG]<Redis>(hasBought): {CACHE MISS} Did not find productId = " +
                    product.productId() + " for Customer with ID = " + customer.customerId() + " ");

            boolean hasBought = h2Shop.hasBought(customer, product);

            if (hasBought) {
                System.out.println("(Found the Order in the H2 DB)");
            } else {
                System.out.println("(Did not find the Order in the H2 DB)");
            }

            return hasBought;
        }

        System.out.println("[DEBUG]<Redis>(hasBought): {CACHE HIT} Found productId = " +
        product.productId() + " for Customer with ID = " + customer.customerId() + " ");
        return true;
    }

    @Override
    public Product getProduct(int productId) {
        String key = getProductKey(productId);
        String productJson = jedis.get(key);

        if (productJson != null) {
            System.out.println("[DEBUG]<Redis>(getProduct): {CACHE HIT} Found productId = " + productId);
            return gson.fromJson(productJson, Product.class);
        }

        System.out.print("[DEBUG]<Redis>(getProduct): {CACHE MISS} Did not find productId = " + productId + " ");
        Product foundProduct = h2Shop.getProduct(productId);
        if (foundProduct != null) {
            System.out.println("(Found in the H2 DB - Added to the cache!)");
            jedis.set(key, gson.toJson(foundProduct));
        } else {
            System.out.println("(Did not find in the H2 DB)");
        }

        return foundProduct;
    }

    @Override
    public void close() throws Exception {
        h2Shop.close();
        jedis.close();
    }
}
