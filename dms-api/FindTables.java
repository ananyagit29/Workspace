import java.sql.*;

public class FindTables {
    public static void main(String[] args) throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        try(Connection c = DriverManager.getConnection("jdbc:oracle:thin:@10.1.1.204:1527:dmsdevdb","dmsinhouse","dmsdev")) {
            System.out.println("CAPEX TYPE TABLES:");
            try {
                ResultSet rs = c.createStatement().executeQuery("SELECT table_name FROM all_tables@IPCASCMDRDB WHERE table_name LIKE '%CAPEX%' OR table_name LIKE '%TYPE%'");
                while(rs.next()) {
                    String t = rs.getString(1);
                    if(t.contains("CAPEX") || t.contains("TYPE")) {
                        System.out.println(t);
                    }
                }
            } catch(Exception e) { e.printStackTrace(); }
        }
    }
}
