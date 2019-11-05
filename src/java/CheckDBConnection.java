import java.sql.Connection;
import java.sql.DriverManager;

public class CheckDBConnection {
    //DB Ports
    private static final String MYSQL_PORT = "3306";
    private static final String MSSQL_PORT = "1433";
    private static final String POSTGRES_PORT = "5432";
    private static final String ORACLE_PORT = "1521";

    // DB Urls
    private static final String MYSQL_URL = "jdbc:mysql://localhost:" + MYSQL_PORT + "/";
    private static final String MSSQL_URL = "jdbc:microsoft:sqlserver://localhost:" + MSSQL_PORT + "/";
    private static final String POSTGRES_URL = "jdbc:postgresql://localhost:" + POSTGRES_PORT + "/";
    private static final String ORACLE_URL = "jdbc:oracle://localhost:" + ORACLE_PORT + "/";

    //DB Driver Names
    private static final String MYSQL_DRIVER_NAME = "com.mysql.jdbc.Driver";
    private static final String MSSQL_DRIVER_NAME = "com.microsoft.jdbc.sqlserver.SQLServerDriver";
    private static final String POSTGRES_DRIVER_NAME = "org.postgresql.Driver";
    private static final String ORACLE_DRIVER_NAME = "oracle.jdbc.driver.OracleDriver";

    private static final String DB_NAME = "jbilling_test";
    private static final String USERNAME = "jbilling";
    private static final String PASSWORD = "";

    public static void main(String[] args) {
        System.out.println("Checking the connection to the DB...");
        Connection conn = null;
        String url = POSTGRES_URL;
        String dbName = DB_NAME;
        String driver = POSTGRES_DRIVER_NAME;
        String userName = USERNAME;
        String password = PASSWORD;

        try {
            Class.forName(driver).newInstance();
            conn = DriverManager.getConnection(url + dbName, userName, password);
            System.out.println("Connected to the database...");
            conn.close();
            System.out.println("Disconnected from database...");
        } catch (Exception e) {
            System.out.println("An error ocurred while trying to connect to the DB...");
            e.printStackTrace();
        }
    }
}
