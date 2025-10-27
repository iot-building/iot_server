package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DBUtil {
    private static final String PROPERTIES_FILE = "src/main/java/config/db.properties";
    private static Properties props;

    static {
        try {
            FileInputStream fis = new FileInputStream(PROPERTIES_FILE);

            props = new Properties();
            props.load(fis);

            fis.close();

        } catch (IOException e) {
            System.err.println("db.properties 파일을 찾거나 읽을 수 없습니다.");
            e.printStackTrace();
        }
    }
    // DBMS 초기 연결
    public static Connection getConnect() {
        Connection con = null;
        String DB_ip = props.getProperty("db.ip");
        String DB_port = props.getProperty("db.port");
        String DB_name = props.getProperty("db.database");

        String url = "jdbc:mysql://"+DB_ip+":"+DB_port+"/"+DB_name+"?serverTimezone=UTC";
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");
        try {
            con = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return con;
    }

    // Connection 자원 반납
    public static void close(ResultSet rs, Statement stmt, Connection con) {
        try {
            if(rs!=null)rs.close();
            if(stmt!=null)stmt.close();
            if(con!=null)con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
