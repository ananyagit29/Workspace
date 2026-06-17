import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDBLink {
    public static void main(String[] args) throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@10.1.1.204:1527:dmsdevdb", "dmsinhouse", "dmsdev");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT doc_code AS invoice_number FROM scm_excise_invoice_header@IPCASCMDRDB WHERE division_code='B' AND entity_code='102' AND ROWNUM <= 5");
        while(rs.next()) {
            System.out.println("Invoice: " + rs.getString("invoice_number"));
        }
        rs.close();
        stmt.close();
        conn.close();
    }
}
