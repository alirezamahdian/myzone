<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html> <xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8">
<title>MyZone</title>
<link href="default.css" rel="stylesheet" type="text/css" media="screen" />
<script type="text/javascript" src="media_player/jwplayer.js"></script>
<script src="jquery.min.js"></script>
<script>
var auto_refresh = setInterval(
                               function()
                               {
                               $('#reload').fadeOut('slow').load('index.jsp').fadeIn("slow");
                               }, 600000);
</script>
</head>
<body>
<div id="reload">
<%@ page import="java.util.*" %>
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
if (!hasInfo && userProfile.profileOwner.equals(mainSettings.username)){
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
List<feed> feeds = new ArrayList();
feeds.clear();
for (int i = 0; i < mainSettings.zones.size(); i++){
    zone z = (zone)mainSettings.zones.get(i);
    feed f = new feed("./MyZone/", z.getName(), z.getMembers());
    feeds.add(f);
}

if (request.getParameter("op") != null && request.getParameter("op").equals("add")){
    if (request.getParameter("el")!= null && request.getParameter("el").equals("comment")){
        if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
            if (request.getParameter("grandParentID") != null && !request.getParameter("grandParentID").equals("")){
                if (request.getParameter("parentID") != null && !request.getParameter("parentID").equals("")){
                    if (request.getParameter("type") != null && !request.getParameter("type").equals("")){
                        if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                            if (request.getParameter("body") != null && !request.getParameter("body").equals("")){
                                userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                                hasInfo = userProfile.loadUserInfo();
                                user u;
                                if (hasInfo){
                                    u = new user(userProfile.profileOwner, userProfile.userInfo.getFirstName(), userProfile.userInfo.getLastName(), 0); 
                                }else{
                                    u = new user(userProfile.profileOwner, null, null, 0);
                                }
                                userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                                comment c = new comment(request.getParameter("type"),
                                                        Long.parseLong(request.getParameter("grandParentID")), 
                                                        Long.parseLong(request.getParameter("parentID")), 
                                                        Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), 
                                                        request.getParameter("profileOwner"), 
                                                        request.getParameter("shareWith"), 
                                                        u,
                                                        request.getParameter("body"));
                                userProfile.addComment(c, false);
                                hasInfo = userProfile.loadUserInfo();
                            }
                        }
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("like")){
        if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
            if (request.getParameter("grandParentID") != null && !request.getParameter("grandParentID").equals("")){
                if (request.getParameter("parentID") != null && !request.getParameter("parentID").equals("")){
                    if (request.getParameter("type") != null && !request.getParameter("type").equals("")){
                        if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                            userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                            hasInfo = userProfile.loadUserInfo();
                            user u;
                            if (hasInfo){
                                u = new user(userProfile.profileOwner, userProfile.userInfo.getFirstName(), userProfile.userInfo.getLastName(), 0); 
                            }else{
                                u = new user(userProfile.profileOwner, null, null, 0);
                            }
                            userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                            like l = new like(request.getParameter("type"),
                                              Long.parseLong(request.getParameter("grandParentID")), 
                                              Long.parseLong(request.getParameter("parentID")), 
                                              Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), 
                                              request.getParameter("profileOwner"), 
                                              request.getParameter("shareWith"), 
                                              u);
                            userProfile.addlike(l, false);
                            hasInfo = userProfile.loadUserInfo();
                        }
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("dislike")){
        if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
            if (request.getParameter("grandParentID") != null && !request.getParameter("grandParentID").equals("")){
                if (request.getParameter("parentID") != null && !request.getParameter("parentID").equals("")){
                    if (request.getParameter("type") != null && !request.getParameter("type").equals("")){
                        if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                            userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                            hasInfo = userProfile.loadUserInfo();
                            user u;
                            if (hasInfo){
                                u = new user(userProfile.profileOwner, userProfile.userInfo.getFirstName(), userProfile.userInfo.getLastName(), 0); 
                            }else{
                                u = new user(userProfile.profileOwner, null, null, 0);
                            }
                            userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                            dislike dl = new dislike(request.getParameter("type"),
                                                     Long.parseLong(request.getParameter("grandParentID")), 
                                                     Long.parseLong(request.getParameter("parentID")), 
                                                     Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), 
                                                     request.getParameter("profileOwner"), 
                                                     request.getParameter("shareWith"), 
                                                     u);
                            userProfile.addDislike(dl, false);
                            hasInfo = userProfile.loadUserInfo();
                        }
                    }
                }
            }
        }
    }
    
}else if (request.getParameter("op") != null && request.getParameter("op").equals("remove")){
    if (request.getParameter("el")!= null && request.getParameter("el").equals("wallPost")){
        if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
            if (request.getParameter("id") != null && !request.getParameter("id").equals("")){
                if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                    if (request.getParameter("postedBy") != null && !request.getParameter("postedBy").equals("")){
                        userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                        userProfile.removewallPost(Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                        hasInfo = userProfile.loadUserInfo();
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("audioAlbum")){
        if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
            if (request.getParameter("id") != null && !request.getParameter("id").equals("")){
                if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                    if (request.getParameter("postedBy") != null && !request.getParameter("postedBy").equals("")){
                        userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                        userProfile.removeAudioAlbum(Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                        hasInfo = userProfile.loadUserInfo();
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("videoAlbum")){
        if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
            if (request.getParameter("id") != null && !request.getParameter("id").equals("")){
                if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                    if (request.getParameter("postedBy") != null && !request.getParameter("postedBy").equals("")){
                        userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                        userProfile.removeVideoAlbum(Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                        hasInfo = userProfile.loadUserInfo();
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("photoAlbum")){
        if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
            if (request.getParameter("id") != null && !request.getParameter("id").equals("")){
                if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                    if (request.getParameter("postedBy") != null && !request.getParameter("postedBy").equals("")){
                        userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                        userProfile.removePhotoAlbum(Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                        hasInfo = userProfile.loadUserInfo();
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("audio")){
        if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
            if (request.getParameter("parentID") != null && !request.getParameter("parentID").equals("")){
                if (request.getParameter("id") != null && !request.getParameter("id").equals("")){
                    if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                        if (request.getParameter("postedBy") != null && !request.getParameter("postedBy").equals("")){
                            userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                            userProfile.removeAudio(Long.parseLong(request.getParameter("parentID")), Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                            hasInfo = userProfile.loadUserInfo();
                        }
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("video")){
        if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
            if (request.getParameter("parentID") != null && !request.getParameter("parentID").equals("")){
                if (request.getParameter("id") != null && !request.getParameter("id").equals("")){
                    if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                        if (request.getParameter("postedBy") != null && !request.getParameter("postedBy").equals("")){
                            userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                            userProfile.removeVideo(Long.parseLong(request.getParameter("parentID")), Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                            hasInfo = userProfile.loadUserInfo();
                        }
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("photo")){
        if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
            if (request.getParameter("parentID") != null && !request.getParameter("parentID").equals("")){
                if (request.getParameter("id") != null && !request.getParameter("id").equals("")){
                    if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                        if (request.getParameter("postedBy") != null && !request.getParameter("postedBy").equals("")){
                            userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                            userProfile.removePhoto(Long.parseLong(request.getParameter("parentID")), Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                            hasInfo = userProfile.loadUserInfo();
                        }
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("comment")){
        if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
            if (request.getParameter("grandParentID") != null && !request.getParameter("grandParentID").equals("")){
                if (request.getParameter("parentID") != null && !request.getParameter("parentID").equals("")){
                    if (request.getParameter("actualId") != null && !request.getParameter("actualId").equals("")){
                        if (request.getParameter("type") != null && !request.getParameter("type").equals("")){
                            if (request.getParameter("postedBy") != null && !request.getParameter("postedBy").equals("")){ 
                                if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                                    userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                                    userProfile.removeComment(request.getParameter("type"), Long.parseLong(request.getParameter("grandParentID")), Long.parseLong(request.getParameter("parentID")), Long.parseLong(request.getParameter("actualId")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                                    hasInfo = userProfile.loadUserInfo();
                                }
                            }
                        }
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("like")){
        if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
            if (request.getParameter("grandParentID") != null && !request.getParameter("grandParentID").equals("")){
                if (request.getParameter("parentID") != null && !request.getParameter("parentID").equals("")){
                    if (request.getParameter("id") != null && !request.getParameter("id").equals("")){
                        if (request.getParameter("type") != null && !request.getParameter("type").equals("")){
                            if (request.getParameter("postedBy") != null && !request.getParameter("postedBy").equals("")){ 
                                if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                                    userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                                    userProfile.removelike(request.getParameter("type"), Long.parseLong(request.getParameter("grandParentID")), Long.parseLong(request.getParameter("parentID")), Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                                    hasInfo = userProfile.loadUserInfo();
                                }
                            }
                        }
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("dislike")){
        if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
            if (request.getParameter("grandParentID") != null && !request.getParameter("grandParentID").equals("")){
                if (request.getParameter("parentID") != null && !request.getParameter("parentID").equals("")){
                    if (request.getParameter("id") != null && !request.getParameter("id").equals("")){
                        if (request.getParameter("type") != null && !request.getParameter("type").equals("")){
                            if (request.getParameter("postedBy") != null && !request.getParameter("postedBy").equals("")){ 
                                if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                                    userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                                    userProfile.removeDislike(request.getParameter("type"), Long.parseLong(request.getParameter("grandParentID")), Long.parseLong(request.getParameter("parentID")), Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                                    hasInfo = userProfile.loadUserInfo();
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
%>
<!-- start header -->
<div id="header">
<div id="logo">
<h1><a href="#">MyZone</a></h1>
<p>Uninterrupted Privacy, Uncompromised Security</p>
</div>
<div id="menu">
<ul>
<li style="border-top: 5px solid #169418; padding:0 0 0 0"><a href="index.jsp">My Feed</a></li>
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
<% 
for (int i = 0; i < feeds.size(); i++){
    feed f = (feed)feeds.get(i);
    %>
    <div class="left-sidebar-title-row">
    <div class="sidebar-title-textbox1">
    <a href="?feed=<%=f.getName()%>"><%=f.getName()%></a>
    </div>
    </div>
    <%
}
%>
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
<!-- start of contents --> 
<div id="wrapper">
<%
if(request.getParameter("feed") != null){
    if (!request.getParameter("feed").equals("")){
        for (int i = 0; i < feeds.size(); i++){
            feed f = (feed)feeds.get(i);
            if(f.getName().equals(request.getParameter("feed"))){
                if (request.getParameter("duration") != null && !request.getParameter("duration").equals("")){
                    f.createFeed(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() - (86400000 * Integer.valueOf(request.getParameter("duration"))), Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis());
                    out.println(f.displayFeed());
                    out.println("<div class=\"content\">\n<div class=\"header\">\n<div class=\"posted-entry\">\n<a href=\"index.jsp?feed=" + f.getName() + "&duration=" + (Integer.valueOf(request.getParameter("duration")) + 1 ) + "\">Display older posts</a>\n</div>\n</div>\n</div>");
                }else{
                    f.createFeed(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() - 86400000, Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis());
                    out.println(f.displayFeed());
                    out.println("<div class=\"content\">\n<div class=\"header\">\n<div class=\"posted-entry\">\n<a href=\"index.jsp?feed=" + f.getName() + "&duration=2" + "\">Display older posts</a>\n</div>\n</div>\n</div>");
                }
                
            }
        }
    }
}else if(feeds.size() > 0){
    feed f = (feed)feeds.get(0);
    if (request.getParameter("duration") != null && !request.getParameter("duration").equals("")){
        f.createFeed(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() - (86400000 * Integer.valueOf(request.getParameter("duration"))), Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis());
        out.println(f.displayFeed());
        out.println("<div class=\"content\">\n<div class=\"header\">\n<div class=\"posted-entry\">\n<a href=\"index.jsp?feed=" + f.getName() + "&duration=" + (Integer.valueOf(request.getParameter("duration")) + 1 ) + "\">Display older posts</a>\n</div>\n</div>\n</div>");
    }else{
        f.createFeed(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() - 86400000, Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis());
        out.println(f.displayFeed());
        out.println("<div class=\"content\">\n<div class=\"header\">\n<div class=\"posted-entry\">\n<a href=\"index.jsp?feed=" + f.getName() + "&duration=2" + "\">Display older posts</a>\n</div>\n</div>\n</div>");
    }
}
%>
</div>
<!-- end page -->
<div id="footer">
<p class="copyright">&copy;&nbsp;&nbsp;2012 All Rights Reserved &nbsp;&bull;&nbsp; Created by <a href="mailto:alireza.mahdian@colorado.edu">Alireza Mahdian</a>.</p>
</div>
</div>
</body>
</html>