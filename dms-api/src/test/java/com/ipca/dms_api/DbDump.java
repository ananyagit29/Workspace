package com.ipca.dms_api;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DbDump {
    public static void main(String[] args) throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        try (Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@10.33.26.118:1521/dmsdevdb", "dmsinhouse", "dmsdev")) {
            PreparedStatement stmt = conn.prepareStatement("SELECT INVOICE_NUMBER, FILE_NAME, FILE_PATH, OTHER_FILE_NAME, OTHER_FILE_PATH FROM DMS_INVOICE_DOCUMENTS WHERE INVOICE_NUMBER = '102BT600086'");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("INVOICE_NUMBER: " + rs.getString("INVOICE_NUMBER"));
                System.out.println("FILE_NAME: " + rs.getString("FILE_NAME"));
                System.out.println("FILE_PATH: " + rs.getString("FILE_PATH"));
                System.out.println("OTHER_FILE_NAME: " + rs.getString("OTHER_FILE_NAME"));
                System.out.println("OTHER_FILE_PATH: " + rs.getString("OTHER_FILE_PATH"));
            }
        }
    }
}
