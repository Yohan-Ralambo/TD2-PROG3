import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    public static Connection getDBConnection() {
        String url = System.getenv("JDBC_URL");
        String user = System.getenv("USERNAME");
        String password = System.getenv("PASSWORD");

        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}