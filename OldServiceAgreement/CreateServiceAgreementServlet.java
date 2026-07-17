package com.ipca.serviceAgreement;

import com.ipca.connection.DMSConnection;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.jakarta.servlet5.JakartaServletFileUpload;

/**
 *
 * @author ruchita.saroj
 */
public class CreateServiceAgreementServlet extends HttpServlet {

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
        HttpSession session = request.getSession();
        String uid = ((session.getAttribute("userid") != null) ? (String) session.getAttribute("userid") : "");
//        String userName = Support.getName(uid);
        String loc = ((session.getAttribute("loc") != null) ? (String) session.getAttribute("loc").toString() : "");
        String com = ((session.getAttribute("com") != null) ? (String) session.getAttribute("com").toString() : "");
        String div = ((session.getAttribute("div") != null) ? (String) session.getAttribute("div").toString() : "");
        String app = ((session.getAttribute("app") != null) ? (String) session.getAttribute("app").toString() : "");
        String finyear = ((session.getAttribute("finyear") != null) ? (String) session.getAttribute("finyear").toString() : "");
        session.setAttribute("com", (String) com);
        session.setAttribute("div", (String) div);
        session.setAttribute("loc", (String) loc);
        session.setAttribute("app", (String) app);
        session.setAttribute("finyear", (String) finyear);
        ResourceBundle rb = ResourceBundle.getBundle("com.ipca.connection.ConnectionFile");
        String subappcode = "", subapp = "";
        String page = "create", saPan = "", saDoctorName = "", saDoctorCode = "", saInfavourOf = "", saCMELogNumber = "", saInterfaceNumber = "", saEmployeeID = "", saEmployeeName = "", saRCCode = "", saAmount = "", saEventName = "", filepath, name;
        Date saEventFromDate = null, saEventToDate = null;
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        int insertresult = 0, doc_code = 0;
        Connection con = null, concode = null, condoccode = null;
        PreparedStatement stmt = null;
        Statement st = null, stcode = null, stdoccode = null;
        int counter = 0;
        try {
            String UPLOAD_DIRECTORY = rb.getString("UPLOAD_DIRECTORY");
            UPLOAD_DIRECTORY = UPLOAD_DIRECTORY + "/" + app + "/" + com + "/" + div + "/" + loc + "/";
            boolean ismultipart = JakartaServletFileUpload.isMultipartContent(request);
            if (ismultipart) {
                DiskFileItemFactory factory = DiskFileItemFactory.builder().get();
                JakartaServletFileUpload upload = new JakartaServletFileUpload(factory);
                List<FileItem> multiparts = upload.parseRequest(request);
                String desc = "";
                for (FileItem item : multiparts) {
                    String fieldName = item.getFieldName();
                    if (fieldName.equals("saFile")) {
                    } else {
                        desc = item.getString();
                    }
                    if (fieldName.equalsIgnoreCase("saPan")) {
                        saPan = desc;
                    }
                    if (fieldName.equalsIgnoreCase("saDoctorName")) {
                        saDoctorName = desc;
                    }
                    if (fieldName.equals("saDoctorCode")) {
                        saDoctorCode = desc;
                    }
                    if (fieldName.equals("saInfavourOf")) {
                        saInfavourOf = desc;
                    }
                    if (fieldName.equals("saEmployeeID")) {
                        saEmployeeID = desc;
                    }
                    if (fieldName.equals("saEmployeeName")) {
                        saEmployeeName = desc;
                    }
                    if (fieldName.equals("saRCCode")) {
                        saRCCode = desc;
                    }
                    if (fieldName.equalsIgnoreCase("saSubApp")) {
                        if (desc != null) {
                            String SubAppDetails[] = desc.split("~");
                            subappcode = SubAppDetails[0];
                            subapp = SubAppDetails[1];
                            condoccode = DMSConnection.getNewInstance().getConnection();
                            stdoccode = condoccode.createStatement();
                            String querydoccode = "select nvl(max(to_number(doc_code)),0) + 1 as doc_code from DMS_SERVICE_AGREEMENT where financial_year='" + finyear + "' and subdivision_code='" + subappcode + "'";
                            ResultSet rsdoccode = stdoccode.executeQuery(querydoccode);
                            if (rsdoccode.next()) {
                                doc_code = rsdoccode.getInt("doc_code");
                            }
                            con = DMSConnection.getNewInstance().getConnection();
                            UPLOAD_DIRECTORY = UPLOAD_DIRECTORY + "/" + subappcode + "/" + finyear + "/" + doc_code + "/";
                        }
                    }
                    if (fieldName.equals("saInterfaceNumber")) {
                        saInterfaceNumber = desc;
                    }
                    if (fieldName.equals("saCMELogNumber")) {
                        saCMELogNumber = desc;
                    }
                    if (fieldName.equals("saAmount")) {
                        saAmount = desc;
                    }
                    if (fieldName.equalsIgnoreCase("saEventFromDate")) {
                        if (!desc.equals("null")) {
                            java.util.Date date = sdf1.parse(desc);
                            saEventFromDate = new java.sql.Date(date.getTime());
                        } else {
                            saEventFromDate = null;
                        }
                    }
                    if (fieldName.equalsIgnoreCase("saEventToDate")) {
                        if (!desc.equals("null")) {
                            java.util.Date date = sdf1.parse(desc);
                            saEventToDate = new java.sql.Date(date.getTime());
                        } else {
                            saEventToDate = null;
                        }
                    }
                    if (fieldName.equalsIgnoreCase("saEventName")) {
                        saEventName = desc;
                    }
                    if (subappcode != null || doc_code != 0) {
                        if (!item.isFormField()) {
                            File directory = new File(UPLOAD_DIRECTORY);
                            if (!directory.exists()) {
                                directory.mkdirs();
                            }
                            if (!item.isFormField()) {
                                name = new File(item.getName()).getName();
                                if (!name.equals("")) {
                                    name = name.replaceAll("&", "and");
                                    name = name.replaceAll("'", "-");
                                    filepath = UPLOAD_DIRECTORY + name;
                                    Path fullPath = Paths.get(UPLOAD_DIRECTORY, name);
                                    item.write(fullPath);
                                } else {
                                    filepath = UPLOAD_DIRECTORY;
                                }
                                if (!name.equals("")) {
                                    stmt = con.prepareStatement("insert into DMS_SERVICE_AGREEMENT "
                                            + "(COMPANY_ID,LOCATION_ID,DIVISION_NAME,APPLICATION_NAME,FINANCIAL_YEAR, "
                                            + "SUBDIVISION_CODE,SUBDIVISION_NAME,DOC_CODE, "
                                            + "DOCTOR_PAN,DOCTOR_CODE,DOCTOR_NAME,IN_FAVOUR_OF, "
                                            + "EMPLOYEE_ID,EMPLOYEE_NAME,RC_CODE, "
                                            + "INTERFACE_APP_NO,CME_LOG_NO,AMOUNT, "
                                            + "EVENT_FROM_DATE,EVENT_TO_DATE,EVENT_NAME, "
                                            + "CREATED_BY,CREATED_ON,FILE_NAME,FILE_PATH) "
                                            + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                                    con.setAutoCommit(false);
                                    stmt.setString(1, com);
                                    stmt.setString(2, loc);
                                    stmt.setString(3, div);
                                    stmt.setString(4, app);
                                    stmt.setString(5, finyear);
                                    stmt.setString(6, subappcode);
                                    stmt.setString(7, subapp);
                                    stmt.setInt(8, doc_code);
                                    stmt.setString(9, saPan);
                                    stmt.setString(10, saDoctorCode);
                                    stmt.setString(11, saDoctorName);
                                    stmt.setString(12, saInfavourOf);
                                    stmt.setString(13, saEmployeeID);
                                    stmt.setString(14, saEmployeeName);
                                    stmt.setString(15, saRCCode);
                                    stmt.setString(16, saInterfaceNumber);
                                    stmt.setString(17, saCMELogNumber);
                                    stmt.setString(18, saAmount);
                                    stmt.setDate(19, saEventFromDate);
                                    stmt.setDate(20, saEventToDate);
                                    stmt.setString(21, saEventName);
                                    stmt.setString(22, uid);
                                    stmt.setTimestamp(23, new java.sql.Timestamp(System.currentTimeMillis()));
                                    stmt.setString(24, name);
                                    stmt.setString(25, filepath);
                                    insertresult = stmt.executeUpdate();
                                }
                            }
                        }
                    }
                }
                if ((insertresult != 0)) {
                    con.setAutoCommit(true);
                    con.commit();
                    request.setAttribute("message", "Doc Code " + doc_code + " Created Successfully");
                    if (page.equals("create")) {
                        request.getRequestDispatcher(app + "/Create.jsp").forward(request, response);
                    } else {
                        request.getRequestDispatcher(app + "/Addfile.jsp").forward(request, response);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ex1) {
            }
            request.setAttribute("error", ex.getMessage());
            if (page.equals("create")) {
                request.getRequestDispatcher(app + "/Create.jsp").forward(request, response);
            } else {
                request.getRequestDispatcher(app + "/Addfile.jsp").forward(request, response);
            }
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (stmt != null) {
                    stmt.close();
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
