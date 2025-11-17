package de.umr.nosql.redis.shop;

import de.umr.nosql.redis.model.Customer;
import de.umr.nosql.redis.model.Order;
import de.umr.nosql.redis.model.Product;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.DeleteDbFiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class H2Shop implements Shop, AutoCloseable {

    private static final String DB_CONNECTION = "jdbc:h2:./h2Example";
    private static final String DB_USER = "";
    private static final String DB_PASSWORD= "";


    private DataSource dataSource;

    public H2Shop() {
        try {
            cleanUp();

            this.dataSource = JdbcConnectionPool.create(DB_CONNECTION, DB_USER, DB_PASSWORD);

            setupDatabase();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

    }

    private static void cleanUp() {
        DeleteDbFiles.execute(".", "h2Example", true);
    }

    private void setupDatabase() throws SQLException {

        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()
        ) {
            String createProductsTable = """
                    CREATE TABLE IF NOT EXISTS products(
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255)
                    );
                    """;

            String createCustomersTable = """
                    CREATE TABLE IF NOT EXISTS customers(
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_name VARCHAR(255),
                        address VARCHAR(255)
                    );
                    """;

            String createOrdersTable = """
                    CREATE TABLE IF NOT EXISTS orders(
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        shipping_address VARCHAR(255)
                    );
                    """;

            String createCustomerOrderTable = """
                    CREATE TABLE IF NOT EXISTS customers_orders(
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        customer_id INT,
                        order_id INT,
                        FOREIGN KEY (customer_id) REFERENCES customers(id),
                        FOREIGN KEY (order_id) REFERENCES orders(id)
                    );
                    """;

            String createOrderProductSql = """
                    CREATE TABLE IF NOT EXISTS orders_products(
                        id  INT AUTO_INCREMENT PRIMARY KEY,
                        order_id INT,
                        product_id INT,
                        FOREIGN KEY (order_id) REFERENCES orders(id),
                        FOREIGN KEY (product_id) REFERENCES products(id)
                    );
                    """;


            stmt.executeUpdate(createProductsTable);
            stmt.executeUpdate(createCustomersTable);
            stmt.executeUpdate(createOrdersTable);
            stmt.executeUpdate(createCustomerOrderTable);
            stmt.executeUpdate(createOrderProductSql);
        }
    }

    @Override
    public Product addProductToShop(String productName) {
        String insertProductSql = "INSERT INTO products (name) VALUES (?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement insertStmt =  connection.prepareStatement(
                     insertProductSql,
                     Statement.RETURN_GENERATED_KEYS
             )
        ) {

            insertStmt.setString(1, productName);

            int affectedRows = insertStmt.executeUpdate();

            if  (affectedRows == 0) {
                throw new SQLException("Failed to insert product " + productName);
            }

            try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int newId = generatedKeys.getInt(1);

                    Product newProduct = new Product(newId, productName);

                    System.out.println("[DEBUG]<H2>(addProductToShop): Added " + newProduct +
                            " to the H2 DB");

                    return newProduct;
                } else {
                    throw new SQLException("Failed to insert Product " + productName);
                }
            }

        } catch(SQLException e) {
            System.err.println("[ERROR]<H2>(addProductToShop): " + e.getMessage());
            return null;
        }
    }

    @Override
    public Customer registerCustomer(String userName) {
        String insertCustomerSql = "INSERT INTO customers (user_name) VALUES (?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement insertStmt = connection.prepareStatement(
                     insertCustomerSql,
                     Statement.RETURN_GENERATED_KEYS
             )
        ) {
            insertStmt.setString(1, userName);

            int affectedRows = insertStmt.executeUpdate();

            if  (affectedRows == 0) {
                throw new SQLException("Failed to insert customer " + userName);
            }

            try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int newId = generatedKeys.getInt(1);

                    Customer newCustomer = new  Customer(newId, userName);

                    System.out.println("[DEBUG]<H2>(registerCustomer): Added " + newCustomer +
                            " to the H2 DB");

                    return newCustomer;
                } else {
                    throw new SQLException("Failed to insert customer " + userName);
                }
            }

        } catch(SQLException e) {
            System.err.println("[ERROR]<H2>(registerCustomer): " + e.getMessage());
            return null;
        }
    }

    @Override
    public Order buyProduct(Customer customer, Product product) {
        String insertOrderSql = "INSERT INTO orders (shipping_address) VALUES (?)";

        String insertCustomerOrderSql = """
                INSERT INTO customers_orders (customer_id, order_id) VALUES (?, ?)
                """;

        String insertOrderProductSql = """
                INSERT INTO orders_products (order_id, product_id) VALUES (?, ?)
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement orderStmt = connection.prepareStatement(
                     insertOrderSql,
                     Statement.RETURN_GENERATED_KEYS
             );
             PreparedStatement customerOrderStmt = connection.prepareStatement(
                     insertCustomerOrderSql,
                     Statement.RETURN_GENERATED_KEYS
             );
             PreparedStatement orderProductStmt = connection.prepareStatement(
                     insertOrderProductSql,
                     Statement.RETURN_GENERATED_KEYS
             )
        ) {
            try {
                connection.setAutoCommit(false);

                int newOrderId = -1;

                orderStmt.setString(1, customer.address());
                orderStmt.executeUpdate();

                try (ResultSet generatedKeys = orderStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newOrderId = generatedKeys.getInt(1);
                    }
                }

                if (newOrderId == -1) {
                    throw new SQLException("Failed to add order");
                }

                customerOrderStmt.setInt(1, customer.customerId());
                customerOrderStmt.setInt(2, newOrderId);
                customerOrderStmt.executeUpdate();

                orderProductStmt.setInt(1, newOrderId);
                orderProductStmt.setInt(2, product.productId());
                orderProductStmt.executeUpdate();

                connection.commit();

                System.out.println("[DEBUG]<H2>(buyProduct): Transaction was successful! " +
                        "The Order ID is: " + newOrderId);

                return new Order(
                    newOrderId,
                    customer.address(),
                    List.of(product)
                );

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch(SQLException e) {
            System.out.println("[ERROR]<H2>(buyProduct): " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean hasBought(Customer customer, Product product) {
        String selectSql = """
                SELECT 1
                FROM customers_orders co
                JOIN orders_products op ON co.order_id = op.order_id
                WHERE co.customer_id = ? AND op.product_id = ?
                LIMIT 1
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement selectStmt = connection.prepareStatement(selectSql)
        ) {
            selectStmt.setInt(1, customer.customerId());
            selectStmt.setInt(2, product.productId());

            try (ResultSet resultSet = selectStmt.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            System.err.println("[ERROR]<H2>(hasBought): " + e.getMessage());
            return false;
        }
    }

    @Override
    public Product getProduct(int productId) {
        String selectSql = "SELECT * FROM  products WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(selectSql)
        ) {
            stmt.setInt(1, productId);

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    int id =  resultSet.getInt("id");
                    String name = resultSet.getString("name");
                    return new Product(id, name);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR]<H2>(getProduct): " + e.getMessage());
            return null;
        }
    }

    @Override
    public void close() {
        ((JdbcConnectionPool) dataSource).dispose();
    }
}
