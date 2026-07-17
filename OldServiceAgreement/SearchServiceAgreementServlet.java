package com.ipca.serviceAgreement;

import com.ipca.common.Support;
import com.ipca.connection.DMSConnection;
import com.ipca.connection.SCMConnection;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 *
 * @author ruchita.saroj
 */
public class SearchServiceAgreementServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        Connection con = null, con1 = null;
        Statement st = null, st1 = null;
        try {
            con = DMSConnection.getNewInstance().getConnection();
            st = con.createStatement();
            con1 = SCMConnection.getNewInstance().getConnection();
            st1 = con1.createStatement();
            PrintWriter out = response.getWriter();
            HttpSession session = request.getSession();
            String op = request.getParameter("op");
            String saPan = request.getParameter("saPan");
            String saEmployeeID = request.getParameter("saEmployeeID");
            String saCreatedDate = request.getParameter("saCreatedDate");
            String saPaymentStatus = request.getParameter("saPaymentStatus");
            String saSubApp = request.getParameter("saSubApp");
            String uid = ((session.getAttribute("userid") != null) ? (String) session.getAttribute("userid") : "");
            String loc = ((session.getAttribute("loc") != null) ? (String) session.getAttribute("loc").toString() : "");
            String com = ((session.getAttribute("com") != null) ? (String) session.getAttribute("com").toString() : "");
            String div = ((session.getAttribute("div") != null) ? (String) session.getAttribute("div").toString() : "");
            String app = ((session.getAttribute("app") != null) ? (String) session.getAttribute("app").toString() : "");
            String finyear = ((session.getAttribute("finyear") != null) ? (String) session.getAttribute("finyear") : "");
            session.setAttribute("com", (String) com);
            session.setAttribute("div", (String) div);
            session.setAttribute("loc", (String) loc);
            session.setAttribute("app", (String) app);
            String subappcode = "", subapp = "";
            if (saSubApp != null) {
                if (!saSubApp.equalsIgnoreCase("SELECT")) {
                    String SubAppDetails[] = saSubApp.split("~");
                    subappcode = SubAppDetails[0];
                    subapp = SubAppDetails[1];
                }
            }
            response.setContentType("text/html;charset=UTF-8");
            SimpleDateFormat oldsdf = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy");
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat format2 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            java.util.Date createdon = null, createddate = null;
            String createdOn = null, createdDate = null;
            if (saCreatedDate != null) {
                if (!saCreatedDate.equalsIgnoreCase("")) {
                    createddate = oldsdf.parse(saCreatedDate);
                    createdDate = sdf.format(createddate);
                }
            }
            String Role = Support.getRole(uid, app, loc);
            String query = "select distinct SUBDIVISION_NAME,SUBDIVISION_CODE,DOC_CODE,DOCTOR_PAN,DOCTOR_CODE,DOCTOR_NAME,"
                    + "IN_FAVOUR_OF,EMPLOYEE_ID,EMPLOYEE_NAME,RC_CODE,INTERFACE_APP_NO,CME_LOG_NO,AMOUNT, "
                    + "EVENT_FROM_DATE,EVENT_TO_DATE,EVENT_NAME,VOUCHER_NO,VOUCHER_DATE,CHEQUE_NO,CHEQUE_DATE, "
                    + "CREATED_BY,CREATED_ON,FILE_NAME,FILE_PATH "
                    + "from DMS_SERVICE_AGREEMENT da,DMS_USER_RIGHTS d "
                    + "where FINANCIAL_YEAR = '" + finyear + "' "
                    + "and d.USER_ID='" + uid + "' "
                    + "and d.SUB_APPLICATION_NAME = da.SUBDIVISION_CODE ";
            if (subappcode != "") {
                query = query + "AND SUBDIVISION_CODE='" + subappcode + "' ";
            }
            if (saPan != "") {
                query = query + "AND DOCTOR_PAN='" + saPan + "' ";
            }
            if (saEmployeeID != "") {
                query = query + "AND EMPLOYEE_ID='" + saEmployeeID + "' ";
            }
            if (createdDate != null) {
                query = query + "AND to_date(CREATED_ON,'DD-MM-YYYY') = TO_DATE('" + createdDate + "', 'DD-MM-YYYY')";
            }
            if (saPaymentStatus != "") {
                if (saPaymentStatus.equalsIgnoreCase("Paid")) {
                    query = query + "AND CHEQUE_NO IS NOT NULL ";
                } else {
                    query = query + "AND CHEQUE_NO IS NULL ";
                }
            }
            query = query + "ORDER BY 1,3 desc";

            String savequery = query.replaceAll(",FILE_PATH", "");
            ResultSet rs = st.executeQuery(query);
            if (rs.next()) {
                savequery = savequery.replaceAll("'", "%27");
                savequery = savequery.replaceAll(" ", "%20");
                savequery = savequery.replaceAll("-", "%2D");
                savequery = savequery.replaceAll(":", "%3a");
                savequery = savequery.replaceAll("_", "%5F");
                savequery = savequery.replaceAll("\\+", "plus");
                String SUBDIVISION_NAME = "", SUBDIVISION_CODE = "", DOCTOR_PAN = "", DOCTOR_CODE = "", DOCTOR_NAME = "",
                        IN_FAVOUR_OF = "", INTERFACE_APP_NO = "", CME_LOG_NO = "", EMPLOYEE_ID = "", EMPLOYEE_NAME = "", RC_CODE = "",
                        EVENT_FROM_DATE = "", EVENT_TO_DATE = "", EVENT_NAME = "",
                        AMOUNT = "", VOUCHER_NO = "", VOUCHER_DATE = "", CHEQUE_NO = "", CHEQUE_DATE = "",
                        CREATED_BY = "", CREATED_ON = "", FILE_NAME = "", FILE_PATH = "";
                int DOC_CODE = 0;
                out.println("<div style='float: right;'><input type='button' id='btnexcel' value='Save As Excel' onclick=\"document.getElementById('btnexcel').disabled = true ;window.open('" + request.getContextPath() + "/SaveAsExcel?filename=ServiceAgreementReport&query=" + savequery + "','_self')\"/></div>");
                out.println("<br/><br/><div id='scrollda'><table id='showData' name='showData' width='250%'>");
                out.println("<tr><th>Sub Division</th><th>Doc Code</th><th>Doctor PAN</th><th>Doctor Name</th><th>Interface Number</th><th>Event From Date</th>"
                        + "<th>Event To Date</th><th>Amount</th><th>Filename</th>");
                if (Role.contains("Remove")) {
                    out.println("<th>Remove</th>");
                }
                out.println("<th>Event Name</th><th>CME Log Number</th><th>Doctor Code</th><th>In Favour Of</th><th>Employee ID</th><th>Employee Name</th>"
                        + "<th>RC Code</th>"
                        + "<th>Voucher No.</th><th>Voucher Date</th><th>Cheque No.</th><th>Cheque Date</th>"
                        + "<th>Created By</th><th>Created On</th></tr>");
                do {
                    SUBDIVISION_NAME = rs.getString("SUBDIVISION_NAME") == null ? "" : rs.getString("SUBDIVISION_NAME").trim().toUpperCase();
                    SUBDIVISION_CODE = rs.getString("SUBDIVISION_CODE") == null ? "" : rs.getString("SUBDIVISION_CODE").trim().toUpperCase();
                    DOC_CODE = rs.getInt("DOC_CODE");
                    DOCTOR_PAN = rs.getString("DOCTOR_PAN") == null ? "" : rs.getString("DOCTOR_PAN").trim().toUpperCase();
                    DOCTOR_CODE = rs.getString("DOCTOR_CODE") == null ? "" : rs.getString("DOCTOR_CODE").trim().toUpperCase();
                    DOCTOR_NAME = rs.getString("DOCTOR_NAME") == null ? "" : rs.getString("DOCTOR_NAME").trim().toUpperCase();
                    IN_FAVOUR_OF = rs.getString("IN_FAVOUR_OF") == null ? "" : rs.getString("IN_FAVOUR_OF").trim().toUpperCase();
                    INTERFACE_APP_NO = rs.getString("INTERFACE_APP_NO") == null ? "" : rs.getString("INTERFACE_APP_NO").trim().toUpperCase();
                    CME_LOG_NO = rs.getString("CME_LOG_NO") == null ? "" : rs.getString("CME_LOG_NO").trim().toUpperCase();
                    EMPLOYEE_ID = rs.getString("EMPLOYEE_ID") == null ? "" : rs.getString("EMPLOYEE_ID").trim().toUpperCase();
                    EMPLOYEE_NAME = rs.getString("EMPLOYEE_NAME") == null ? "" : rs.getString("EMPLOYEE_NAME").trim().toUpperCase();
                    RC_CODE = rs.getString("RC_CODE") == null ? "" : rs.getString("RC_CODE").trim().toUpperCase();
                    if (rs.getDate("EVENT_FROM_DATE") == null) {
                        EVENT_FROM_DATE = "";
                    } else {
                        EVENT_FROM_DATE = sdf.format(rs.getDate("EVENT_FROM_DATE"));
                    }
                    if (rs.getDate("EVENT_TO_DATE") == null) {
                        EVENT_TO_DATE = "";
                    } else {
                        EVENT_TO_DATE = sdf.format(rs.getDate("EVENT_TO_DATE"));
                    }
                    EVENT_NAME = rs.getString("EVENT_NAME") == null ? "" : rs.getString("EVENT_NAME").trim().toUpperCase();
                    AMOUNT = rs.getString("AMOUNT") == null ? "" : rs.getString("AMOUNT").trim().toUpperCase();
                    VOUCHER_NO = rs.getString("VOUCHER_NO") == null ? "" : rs.getString("VOUCHER_NO");
                    if (rs.getDate("VOUCHER_DATE") == null) {
                        VOUCHER_DATE = "";
                    } else {
                        VOUCHER_DATE = sdf.format(rs.getDate("VOUCHER_DATE"));
                    }
                    CHEQUE_NO = rs.getString("CHEQUE_NO") == null ? "" : rs.getString("CHEQUE_NO");
                    if (rs.getDate("CHEQUE_DATE") == null) {
                        CHEQUE_DATE = "";
                    } else {
                        CHEQUE_DATE = sdf.format(rs.getDate("CHEQUE_DATE"));
                    }
                    CREATED_BY = rs.getString("CREATED_BY") == null ? "" : rs.getString("CREATED_BY");
                    createdon = format1.parse(rs.getString("created_on"));
                    createdOn = format2.format(createdon);
                    if (rs.getString("CREATED_ON") == null) {
                        CREATED_ON = "";
                    } else {
                        CREATED_ON = createdOn;
                    }
                    FILE_NAME = rs.getString("FILE_NAME") == null ? "" : rs.getString("FILE_NAME");
                    FILE_PATH = rs.getString("FILE_PATH") == null ? "" : rs.getString("FILE_PATH");
                    out.println("<tr><td style='width:100px;'>" + SUBDIVISION_NAME + "</td>"
                            + "<td style='text-align: end;width:50px;'>" + DOC_CODE + "</td>"
                            + "<td style='width:120px;'>" + DOCTOR_PAN + "</td>"
                            + "<td style='width:350px;'>" + DOCTOR_NAME + "</td>"
                            + "<td style='width:100px;'>" + INTERFACE_APP_NO + "</td>"
                            + "<td style='width:200px;'>" + EVENT_FROM_DATE + "</td>"
                            + "<td style='width:200px;'>" + EVENT_TO_DATE + "</td>"
                            + "<td style='text-align: end;width:100px;'>" + AMOUNT + "</td>");
                    if (FILE_NAME != "") {
                        String path = FILE_PATH.replaceAll(" ", "%20");
                        path = path.replaceAll("\\\\", "/");
                        out.println("<td style='width:300px;'><input type='button' id='buttonlink' onclick=\"callservlet('" + path + "')\" value='" + FILE_NAME + "'/></td>");
                        if (Role.contains("Remove")) {
                            if (CHEQUE_NO.equals("")) {
                                out.println("<td style='width:50px;'><button type='button' onclick=\"removeFile('" + DOC_CODE + "','" + SUBDIVISION_NAME + "','" + FILE_NAME + "','" + path + "')\"><img src='" + request.getContextPath() + "/images/remove.png' alt='Remove'></button></td>");
                            } else {
                                out.println("<td></td>");
                            }
                        }
                    }
                    out.println("<td style='width:200px;'>" + EVENT_NAME + "</td>"
                            + "<td style='width:200px;'>" + CME_LOG_NO + "</td>"
                            + "<td style='width:150px;'>" + DOCTOR_CODE + "</td>"
                            + "<td style='width:300px;'>" + IN_FAVOUR_OF + "</td>"
                            + "<td style='width:100px;'>" + EMPLOYEE_ID + "</td>"
                            + "<td style='width:400px;'>" + EMPLOYEE_NAME + "</td>"
                            + "<td style='width:100px;'>" + RC_CODE + "</td>"
                            + "<td style='width:150px;'>" + VOUCHER_NO + "</td>"
                            + "<td style='width:200px;'>" + VOUCHER_DATE + "</td>"
                            + "<td style='width:150px;'>" + CHEQUE_NO + "</td>"
                            + "<td style='width:200px;'>" + CHEQUE_DATE + "</td>"
                            + "<td style='width:100px;'>" + CREATED_BY + "</td>"
                            + "<td style='width:200px;'>" + CREATED_ON + "</td>"
                            + "</tr>");
                } while (rs.next());
                out.println("</table></div>");
            } else {
                out.println("<tr>");
                out.println("<td><h1>Data Not Found For Entered Criteria</h1></td>");
                out.println("</tr>");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (st != null) {
                    st.close();
                }
                if (con1 != null) {
                    con1.close();
                }
                if (st1 != null) {
                    st1.close();
                }
            } catch (SQLException ex) {
            }
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }

}
