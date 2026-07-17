<%-- 
    Document   : Create
    Created on : 17 Aug, 2022, 5:44:29 PM
    Author     : ruchita.saroj
--%>

<%@page import="com.ipca.connection.SCMConnection"%>
<%@page import="com.ipca.connection.DMSConnection"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.Statement"%>
<%@page import="java.sql.DriverManager"%>
<%@page import="java.sql.Connection"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
        
        <%
            String uid = "", app = "", subapp = "", subappcode = "", com = "", div = "", loc = "", com_name = "", loc_name = "", finyear = "", daybook = "";
            uid = ((session.getAttribute("userid") != null) ? (String) session.getAttribute("userid") : "");
            com = ((session.getAttribute("com") != null) ? (String) session.getAttribute("com") : "");
            div = ((session.getAttribute("div") != null) ? (String) session.getAttribute("div") : "");
            loc = ((session.getAttribute("loc") != null) ? (String) session.getAttribute("loc") : "");
            app = ((session.getAttribute("app") != null) ? (String) session.getAttribute("app") : "");
            finyear = ((session.getAttribute("finyear") != null) ? (String) session.getAttribute("finyear") : "");
            com_name = ((session.getAttribute("com_name") != null) ? (String) session.getAttribute("com_name") : "");
            loc_name = ((session.getAttribute("loc_name") != null) ? (String) session.getAttribute("loc_name") : "");

            session.setAttribute("userid", uid);
            session.setAttribute("com", com);
            session.setAttribute("div", div);
            session.setAttribute("loc", loc);
            session.setAttribute("app", app);
            session.setAttribute("finyear", finyear);
            session.setAttribute("com_name", com_name);
            session.setAttribute("loc_name", loc_name);
        %>
        <script src="../js/jquery.min.js" type="text/javascript"></script>
        <script language="javascript" type="text/javascript">
            function repl() {
                document.location.replace("index.jsp");
            }
            function getxmlHttpRequest() {
                try {
                    xmlHttpRequest = new XMLHttpRequest();
                } catch (e) {
                    try {
                        xmlHttpRequest = new ActiveXObject("Msxml2.XMLHTTP");
                    } catch (ee) {
                        try {
                            xmlHttpRequest = new ActiveXObject("Microsoft.XMLHTTP");
                        } catch (ex) {
                            alert("your browser doesn't support Ajax");
                            return false;
                        }
                    }
                }
            }

            function callservlet(file) {
                window.open('<%=request.getContextPath()%>/ViewDocumentServlet?filename=' + file, '_blank');
            }

            function removeFile(saDocCode, subAppName, filename, filepath) {
                let text = "Are you sure you want to remove the document?";
                if (confirm(text) === true) {
                    document.getElementById('formSearchServiceAgreement').action = "<%=request.getContextPath()%>/RemoveDocumentServlet?saDocCode=" + saDocCode + "&subAppName=" + subAppName + "&filename=" + filename + "&filepath=" + filepath + "&tablename=DMS_SERVICE_AGREEMENT";
                    document.getElementById('formSearchServiceAgreement').submit();
                }
            }

            function doClick() {
                var saSubApp = document.getElementById('saSubApp').value;
                var saPan = document.getElementById('saPan').value;
                var saEmployeeID = document.getElementById('saEmployeeID').value;
                var saCreatedDate = document.getElementById('saCreatedDate').value;
                var saPaymentStatus = "";
                var ele = document.getElementsByName('saPaymentStatus');
                for (var i = 0; i < ele.length; i++) {
                    if (ele[i].checked)
                    {
                        saPaymentStatus = ele[i].value;
                    }
                }
                getxmlHttpRequest();
                var url = "<%=request.getContextPath()%>/SearchServiceAgreementServlet?saSubApp=" + saSubApp + "&saPan=" + saPan + "&saEmployeeID=" + saEmployeeID + "&saCreatedDate=" + saCreatedDate + "&saPaymentStatus=" + saPaymentStatus;
                xmlHttpRequest.onreadystatechange = getdetaildata;
                xmlHttpRequest.open("POST", url, true);
                xmlHttpRequest.send(null);
            }

            function getdetaildata() {
                if (xmlHttpRequest.readyState === 4) {
                    if (xmlHttpRequest.status === 200) {
                        document.getElementById("data").innerHTML = this.responseText;
                    }
                }
            }
        </script>
    </head>
    <body class="search">
        <%
            if (uid == "") {
                response.sendRedirect(request.getContextPath() + "/index.jsp");
            } else {
        %>
        <jsp:include page="/header.jsp"></jsp:include>
            <div id="templatemo_body" align="center">
                <form action="" id="formSearchServiceAgreement" name="formSearchServiceAgreement" method="post">
                    <div id="alldetails"> <%=com_name%> | <%=div%> | <%=loc_name%> | <%=app%> | <%=finyear%></div>
                <div id="heading">
                    <h3>Search Service Agreement</h3>
                </div>
                <%
                    Connection con = DMSConnection.getNewInstance().getConnection();
                    Statement st = con.createStatement();
                    String query = "select distinct h.code,h.name from hrms_live.subdivision@hrmsdrdb h, DMS_USER_RIGHTS d "
                            + "where h.company_id='" + com + "' "
                            + "and h.code=d.SUB_APPLICATION_NAME "
                            + "and d.USER_ID='" + uid + "' "
                            + "order by 2";
                    ResultSet rs = st.executeQuery(query);
                %>
                <table>
                    <tr>
                        <td>Select Sub Division:</td>
                        <td>
                            <select id="saSubApp" name="saSubApp" style="width: 150px;">
                                <option value="SELECT">SELECT</option>
                                <%
                                    while (rs.next()) {
                                        subappcode = rs.getString("code");
                                        subapp = rs.getString("name");
                                %>
                                <option value="<%=subappcode%>~<%=subapp%>"><%=subapp%></option>
                                <%
                                    }
                                %>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td> 
                            Enter Employee ID:
                        </td>
                        <td>
                            <input type="text" name="saEmployeeID" id="saEmployeeID" style="width: 100px;" />
                        </td>
                    </tr>
                    <tr>
                        <td>
                            Entry Date:
                        </td>
                        <td>
                            <input type="date" name="saCreatedDate" id="saCreatedDate" style="width: 100px;" />
                        </td>
                    </tr>
                    <tr>
                        <td>
                            Enter Doctor's PAN:
                        </td>
                        <td>
                            <input type="text" name="saPan" id="saPan" onkeyup="this.value = this.value.toUpperCase();" style="width: 100px;" />
                        </td>
                    </tr>
                    <tr>
                        <td>
                            Select Payment Status:
                        </td>
                        <td>
                            <input type="radio" id="saPaymentStatus" name="saPaymentStatus" value="Paid">
                            <label for="Paid">Paid</label>
                            <input type="radio" id="saPaymentStatus" name="saPaymentStatus" value="Unpaid">
                            <label for="Unpaid">Unpaid</label>
                        </td>
                    </tr>
                    <tr>
                        <td align='center'><input type='button' value='GO' id='btnGo' name='btnGo' onclick='doClick();'/>&nbsp;
                        </td>
                    </tr>
                </table>
                <div id="data" name="data">
                </div>
            </form>
            <div id="message" align="center">
                <%
                    String message = (String) request.getAttribute("message");
                    if (message != null) {
                %> 
                <%=message%> 
                <%
                    }
                %>
            </div>
            <div id="error" align="center">
                <%
                    String error = (String) request.getAttribute("error");
                    if (error != null) {
                %> 
                <%=error%> 
                <%
                    }
                %>
            </div>
        </div>    
    </body>
    <%}%>
</html>