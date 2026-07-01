import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbDump {
    public static void main(String[] args) throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@10.1.1.204:1527:dmsdevdb", "dmsinhouse", "dmsdev");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT DISTINCT APPLICATION_NAME FROM DMS_USER_RIGHTS");
        System.out.println("--- APPLICATION NAMES IN DB ---");
        while (rs.next()) {
            System.out.println(rs.getString("APPLICATION_NAME"));
        }
        System.out.println("--- RIGHTS FOR ANANYA PARBAT ---");
        ResultSet rs2 = stmt.executeQuery("SELECT APPLICATION_NAME, SUB_APPLICATION_NAME, ACCESS_TYPE FROM DMS_USER_RIGHTS");
        int count = 0;
        while (rs2.next() && count < 50) {
            System.out.println("App: " + rs2.getString("APPLICATION_NAME") + " SubApp: " + rs2.getString("SUB_APPLICATION_NAME") + " Type: " + rs2.getString("ACCESS_TYPE"));
            count++;
        }
        conn.close();
    }
}
