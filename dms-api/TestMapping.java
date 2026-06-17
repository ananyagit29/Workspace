import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestMapping {
    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@10.1.1.204:1527:dmsdevdb", "dmsinhouse", "dmsdev");
            Statement stmt = conn.createStatement();

            // Check DMS_USER_RIGHTS for the user 
            System.out.println("=== DMS_USER_RIGHTS for ananya.parbat ===");
            ResultSet rs = stmt.executeQuery("SELECT COMPANY_ID, DIVISION_NAME, LOCATION_ID, LOCATION_NAME, APPLICATION_NAME FROM DMS_USER_RIGHTS WHERE UPPER(USER_ID) LIKE '%ANANYA%'");
            while(rs.next()) {
                System.out.println("COM=" + rs.getString("COMPANY_ID") + " DIV=" + rs.getString("DIVISION_NAME") 
                    + " LOC_ID=" + rs.getString("LOCATION_ID") + " LOC_NAME=" + rs.getString("LOCATION_NAME")
                    + " APP=" + rs.getString("APPLICATION_NAME"));
            }
            rs.close();

            // Check scm_division_master or similar in linked DB
            System.out.println("\n=== SCM division_master@IPCASCMDRDB ===");
            try {
                ResultSet rs2 = stmt.executeQuery("SELECT * FROM scm_division_master@IPCASCMDRDB WHERE entity_code='102'");
                var meta = rs2.getMetaData();
                int colCount = meta.getColumnCount();
                for (int i = 1; i <= colCount; i++) System.out.print(meta.getColumnName(i) + "\t");
                System.out.println();
                while(rs2.next()) {
                    for (int i = 1; i <= colCount; i++) System.out.print(rs2.getString(i) + "\t");
                    System.out.println();
                }
                rs2.close();
            } catch (Exception e) {
                System.out.println("No scm_division_master: " + e.getMessage());
            }

            // Try checking what "PLANT" maps to
            System.out.println("\n=== SCM store_code and division combos for entity 102 (sample) ===");
            ResultSet rs3 = stmt.executeQuery("SELECT DISTINCT division_code, store_code FROM scm_excise_invoice_header@IPCASCMDRDB WHERE entity_code='102' AND ROWNUM <= 30 ORDER BY division_code, store_code");
            while(rs3.next()) {
                System.out.println("div=" + rs3.getString("division_code") + " store=" + rs3.getString("store_code"));
            }
            rs3.close();

            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
