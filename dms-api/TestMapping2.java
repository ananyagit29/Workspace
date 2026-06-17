import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestMapping2 {
    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@10.1.1.204:1527:dmsdevdb", "dmsinhouse", "dmsdev");
            Statement stmt = conn.createStatement();

            // Check DMS companies table structure
            System.out.println("=== DMS_USERS for ananya ===");
            try {
                ResultSet rs = stmt.executeQuery("SELECT * FROM DMS_USERS WHERE UPPER(USER_ID) LIKE '%ANANYA%' AND ROWNUM = 1");
                var meta = rs.getMetaData();
                int c = meta.getColumnCount();
                for (int i = 1; i <= c; i++) System.out.print(meta.getColumnName(i) + "\t");
                System.out.println();
                while(rs.next()) {
                    for (int i = 1; i <= c; i++) System.out.print(rs.getString(i) + "\t");
                    System.out.println();
                }
                rs.close();
            } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }

            // Find what SCM entity has invoices at loc 102 (Ratlam)
            System.out.println("\n=== SCM invoices where store_code involves Ratlam/102 ===");
            try {
                ResultSet rs = stmt.executeQuery("SELECT DISTINCT entity_code, division_code, store_code FROM scm_excise_invoice_header@IPCASCMDRDB WHERE entity_code='102' AND ROWNUM <= 20");
                while(rs.next()) {
                    System.out.println("entity=" + rs.getString("entity_code") + " div=" + rs.getString("division_code") + " store=" + rs.getString("store_code"));
                }
                rs.close();
            } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }

            // Try to find entity master in linked DB
            System.out.println("\n=== SCM entity_master@IPCASCMDRDB ===");
            try {
                ResultSet rs = stmt.executeQuery("SELECT * FROM scm_entity_master@IPCASCMDRDB WHERE ROWNUM <= 10");
                var meta = rs.getMetaData();
                int c = meta.getColumnCount();
                for (int i = 1; i <= c; i++) System.out.print(meta.getColumnName(i) + "\t");
                System.out.println();
                while(rs.next()) {
                    for (int i = 1; i <= c; i++) System.out.print(rs.getString(i) + "\t");
                    System.out.println();
                }
                rs.close();
            } catch (Exception e) { System.out.println("No entity_master: " + e.getMessage()); }

            // Check getCompanies API source
            System.out.println("\n=== DMS_USER_RIGHTS full columns for PLANT ===");
            try {
                ResultSet rs = stmt.executeQuery("SELECT * FROM DMS_USER_RIGHTS WHERE DIVISION_NAME='PLANT' AND ROWNUM = 1");
                var meta = rs.getMetaData();
                int c = meta.getColumnCount();
                for (int i = 1; i <= c; i++) System.out.print(meta.getColumnName(i) + "[" + meta.getColumnTypeName(i) + "]\t");
                System.out.println();
                while(rs.next()) {
                    for (int i = 1; i <= c; i++) System.out.print(rs.getString(i) + "\t");
                    System.out.println();
                }
                rs.close();
            } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }

            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
