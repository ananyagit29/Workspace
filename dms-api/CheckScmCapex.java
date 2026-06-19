import java.sql.*;
public class CheckScmCapex {
    public static void main(String[] args) throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@10.1.1.204:1527:dmsdevdb", "dmsinhouse", "dmsdev");
        DatabaseMetaData md = conn.getMetaData();
        System.out.println("--- local tables ---");
        ResultSet rs1 = md.getTables(null, null, "%BUDGET%", new String[]{"TABLE"});
        while (rs1.next()) {
            System.out.println(rs1.getString(3));
        }
        
        System.out.println("--- trying manual query on SCM tables ---");
        Statement stmt = conn.createStatement();
        try {
            ResultSet rs2 = stmt.executeQuery("SELECT table_name FROM all_tables@IPCASCMDRDB WHERE table_name LIKE '%BUDGET%' OR table_name LIKE '%CAPEX%'");
            while (rs2.next()) {
                System.out.println(rs2.getString(1));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        conn.close();
    }
}
