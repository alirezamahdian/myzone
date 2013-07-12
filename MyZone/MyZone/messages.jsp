<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ page import="java.util.*, java.io.*" %>
<%@ page import="MyZone.*" %>
<%@ page import="MyZone.elements.*" %>
<%  
Settings mainSettings = new Settings("./MyZone/");
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
if (request.getParameter("op") != null && request.getParameter("op").equals("send") &&
    request.getParameter("subject") != null && 
    request.getParameter("body") != null && !request.getParameter("body").equals("") &&
    request.getParameter("receiver") != null && !request.getParameter("receiver").equals("")){
    friend f = new friend();
    for (int i = 0; i < mainSettings.friends.size(); i++){
        f = (friend)mainSettings.friends.get(i);
        if (f.getUser().getUsername().equals(request.getParameter("receiver")))
            break;
    }
    user receiver = f.getUser();
    user sender = new user();
    if (hasInfo){
        sender = new user(userProfile.userInfo.getUsername(), 
                          userProfile.userInfo.getFirstName(),
                          userProfile.userInfo.getLastName(), 0);
    }
    message m = new message(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "false", "sent", sender, receiver, request.getParameter("subject"), request.getParameter("body"));
    userProfile.sendMessage(m);
}

%>
<html> <xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8">
<title>MyZone</title>
<link href="default.css" rel="stylesheet" type="text/css" media="screen" />
</head>
<body>
<!-- start header -->
<div id="header">
<div id="logo">
<h1><a href="index.jsp">MyZone</a></h1>
<p>Uninterrupted Privacy, Uncompromised Security</p>
</div>
<div id="menu">
<ul><li><a href="index.jsp">My Feed</a></li>
<li><a href="profile.jsp">My Profile</a></li>
<li><a href="settings.jsp?settings=basic">My Settings</a></li>
</ul>
</div>
</div>
<!-- end header -->
<!-- start page --> 
<div id="page">
<!-- start sidebars -->
<!-- start left sidebar -->
<div id="left-sidebar">
<div class="left-sidebar-title-row">
<div class="sidebar-title-textbox1">
<a href="?show=received">Received</a>
</div>
</div>
<div class="left-sidebar-title-row">
<div class="sidebar-title-textbox1">
<a href="?show=sent">Sent</a>
</div>
</div>
<div class="left-sidebar-title-row">
<div class="sidebar-title-textbox1">
<a href="?show=compose">Compose</a>
</div>
</div>
<div class="sidebar-header">
<a href="friends.jsp">My Friends</a>
</div>
<div class="scrollable-area">
<%
for (int i = 0; i < mainSettings.friends.size(); i++){
    friend f = (friend)mainSettings.friends.get(i);
    out.println(f.generateHTML("abstract"));
}
%>
</div>
<div class="sidebar-header">
<a href="mirrors.jsp">My Mirrors</a>
</div>
<div class="scrollable-area">
<%
for (int i = 0; i < mainSettings.mirrors.size(); i++){
    mirror m = (mirror)mainSettings.mirrors.get(i);
    for (int j = 0; j < mainSettings.friends.size(); j++){
        friend f = (friend)mainSettings.friends.get(j);
        if (m.getUsername().equals(f.getUser().getUsername())){
            out.println(f.generateHTML("abstract"));
        }
    }
}
%>
</div>
</div>
<!-- end left sidebar -->
<!-- start right-sidebar -->
<div id="right-sidebar">
<div class="right-sidebar-title-row">
<div class="sidebar-title-textbox1">
<a>Notifications</a>
</div>
</div>
<div class="scrollable-area">
<%
profile notificationProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
notificationProfile.loadNotifications();
for (int i = 0; i < notificationProfile.notifications.size(); i++){
    if (i > 100){
        break;
    }
    notification n = (notification)notificationProfile.notifications.get(i);
    if (mainSettings.isFriend(n.getPostedBy().getUsername())){
        out.println(n.generateHTML("sidebarAbstract"));
    }
}
%>
</div>
<div class="right-sidebar-title-row">
<div class="sidebar-title-textbox1">
<a href="messages.jsp">Messages</a>
</div>
</div>
<div class="scrollable-area">
<%
profile msgProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
msgProfile.loadInbox();
for (int i = 0; i < msgProfile.receivedMsgs.size(); i++){
    message m = (message)msgProfile.receivedMsgs.get(i);
    if (mainSettings.isFriend(m.getSender().getUsername())){
        out.println(m.generateHTML("sidebarAbstract"));
    }
}
%>
</div>
<div class="right-sidebar-title-row">
<div class="sidebar-title-textbox1">
<a href="events.jsp">Events</a>
</div>
</div>
<div class="scrollable-area">
<%
profile eventProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
eventProfile.loadEvents();
for (int i = 0; i < eventProfile.events.size(); i++){
    event e = (event)eventProfile.events.get(i);
    out.println(e.generateHTML("rightSideBar"));
}
%>
</div>
</div>
<!-- end right-sidebar -->
<!-- end sidebars -->
<div id="message_wrapper">
<div class="message">
<% 
if (request.getParameter("show") != null && request.getParameter("show").equals("message") &&
    request.getParameter("id") != null && !request.getParameter("id").equals("")){
    userProfile.loadInbox();
    userProfile.loadOutbox();
    int i = 0;
    message m = new message();
    for (i = 0; i < userProfile.receivedMsgs.size(); i++){
        m = (message)userProfile.receivedMsgs.get(i);
        if (m.getId() == Long.parseLong(request.getParameter("id"))){
            break;
        }
    }
    if (i < userProfile.receivedMsgs.size()){
        if (mainSettings.isFriend(m.getSender().getUsername()))
            out.println(m.generateHTML("complete"));
    }else{
        for (i = 0; i < userProfile.sentMsgs.size(); i++){
            m = (message)userProfile.sentMsgs.get(i);
            if (m.getId() == Long.parseLong(request.getParameter("id"))){
                break;
            }
        }
        if (i < userProfile.sentMsgs.size()){
            if (mainSettings.isFriend(m.getReceiver().getUsername()))
                out.println(m.generateHTML("complete"));
        }
    }
}
if (request.getParameter("show") != null){
if (request.getParameter("show").equals("received")){
%>
    <div class="message-header">
    <h2>Received Messages</h2>
    </div>
<%
    userProfile.loadInbox();
    for (int i = 0; i < userProfile.receivedMsgs.size(); i++){
        message m = (message)userProfile.receivedMsgs.get(i);
        if (mainSettings.isFriend(m.getSender().getUsername()))
            out.println(m.generateHTML("abstract"));
    }
}else if (request.getParameter("show").equals("sent")){
%>
    <div class="message-header">
    <h2>Sent Messages</h2>
    </div>
<%
    userProfile.loadOutbox();
    for (int i = 0; i < userProfile.sentMsgs.size(); i++){
        message m = (message)userProfile.sentMsgs.get(i);
        if (mainSettings.isFriend(m.getReceiver().getUsername()))
            out.println(m.generateHTML("abstract"));
    }
}else if (request.getParameter("show").equals("compose")){
%>
    <div class="message-header">
    <h2>Compose A Message</h2>
    </div>
    <div id="compose-message">
    <form name="composeMessage" action="messages.jsp" method="post">
    <input type="hidden" name="op" value="send"/>
    <h2>Subject: </h2><input type="text" name="subject" value=""/>
    </br>
    <h2>Send To:</h2>
    <select name="receiver">
<%
    for (int i = 0; i < mainSettings.friends.size(); i++){
        friend f = (friend)mainSettings.friends.get(i);
        if (new File("./MyZone/" + mainSettings.username + "/friends/" + f.getUser().getUsername() + "/cert/" + f.getUser().getUsername() + ".cert").exists()){
%>
        <option value="<%=f.getUser().getUsername()%>"><%=f.getUser().getFirstName() + " " + f.getUser().getLastName()%></option>
<%
        }
    }
%>
    </select>
    <textarea name="body"></textarea>
    <input type="submit" value="Send" width="170"/>
    </form>
    </div>
<%
}
}else{
    %>      
    <div class="message-header">
    <h2>Received Messages</h2>
    </div>
    <%
    userProfile.loadInbox();
    for (int i = 0; i < userProfile.receivedMsgs.size(); i++){
        message m = (message)userProfile.receivedMsgs.get(i);
        if (mainSettings.isFriend(m.getSender().getUsername()))
            out.println(m.generateHTML("abstract"));
    }
}
%>
</div>
</div>
<!-- end of contents -->
<div id="footer">
<p class="copyright">&copy;&nbsp;&nbsp;2012 All Rights Reserved &nbsp;&bull;&nbsp; Created by <a href="mailto:alireza.mahdian@colorado.edu">Alireza Mahdian</a>.</p>
</div>
</div>
<!-- end page -->

</body>
</html>