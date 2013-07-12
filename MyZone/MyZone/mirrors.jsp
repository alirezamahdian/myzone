<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ page import="MyZone.*"%>
<%@ page import="MyZone.elements.*"%>
<%@ page import="java.util.*, java.io.File"%>
<%  Settings mainSettings = new Settings("./MyZone/");
mainSettings.refresh(mainSettings.ALL); 
if ( mainSettings.username == null || mainSettings.username.equals("") ){
    response.sendRedirect("settings.jsp?show=createNewUser");
}else if (!mainSettings.keyFound){
    response.sendRedirect("settings.jsp?show=createNewUser&promptError=myKeyNotFound");
}else if (!mainSettings.CAKeyFound){
    response.sendRedirect("settings.jsp?settings=basic&select=CAKey&promptError=CAKeyNotFound");
}else if (!mainSettings.certFound){
    response.sendRedirect("settings.jsp?settings=basic&select=MyCert&promptError=myCertNotFound");
}else if (mainSettings.certCorrupted){
    response.sendRedirect("settings.jsp?settings=basic&select=MyCert&promptError=myCertCorrupted");
}

profile userProfile;
boolean hasInfo;
userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
hasInfo = userProfile.loadUserInfo();
if (!hasInfo){
    if (request.getParameter("show") == null){
        response.sendRedirect("profile.jsp?profileOwner=" + mainSettings.username + "&show=info");
    }else if (!request.getParameter("show").equals("info")){
        response.sendRedirect("profile.jsp?profileOwner=" + mainSettings.username + "&show=info");
    }
}else{
    if (userProfile.userInfo.getFirstName().equals("") || userProfile.userInfo.getLastName().equals("")){
        response.sendRedirect("profile.jsp?profileOwner=" + mainSettings.username + "&show=info");
    }
}

boolean capacityError = false;
if (request.getParameter("op") != null && request.getParameter("op").equals("sendMirroringRequest") && 
    request.getParameter("username") != null && !request.getParameter("username").equals("")){
    mainSettings.sendMirrorRequest(request.getParameter("username"));
}else if (request.getParameter("op") != null && request.getParameter("op").equals("acceptedMirroringRequest") &&
          request.getParameter("username") != null && !request.getParameter("username").equals("") &&
          request.getParameter("capacity") != null){
    if (request.getParameter("capacity").equals("")){
        capacityError = true;
    }else{
        if (Long.parseLong(request.getParameter("capacity")) >= 500){
            mainSettings.acceptMirroringRequest(request.getParameter("username"), Long.parseLong(request.getParameter("capacity")) * 1024 * 1024);
        }else{
            capacityError = true;
        }
    }
}else if (request.getParameter("op") != null && request.getParameter("op").equals("deleteSentMirroringRequest") &&
          request.getParameter("username") != null && !request.getParameter("username").equals("")){
    mainSettings.deleteSentMirroringRequest(request.getParameter("username"));
}else if (request.getParameter("op") != null && request.getParameter("op").equals("deleteReceivedMirroringRequest") &&
          request.getParameter("username") != null && !request.getParameter("username").equals("")){
    mainSettings.deleteReceivedMirroringRequest(request.getParameter("username"));
}else if (request.getParameter("op") != null && request.getParameter("op").equals("moveUp") &&
          request.getParameter("username") != null && !request.getParameter("username").equals("")){
    mainSettings.moveUpMirror(request.getParameter("username"));
}else if (request.getParameter("op") != null && request.getParameter("op").equals("moveDown") &&
          request.getParameter("username") != null && !request.getParameter("username").equals("")){
    mainSettings.moveDownMirror(request.getParameter("username"));
}else if (request.getParameter("op") != null && request.getParameter("op").equals("deleteMirror") &&
          request.getParameter("username") != null && !request.getParameter("username").equals("")){
    mainSettings.deleteMirror(request.getParameter("username"));
}else if (request.getParameter("op") != null && request.getParameter("op").equals("deleteOriginal") &&
          request.getParameter("username") != null && !request.getParameter("username").equals("")){
    mainSettings.deleteOriginal(request.getParameter("username"));
}else if (request.getParameter("op") != null && request.getParameter("op").equals("updateOriginal") &&
          request.getParameter("username") != null && !request.getParameter("username").equals("")){
    if (Long.parseLong(request.getParameter("capacity")) > 500){
        mainSettings.updateOriginal(request.getParameter("username"), Long.parseLong(request.getParameter("capacity")) * 1024 * 1024);
    }else{
        capacityError = true;
    }
}
%>
<html> <xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8">
<title>MyZone</title>
<link href="default.css" rel="stylesheet" type="text/css" media="screen" />
<script type="text/javascript" src="media_player/flowplayer-3.2.6.min.js"></script>

</head>
<body>
<!-- start header -->
<div id="header">
<div id="logo">
<h1><a href="index.jsp">MyZone</a></h1>
<p>Uninterrupted Privacy, Uncompromised Security</p>
</div>
<div id="menu">
<ul>
<li><a href="index.jsp">My Feed</a></li>
<li><a href="profile.jsp">My Profile</a></li>
<li><a href="settings.jsp?settings=basic">My Settings</a></li>
</ul>
</div>
</div>
<!-- end header -->
<!-- start page --> 
<div id="page">
<!-- start sidebars -->
<!-- start left-sidebar -->
<div id="left-sidebar">
<div class="left-sidebar-title-row">
<div class="sidebar-title-textbox1">
<a href="?show=mirrors">My Mirrors</a>
</div>
</div>
<div class="left-sidebar-title-row">
<div class="sidebar-title-textbox1">
<a href="?show=mirrored">Mirrored Friends</a>
</div>
</div>
</div>
<!-- end left-sidebar -->
<!-- start right-sidebar -->
<div id="right-sidebar">
<div class="right-sidebar-title-row">
<div class="sidebar-title-textbox1">
<a>Received Mirroring Requests</a>
</div>
</div>
<div class="scrollable-area">
<%
for (int i = 0; i < mainSettings.receivedMirroringRequests.size(); i++){
    mirror m = (mirror)mainSettings.receivedMirroringRequests.get(i);
    out.println(m.generateHTML("receivedRequest"));
}
%>
</div>
<div class="right-sidebar-title-row">
<div class="sidebar-title-textbox1">
<a>Sent Mirroring Requests</a>
</div>
<div class="sidebar-title-textbox2">
<a href="?op=addNewMirror">Add New Mirror</a>
</div>
</div>
<div class="scrollable-area">
<%
for (int i = 0; i < mainSettings.sentMirroringRequests.size(); i++){
    mirror m = (mirror)mainSettings.sentMirroringRequests.get(i);
    out.println(m.generateHTML("sentRequest"));
}
%>
</div>
</div>
<!-- end sidebars -->
<!-- start of contents --> 
<div id=wrapper>
<%
if (request.getParameter("op") != null && request.getParameter("op").equals("addNewMirror")){
    %>
    <form method="post" action="mirrors.jsp">
    <input type="hidden" name="op" value="sendMirroringRequest">
    <table border="0" cellpadding="0" cellspacing="0">
    <tr>
    <tr><td>&nbsp;</td></tr>
    <tr>
    <td>Select friend :</td>
    <td><select name="username">
    <%
    for (int i = 0; i < mainSettings.friends.size(); i++){
        friend f = (friend)mainSettings.friends.get(i);
        %>
        <option value="<%=f.getUser().getUsername()%>"><%=f.getUser().getFirstName() + " " + f.getUser().getLastName()%></option>
        <%
    }
    %>
    </select></td>
    </tr>
    <tr>
    <td><input type="submit" name="B1" value="Send Mirroring Request" /></td>
    </tr>
    <td>&nbsp;</td>
    </table>
    </form>
    <%
}else if (request.getParameter("op") != null && (request.getParameter("op").equals("acceptMirroringRequest") || (request.getParameter("op").equals("acceptedMirroringRequest") && capacityError)) && request.getParameter("username") != null && !request.getParameter("username").equals("")){
    %>
    <form method="post" action="mirrors.jsp">
    <input type="hidden" name="op" value="acceptedMirroringRequest">
    <input type="hidden" name="username" value="<%=request.getParameter("username")%>">
    <input type="hidden" name="show" value="mirrored">
    <table border="0" cellpadding="0" cellspacing="0">
    <tr><td>&nbsp;</td></tr>
    <tr>
    <td>Friend's Username : </td><td><%=request.getParameter("username")%></td>
    </tr>
    <tr>
    <%
    String name = "";
    for (int i = 0; i < mainSettings.friends.size(); i++){
        friend f = (friend)mainSettings.friends.get(i);
        if (f.getUser().getUsername().equals(request.getParameter("username"))){
            name = f.getUser().getFirstName() + " " + f.getUser().getLastName();
            break;
        }
    }
    %>
    <td>Friend's Name : </td><td><%=name%></td>
    </tr>
    <tr>
    <%
    if (capacityError){
    %>
        <td><font
        color=red>Allocated capacity should be larger than 500 MB.</font></td>
    <%
    }
    %>
    <td>Allocated Disk Capacity: </td><td><input type="text" size="20"
    name="capacity"/> in MB</td>
    </tr>
    <tr>
    <td><input type="submit" name="B1" value="Accept Mirroring Request" /></td>
    </tr>
    <td>&nbsp;</td>
    </table>
    </form>
    <%
}else if (request.getParameter("show") != null && request.getParameter("show").equals("mirrored")){
    %>
    <div id="friends_header">
    <h2>List of Friends that are Mirrored by Me</h2>
    </div>
    <%
    for (int i = 0; i < mainSettings.originals.size(); i++){
        mirror m = mainSettings.originals.get(i);
        out.println(m.generateHTML("original"));
    }
}else{
    %>
    <div id="friends_header">
        <h2>List of My Mirrors</h2>
    </div>
    <%
    for (int i = 0; i < mainSettings.mirrors.size(); i++){
        mirror m = mainSettings.mirrors.get(i);
        out.println(m.generateHTML("mirror"));
    }
}
%>
</div>
<!-- end of contents -->
</div>
<!-- end page -->
<div id="footer">
<p class="copyright">&copy;&nbsp;&nbsp;2012 All Rights Reserved &nbsp;&bull;&nbsp; Created by <a href="mailto:alireza.mahdian@colorado.edu">Alireza Mahdian</a>.</p>
</div>
</body>
</html>