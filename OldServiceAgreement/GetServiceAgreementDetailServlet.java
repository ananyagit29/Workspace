package com.ipca.serviceAgreement;

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
public class GetServiceAgreementDetailServlet extends HttpServlet {

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
            String saSubApp = request.getParameter("saSubApp");
            String saInterfaceNumber = request.getParameter("saInterfaceNumber");
            String uid = ((session.getAttribute("userid") != null) ? (String) session.getAttribute("userid") : "");
            String loc = ((session.getAttribute("loc") != null) ? (String) session.getAttribute("loc").toString() : "");
            String com = ((session.getAttribute("com") != null) ? (String) session.getAttribute("com").toString() : "");
            String div = ((session.getAttribute("div") != null) ? (String) session.getAttribute("div").toString() : "");
            String app = ((session.getAttribute("app") != null) ? (String) session.getAttribute("app").toString() : "");
            String finyear = ((session.getAttribute("finyear") != null) ? (String) session.getAttribute("finyear") : "");
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat format2 = new SimpleDateFormat("dd-MM-yyyy");
            java.util.Date createdon = null, createddate = null;
            String createdOn = null, createdDate = null;
            String subappcode = null, subapp = null;
            if (saSubApp != null) {
                if (!saSubApp.equalsIgnoreCase("SELECT")) {
                    String SubAppDetails[] = saSubApp.split("~");
                    subappcode = SubAppDetails[0];
                    subapp = SubAppDetails[1];
                }
            }
            session.setAttribute("com", (String) com);
            session.setAttribute("div", (String) div);
            session.setAttribute("loc", (String) loc);
            session.setAttribute("app", (String) app);
            response.setContentType("text/html;charset=UTF-8");

            if (op.equalsIgnoreCase("getdoctordetail")) {
//                String query = "select code,name,in_favour_of from ipcaprod.account_head@ipcascmdb where entity_code = '*' and control_code = '4' and active='Y' and pan_no_1 = '" + saPan + "'";
                String query = "Select h.code,h.name,a.infavour as in_favour_of  from account_head@ipcascmdb h, account_head_infavour@ipcascmdb a\n"
                        + "Where a.company_code='1' and h.pan_no_1 = '" + saPan + "' and nvl(final_approved,'N') = 'Y' and a.entity_code = '*'\n"
                        + "    and a.control_code in ('4','2') and a.active='Y' and a.sr_no = (select max(b.sr_no) from account_head_infavour@ipcascmdb b \n"
                        + "    where b.company_code =a.company_code and b.entity_code = a.entity_code and \n"
                        + "    a.code = b.code and a.control_code = b.control_code and b.active ='Y' and nvl(b.final_approved,'N') = 'Y') and\n"
                        + "    h.company_code=a.company_code and h.entity_code= a.entity_code and h.control_code = a.control_code and h.code = a.code \n"
                        + "    and h.active = 'Y'";
                ResultSet rs = st.executeQuery(query);
//                System.out.println("SAquery: " + query);
                String code = "", name = "", in_favour_of = "";
                if (rs.next()) {
                    code = rs.getString("code");
                    name = rs.getString("name");
                    in_favour_of = rs.getString("in_favour_of");
                    if (name.equalsIgnoreCase("") || in_favour_of.equalsIgnoreCase("")) {
                        out.println("<tr>");
                        out.println("<td><h1 style='color:red'>Doctor Master Not Found</h1></td>");
                        out.println("</tr>");
                    } else {
                        out.println("<tr><td>Doctor Name : </td><td><input type='text' id='saDoctorName' name='saDoctorName' value='" + name + "' readonly style='width:250px'/></td></tr>");
                        out.println("<tr><td>Doctor Code : </td><td><input type='text' id='saDoctorCode' name='saDoctorCode' value='" + code + "' readonly style='width:100px'/></td></tr>");
                        out.println("<tr><td>In Favour Of : </td><td><input type='text' id='saInfavourOf' name='saInfavourOf' value='" + in_favour_of + "' readonly style='width:250px'/></td></tr>");
                        out.println("<tr><td>Employee ID : </td><td><input type='text' id='saEmployeeID' name='saEmployeeID' style='width: 100px;' onblur='getEmployeeDetail()'></td></tr>");
                        out.println("<tr><td>Employee Name :</td><td><input type='text' id='saEmployeeName' name='saEmployeeName' style='width: 250px;' readonly></td></tr>");
                        out.println("<tr><td>RC Code :</td><td><input type='text' id='saRCCode' name='saRCCode' style='width: 100px;'/></td></tr>");
                        out.println("<tr><td>Select Sub Division:</td><td id='saSubAppfield'>");
                        Connection consubapp = DMSConnection.getNewInstance().getConnection();
                        Statement stsubapp = consubapp.createStatement();
                        String querysubapp = "select distinct h.code,h.name from hrms_live.subdivision@hrmsdrdb h, DMS_USER_RIGHTS d "
                                + "where h.company_id='" + com + "' "
                                + "and h.code=d.SUB_APPLICATION_NAME "
                                + "and d.USER_ID='" + uid + "' "
                                + "order by 2";
                        ResultSet rssubapp = stsubapp.executeQuery(querysubapp);
                        out.println("<select id='saSubAppList' name='saSubAppList' style='width: 150px;' onchange='setsaSubApp();'>");
                        out.println("<option value='SELECT'>SELECT</option>");
                        while (rssubapp.next()) {
                            subappcode = rssubapp.getString("code");
                            subapp = rssubapp.getString("name");
                            out.println("<option value='" + subappcode + "~" + subapp + "'>" + subapp + "</option>");
                        }
                        out.println("</select></td><td><input type='hidden' name='saSubApp' id='saSubApp' value=''/></td></tr>");
                        out.println("<tr><td>Interface App No. : </td><td><input type='text' maxlength='20' id='saInterfaceNumber' name='saInterfaceNumber' style='width:110px' onblur='getInterfaceDetail()'/></td></tr>");
                        out.println("<tr><td>CME Log No. : </td><td><input type='text' maxlength='14' id='saCMELogNumber' name='saCMELogNumber' onkeyup='this.value = this.value.toUpperCase();' style='width:110px'/></td></tr>");
                        out.println("<tr><td>Amount : </td><td><input type='text' maxlength='6' id='saAmount' name='saAmount' style='width:110px'/></td></tr>");
                        out.println("<tr><td>Event From Date : </td><td><input type='date' id='saEventFromDate' name='saEventFromDate' /></td></tr>");
                        out.println("<tr><td>Event To Date : </td><td><input type='date' id='saEventToDate' name='saEventToDate' onchange='checkDate();'/></td></tr>");
                        out.println("<tr><td>Event Name : </td><td><input type='text' id='saEventName' name='saEventName' onkeyup='this.value = this.value.toUpperCase();' style='width:250px'/></td></tr>");
                        out.println("<tr><td>Upload File : </td><td><input type='file' name='saFile' id='saFile' accept='application/pdf'/></td></tr>");
                        out.println("<tr><td align='center'><input type='button' value='Submit' id='btnsearch' name='btnsearch' onclick='doClick();'/></td></tr>");
                    }
                } else {
                    out.println("<tr>");
                    out.println("<td><h1 style='color:red'>Doctor Master Not Found</h1></td>");
                    out.println("</tr>");
                }
            }
            if (op.equalsIgnoreCase("empdetails")) {
                Connection conn = null;
                Statement stmt = null;
                try {
                    conn = DMSConnection.getNewInstance().getConnection();
                    stmt = conn.createStatement();
                    String query = "SELECT 'H01',h.employee_id,first_name||' '||last_name as EMPLOYEE_NAME, "
                            + "responsibility_code, h.subdivision_code "
                            + "from hrms_live.pms_employee@hrmsdrdb h, dms_user_rights d "
                            + "where h.COMPANY_ID='1'  "
                            + "and h.LOCATION_ID = 'H01' "
                            + "and emp_status ='A'  "
                            + "and h.employee_id = '" + saEmployeeID + "' "
                            + "and APPLICATION_NAME = '" + app + "' and user_id = '" + uid + "' "
                            + "and h.subdivision_code = sub_application_name "
                            + "union all "
                            + "SELECT '101',employee_id,first_name||' '||last_name as EMPLOYEE_NAME, "
                            + "responsibility_code, subdivision_code "
                            + "from hrms_live.pms_employee@hrmsdrdb "
                            + "where COMPANY_ID='1'  "
                            + "and LOCATION_ID = '101' "
                            + "and emp_status ='A'  "
                            + "and employee_id = '" + saEmployeeID + "' ";

                    ResultSet rs = stmt.executeQuery(query);
                    String employee_id = "", employee_name = "", rc_code = "", subdivision_code = "";
                    if (rs.next()) {
                        employee_id = rs.getString("employee_id");
                        employee_name = rs.getString("employee_name");
                        rc_code = rs.getString("responsibility_code");
                        subdivision_code = rs.getString("subdivision_code");
                    } else {
                        employee_id = null;
                        employee_name = null;
                        rc_code = null;
                        subdivision_code = null;
                    }
                    out.println(employee_id + ";" + employee_name + ";" + rc_code + ";" + subdivision_code);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (op.equalsIgnoreCase("interfacedetails")) {
                Connection conn = null;
                Statement stmt = null;
                try {
                    conn = DMSConnection.getNewInstance().getConnection();
                    stmt = conn.createStatement();
                    String query = "select DOC_CODE,SUBDIVISION_NAME,DOCTOR_NAME,EMPLOYEE_NAME,CREATED_ON from DMS_SERVICE_AGREEMENT where INTERFACE_APP_NO='" + saInterfaceNumber + "'";
                    ResultSet rs = stmt.executeQuery(query);
                    String DOC_CODE = "", SUBDIVISION_NAME = "", DOCTOR_NAME = "", EMPLOYEE_NAME = "", CREATED_ON = "";
                    if (rs.next()) {
                        DOC_CODE = rs.getString("DOC_CODE");
                        SUBDIVISION_NAME = rs.getString("SUBDIVISION_NAME");
                        DOCTOR_NAME = rs.getString("DOCTOR_NAME");
                        EMPLOYEE_NAME = rs.getString("EMPLOYEE_NAME");
                        if (rs.getString("CREATED_ON") == null) {
                            CREATED_ON = null;
                        } else {
                            createdon = format1.parse(rs.getString("created_on"));
                            createdOn = format2.format(createdon);
                            CREATED_ON = createdOn;
                        }
                    } else {
                        DOC_CODE = null;
                        SUBDIVISION_NAME = null;
                        DOCTOR_NAME = null;
                        EMPLOYEE_NAME = null;
                        CREATED_ON = null;
                    }
                    out.println(DOC_CODE + ";" + SUBDIVISION_NAME + ";" + DOCTOR_NAME + ";" + EMPLOYEE_NAME + ";" + CREATED_ON);
                } catch (Exception e) {
                }
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
