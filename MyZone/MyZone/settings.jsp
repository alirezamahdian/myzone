<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ page import="MyZone.*"%>
<%@ page import="MyZone.elements.*"%>
<%@ page import="java.util.*"%>
<%@ page import="java.io.File"%>
<%  
Settings mainSettings = new Settings("./MyZone/");
mainSettings.refresh(mainSettings.ALL); 
boolean maxNumberOfChangesError = false;
boolean CAServerNameError = false;
boolean rendezvousServerAddressError = false;
boolean rendezvousServerPortError = false;
boolean STUNServerAddressError = false;
boolean STUNServerPortError = false;
boolean MyZonePortError = false;
boolean cacheSizeError = false;
boolean devPriorityError = false;
boolean syncPeriodError = false;

if (request.getParameter("op") != null && request.getParameter("op").equals("createNewUser")){
    if (request.getParameter("firstName") != null && (request.getParameter("firstName").equals("") || !request.getParameter("firstName").matches("[a-zA-Z]+")))
        response.sendRedirect("settings.jsp?show=createNewUser&error=firstname");
    else if (request.getParameter("lastName") != null && (request.getParameter("lastName").equals("") || !request.getParameter("lastName").matches("[a-zA-Z]+")))
        response.sendRedirect("settings.jsp?show=createNewUser&error=lastname");
    else if (request.getParameter("gender") != null && request.getParameter("gender").equals(""))
        response.sendRedirect("settings.jsp?show=createNewUser&error=gender");
    else if (request.getParameter("username") != null && !request.getParameter("username").equals("") && request.getParameter("username").contains("@") && request.getParameter("username").contains(".")){
        mainSettings.createNewUser(request.getParameter("username"), request.getParameter("firstName"), request.getParameter("lastName"), request.getParameter("gender"), request.getParameter("searchable"));
        if (!mainSettings.CAKeyFound){
            response.sendRedirect("settings.jsp?settings=basic&select=CAKey");
        }
    }else{
        response.sendRedirect("settings.jsp?show=createNewUser&error=username");
    }
}else if (request.getParameter("op") != null && request.getParameter("op").equals("saveZone")){
    if (request.getParameter("newZone") != null && request.getParameter("newZone").equals("added")){
        if (request.getParameter("zoneMembers") != null 
            && request.getParameter("zoneName") != null && !request.getParameter("zoneName").equals("")
            && request.getParameter("refreshInterval") != null && Long.parseLong(request.getParameter("refreshInterval")) >= 15 ){
            int i = 0;
            for (i = 0; i < mainSettings.zones.size(); i++){
                zone z = (zone)mainSettings.zones.get(i);
                if (z.getName().equals(request.getParameter("zoneName"))){
                    break;
                }
            }
            if (i == mainSettings.zones.size()){
                zone z = new zone();
                z.setName(request.getParameter("zoneName"));
                z.setRefreshInterval(Long.parseLong(request.getParameter("refreshInterval")) * 60);
                List<user> members = new ArrayList();
                if (request.getParameter("zoneMembers").equals("")){
                    z.setMembers(members);
                }else{
                    String[] usernames = request.getParameter("zoneMembers").split("\\|");
                    for (int k = 0; k < usernames.length; k++){
                        for (int j = 0; j < mainSettings.friends.size(); j++){
                            friend f = (friend)mainSettings.friends.get(j);
                            if (f.getUser().getUsername().equals(usernames[k])){
                                members.add(f.getUser());
                                break;
                            }
                        }
                    }
                    z.setMembers(members);
                }
                z.setId(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis());
                mainSettings.zones.add(z);
                if (!(new File(mainSettings.prefix + mainSettings.username + "/zones/" + z.getName()).exists())){
                    (new File(mainSettings.prefix + mainSettings.username + "/zones/" + z.getName())).mkdirs();
                }
                if (!(new File(mainSettings.prefix + mainSettings.username + "/zones/" + z.getName() + "/wall").exists())){
                    boolean success = (new File(mainSettings.prefix + mainSettings.username + "/zones/" + z.getName() + "/wall")).mkdirs();
                }
                mainSettings.save(mainSettings.ZONES);
            }
        }
    }else if (request.getParameter("zoneId") != null 
              && (request.getParameter("zoneMembers") != null || request.getParameter("zoneName").equals("All"))
              && request.getParameter("zoneName") != null && !request.getParameter("zoneName").equals("")
              && request.getParameter("refreshInterval") != null && Long.parseLong(request.getParameter("refreshInterval")) >= 15 ){
        zone z = new zone();
        int i = 0;
        for (i = 0; i < mainSettings.zones.size(); i++){
            z = (zone)mainSettings.zones.get(i);
            if (z.getId() == Long.parseLong(request.getParameter("zoneId"))){
                break;
            }
        }
        z.setName(request.getParameter("zoneName"));
        z.setRefreshInterval(Long.parseLong(request.getParameter("refreshInterval")) * 60);
        List<user> members = new ArrayList();
        if (request.getParameter("zoneMembers") != null){
            if (request.getParameter("zoneMembers").equals("")){
                z.setMembers(members);
            }else{
                String[] usernames = request.getParameter("zoneMembers").split("\\|");
                for (int k = 0; k < usernames.length; k++){
                    for (int j = 0; j < mainSettings.friends.size(); j++){
                        friend f = (friend)mainSettings.friends.get(j);
                        if (f.getUser().getUsername().equals(usernames[k])){
                            members.add(f.getUser());
                            break;
                        }
                    }
                }
                z.setMembers(members);
            }
        }
        mainSettings.zones.add(i, z);
        mainSettings.zones.remove(i + 1);
        mainSettings.save(mainSettings.ZONES);
    }
}
mainSettings.refresh(mainSettings.BASIC_INFO);
if ( mainSettings.username == null || mainSettings.username.equals("") ){
    if (request.getParameter("show") == null){
        response.sendRedirect("settings.jsp?show=createNewUser");
    }
}else if (!mainSettings.keyFound){
    if (request.getParameter("promptError") == null){
        response.sendRedirect("settings.jsp?show=createNewUser&promptError=myKeyNotFound");
    }
}else if (!mainSettings.CAKeyFound){
    if (request.getParameter("promptError") == null)
        response.sendRedirect("settings.jsp?settings=basic&select=CAKey&promptError=CAKeyNotFound");
}else if (!mainSettings.certFound){
    if (request.getParameter("promptError") == null)
        response.sendRedirect("settings.jsp?settings=basic&select=MyCert&promptError=myCertNotFound");
}else if (mainSettings.certCorrupted){
    if (request.getParameter("promptError") == null)
        response.sendRedirect("settings.jsp?settings=basic&select=MyCert&promptError=myCertCorrupted");
}

%>
<html>
<xmlns="http://www.w3.org/1999/xhtml">
<head>
<script>
var NS4 = (navigator.appName == "Netscape" && parseInt(navigator.appVersion) < 5);

function addOption(theSel, theText, theValue)
{
    var newOpt = new Option(theText, theValue);
    var selLength = theSel.length;
    theSel.options[selLength] = newOpt;
}

function deleteOption(theSel, theIndex)
{
    var selLength = theSel.length;
    if(selLength>0)
    {
        theSel.options[theIndex] = null;
    }
}

function moveOptions(theSelFrom, theSelTo)
{
    var selLength = theSelFrom.length;
    var selectedText = new Array();
    var selectedValues = new Array();
    var selectedCount = 0;
    
    var i;   
    // Find the selected Options in reverse order
    // and delete them from the 'from' Select.
    for(i=selLength-1; i>=0; i--)
    {
        if(theSelFrom.options[i].selected)
        {
            selectedText[selectedCount] = theSelFrom.options[i].text;
            selectedValues[selectedCount] = theSelFrom.options[i].value;
            deleteOption(theSelFrom, i);
            selectedCount++;
        }
    }
    
    // Add the selected text/values in reverse order.
    // This will add the Options to the 'to' Select
    // in the same order as they were in the 'from' Select.
    for(i=selectedCount-1; i>=0; i--)
    {
        addOption(theSelTo, selectedText[i], selectedValues[i]);
    }      
    if(NS4) history.go(0);
}    
function placeInHidden(delim, selStr, hidStr)
{
    var selObj = document.getElementById(selStr);
    var hideObj = document.getElementById(hidStr);
    hideObj.value = '';
    for (var i=0; i<selObj.options.length; i++) {
        hideObj.value = hideObj.value ==
        '' ? selObj.options[i].value : hideObj.value + delim + selObj.options[i].value;
    }
}
</script>
<meta http-equiv="content-type" content="text/html; charset=utf-8">
<title>MyZone</title>
<link href="default.css" rel="stylesheet" type="text/css"
media="screen" />
</head>
<body>
<!-- start header -->
<div id="header">
<div id="logo">
<h1>
<a href="index.jsp">MyZone</a>
</h1>
<p>Uninterrupted Privacy, Uncompromised Security</p>
</div>
<div id="menu">
<ul>
<li><a href="index.jsp">My Feed</a></li>
<li><a href="profile.jsp">My Profile</a></li>
<li style="border-top: 5px solid #169418; padding: 0 0 0 0"><a
href="settings.jsp?settings=basic">My Settings</a></li>
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
<a href="?settings=basic">Basic Settings</a>
</div>
</div>
<div class="left-sidebar-title-row">
<div class="sidebar-title-textbox1">
<a href="?settings=zones">My Zones</a>
</div>
</div>
<div class="sidebar-header">
<a href="friends.jsp">My Friends</a>
</div>
<div class="scrollable-area">
<%
if (mainSettings.username != null){
    for (int i = 0; i < mainSettings.friends.size(); i++){
        friend f = (friend)mainSettings.friends.get(i);
        out.println(f.generateHTML("abstract"));
    }
}
%>
</div>
<div class="sidebar-header">
<a href="mirrors.jsp">My Mirrors</a>
</div>
<div class="scrollable-area">
<%
if (mainSettings.username != null){
    for (int i = 0; i < mainSettings.mirrors.size(); i++){
        mirror m = (mirror)mainSettings.mirrors.get(i);
        for (int j = 0; j < mainSettings.friends.size(); j++){
            friend f = (friend)mainSettings.friends.get(j);
            if (m.getUsername().equals(f.getUser().getUsername())){
                out.println(f.generateHTML("abstract"));
            }
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
if (mainSettings.username != null){
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
if (mainSettings.username != null){
    profile msgProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
    msgProfile.loadInbox();
    for (int i = 0; i < msgProfile.receivedMsgs.size(); i++){
        message m = (message)msgProfile.receivedMsgs.get(i);
        if (mainSettings.isFriend(m.getSender().getUsername())){
            out.println(m.generateHTML("sidebarAbstract"));
        }
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
if (mainSettings.username != null){
    profile eventProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
    eventProfile.loadEvents();
    for (int i = 0; i < eventProfile.events.size(); i++){
        event e = (event)eventProfile.events.get(i);
        out.println(e.generateHTML("rightSideBar"));
    }
}
%>
</div>
</div>
<!-- end right-sidebar -->
<!-- end sidebars -->
<!-- start of contents -->
<div id="wrapper">
<div class="content">
<% 
if(request.getParameter("show") != null && request.getParameter("show").equals("createNewUser")){
    %>
    <form method="post" action="settings.jsp">
    <input type="hidden" name="op" value="createNewUser">
    <table border="0" cellpadding="0" cellspacing="0">
    <tr><td>&nbsp;</td></tr>
    
    <%
    if(request.getParameter("promptError") != null && request.getParameter("promptError").equals("myKeyNotFound")){
        %>
        <tr><td><h2><font
        color=red>Your Keys Were Not Found.</font></h2></td><td><h2><font
        color=red> Please copy them in the keys directory or create a new user.</font></h2></td>
        </tr>
        <%
    }
    %>
    <tr>
        <td><font color=red>*** All fields are mandatory ***</font></td>
    </tr>
    <tr>
    <% 
    if (request.getParameter("error") != null && request.getParameter("error").equals("username")){
        %>
        <td><font
        color=red>(Please enter a valid email address)</font></td>
        <%
    }
    %>
    <td>Enter Your Desired Username:</td>
    <td><input type="text" size="20"
    name="username"/></td>
    <td><font size=1 color=red>Your username has to be a valid email address</font></td>
    </tr>
    <tr>
    <%
    if (request.getParameter("error") != null && request.getParameter("error").equals("firstname")){
        %>
        <td><font
        color=red>(Please enter your first name)</font></td>
        <%
    }
    %>
    <td>Enter Your First Name:</td>
    <td><input type="text" size="20"
    name="firstName"/></td>
    <td><font size=1 color=red>Only alphabets are acceptable</font></td>
    </tr>
    <tr>
    <%
    if (request.getParameter("error") != null && request.getParameter("error").equals("lastname")){
        %>
        <td><font
        color=red>(Please enter your last name)</font></td>
        <%
    }
    %>
    <td>Enter Your Last Name:</td>
    <td><input type="text" size="20"
    name="lastName"/></td>
    <td><font size=1 color=red>Only alphabets are acceptable</font></td>
    </tr>
    <tr>
    <%
    if (request.getParameter("error") != null && request.getParameter("error").equals("gender")){
        %>
        <td><font
        color=red>(Please select your gender name)</font></td>
        <%
    }
    %>
    <td>Select your gender:</td>
    <td><select name="gender"><option value="n/a">Select Sex</option><option value="Female">Female</option><option value="Male">Male</option></select></td>
    </tr>
    <tr>
    <td>Make my information publicly available:</p></td><td><input type="radio" name="searchable" value="yes" checked="true"/><font size="2">Yes</font></td><td><input type="radio" name="searchable" value="no"/><font size="2">No</font></td>
    </tr>
    <tr>
    <td><input type="submit" name="B1" value="Create New User" /></td>
    </tr>
    <td>&nbsp;</td>
    </table>
    </form>
    <%
}else if(request.getParameter("settings") != null){
    if (request.getParameter("settings").equals("basic")){
        if (request.getParameter("select") != null){
            if (request.getParameter("select").equals("CAKey")){
                if (request.getParameter("promptError") != null && request.getParameter("promptError").equals("CAKeyNotFound")){
                    %>
                    <tr>
                    <td><h3>&nbsp;&nbsp;&nbsp;<font color=red>Certificate Authority's Public Key Was not Found: </font></h3></td>
                    <td></td>
                    </tr>
                    <%
                    }
                    %>
                    <form action="file_upload.jsp" method="post" enctype="multipart/form-data">
                    <input type="hidden" name="filetype" value="CAKey">
                    <input type="hidden" name="calledFrom" value="settings">
                    <table border="0" cellpadding="0" cellspacing="0">
                    <tr>
                    <td><h3>Browse for Certificate Authority's Public Key: </h3></td>
                    <td><input type="file" name="myFile"/></td>
                    </tr>
                    <tr>
                    <td><input type="submit" value="Upload"/></td>
                    </tr>
                    </table>
                    </form>
                    <%
                }
                if (request.getParameter("select").equals("MyCert")){
                    if (request.getParameter("promptError") != null && request.getParameter("promptError").equals("myCertNotFound")){
                        %>
                        <tr>
                        <td><h3><font size=2 color=red>&nbsp;&nbsp;An email has been sent to you with the activation link.</font></h3></td>
                        <td><h3><font size=2 color=red>&nbsp;&nbsp;If you can not find it in your mail box please check the spam folder.</font></h3></td>
                        <td><h3><font size=2 color=red>&nbsp;&nbsp;Please verify your email address by following the activation link and then &nbsp;&nbsp;click on the download my certificate.</font></h3></td>
                        <td><h3><font size=2 color=red>&nbsp;&nbsp;Please make sure that you are connected to the internet before proceeding.</font></h3></td>
                        <td></td>
                        </tr>
                        <%
                    }
                    if (request.getParameter("promptError") != null && request.getParameter("promptError").equals("myCertCorrupted")){
                        %>
                        <tr>
                        <td><h3>&nbsp;&nbsp;&nbsp;<font color=red>Your Certificate Is Corrupted: </font></h3></td>
                        <td></td>
                        </tr>
                        <%
                    }
                    %>
                    <form action="getCert.jsp" method="post">
                    <table border="0" cellpadding="0" cellspacing="0">
                        <tr>
                        <td><input type="submit" value="Download My Certificate"/></td>
                        </tr>
                        </table>
                    <!--<form action="file_upload.jsp" method="post" enctype="multipart/form-data">
                    <input type="hidden" name="filetype" value="MyCert">
                    <input type="hidden" name="calledFrom" value="settings">
                    <table border="0" cellpadding="0" cellspacing="0">
                    <tr>
                    <td><h3>Browse for Your Certificate: </h3></td>
                        <td><input type="file" name="myFile"/></td>
                        </tr>
                        <tr>
                        <td><input type="submit" value="Upload"/></td>
                        </tr>
                        </table>
                        </form>//-->
                        <%
                        }
            }else{
                if(request.getParameter("maxNumberOfChanges") != null){
                    if(request.getParameter("maxNumberOfChanges").equals("") || Integer.parseInt(request.getParameter("maxNumberOfChanges")) < 10 || Integer.parseInt(request.getParameter("maxNumberOfChanges")) > 100){
                        maxNumberOfChangesError = true;
                    }else{
                        maxNumberOfChangesError = false;
                        mainSettings.maxNumberOfChanges = Integer.parseInt(request.getParameter("maxNumberOfChanges"));
                        mainSettings.save(mainSettings.BASIC_INFO);
                    }
                }else{
                    maxNumberOfChangesError = false;        
                }
                if(request.getParameter("CAServerName") != null){
                    if(request.getParameter("CAServerName").equals("")){
                        CAServerNameError = true;
                    }else{
                        CAServerNameError = false;
                        mainSettings.CAServerName = request.getParameter("CAServerName");
                        mainSettings.save(mainSettings.BASIC_INFO);
                    }
                }else{
                    CAServerNameError = false;        
                }
                if(request.getParameter("rendezvousServerAddress") != null){
                    if(request.getParameter("rendezvousServerAddress").equals("")){
                        rendezvousServerAddressError = true;
                    }else{
                        rendezvousServerAddressError = false;
                        mainSettings.rendezvousServerAddress = request.getParameter("rendezvousServerAddress");
                        mainSettings.save(mainSettings.BASIC_INFO);
                    }
                }else{
                    rendezvousServerAddressError = false;        
                }
                if(request.getParameter("rendezvousServerPort") != null){
                    if(request.getParameter("rendezvousServerPort").equals("") || Integer.parseInt(request.getParameter("rendezvousServerPort")) < 0 || Integer.parseInt(request.getParameter("rendezvousServerPort")) > 65535){
                        rendezvousServerPortError = true;
                    }else{
                        rendezvousServerPortError = false;
                        mainSettings.rendezvousServerPort = Integer.parseInt(request.getParameter("rendezvousServerPort"));
                        mainSettings.save(mainSettings.BASIC_INFO);
                    }
                }else{
                    rendezvousServerPortError =false;        
                }
                if(request.getParameter("STUNServerAddress") != null){
                    if(request.getParameter("STUNServerAddress").equals("")){
                        STUNServerAddressError = true;
                    }else{
                        STUNServerAddressError = false;
                        mainSettings.STUNServerAddress = request.getParameter("STUNServerAddress");
                        mainSettings.save(mainSettings.BASIC_INFO);
                    }
                }else{
                    STUNServerAddressError = false;        
                }
                if(request.getParameter("STUNServerPort") != null){
                    if(request.getParameter("STUNServerPort").equals("") || Integer.parseInt(request.getParameter("STUNServerPort")) < 0 || Integer.parseInt(request.getParameter("STUNServerPort")) > 65535){
                        STUNServerPortError = true;
                    }else{
                        STUNServerPortError = false;
                        mainSettings.STUNServerPort = Integer.parseInt(request.getParameter("STUNServerPort"));
                        mainSettings.save(mainSettings.BASIC_INFO);
                    }
                }else{
                    STUNServerPortError = false;        
                }
                if(request.getParameter("MyZonePort") != null){
                    if(request.getParameter("MyZonePort").equals("") || Integer.parseInt(request.getParameter("MyZonePort")) < 1024 || Integer.parseInt(request.getParameter("MyZonePort")) > 65535){
                        MyZonePortError = true;
                    }else{
                        MyZonePortError = false;
                        mainSettings.MyZonePort = Integer.parseInt(request.getParameter("MyZonePort"));
                        mainSettings.save(mainSettings.BASIC_INFO);
                    }
                }else{
                    MyZonePortError = false;        
                }
                if(request.getParameter("cacheSize") != null){
                    if(request.getParameter("cacheSize").equals("") || Integer.parseInt(request.getParameter("cacheSize")) < 100){
                        cacheSizeError = true;
                    }else{
                        cacheSizeError = false;
                        mainSettings.cacheSize = Integer.parseInt(request.getParameter("cacheSize"));
                        mainSettings.save(mainSettings.BASIC_INFO);
                    }
                }else{
                    cacheSizeError = false;        
                }
                if(request.getParameter("devPriority") != null){
                    if(request.getParameter("devPriority").equals("") || Integer.parseInt(request.getParameter("devPriority")) < 0){
                        devPriorityError = true;
                    }else{
                        devPriorityError = false;
                        mainSettings.devPriority = Integer.parseInt(request.getParameter("devPriority"));
                        mainSettings.save(mainSettings.BASIC_INFO);
                    }
                }else{
                    devPriorityError = false;        
                }
                if(request.getParameter("syncPeriod") != null){
                    if(request.getParameter("syncPeriod").equals("") || Integer.parseInt(request.getParameter("syncPeriod")) < 15){
                        syncPeriodError = true;
                    }else{
                        syncPeriodError = false;
                        mainSettings.syncPeriod = Integer.parseInt(request.getParameter("syncPeriod")) * 60;
                        mainSettings.save(mainSettings.BASIC_INFO);
                    }
                }else{
                    syncPeriodError = false;        
                }
                if(request.getParameter("log") != null){
                    if(request.getParameter("log").equals("") || Integer.parseInt(request.getParameter("log")) < 0){
                    }else{
                        if (Integer.parseInt(request.getParameter("log")) == 0){
                            mainSettings.logUsage = false;
                        }else{
                            mainSettings.logUsage = true;
                        }
                        mainSettings.save(mainSettings.BASIC_INFO);
                    }
                }
                %>
                <form method="get" action="settings.jsp">
                <input type="hidden" name="settings" value="basic">
                <table border="0" cellpadding="0" cellspacing="0">
                <tr>
                <tr><td>&nbsp;</td></tr>
                <tr>
                <td>Your username:</td>
                <td><%=mainSettings.username%></td>
                </tr>
                <td>&nbsp;</td>
                <tr>
                <td>Certificate Authority's Name: </td>
                <td><%=mainSettings.CAServerName%></td><td></td>
                </tr>
                <td>&nbsp;</td>
                <tr>
                <td><a href="?settings=basic&select=CAKey">Upload Certificate Authority's Public Key</a></td><td></td><td></td>
                </tr>
                <td>&nbsp;</td>
                <tr>
                <td>Found your certificate and your matching key pair </td><td>&nbsp;&nbsp;<img src="images/check.png" width="15">
                </img></td>
                </tr>
                <td>&nbsp;</td>
                <tr>
                <td><a href="?settings=basic&select=MyCert">Upload Your Certificate</a></td><td></td> 
                </tr>
                <td>&nbsp;</td>
                <tr>
                <% 
                if (maxNumberOfChangesError){
                    %><td><font
                    color=red>(Please enter a number between 10 and
                               100)</font></td>
                    <%
                }
                %>
                <td>Number of entries per wall session*:</td>
                <td><input type="text" size="20"
                name="maxNumberOfChanges"
                value='<%=mainSettings.maxNumberOfChanges%>' /><h3>*Should be at a number between 10 and 100 </h3></td>
                </tr>
                <td>&nbsp;</td>
                <tr>
                <% if (rendezvousServerAddressError){
                    %><td><font
                    color=red>(Please enter a valid rendezvous server
                               address)</font></td>
                    <%
                }%>
                <td>Rendezvous Server Address:</td>
                <td><input type="text" size="20"
                name="rendezvousServerAddress"
                value='<%=mainSettings.rendezvousServerAddress%>' /></td>
                </tr>
                <td>&nbsp;</td>
                <tr>
                <% if (rendezvousServerPortError){
                    %><td><font
                    color=red>(Please enter a valid rendezvous server
                               port)</font></td>
                    <%
                }%>
                <td>Rendezvous Server Port:</td>
                <td><input type="text" size="20"
                name="rendezvousServerPort"
                value='<%=mainSettings.rendezvousServerPort%>' /></td>
                </tr>
                <td>&nbsp;</td>
                <tr>
                <% if (STUNServerAddressError){
                    %><td><font
                    color=red>(Please enter a valid STUN server address)</font></td>
                    <%
                }%>
                <td>STUN Server Address:</td>
                <td><input type="text" size="20" name="STUNServerAddress"
                value='<%=mainSettings.STUNServerAddress%>' /></td>
                </tr>
                <td>&nbsp;</td>
                <tr>
                <% if (STUNServerPortError){
                    %><td><font
                    color=red>(Please enter a valid STUN server port)</font></td>
                    <%
                }%>
                <td>STUN Server Port:</td>
                <td><input type="text" size="20" name="STUNServerPort"
                value='<%=mainSettings.STUNServerPort%>' /></td>
                </tr>
                <td>&nbsp;</td>
                <tr>
                <% if (MyZonePortError){
                    %><td><font
                    color=red>(Please enter a valid port number. Value should be more than 1024.)</font></td>
                    <%
                }%>
                <td>MyZone Server Port*:</td>
                <td><input type="text" size="20" name="MyZonePort"
                value='<%=mainSettings.MyZonePort%>' />&nbsp; <h3>*Should be different than STUN server port </h3></td>
                </tr>
                <td>&nbsp;</td>
                <tr>
                <% if (cacheSizeError){
                    %><td><font
                    color=red>(Cache size should not be smaller than 100 MB)</font></td>
                    <%
                }%>
                <td>MyZone Cache Size*:</td>
                <td><input type="text" size="20" name="cacheSize"
                value='<%=mainSettings.cacheSize%>' />&nbsp;MB <h3>*Should be at least 100 MB </h3></td>
                </tr>
                <td>&nbsp;</td>
                <tr>
                <% if (devPriorityError){
                    %><td><font
                    color=red>(Please select a priority for the current device)</font></td>
                    <%
                }%>
                <td>Priority of this device:</td>
                <td>
                <div class="radio">
                <p><input type="radio" name="devPriority" value="0" <% if (mainSettings.devPriority == 0) out.println("checked"); %>> Primary</p>
                <p><input type="radio" name="devPriority" value="1" <% if (mainSettings.devPriority == 1) out.println("checked"); %>> Secondary</p>
                <p><input type="radio" name="devPriority" value="2" <% if (mainSettings.devPriority == 2) out.println("checked"); %>>Tertiary</p>
                </div>
                </td>
                </tr>
                <td>&nbsp;</td>
                <tr>
                <% if (syncPeriodError){
                    %><td><font
                    color=red>(Sync period can not be less than 15 minutes)</font></td>
                    <%
                }%>
                <td>Sync with mirrors every:</td>
                <td><input type="text" size="20" name="syncPeriod"
                value='<%=(mainSettings.syncPeriod/60)%>' />&nbsp;Minutes</td>
                </tr>
                <td>&nbsp;</td>
                <tr>
                <td>Log usage information:</td>
                <td>
                <div class="radio">
                <p><input type="radio" name="log" value="1" <% if (mainSettings.logUsage) out.println("checked"); %>> Yes</p>
                <p><input type="radio" name="log" value="0" <% if (!mainSettings.logUsage) out.println("checked"); %>> No</p>
                </div>
                </td>
                </tr>
                <td>&nbsp;</td>
                <tr>
                <td><input type="submit" name="B1" value="Submit" /></td>
                </tr>
                <td>&nbsp;</td>
                </table>
                </form>
                <%
            }    
        }else if(request.getParameter("settings").equals("zones")){
            if(request.getParameter("delete") != null && 
               !request.getParameter("delete").equals("All") &&
               !request.getParameter("delete").equals("")){
                mainSettings.deleteZone(Long.parseLong(request.getParameter("zoneId")), request.getParameter("delete"));
            }
            %>  <h1>Zone Settings:</h1><a href="settings.jsp?settings=zones&newZone=true" class="new-zone">add a new zone</a>
            <%
            if (request.getParameter("newZone") != null && request.getParameter("newZone").equals("true")){
                %>
                <form method="get" action="settings.jsp"
                onsubmit="placeInHidden('|', 'selection<%=mainSettings.zones.size()%>', 'zoneMembersID<%=mainSettings.zones.size()%>');">
                <input type="hidden" name="op" value="saveZone" />
                <input type="hidden" name="settings" value="zones" />
                <input type="hidden" name="newZone" value="added" />
                <table class="zone">
                <tr>
                <td class="left">Zone Name:</td><td class="button"></td>
                <td class="right">
                <input type="text" size="20" name="zoneName" value="" />
                </td>
                </tr>
                <tr>
                <td class="left">Refresh interval in minutes:</td><td class="button"></td>
                <td class="right"><input type="text" size="7" name="refreshInterval" value="" /><h3>Refresh interval should be more than 15 minutes</h3></td>
                </tr>
                <tr><td class="left"><p>Friends</p></td><td class="button"></td><td class="right"><p>Members</p></td></tr>
                <tr>
                <td class="left">
                <input type="hidden" name="zoneMembers" id="zoneMembersID<%=mainSettings.zones.size()%>" />
                <select id="friends<%=mainSettings.zones.size()%>" multiple="multiple" size="20">
                <%
                for (int j = 0; j < mainSettings.friends.size(); j++) {
                    friend f = (friend)mainSettings.friends.get(j);
                    %>
                    <option value="<%=f.getUser().getUsername()%>"><%=f.getUser().getFirstName() + " " + f.getUser().getLastName()%></option>
                    <%
                }
                %>
                </select>
                </td>
                <td class="button">
                <form>
                <input type="button" value="--&gt;"
                onclick="moveOptions(document.getElementById('friends<%=mainSettings.zones.size()%>'), document.getElementById('selection<%=mainSettings.zones.size()%>'));" />
                <br />
                <input type="button" value="&lt;--"
                onclick="moveOptions(document.getElementById('selection<%=mainSettings.zones.size()%>'), document.getElementById('friends<%=mainSettings.zones.size()%>'));" />
                </form>
                </td>
                <td class="right">
                <select id="selection<%=mainSettings.zones.size()%>" multiple="multiple" size="20">
                </select> 
                </td>
                <tr><td class="left"><input type="submit" value="Update" /></td><td class="button"></td><td class="right">
                </td></tr>
                </table>
                </form>
                <%
            }
            for (int i = 0; i < mainSettings.zones.size(); i++){
                zone z = (zone)mainSettings.zones.get(i);
                List<user> members = z.getMembers();
                %>
                <form method="get" action="settings.jsp"
                onsubmit="placeInHidden('|', 'selection<%=i%>', 'zoneMembersID<%=i%>');">
                <input type="hidden" name="op" value="saveZone" />
                <input type="hidden" name="zoneId" value="<%=z.getId()%>" />
                <input type="hidden" name="settings" value="zones" />
                <table class="zone">
                <tr>
                <td class="left">Zone Name:</td><td class="button"></td>
                <td class="right">
                <%
                if (z.getName().equals("All")){
                    %>
                    <input type="hidden" name="zoneName" value="<%=z.getName()%>" />
                    <%
                    out.println("&nbsp;" + z.getName());
                }else{
                    %>
                    <input type="text" size="20" name="zoneName" value="<%=z.getName()%>" />
                    <%
                }
                %>
                </td>
                </tr>
                <tr>
                <td class="left">Refresh interval in minutes:</td><td class="button"></td>
                <td class="right"><input type="text" size="7" name="refreshInterval" value="<%=(z.getRefreshInterval()/60)%>" /><h3>Refresh interval should be more than 15 minutes</h3></td>
                </tr>
                <% 
                if (!z.getName().equals("All")){
                    %>
                    <tr><td class="left"><p>Friends</p></td><td class="button"></td><td class="right"><p>Members</p></td></tr>
                    <tr>
                    <td class="left">
                    <input type="hidden" name="zoneMembers" id="zoneMembersID<%=i%>" />
                    <select id="friends<%=i%>" multiple="multiple" size="20">
                    <%
                    for (int j = 0; j < mainSettings.friends.size(); j++) {
                        friend f = (friend)mainSettings.friends.get(j);
                        boolean print = true;
                        for (int k = 0; k < members.size(); k++){
                            user u = (user)members.get(k);
                            if (u.getUsername().equals(f.getUser().getUsername())){
                                print = false;
                            }
                        }
                        if (print){
                            %>
                            <option value="<%=f.getUser().getUsername()%>"><%=f.getUser().getFirstName() + " " + f.getUser().getLastName()%></option>
                            <%
                        }
                    }
                    %>
                    </select>
                    </td>
                    <td class="button">
                    <form>
                    <input type="button" value="--&gt;"
                    onclick="moveOptions(document.getElementById('friends<%=i%>'), document.getElementById('selection<%=i%>'));" />
                    <br />
                    <input type="button" value="&lt;--"
                    onclick="moveOptions(document.getElementById('selection<%=i%>'), document.getElementById('friends<%=i%>'));" />
                    </form>
                    </td>
                    <td class="right">
                    <select id="selection<%=i%>" multiple="multiple" size="20">
                    <% 
                    for (int j = 0; j < members.size(); j++){
                        user u = (user)members.get(j);
                        %>
                        <option value="<%=u.getUsername()%>"><%=u.getFirstName() + " " + u.getLastName()%></option>
                        <%
                    }
                    %>
                    </select> 
                    </td>
                    <%
                }
                %>
                <tr><td class="left"><input type="submit" value="Update" /></td><td class="button"></td><td class="right">
                <%
                if (!z.getName().equals("All")){
                    %>
                    <a href="settings.jsp?settings=zones&delete=<%=z.getName()%>&zoneId=<%=z.getId()%>">delete zone</a>
                    <%
                }
                %>
                </td></tr>
                </table>
                </form>
                <%
            }        
        }
    }else{
        %>
        <form method="get" action="settings.jsp">
        <input type="hidden" name="settings" value="basic">
        <table border="0" cellpadding="0" cellspacing="0">
        <tr><td>&nbsp;</td></tr>
        <tr>
        <%
        if (mainSettings.username == null){
            %>
            <td><a href="?settings=basic&select=MyCert">Upload Your Certificate</a></td><td></td> 
            <%
        }else{
            %>
            <td>Your username:</td>
            <td><%=mainSettings.username%></td>
            <%
        }
        %>
        </tr>
        <td>&nbsp;</td>
        <tr>
        <td>Your username:</td>
        <td><%=mainSettings.username%></td>
        </tr>
        <td>&nbsp;</td>
        <tr>
        <td>Certificate Authority's Name: </td>
        <td><%=mainSettings.CAServerName%></td><td></td>
        </tr>
        <td>&nbsp;</td>
        <tr>
        <td><a href="?settings=basic&select=CAKey">Upload Certificate Authority's Public Key</a></td><td></td><td></td>
        </tr>
        <td>&nbsp;</td>
        <%
        if (!mainSettings.keyFound){
            %>
            <tr>
            <td><font color=red><h2>Your Keys Were Not Found. Please copy them in the keys directory</font></h2></td> 
            </tr>
            <%
        }else if (mainSettings.certCorrupted){
            %>
            <tr>
            <td><font color=red>Your Certificate Is Corrupted.</font></td> 
            </tr>
            <%
        }else {
            %>
            <tr>
            <td>Found your certificate and your matching key pair</td><td>&nbsp;&nbsp;<img src="images/check.png" width="15">
            </img></td>
            </tr>
            <%
        }
        %>
        <tr>
        <td><a href="?settings=basic&select=MyCert">Upload Your Certificate</a></td><td></td> 
        </tr>
        <td>&nbsp;</td>
        <tr>
        <td>Number of entries per wall session*:</td>
        <td><input type="text" size="20"
        name="maxNumberOfChanges"
        value='<%=mainSettings.maxNumberOfChanges%>' /><h3>*Should be at a number between 10 and 100</h3></td>
        </tr>
        <td>&nbsp;</td>
        <tr>
        <td>Rendezvous Server Address:</td>
        <td><input type="text" size="20"
        name="rendezvousServerAddress"
        value='<%=mainSettings.rendezvousServerAddress%>' /></td>
        </tr>
        <td>&nbsp;</td>
        <tr>
        <td>Rendezvous Server Port:</td>
        <td><input type="text" size="20"
        name="rendezvousServerPort"
        value='<%=mainSettings.rendezvousServerPort%>' /></td>
        </tr>
        <td>&nbsp;</td>
        <tr>
        <td>STUN Server Address:</td>
        <td><input type="text" size="20" name="STUNServerAddress"
        value='<%=mainSettings.STUNServerAddress%>' /></td>
        </tr>
        <td>&nbsp;</td>
        <tr>
        <td>STUN Server Port:</td>
        <td><input type="text" size="20" name="STUNServerPort"
        value='<%=mainSettings.STUNServerPort%>' /></td>
        </tr>
        <td>&nbsp;</td>
        <tr>
        <td>MyZone Server Port*:</td>
        <td><input type="text" size="20" name="MyZonePort"
        value='<%=mainSettings.MyZonePort%>' /><h3>*Should be different than STUN server port and more than 1024</h3></td>
        </tr>
        <td>&nbsp;</td>
        <tr>
        <td>MyZone Cache Size*:</td>
        <td><input type="text" size="20" name="cacheSize"
        value='<%=mainSettings.cacheSize%>' />&nbsp;MB<h3>*Should be at least 100 MB</h3></td>
        </tr>
        <td>&nbsp;</td>
        <tr>
        <td>Priority of this device:</td>
        <td>
        <div class="radio">
        <p><input type="radio" name="devPriority" value="0" <% if (mainSettings.devPriority == 0) out.println("checked"); %>> Primary</p>
        <p><input type="radio" name="devPriority" value="1" <% if (mainSettings.devPriority == 1) out.println("checked"); %>> Secondary</p>
        <p><input type="radio" name="devPriority" value="2" <% if (mainSettings.devPriority == 2) out.println("checked"); %>>Tertiary</p>
        </div>
        </td>
        </tr>
        <td>&nbsp;</td>
        <tr>
        <td>Sync with mirrors every:</td>
        <td><input type="text" size="20" name="syncPeriod"
        value='<%=(mainSettings.syncPeriod/60)%>' />&nbsp;Minutes (Value needs to be larger than 15)</td>
        </tr>
        <td>&nbsp;</td>
        <tr>
        <td>Log usage information:</td>
        <td>
        <div class="radio">
        <p><input type="radio" name="log" value="1" <% if (mainSettings.logUsage) out.println("checked"); %>> Yes</p>
        <p><input type="radio" name="log" value="0" <% if (!mainSettings.logUsage) out.println("checked"); %>> No</p>
        </div>
        </td>
        </tr>
        <td>&nbsp;</td>
        <tr>
        <td><input type="submit" name="B1" value="Submit" /></td>
        </tr>
        <td>&nbsp;</td>
        </table>
        </form>
        
        <%  
    }
    %>
    </div>
    </div>
    <!-- end of contents -->
    </div>
    <!-- end page -->
    <div id="footer">
    <p class="copyright">
    &copy;&nbsp;&nbsp;2012 All Rights Reserved &nbsp;&bull;&nbsp; Created
    by <a href="mailto:alireza.mahdian@colorado.edu">Alireza Mahdian</a>.
    </p>
    </div>
    </body>
    </html>