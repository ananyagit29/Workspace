import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class TestScmCols {
    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@10.1.1.204:1527:dmsdevdb", "dmsinhouse", "dmsdev");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM scm_excise_invoice_header@IPCASCMDRDB WHERE ROWNUM = 1");
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            System.out.println("=== COLUMNS (" + colCount + ") ===");
            for (int i = 1; i <= colCount; i++) {
                System.out.println(i + ": " + meta.getColumnName(i) + " [" + meta.getColumnTypeName(i) + "]");
            }
            if (rs.next()) {
                System.out.println("\n=== SAMPLE ROW ===");
                for (int i = 1; i <= colCount; i++) {
                    System.out.println(meta.getColumnName(i) + " = " + rs.getString(i));
                }
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
