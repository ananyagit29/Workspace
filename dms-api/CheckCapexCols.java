import java.sql.*;
public class CheckCapexCols {
    public static void main(String[] args) throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@10.1.1.204:1527:dmsdevdb", "dmsinhouse", "dmsdev");
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getColumns(null, null, "DMS_CAPEX_BUDGET", null);
        while (rs.next()) {
            System.out.println(rs.getString("COLUMN_NAME") + " - " + rs.getString("TYPE_NAME") + " (" + rs.getInt("COLUMN_SIZE") + ")");
        }
        conn.close();
    }
}
