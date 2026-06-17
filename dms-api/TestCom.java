import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestCom {
    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@10.1.1.204:1527:dmsdevdb", "dmsinhouse", "dmsdev");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT ID, COMPANY_CODE, COMPANY_NAME FROM DMSINHOUSE.DMS_COMPANY");
            while(rs.next()) {
                System.out.println("ID: " + rs.getString("ID") + " CODE: " + rs.getString("COMPANY_CODE") + " Name: " + rs.getString("COMPANY_NAME"));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
