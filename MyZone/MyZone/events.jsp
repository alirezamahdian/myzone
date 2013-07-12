<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ page import="MyZone.*"%>
<%@ page import="MyZone.elements.*"%>
<%@ page import="java.util.*"%>
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
userProfile.loadEvents();
if (request.getParameter("eventAdded") != null && request.getParameter("eventAdded").equals("true") &&
    request.getParameter("invitedFriends") != null && !request.getParameter("invitedFriends").equals("") &&
    request.getParameter("title") != null && !request.getParameter("title").equals("") &&
    request.getParameter("location") != null && !request.getParameter("location").equals("") &&
    request.getParameter("description") != null && 
    request.getParameter("eventStartDate") != null && !request.getParameter("eventStartDate").equals("") &&
    request.getParameter("startTimeHour") != null && !request.getParameter("startTimeHour").equals("") &&
    request.getParameter("startTimeMinute") != null && !request.getParameter("startTimeMinute").equals("") &&
    request.getParameter("eventEndDate") != null && !request.getParameter("eventEndDate").equals("") &&
    request.getParameter("endTimeHour") != null && !request.getParameter("endTimeHour").equals("") &&
    request.getParameter("endTimeMinute") != null && !request.getParameter("endTimeMinute").equals("")){
    event e = new event();
    e.setTitle(request.getParameter("title"));
    e.setLocation(request.getParameter("location"));
    e.setDescription(request.getParameter("description"));
    e.setDecision("Attending");
    user u = new user();
    if (hasInfo){
        u = new user(userProfile.profileOwner, userProfile.userInfo.getFirstName(), userProfile.userInfo.getLastName(), 0); 
    }else{
        u = new user(userProfile.profileOwner, null, null, 0);
    }
    e.setCreator(u);
    String[] parts = request.getParameter("eventStartDate").split("-");
    date startDate = new date(Integer.parseInt(parts[2]), Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(request.getParameter("startTimeHour")), Integer.parseInt(request.getParameter("startTimeMinute")));
    e.setStartDate(startDate);
    parts = request.getParameter("eventEndDate").split("-");
    date endDate = new date(Integer.parseInt(parts[2]), Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(request.getParameter("endTimeHour")), Integer.parseInt(request.getParameter("endTimeMinute")));
    e.setEndDate(endDate);
    String[] usernames = request.getParameter("invitedFriends").split("\\|");
    List<user> invitees = new ArrayList();
    for (int k = 0; k < usernames.length; k++){
        for (int j = 0; j < mainSettings.friends.size(); j++){
            friend f = (friend)mainSettings.friends.get(j);
            if (f.getUser().getUsername().equals(usernames[k])){
                invitees.add(f.getUser());
                break;
            }
        }
    }
    e.setInvitees(invitees);
    e.setPendingNotification(invitees);
    e.addAccepted(u);
    e.setDecision("Attending");
    e.setId(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis());
    userProfile.addEvent(e, false);
    response.sendRedirect("events.jsp?show=allEvents");
}else if (request.getParameter("eventUpdated") != null && request.getParameter("eventUpdated").equals("true") &&
    request.getParameter("id")!= null && !request.getParameter("id").equals("") &&
    request.getParameter("invitedFriends") != null && !request.getParameter("invitedFriends").equals("") &&
    request.getParameter("title") != null && !request.getParameter("title").equals("") &&
    request.getParameter("location") != null && !request.getParameter("location").equals("") &&
    request.getParameter("description") != null && 
    request.getParameter("eventStartDate") != null && !request.getParameter("eventStartDate").equals("") &&
    request.getParameter("startTimeHour") != null && !request.getParameter("startTimeHour").equals("") &&
    request.getParameter("startTimeMinute") != null && !request.getParameter("startTimeMinute").equals("") &&
    request.getParameter("eventEndDate") != null && !request.getParameter("eventEndDate").equals("") &&
    request.getParameter("endTimeHour") != null && !request.getParameter("endTimeHour").equals("") &&
    request.getParameter("endTimeMinute") != null && !request.getParameter("endTimeMinute").equals("")){
    event e = new event();
    e.setId(Long.parseLong(request.getParameter("id")));
    e.setTitle(request.getParameter("title"));
    e.setLocation(request.getParameter("location"));
    e.setDescription(request.getParameter("description"));
    e.setDecision("Attending");
    user u = new user();
    if (hasInfo){
        u = new user(userProfile.profileOwner, userProfile.userInfo.getFirstName(), userProfile.userInfo.getLastName(), 0); 
    }else{
        u = new user(userProfile.profileOwner, null, null, 0);
    }
    e.setCreator(u);
    String[] parts = request.getParameter("eventStartDate").split("-");
    date startDate = new date(Integer.parseInt(parts[2]), Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(request.getParameter("startTimeHour")), Integer.parseInt(request.getParameter("startTimeMinute")));
    e.setStartDate(startDate);
    parts = request.getParameter("eventEndDate").split("-");
    date endDate = new date(Integer.parseInt(parts[2]), Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(request.getParameter("endTimeHour")), Integer.parseInt(request.getParameter("endTimeMinute")));
    e.setEndDate(endDate);
    String[] usernames = request.getParameter("invitedFriends").split("\\|");
    List<user> invitees = new ArrayList();
    for (int k = 0; k < usernames.length; k++){
        for (int j = 0; j < mainSettings.friends.size(); j++){
            friend f = (friend)mainSettings.friends.get(j);
            if (f.getUser().getUsername().equals(usernames[k])){
                invitees.add(f.getUser());
                break;
            }
        }
    }
    e.setInvitees(invitees);
    e.setPendingNotification(invitees);
    userProfile.updateEvent(e);
    response.sendRedirect("events.jsp?show=allEvents");
}else if (request.getParameter("deleteEvent") != null && request.getParameter("deleteEvent").equals("true") &&
          request.getParameter("id") != null && !request.getParameter("id").equals("")){
    userProfile.removeEvent(Long.parseLong(request.getParameter("id")));
    response.sendRedirect("events.jsp?show=allEvents");
}
%>
<html> <xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8">
<title>MyZone</title>
<link href="default.css" rel="stylesheet" type="text/css" media="screen" />
<script type="text/javascript" src="calendarDateInput.js"></script>
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
<a href="events.jsp?createEvent=true">Create a New Event</a>
</div>
</div>
<%
if (request.getParameter("show") != null && request.getParameter("show").equals("event") &&
    request.getParameter("id") != null && !request.getParameter("id").equals("")){
%>
    <div class="sidebar-header">
    <a>Invited Friends</a>
    </div>
    <div class="scrollable-area">
<%
    for (int i = 0; i < userProfile.events.size(); i++){
        event e = userProfile.events.get(i);
        if (e.getId() == Long.parseLong(request.getParameter("id"))){
            for (int j = 0; j < e.getInvitees().size(); j++){
                user u = (user)e.getInvitees().get(j);
                friend f = new friend(0, null, u, null, null, 0, null, 0, 0, 0);
                out.println(f.generateHTML("abstract"));
            }
            break;
        }
    }
%>
    </div>
    <div class="sidebar-header">
    <a>Attending</a>
    </div>
    <div class="scrollable-area">
<%
    for (int i = 0; i < userProfile.events.size(); i++){
        event e = userProfile.events.get(i);
        if (e.getId() == Long.parseLong(request.getParameter("id"))){
            for (int j = 0; j < e.getAccepted().size(); j++){
                user u = (user)e.getAccepted().get(j);
                friend f = new friend(0, null, u, null, null, 0, null, 0, 0, 0);
                out.println(f.generateHTML("abstract"));
            }
            break;
        }
    }
%>
    </div>
    <div class="sidebar-header">
    <a>Not Attending</a>
    </div>
    <div class="scrollable-area">
<%
    for (int i = 0; i < userProfile.events.size(); i++){
        event e = userProfile.events.get(i);
        if (e.getId() == Long.parseLong(request.getParameter("id"))){
            for (int j = 0; j < e.getDeclined().size(); j++){
                user u = (user)e.getDeclined().get(j);
                friend f = new friend(0, null, u, null, null, 0, null, 0, 0, 0);
                out.println(f.generateHTML("abstract"));
            }
            break;
        }
    }
%>
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
<!-- start of contents --> 
<div id="wrapper">
<%
if (request.getParameter("show") != null && request.getParameter("show").equals("event") &&
          request.getParameter("id") != null && !request.getParameter("id").equals("")){
    if (request.getParameter("decision") != null){
        userProfile.respondToEvent(Long.parseLong(request.getParameter("id")), request.getParameter("decision"));
        response.sendRedirect("events.jsp?show=event" + "&id=" + Long.parseLong(request.getParameter("id")));
    }
    for (int i = 0; i < userProfile.events.size(); i++){
        event e = (event)userProfile.events.get(i);
        if (e.getId() == Long.parseLong(request.getParameter("id"))){
            if (e.getCreator().getUsername().equals(mainSettings.username)){
            %>
                <div id="create-event">
                <form action="events.jsp" method="post" onsubmit="placeInHidden('|', 'selection', 'invitedFriendsID');">
                <input type="hidden" name="eventUpdated" value="true" />
                <input type="hidden" name="id" value="<%=e.getId()%>" />
                <div class="create-event-row"><h2>Title:</h2><h3>(required)</h3><input type="text" name="title" value="<%=e.getTitle()%>"/></div>
                <div class="create-event-row"><h2>Location:</h2><h3>(required)</h3><textarea name="location"><%=e.getLocation()%></textarea></div>
                <div class="create-event-date-row"><h2>Start Date:</h2><h3>(required)</h3>
                <script>DateInput('eventStartDate', true, 'MM-DD-YYYY', '<%=e.getStartDate().getMonthStr() + "-" + e.getStartDate().getDay() + "-" + e.getStartDate().getYear()%>')</script>
                <select name="startTimeHour">
                <% for(int j = 1; j <= 23; j++){
                    if (e.getStartDate().getHour("24") == j){
                        out.println("<option selected=\"yes\" value=\"" + j + "\">" + j + "</option>");
                    }else{
                        out.println("<option value=\"" + j + "\">" + j + "</option>");
                    }
                }
                %>
                </select>
            <h2>:</h2>
                <select name="startTimeMinute">
                <% for(int j = 0; j <= 59; j++){
                    if (j < 10){
                        if (e.getStartDate().getMinute() == j){
                            out.println("<option selected=\"yes\" value=\"" +  j + "\">" + "0" + j + "</option>");
                        }else{
                            out.println("<option value=\"" + j + "\">" + "0" + j + "</option>");
                        }
                    }else{
                        if (e.getStartDate().getMinute() == j){
                            out.println("<option selected=\"yes\" value=\"" + j + "\">" + j + "</option>");
                        }else{
                            out.println("<option value=\"" + j + "\">" + j + "</option>");
                        }
                    }
                }
                %>
                </select>
                </div>
                <div class="create-event-date-row"><h2>End Date:</h2><h3>(required)</h3>
                <script>DateInput('eventEndDate', true, 'MM-DD-YYYY', '<%=e.getEndDate().getMonthStr() + "-" + e.getEndDate().getDay() + "-" + e.getEndDate().getYear()%>')</script>
                <select name="endTimeHour">
                <% for(int j = 1; j <= 23; j++){
                    if (e.getEndDate().getHour("24") == j){
                        out.println("<option selected=\"yes\" value=\"" + j + "\">" + j + "</option>");
                    }else{
                        out.println("<option value=\"" + j + "\">" + j + "</option>");
                    }
                }
                %>
                </select>
            <h2>:</h2>
                <select name="endTimeMinute">
                <% for(int j = 0; j <= 59; j++){
                    if (j < 10){
                        if (e.getEndDate().getMinute() == j){
                            out.println("<option selected=\"yes\" value=\"" + j + "\">" + "0" + j + "</option>");
                        }else{
                            out.println("<option value=\"" + j + "\">" + "0" + j + "</option>");
                        }
                    }else{
                        if (e.getEndDate().getMinute() == j){
                            out.println("<option selected=\"yes\" value=\"" + j + "\">" + j + "</option>");
                        }else{
                            out.println("<option value=\"" + j + "\">" + j + "</option>");
                        }
                    }
                }
                %>
                </select>
                </div>
                <div class="create-event-row"><h2>Details:</h2>
                <textarea name="description"><%=e.getDescription()%></textarea></div>
                <table class="select">
                <tr>
                <tr><td class="left"><p>Friends</p></td><td class="button"></td><td class="right"><p>Invited Friends</p></td></tr>
                <tr>
                <td class="left">
                <input type="hidden" name="invitedFriends" id="invitedFriendsID" />
                <select id="friends" multiple="multiple" size="20">
                <%
                for (int j = 0; j < mainSettings.friends.size(); j++) {
                    friend f = (friend)mainSettings.friends.get(j);
                    boolean show = true;
                    for (int k = 0; k < e.getInvitees().size(); k++){
                        user u = (user)e.getInvitees().get(k);
                        if(u.getUsername().equals(f.getUser().getUsername())){
                            show = false;
                        }
                    }
                    if (show == true){
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
                onclick="moveOptions(document.getElementById('friends'), document.getElementById('selection'));" />
                <br />
                <input type="button" value="&lt;--"
                onclick="moveOptions(document.getElementById('selection'), document.getElementById('friends'));" />
                </form>
                </td>
                <td class="right">
                <select id="selection" multiple="multiple" size="20">
                <%
                for (int j = 0; j < e.getInvitees().size(); j++){
                    user u = (user)e.getInvitees().get(j);
                %>
                    <option value="<%=u.getUsername()%>"><%=u.getFirstName() + " " + u.getLastName()%></option>
                <%
                }
                %>
                </select> 
                </td>
                <tr><td class="left"><input type="submit" value="Update" /></td><td class="button"></td><td class="right"><a href="?deleteEvent=true&id=<%=e.getId()%>">delete this event</a>
                </td></tr>
                </table>
                </form>
                </div>
            <%
            }
            else{
                out.println(e.generateHTML("complete"));
            }
            break;
        }
    }
}else if ((request.getParameter("createEvent") != null && request.getParameter("createEvent").equals("true")) || request.getParameter("eventAdded") != null){
%>
    <div id="create-event">
    <form action="events.jsp" method="post" onsubmit="placeInHidden('|', 'selection', 'invitedFriendsID');">
    <input type="hidden" name="eventAdded" value="true" />
    <div class="create-event-row"><h2>Title:</h2><h3>(required)</h3><input type="text" name="title" /></div>
    <div class="create-event-row"><h2>Location:</h2><h3>(required)</h3><textarea name="location"></textarea></div>
    <div class="create-event-date-row"><h2>Start Date:</h2><h3>(required)</h3>
    <script>DateInput('eventStartDate', true, 'MM-DD-YYYY')</script>
    <select name="startTimeHour">
    <% for(int i = 1; i <= 23; i++){
        out.println("<option value=\"" + i + "\">" + i + "</option>");
    }
    %>
    </select>
    <h2>:</h2>
    <select name="startTimeMinute">
    <% for(int i = 0; i <= 59; i++){
        if (i < 10){
            out.println("<option value=\"" + i + "\">" + "0" + i + "</option>");
        }else{
            out.println("<option value=\"" + i + "\">" + i + "</option>");
        }
    }
    %>
    </select>
    </div>
    <div class="create-event-date-row"><h2>End Date:</h2><h3>(required)</h3>
    <script>DateInput('eventEndDate', true, 'MM-DD-YYYY')</script>
    <select name="endTimeHour">
    <% for(int i = 1; i <= 23; i++){
        out.println("<option value=\"" + i + "\">" + i + "</option>");
    }
    %>
    </select>
    <h2>:</h2>
    <select name="endTimeMinute">
    <% for(int i = 0; i <= 59; i++){
        if (i < 10){
            out.println("<option value=\"" + i + "\">" + "0" + i + "</option>");
        }else{
            out.println("<option value=\"" + i + "\">" + i + "</option>");
        }
    }
    %>
    </select>
    </div>
    <div class="create-event-row"><h2>Details:</h2>
    <textarea name="description"></textarea></div>
    <table class="select">
    <tr>
    <tr><td class="left"><p>Friends</p></td><td class="button"></td><td class="right"><p>Invited Friends</p></td></tr>
    <tr>
    <td class="left">
    <input type="hidden" name="invitedFriends" id="invitedFriendsID" />
    <select id="friends" multiple="multiple" size="20">
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
    onclick="moveOptions(document.getElementById('friends'), document.getElementById('selection'));" />
    <br />
    <input type="button" value="&lt;--"
    onclick="moveOptions(document.getElementById('selection'), document.getElementById('friends'));" />
    </form>
    </td>
    <td class="right">
    <select id="selection" multiple="multiple" size="20">
    </select> 
    </td>
    <tr><td class="left"><input type="submit" value="Create" /></td><td class="button"></td><td class="right">
    </td></tr>
    </table>
    </form>
    </div>
<%
}else{
%>
    <div id="event-header">
    <h2>Events</h2>
    </div>
<%
    for (int i = 0; i < userProfile.events.size(); i++){
        event e = (event)userProfile.events.get(i);
        out.println(e.generateHTML("abstract"));
    }
}
%>
</div>
<!-- end page -->
<div id="footer">
<p class="copyright">&copy;&nbsp;&nbsp;2012 All Rights Reserved &nbsp;&bull;&nbsp; Created by <a href="mailto:alireza.mahdian@colorado.edu">Alireza Mahdian</a>.</p>
</div>
</body>
</html>