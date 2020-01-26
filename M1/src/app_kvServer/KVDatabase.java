package app_kvServer;

import java.sql.*;

public class KVDatabase {
    private Connection c;

    public KVDatabase() {
        connect();
    }

    public void connect() {
        Statement stmt = null;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:test.db");

            stmt = c.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS KVPAIRS " +
                    "(KEY TEXT PRIMARY KEY     NOT NULL," +
                    " VALUE           TEXT    NOT NULL)";
            stmt.executeUpdate(sql);
            stmt.close();
            clear();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        // logger
        // System.out.println("Opened database successfully");
    }

    public String get(String key) {
        String sql = "SELECT VALUE " + "FROM KVPAIRS WHERE key == ?";
        try (PreparedStatement pstmt = c.prepareStatement(sql)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()){
                return rs.getString("VALUE");
            } else {
                return null;
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    public void put(String key, String value){
        if (value.equals("null")){
            String sql = "DELETE FROM KVPAIRS WHERE KEY = ?";
            try (PreparedStatement pstmt = c.prepareStatement(sql)) {
                pstmt.setString(1, key);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
            return;
        }

        String sql = "INSERT INTO KVPAIRS(KEY, VALUE) VALUES(?,?)";

        try (PreparedStatement pstmt = c.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void clear() {
        String sql = "DELETE FROM KVPAIRS";

        try (PreparedStatement pstmt = c.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

}
