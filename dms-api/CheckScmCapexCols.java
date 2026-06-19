import java.sql.*;
public class CheckScmCapexCols {
    public static void main(String[] args) throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@10.1.1.204:1527:dmsdevdb", "dmsinhouse", "dmsdev");
        Statement stmt = conn.createStatement();
        try {
            ResultSet rs = stmt.executeQuery("SELECT column_name, data_type FROM all_tab_columns@IPCASCMDRDB WHERE table_name = 'FA_CAPEX_BUDGET'");
            while (rs.next()) {
                System.out.println(rs.getString(1) + " - " + rs.getString(2));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        conn.close();
    }
}
