package com.ipca.dms_api;

import java.sql.*;

public class DbScanner {
    public static void main(String[] args) {
        String host = "10.1.1.204";
        int port = 1527;
        String sid = "dmsdevdb";
        
        System.out.println("Trying host: " + host + ", port: " + port + ", SID: " + sid);
        String urlSid = "jdbc:oracle:thin:@" + host + ":" + port + ":" + sid;
        
        try (Connection conn = DriverManager.getConnection(urlSid, "dmsinhouse", "dmsdev")) {
            System.out.println("SUCCESS (SID syntax) -> " + urlSid);
            scanDb(conn);
        } catch (Exception e) {
            System.out.println("Failed SID " + urlSid + " -> " + e.getMessage());
        }
    }

    private static void scanDb(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, "DMSINHOUSE", "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                System.out.println("\nTABLE: " + tableName);
                try (ResultSet cols = meta.getColumns(null, "DMSINHOUSE", tableName, "%")) {
                    while (cols.next()) {
                        String colName = cols.getString("COLUMN_NAME");
                        String colType = cols.getString("TYPE_NAME");
                        int colSize = cols.getInt("COLUMN_SIZE");
                        System.out.println("  - " + colName + " : " + colType + "(" + colSize + ")");
                    }
                }
            }
        }
    }
}
