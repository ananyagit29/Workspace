import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestScmDiv {
    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@10.1.1.204:1527:dmsdevdb", "dmsinhouse", "dmsdev");
            Statement stmt = conn.createStatement();
            
            // Find distinct division_codes in SCM for entity 102
            System.out.println("=== SCM division_codes for entity 102 ===");
            ResultSet rs = stmt.executeQuery("SELECT DISTINCT division_code FROM scm_excise_invoice_header@IPCASCMDRDB WHERE entity_code='102' ORDER BY division_code");
            while(rs.next()) {
                System.out.println("division_code: [" + rs.getString("division_code") + "]");
            }
            rs.close();

            // Check DMS_DIVISION_MASTER or similar table for mapping
            System.out.println("\n=== Checking DMS tables for division mapping ===");
            try {
                ResultSet rs2 = stmt.executeQuery("SELECT table_name FROM user_tables WHERE UPPER(table_name) LIKE '%DIVIS%' OR UPPER(table_name) LIKE '%MASTER%'");
                while(rs2.next()) {
                    System.out.println("Table: " + rs2.getString("table_name"));
                }
                rs2.close();
            } catch (Exception e) {
                System.out.println("No division master tables found: " + e.getMessage());
            }

            // Check what the auth API returns for divisions 
            System.out.println("\n=== DMS_USER_RIGHTS division values ===");
            try {
                ResultSet rs3 = stmt.executeQuery("SELECT DISTINCT DIVISION_NAME FROM DMS_USER_RIGHTS WHERE COMPANY_ID='102' ORDER BY DIVISION_NAME");
                while(rs3.next()) {
                    System.out.println("DIVISION_NAME: [" + rs3.getString("DIVISION_NAME") + "]");
                }
                rs3.close();
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

            // Try to find a mapping table
            System.out.println("\n=== All user tables ===");
            try {
                ResultSet rs4 = stmt.executeQuery("SELECT table_name FROM user_tables ORDER BY table_name");
                while(rs4.next()) {
                    System.out.println("Table: " + rs4.getString("table_name"));
                }
                rs4.close();
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
