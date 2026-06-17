import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestScmDB {
    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@10.1.1.204:1527:dmsdevdb", "dmsinhouse", "dmsdev");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT doc_code, doc_date FROM scm_excise_invoice_header@IPCASCMDRDB WHERE division_code='B' AND entity_code='102' AND doc_date BETWEEN '01-APR-26' AND '31-MAR-2027' ORDER BY doc_date DESC");
            while(rs.next()) {
                System.out.println("Invoice: " + rs.getString("doc_code") + " Date: " + rs.getString("doc_date"));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
