<%-- 
    Document   : ddCreate
    Created on : 17 Aug, 2022, 5:44:29 PM
    Author     : ruchita.saroj
--%>

<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
        
        <%
            String uid = "", app = "", subapp = "", com = "", div = "", loc = "", com_name = "", loc_name = "", finyear = "", daybook = "";
            uid = ((session.getAttribute("userid") != null) ? (String) session.getAttribute("userid") : "");
            com = ((session.getAttribute("com") != null) ? (String) session.getAttribute("com") : "");
            div = ((session.getAttribute("div") != null) ? (String) session.getAttribute("div") : "");
            loc = ((session.getAttribute("loc") != null) ? (String) session.getAttribute("loc") : "");
            app = ((session.getAttribute("app") != null) ? (String) session.getAttribute("app") : "");
            subapp = ((session.getAttribute("subapp") != null) ? (String) session.getAttribute("subapp") : "");
            finyear = ((session.getAttribute("finyear") != null) ? (String) session.getAttribute("finyear") : "");
            com_name = ((session.getAttribute("com_name") != null) ? (String) session.getAttribute("com_name") : "");
            loc_name = ((session.getAttribute("loc_name") != null) ? (String) session.getAttribute("loc_name") : "");

            session.setAttribute("userid", uid);
            session.setAttribute("com", com);
            session.setAttribute("div", div);
            session.setAttribute("loc", loc);
            session.setAttribute("app", app);
            session.setAttribute("subapp", subapp);
            session.setAttribute("finyear", finyear);
            session.setAttribute("com_name", com_name);
            session.setAttribute("loc_name", loc_name);
            session.setAttribute("curr_page", "");
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
    <jsp:include page="Search.jsp"></jsp:include>
</html>