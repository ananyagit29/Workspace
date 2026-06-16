import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestPathDB {
    public static void main(String[] args) throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@10.1.1.204:1527:dmsdevdb", "dmsinhouse", "dmsdev");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT INVOICE_NUMBER FROM DMS_INVOICE_DOCUMENTS WHERE UPPER(INVOICE_NUMBER)='102BT600086'");
        if(rs.next()) {
            System.out.println("INVOICE_NUMBER=[" + rs.getString("INVOICE_NUMBER") + "]");
        } else {
            System.out.println("No row found.");
        }
        rs.close();
        stmt.close();
        conn.close();
    }
}
