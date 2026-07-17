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
        
        <% String uid = "", app = "", subapp = "", subappcode = "", com = "", div = "", loc = "", com_name = "", loc_name = "", finyear = "", daybook = "";
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

            function getDoctorDetail()
            {
                if (document.getElementById("message") !== null)
                    document.getElementById("message").innerHTML = "";
                if (document.getElementById("error") !== null)
                    document.getElementById("error").innerHTML = "";
                document.getElementById("docdetails").innerHTML = "";
                var saPan = document.getElementById('saPan').value;
//                var saSubApp = document.getElementById('saSubApp').value;
                getxmlHttpRequest();
                var url = "<%=request.getContextPath()%>/GetServiceAgreementDetailServlet?op=getdoctordetail&saPan=" + saPan;// + "&saSubApp=" + saSubApp;
                xmlHttpRequest.onreadystatechange = getdoctordetaildata;
                xmlHttpRequest.open("POST", url, true);
                xmlHttpRequest.send(null);
            }

            function getdoctordetaildata()
            {
                if (xmlHttpRequest.readyState === 4) {
                    if (xmlHttpRequest.status === 200) {
                        var data = this.responseText;
                        document.getElementById("docdetails").innerHTML = data;
                    }
                }
            }

            function getInterfaceDetail() {
                var saInterfaceNumber = document.getElementById("saInterfaceNumber").value;
                getxmlHttpRequest();
                var url = "<%=request.getContextPath()%>/GetServiceAgreementDetailServlet?op=interfacedetails&saInterfaceNumber=" + saInterfaceNumber;
                xmlHttpRequest.onreadystatechange = getinterfacedetaildata;
                xmlHttpRequest.open("POST", url, true);
                xmlHttpRequest.send(null);
            }

            function getinterfacedetaildata() {
                if (xmlHttpRequest.readyState === 4) {
                    if (xmlHttpRequest.status === 200) {
                        const interfaceDetails = this.responseText.split(";");
                        var doc_code = interfaceDetails[0];
                        var sub_app_code = interfaceDetails[1];
                        var doc_name = interfaceDetails[2];
                        var emp_name = interfaceDetails[3];
                        var created_on = interfaceDetails[4];
                        if (doc_code === 'null' || sub_app_code === 'null' || doc_name === 'null' || emp_name === 'null' || created_on === 'null')
                        {
//                            document.getElementById("saEmployeeID").disabled = false;
                            document.getElementById("error").innerHTML = "";
                            document.getElementById("btnsearch").disabled = false;
                        } else
                        {
//                            document.getElementById("saEmployeeID").disabled = true;
                            document.getElementById("error").innerHTML = "Record exists for this Interface no. against Doc Code:" + doc_code + " <br/> Created On:" + created_on + " <br/> Sub Division:" + sub_app_code + " <br/> Doctor Name:" + doc_name + " <br/> Employee Name:" + emp_name;
                            document.getElementById("btnsearch").disabled = true;
                        }
                    }
                }
            }

            function getEmployeeDetail() {
                document.getElementById("saSubAppList").disabled = false;
                var saEmployeeID = document.getElementById("saEmployeeID").value;
//                var saSubApp = document.getElementById('saSubApp').value;
                getxmlHttpRequest();
                var url = "<%=request.getContextPath()%>/GetServiceAgreementDetailServlet?op=empdetails&saEmployeeID=" + saEmployeeID;// + "&saSubApp=" + saSubApp;
                xmlHttpRequest.onreadystatechange = getempndetaildata;
                xmlHttpRequest.open("POST", url, true);
                xmlHttpRequest.send(null);
            }

            function getempndetaildata() {
                if (xmlHttpRequest.readyState === 4) {
                    if (xmlHttpRequest.status === 200) {
                        if (this.responseText !== null) {
                            const empDetails = this.responseText.split(";");
                            var employee_id = empDetails[0];
                            var employee_name = empDetails[1];
                            var rc_code = empDetails[2];
                            var subdivision_code = empDetails[3].trim();
                            if (employee_name === 'null' || rc_code === 'null')
                            {
                                document.getElementById("saEmployeeName").value = "";
                                document.getElementById("saRCCode").value = "";
                                document.getElementById("saSubAppList").disabled = true;
                                document.getElementById("error").innerHTML = "Employee not found";
                                document.getElementById("btnsearch").disabled = true;
                            } else
                            {
                                if (subdivision_code === 'null')
                                {
                                    document.getElementById("saEmployeeName").value = employee_name;
                                    document.getElementById("saRCCode").value = rc_code;
                                    document.getElementById('saRCCode').removeAttribute('readonly');
                                    document.getElementById("saSubAppList").disabled = false;
                                    document.getElementById("error").innerHTML = "";
                                    document.getElementById("btnsearch").disabled = false;
                                } else
                                {
                                    document.getElementById("saEmployeeName").value = employee_name;
                                    document.getElementById("saRCCode").value = rc_code;
                                    document.getElementById('saRCCode').setAttribute('readonly', true);
                                    document.getElementById("error").innerHTML = "";
                                    document.getElementById("btnsearch").disabled = false;
                                    for (var i = 0; i < document.getElementById("saSubAppList").options.length; i++) {
                                        var ddvalue = document.getElementById("saSubAppList").options[i].value.split("~");
                                        var subdivcode = ddvalue[0];
                                        if (subdivcode === subdivision_code) {
                                            document.getElementById("saSubAppList").options[i].selected = true;
                                            document.getElementById("saSubAppList").disabled = true;
                                            document.getElementById("saSubApp").value = document.getElementById("saSubAppList").options[i].value;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            function setsaSubApp()
            {
                var saSubAppList = document.getElementById("saSubAppList").value;
                document.getElementById("saSubApp").value = saSubAppList;
            }

            function checkDate() {
                var saEventFromDate = document.getElementById("saEventFromDate");
                var saEventToDate = document.getElementById("saEventToDate");
                if (saEventToDate.value < saEventFromDate.value) {
                    alert("Event To Date cannot be less than Event From Date");
                    saEventToDate.value = "";
                }
            }

            function doClick() {
                var saInterfaceNumber = document.getElementById('saInterfaceNumber').value;
                var saCMELogNumber = document.getElementById('saCMELogNumber').value;
                var saEmployeeID = document.getElementById('saEmployeeID').value;
                var saAmount = document.getElementById('saAmount').value;
                var saEventFromDate = document.getElementById('saEventFromDate').value;
                var saEventToDate = document.getElementById('saEventToDate').value;
                var saEventName = document.getElementById('saEventName').value;
                var saFile = document.getElementById("saFile");
                if (saInterfaceNumber === "")
                {
                    alert("Please Enter Interface Application Number!");
                }
                if (saCMELogNumber === "")
                {
                    alert("Please Enter CME Log Number!");
                } else if (saEmployeeID === "")
                {
                    alert("Please Enter Employee Number!");
                } else if (saAmount === "")
                {
                    alert("Please Enter Amount");
                } else if (saEventFromDate === "")
                {
                    alert("Please Select Event From Date");
                } else if (saEventToDate === "")
                {
                    alert("Please Select Event To Date");
                } else if (saEventToDate < saEventFromDate)
                {
                    alert("Event To Date cannot be less than Event From Date");
                } else if (saEventName === "")
                {
                    alert("Please Select Event Name");
                } else if (saFile.value === "")
                {
                    alert("Please Upload File");
                    return false;
                } else
                {
                    document.getElementById("formCreateServiceAgreement").submit();
                }
            }
        </script>
    </head>
    <body class="create">
        <%
            if (uid == "") {
                response.sendRedirect(request.getContextPath() + "/index.jsp");
            } else {
        %>
        <jsp:include page="/header.jsp"></jsp:include>
            <div id="templatemo_body" align="center">
                <form action="<%=request.getContextPath()%>/CreateServiceAgreementServlet" id="formCreateServiceAgreement" name="formCreateServiceAgreement" method="post" enctype="multipart/form-data">
                <div id="alldetails"> <%=com_name%> | <%=div%> | <%=loc_name%> | <%=app%> | <%=finyear%></div>
                <div id="heading">
                    <h3>Create Service Agreement</h3>
                </div>
                <table>
                    <tr>
                        <td>
                            Enter Doctor's PAN:
                        </td>
                        <td>
                            <input type="text" name='saPan' id='saPan' onkeyup="this.value = this.value.toUpperCase();" style="width: 100px;"/>
                        </td>
                        <td align='center'><input type='button' value='GO' id='btnGo' name='btnGo' onclick='getDoctorDetail();'/>&nbsp;
                        </td>
                    </tr>
                </table>
                <table id="docdetails" name="docdetails"></table>
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
    <% }%>
</html>