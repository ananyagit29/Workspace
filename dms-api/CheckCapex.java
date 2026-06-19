import java.sql.*;
public class CheckCapex {
    public static void main(String[] args) throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@10.1.1.204:1527:dmsdevdb", "dmsinhouse", "dmsdev");
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getTables(null, null, "%CAPEX%", new String[]{"TABLE"});
        while (rs.next()) {
            System.out.println(rs.getString(3));
        }
        conn.close();
    }
}
