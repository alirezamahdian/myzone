<%@ page import="java.util.*, MyZone.*, MyZone.elements.*" %>
<%@ page import="java.io.*" %>
<%@ page import="org.apache.commons.fileupload.*, org.apache.commons.fileupload.servlet.ServletFileUpload, org.apache.commons.fileupload.disk.DiskFileItemFactory, org.apache.commons.io.FilenameUtils, java.util.*, java.io.File, java.lang.Exception" %>
<%
Settings mainSettings = new Settings("./MyZone/");
mainSettings.refresh(mainSettings.ALL);
%>
    <h3>&nbsp;&nbsp;&nbsp;<font color=red>Downloading your certificate ... You will be redirected after your certificate has been downloaded ... Please be patient :)</font></h3>
<%
mainSettings.retrieveCert();
String redirectURL;
redirectURL = "profile.jsp";
response.sendRedirect(redirectURL);
return;
%>