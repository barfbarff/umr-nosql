package de.umr.nosql.redis;

import de.umr.nosql.redis.model.Customer;
import de.umr.nosql.redis.model.Product;
import de.umr.nosql.redis.shop.H2Shop;
import de.umr.nosql.redis.shop.RedisCacheShop;
import de.umr.nosql.redis.shop.Shop;

import java.util.Arrays;
import java.util.List;

public class ShopSimulation {
    public static void main(String[] args) {
        Shop h2Shop = new H2Shop();

        try (RedisCacheShop redisCacheShop = new RedisCacheShop(h2Shop)) {
            System.out.println("\n" + "+".repeat(40) +
                    " EXPERIMENT 01: getProduct " + "+".repeat(40));

            System.out.println("\n[SETUP]<Sim>: Add Product Poutine through H2Shop");
            Product poutine = h2Shop.addProductToShop("Poutine");

            System.out.println("\n[SETUP]<Sim>: Add Product Ramen through RedisCacheShop");
            Product ramen = redisCacheShop.addProductToShop("Ramen");

            System.out.println("\n[SETUP]<Sim>: Create non-existent Product Taco");
            Product taco = new Product(998, "taco");
            System.out.println("[DEBUG]<Sim>: Created " + taco);

            System.out.println("\n[RUN]<Sim>: Get Info on Product Poutine");
            Product getPoutine1 = redisCacheShop.getProduct(poutine.productId());
            System.out.println("[INFO]<Sim>: Info on Poutine: " + getPoutine1);

            System.out.println("\n[RUN]<Sim>: Get Info on Product Poutine (Again)");
            Product getPoutine2 = redisCacheShop.getProduct(poutine.productId());
            System.out.println("[INFO]<Sim>: Info on Poutine: " + getPoutine2);

            System.out.println("\n[RUN]<Sim>: Get Info on Product Ramen");
            Product getRamen = redisCacheShop.getProduct(ramen.productId());
            System.out.println("[INFO]<Sim>: Info on Ramen: " + getRamen);

            System.out.println("\n[RUN]<Sim>: Get Info on Product Taco");
            Product getTaco = redisCacheShop.getProduct(taco.productId());
            System.out.println("[INFO]<Sim>: Info on Taco: " + getTaco);

            System.out.println("\n" + "+".repeat(40) +
                    " EXPERIMENT 02: hasBought " + "+".repeat(40));

            System.out.println("\n[SETUP]<Sim>: Add Customer Clark through H2Shop");
            Customer clark = h2Shop.registerCustomer("Clark");

            System.out.println("\n[SETUP]<Sim>: Add Customer Lily through H2Shop");
            Customer lily = h2Shop.registerCustomer("Lily");

            System.out.println("\n[SETUP]<Sim>: Add Product Apple through H2Shop");
            Product apple = h2Shop.addProductToShop("Apple");

            System.out.println("\n[SETUP]<Sim>: Add Product Banana through H2Shop");
            Product banana = h2Shop.addProductToShop("Banana");

            System.out.println("\n[SETUP]<Sim>: Add Product Orange through H2Shop");
            Product orange = h2Shop.addProductToShop("Orange");

            System.out.println("\n[SETUP]<Sim>: Add Product Espresso through RedisCacheShop");
            Product espresso = redisCacheShop.addProductToShop("Espresso");

            System.out.println("\n[SETUP]<Sim>: Add Product Latte through RedisCacheShop");
            Product latte = redisCacheShop.addProductToShop("Latte");

            System.out.println("\n[SETUP]<Sim>: Add Product Cappuccino through RedisCacheShop");
            Product cappuccino = redisCacheShop.addProductToShop("Cappuccino");

            System.out.println("\n[SETUP]<Sim>: Create non-existent Product Bread");
            Product bread = new Product(999, "Bread");
            System.out.println("[DEBUG]<Sim>: Created " + bread);

            System.out.println("\n[SETUP]<Sim>: Customer Clark buys " +
                    "[apple, orange, apple, espresso, latte, cappuccino, bread, latte]");
            List<Product> clarkShoppingList = Arrays.asList(
                    apple, orange, apple, espresso, latte, cappuccino, bread, latte
            );
            clarkShoppingList.forEach(product -> redisCacheShop.buyProduct(clark, product));

            System.out.println("\n[RUN]<Sim>: Check if Clark hasBought Orange");
            System.out.println("[INFO]<Sim>: Answer - " + redisCacheShop.hasBought(clark, orange));

            System.out.println("\n[RUN]<Sim>: Check if Clark hasBought Espresso");
            System.out.println("[INFO]<Sim>: Answer - " + redisCacheShop.hasBought(clark, espresso));

            System.out.println("\n[RUN]<Sim>: Check if Clark hasBought Banana");
            System.out.println("[INFO]<Sim>: Answer - " + redisCacheShop.hasBought(clark, banana));

            System.out.println("\n[RUN]<Sim>: Check if Clark hasBought Bread");
            System.out.println("[INFO]<Sim>: Answer - " + redisCacheShop.hasBought(clark, bread));

            System.out.println("\n[SETUP]<Sim>: Customer Lily buys " +
                    "[apple, orange, apple, espresso, latte, cappuccino]");
            List<Product> lilyShoppingList = Arrays.asList(
                    orange, bread, cappuccino, latte, apple, banana, banana
            );
            lilyShoppingList.forEach(product -> redisCacheShop.buyProduct(lily, product));

            System.out.println("\n[RUN]<Sim>: Check if Lily hasBought Orange");
            System.out.println("[INFO]<Sim>: Answer - " + redisCacheShop.hasBought(lily, orange));

            System.out.println("\n[RUN]<Sim>: Check if Lily hasBought Espresso");
            System.out.println("[INFO]<Sim>: Answer - " + redisCacheShop.hasBought(lily, espresso));

            System.out.println("\n[RUN]<Sim>: Check if Lily hasBought Banana");
            System.out.println("[INFO]<Sim>: Answer - " + redisCacheShop.hasBought(lily, banana));

            System.out.println("\n[RUN]<Sim>: Check if Lily hasBought Bread");
            System.out.println("[INFO]<Sim>: Answer - " + redisCacheShop.hasBought(lily, bread));

        } catch (Exception e) {
            System.err.println("[ERROR]<Sim>: " + "An error occurred " + e.getMessage());
        }
    }
}
