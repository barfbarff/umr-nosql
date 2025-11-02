package de.umr.nosql.redis.jedis;

import de.umr.nosql.redis.jedis.simpleshop.RedisCacheShop;
import redis.clients.jedis.Jedis;

public class JedisBasics {

    private static void exerciseTwoB(Jedis jedis) {
        //Connection to Redis server on localhost
        //Put data in Redis
        jedis.set("6", "six");
        jedis.set("7", "seven");
        jedis.set("8", "eight");


        //Get Stored Values
        System.out.println(jedis.get("6"));
        System.out.println(jedis.get("7"));
        System.out.println(jedis.get("8"));

        jedis.set("9", "nine");
        jedis.expire("9", 10);
        for (int i = 1; i <= 9; i++) {
            System.out.println(jedis.get("" + i));
        }
    }

    private static void exerciseTwoC(Jedis jedis) {
        for (int i = 0; i < 10; i++) {
            jedis.lpush("list", "list"+i);
            jedis.sadd("set", "set"+i);
            jedis.zadd("sortedset", i, "sortedset"+i);
        }

        System.out.println(jedis.lrange("list", 0, 10));
        System.out.println(jedis.smembers("set"));
        System.out.println(jedis.zrange("sortedset", 0, 10));
    }

    public static void main(String[] args) {
        // For this, of course you have to install redis and run a local instance
        Jedis jedis = new Jedis("redis://localhost:6379");
        jedis.flushDB(); // delete everything
        exerciseTwoB(jedis);
        exerciseTwoC(jedis);


//        RedisCacheShop cshop = new RedisCacheShop(jedis);
//        for (int i=0; i < 10; i++)
//            cshop.addProductToShop("product_" + i);
//
//        for (int i=0; i < 12; i++)
//            System.out.println(cshop.getProduct(i));

        jedis.close();
    }
}