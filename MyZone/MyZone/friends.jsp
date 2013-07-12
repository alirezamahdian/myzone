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

if (request.getParameter("op") != null && request.getParameter("op").equals("delete") &&
    request.getParameter("username") != null && !request.getParameter("username").equals("")){
    mainSettings.deleteFriend(request.getParameter("username"));
}else if (request.getParameter("op") != null && request.getParameter("op").equals("sendFriendshipRequest") &&
    request.getParameter("username") != null && !request.getParameter("username").equals("")){
    mainSettings.sendFriendshipRequest(request.getParameter("username"));
}else if (request.getParameter("op") != null && request.getParameter("op").equals("deleteAwaitingFriendshipRequest") &&
          request.getParameter("username") != null && !request.getParameter("username").equals("")){
    mainSettings.deleteAwaitingFriendshipRequest(request.getParameter("username"));
}else if (request.getParameter("op") != null && request.getParameter("op").equals("deletePendingFriendshipRequest") &&
          request.getParameter("username") != null && !request.getParameter("username").equals("")){
    mainSettings.deletePendingFriendshipRequest(request.getParameter("username"));
}else if (request.getParameter("op") != null && request.getParameter("op").equals("acceptRequest") &&
          request.getParameter("username") != null && !request.getParameter("username").equals("")){
    mainSettings.acceptFriendshipRequest(request.getParameter("username"), false);
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
<div class="sidebar-header">
<a>My Zones</a>
</div>
<%
for (int i = 0; i < mainSettings.zones.size(); i++){
    zone z = mainSettings.zones.get(i);
    %>
    <div class="left-sidebar-title-row">
    <div class="sidebar-title-textbox1">
    <a href="?zone=<%=z.getName()%>"><%=z.getName()%></a>
    </div>
    </div>
    <%
}
%>
</div>
<!-- end left-sidebar -->
<!-- start right-sidebar -->
<div id="right-sidebar">
<div class="right-sidebar-title-row">
<div class="sidebar-title-textbox1">
<a>Received Friendship Requests</a>
</div>
</div>
<div class="scrollable-area">
<%
for (int i = 0; i < mainSettings.awaitingFriendships.size(); i++){
    friend f = (friend)mainSettings.awaitingFriendships.get(i);
    out.println(f.generateHTML("receivedRequest"));
}
%>
</div>
<div class="right-sidebar-title-row">
<div class="sidebar-title-textbox1">
<a>Sent Friendship Requests</a>
</div>
<div class="sidebar-title-textbox2">
<a href="friends.jsp?op=addNewFriend">Add New Friend</a>
</div>
</div>
<div class="scrollable-area">
<%
mainSettings.refresh(mainSettings.ALL);
for (int i = 0; i < mainSettings.pendingFriendships.size(); i++){
    friend f = (friend)mainSettings.pendingFriendships.get(i);
    if (!mainSettings.isFriend(f.getUser().getUsername()))
        out.println(f.generateHTML("sentRequest"));
}
%>
</div>
</div>
<!-- end sidebars -->
<!-- start of contents --> 
<div id=wrapper>
<%
if (request.getParameter("op") != null && request.getParameter("op").equals("addNewFriend")){
    if (request.getParameter("username") != null && !request.getParameter("username").equals("")){
            %>
            <div class="content">
            <div class="content-thumbnail">
            <img src="<%=mainSettings.username + "/thumbnails/" + request.getParameter("username") + ".jpg"%>" width="50" height="50">
            </img>
            </div>
            <form method="post" action="friends.jsp">
            <input type="hidden" name="op" value="sendFriendshipRequest">
            <input type="hidden" name="username" value="<%=request.getParameter("username")%>">
            <div class="content-entry">
            <div class="content-header">
            <div class="content-header-textbox1">
            <a><%=request.getParameter("username")%></a>
            </div>
            <div class="content-header-textbox2">
            <input type="submit" name="B1" value="Send Friendship Request" />
            </div>
            </div>
            </div>
            </form>
            </div>
            <%
        }else{
            %>
            <div class="content">
            <form method="post" action="friends.jsp">
            <input type="hidden" name="op" value="sendFriendshipRequest">
            <table border="0" cellpadding="0" cellspacing="0">
            <tr>
            <tr><td>&nbsp;</td></tr>
            <tr>
            <td>Enter the username of the other person:</td>
            <td><input type="text" size="27"
            name="username"/></td>
            </tr>
            <tr>
            <td><input type="submit" name="B1" value="Send Friendship Request" /></td>
            </tr>
            <td>&nbsp;</td>
            </table>
            </form>
            </div>
            <%
        }
}else{
    %>
    <div id="friends_header">
    <%
    if (request.getParameter("zone") != null && !request.getParameter("zone").equals("")){
        int i = 0;
        zone z = new zone();
        for (i = 0; i < mainSettings.zones.size(); i++){
            z = mainSettings.zones.get(i);
            if (z.getName().equals(request.getParameter("zone"))){
                break;
            }
        }
        if (i < mainSettings.zones.size()){
            %>
            <h2>List of Friends in <%=z.getName()%> Zone</h2>
            <%
        }
    }else{
        %>
        <h2>List of Friends in All Zone</h2>
        <%
    }
    %>
    </div>
    <%
    if (request.getParameter("zone") != null && !request.getParameter("zone").equals("")){
        int i = 0;
        zone z = new zone();
        for (i = 0; i < mainSettings.zones.size(); i++){
            z = mainSettings.zones.get(i);
            if (z.getName().equals(request.getParameter("zone"))){
                break;
            }
        }
        if (i < mainSettings.zones.size()){
            List<user> members = z.getMembers();
            for (int j = 0; j < members.size(); j++){
                user u = members.get(j);
                for (int k = 0; k < mainSettings.friends.size(); k++){
                    friend f = mainSettings.friends.get(k);
                    if (u.getUsername().equals(f.getUser().getUsername())){
                        out.println(f.generateHTML("complete"));
                    }
                }
            }
        }
    }else{
        int i = 0;
        zone z = new zone();
        for (i = 0; i < mainSettings.zones.size(); i++){
            z = mainSettings.zones.get(i);
            if (z.getName().equals("All")){
                break;
            }
        }
        if (i < mainSettings.zones.size()){
            List<user> members = z.getMembers();
            for (int j = 0; j < members.size(); j++){
                user u = members.get(j);
                for (int k = 0; k < mainSettings.friends.size(); k++){
                    friend f = mainSettings.friends.get(k);
                    if (u.getUsername().equals(f.getUser().getUsername())){
                        out.println(f.generateHTML("complete"));
                    }
                }
            }
        }
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