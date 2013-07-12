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
                               $('#reload').fadeOut('slow').load('profile.jsp<%if (request.getParameter("profileOwner") != null){out.println("?profileOwner=" + request.getParameter("profileOwner"));}%>').fadeIn("slow");
                               }, 900000);
</script>
</head>
<body>
<div id="reload">
<%@ page import="java.util.*, java.io.*" %>
<%@ page import="MyZone.*" %>
<%@ page import="MyZone.elements.*" %>
<%  
Settings mainSettings = new Settings("./MyZone/");
mainSettings.refresh(mainSettings.ALL);
if ( mainSettings.username == null || mainSettings.username.equals("") ){
    response.sendRedirect("settings.jsp?show=createNewUser");
    return;
}else if (!mainSettings.keyFound){
    response.sendRedirect("settings.jsp?show=createNewUser&promptError=myKeyNotFound");
    return;
}else if (!mainSettings.CAKeyFound){
    response.sendRedirect("settings.jsp?settings=basic&select=CAKey&promptError=CAKeyNotFound");
    return;
}else if (!mainSettings.certFound){
    response.sendRedirect("settings.jsp?settings=basic&select=MyCert&promptError=myCertNotFound");
    return;
}else if (mainSettings.certCorrupted){
    response.sendRedirect("settings.jsp?settings=basic&select=MyCert&promptError=myCertCorrupted");
    return;
}
profile userProfile;
boolean hasInfo = false;
if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
    if (!mainSettings.isFriend(request.getParameter("profileOwner")) && !mainSettings.username.equals(request.getParameter("profileOwner"))){
        response.sendRedirect("friends.jsp?op=addNewFriend&username=" + request.getParameter("profileOwner"));
        return;
    }
    userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
}else{
    userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
}
// save basic information
if (request.getParameter("belongsTo") != null && !request.getParameter("belongsTo").equals("")){
    userProfile = new profile(request.getParameter("belongsTo"), mainSettings.username, "./MyZone/");
    hasInfo = userProfile.loadUserInfo();
}
if (request.getParameter("op") != null && request.getParameter("op").equals("saveUserInfo")){
    userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
    hasInfo = userProfile.loadUserInfo();
    if (request.getParameter("firstname") != null && !request.getParameter("firstname").equals("")){
        userProfile.userInfo.setFirstName(request.getParameter("firstname"));
    }
    if (request.getParameter("lastname") != null && !request.getParameter("lastname").equals("")){
        userProfile.userInfo.setLastName(request.getParameter("lastname"));
    }
    if (request.getParameter("gender") != null && !request.getParameter("gender").equals("")){
        userProfile.userInfo.setGender(request.getParameter("gender"));
    }
    if (request.getParameter("relationshipStatus") != null && !request.getParameter("relationshipStatus").equals("")){
        userProfile.userInfo.setRelationshipStatus(request.getParameter("relationshipStatus"));
    }
    if (request.getParameter("aboutMe") != null && !request.getParameter("aboutMe").equals("")){
        String a = request.getParameter("aboutMe");
        if (a.lastIndexOf("\n") == a.length() - 1){
            a = a.substring(0, a.length() - 1);
        }
        userProfile.userInfo.setAboutMe(a);
    }
    date dob = new date();
    boolean dobSet = false;
    if (request.getParameter("month") != null && !request.getParameter("month").equals("")){
        dob.setMonth(Integer.parseInt(request.getParameter("month")));
        dobSet = true;
    }
    if (request.getParameter("day") != null && !request.getParameter("day").equals("")){
        dob.setDay(Integer.parseInt(request.getParameter("day")));
        dobSet = true;
    }
    if (request.getParameter("year") != null && !request.getParameter("year").equals("") && Integer.parseInt(request.getParameter("year")) > 0 && Integer.parseInt(request.getParameter("year")) < 3000){
        dob.setYear(Integer.parseInt(request.getParameter("year")));
        dobSet = true;
    }
    if (dobSet)
        userProfile.userInfo.setDateOfBirth(dob);
    if (request.getParameter("profilePic") != null && !request.getParameter("profilePic").equals("") && !request.getParameter("profilePic").equals("null")){
        String thumbnail = mainSettings.username + ".jpg";
        File src = new File("./MyZone/" + mainSettings.username + "/photos/" + request.getParameter("profilePic"));
        imageUtils iu = new imageUtils(src);
        iu.resizeImageWithHint("./MyZone/" + mainSettings.username + "/thumbnails/" + thumbnail, 80, 80);
        userProfile.userInfo.setProfilePic(request.getParameter("profilePic"));
        userProfile.updateModifiedFiles("add", Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "./MyZone/" + mainSettings.username + "/thumbnails/" + thumbnail, "public");
    }
    // add/modify education entries
    int j = userProfile.userInfo.getEducations().size();
    education ed = new education();
    boolean educationAdded = false;
    if (request.getParameter("institutionEd" + j) != null){
        if (!request.getParameter("institutionEd" + j).equals("") && !request.getParameter("institutionEd" + j).equals("\n")){
            String institutionEd = request.getParameter("institutionEd" + j);
            if (request.getParameter("institutionEd" + j).lastIndexOf("\n") == request.getParameter("institutionEd" + j).length() - 1){
                institutionEd = request.getParameter("institutionEd" + j).substring(0, request.getParameter("institutionEd" + j).length() - 1);
            }
            ed.setInstitution(institutionEd);
            educationAdded = true;
        }
    }
    if (request.getParameter("level" + j) != null){
        if (!request.getParameter("level" + j).equals("") && !request.getParameter("level" + j).equals("\n")){
            String level = request.getParameter("level" + j);
            if (request.getParameter("level" + j).lastIndexOf("\n") == request.getParameter("level" + j).length() - 1){
                level = request.getParameter("level" + j).substring(0, request.getParameter("level" + j).length() - 1);
            }
            ed.setLevel(level);
            educationAdded = true;
        }
    }
    if (request.getParameter("degree" + j) != null){
        if (!request.getParameter("degree" + j).equals("") && !request.getParameter("degree" + j).equals("\n")){
            String degree = request.getParameter("degree" + j);
            if (request.getParameter("degree" + j).lastIndexOf("\n") == request.getParameter("degree" + j).length() - 1){
                degree = request.getParameter("degree" + j).substring(0, request.getParameter("degree" + j).length() - 1);
            }
            ed.setDegree(degree);
            educationAdded = true;
        }
    }
    if (request.getParameter("major" + j) != null){
        if (!request.getParameter("major" + j).equals("") && !request.getParameter("major" + j).equals("\n")){
            String major = request.getParameter("major" + j);
            if (request.getParameter("major" + j).lastIndexOf("\n") == request.getParameter("major" + j).length() - 1){
                major = request.getParameter("major" + j).substring(0, request.getParameter("major" + j).length() - 1);
            }
            ed.setMajor(major);
            educationAdded = true;
        }
    }
    if (request.getParameter("sinceEducation" + j) != null){
        if (!request.getParameter("sinceEducation" + j).equals("")){
            ed.setStartYear(request.getParameter("sinceEducation" + j));
            educationAdded = true;
        }
    }
    if (request.getParameter("toEducation" + j) != null){
        if (!request.getParameter("toEducation" + j).equals("")){
            ed.setFinishYear(request.getParameter("toEducation" + j));
            educationAdded = true;
        }
    }
    if (educationAdded){
        ed.setId(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis());
        userProfile.userInfo.addEducation(ed);
    }
    for (int i = 0; i < userProfile.userInfo.getEducations().size(); i++){
        ed = (education)userProfile.userInfo.getEducations().get(i);
        if (request.getParameter("deleteEd" + i) != null && request.getParameter("deleteEd" + i).equals("true")){
            userProfile.userInfo.removeEducation(ed.getId());
        }
        if (request.getParameter("institutionEd" + i) != null){
            if (!request.getParameter("institutionEd" + i).equals("")){
                String institutionEd = request.getParameter("institutionEd" + i);
                if (request.getParameter("institutionEd" + i).lastIndexOf("\n") == request.getParameter("institutionEd" + i).length() - 1){
                    institutionEd = request.getParameter("institutionEd" + i).substring(0, request.getParameter("institutionEd" + i).length() - 1);
                }
                ed.setInstitution(institutionEd);
            }
        }
        if (request.getParameter("level" + i) != null){
            if (!request.getParameter("level" + i).equals("")){
                String level = request.getParameter("level" + i);
                if (request.getParameter("level" + i).lastIndexOf("\n") == request.getParameter("level" + i).length() - 1){
                    level = request.getParameter("level" + i).substring(0, request.getParameter("level" + i).length() - 1);
                }
                ed.setLevel(level);
            }
        }
        if (request.getParameter("degree" + i) != null){
            if (!request.getParameter("degree" + i).equals("")){
                String degree = request.getParameter("degree" + i);
                if (request.getParameter("degree" + i).lastIndexOf("\n") == request.getParameter("degree" + i).length() - 1){
                    degree = request.getParameter("degree" + i).substring(0, request.getParameter("degree" + i).length() - 1);
                }
                ed.setDegree(degree);
            }
        }
        if (request.getParameter("major" + i) != null){
            if (!request.getParameter("major" + i).equals("")){
                String major = request.getParameter("major" + i);
                if (request.getParameter("major" + i).lastIndexOf("\n") == request.getParameter("major" + i).length() - 1){
                    major = request.getParameter("major" + i).substring(0, request.getParameter("major" + i).length() - 1);
                }
                ed.setMajor(major);
            }
        }
        if (request.getParameter("sinceEducation" + i) != null){
            if (!request.getParameter("sinceEducation" + i).equals("")){
                ed.setStartYear(request.getParameter("sinceEducation" + i));
            }
        }
        if (request.getParameter("toEducation" + i) != null){
            if (!request.getParameter("toEducation" + i).equals("")){
                ed.setFinishYear(request.getParameter("toEducation" + i));
            }
        }
    }
    
    // add/modify employment entries 
    j = userProfile.userInfo.getEmployments().size();
    employment em = new employment();
    boolean employmentAdded = false;
    if (request.getParameter("institutionEm" + j) != null){
        if (!request.getParameter("institutionEm" + j).equals("") && !request.getParameter("institutionEm" + j).equals("\n")){
            String institutionEm = request.getParameter("institutionEm" + j);
            if (request.getParameter("institutionEm" + j).lastIndexOf("\n") == request.getParameter("institutionEm" + j).length() - 1){
                institutionEm = request.getParameter("institutionEm" + j).substring(0, request.getParameter("institutionEm" + j).length() - 1);
            }
            em.setInstitution(institutionEm);
            employmentAdded = true;
        }
    }
    if (request.getParameter("position" + j) != null){
        if (!request.getParameter("position" + j).equals("") && !request.getParameter("position" + j).equals("\n")){
            String position = request.getParameter("position" + j);
            if (request.getParameter("position" + j).lastIndexOf("\n") == request.getParameter("position" + j).length() - 1){
                position = request.getParameter("position" + j).substring(0, request.getParameter("position" + j).length() - 1);
            }
            em.setPosition(position);
            employmentAdded = true;
        }
    }
    if (request.getParameter("sinceEmployment" + j) != null){
        if (!request.getParameter("sinceEmployment" + j).equals("")){
            em.setStartYear(request.getParameter("sinceEmployment" + j));
            employmentAdded = true;
        }
    }
    if (request.getParameter("toEmployment" + j) != null){
        if (!request.getParameter("toEmployment" + j).equals("")){
            em.setFinishYear(request.getParameter("toEmployment" + j));
            employmentAdded = true;
        }
    }
    if (employmentAdded){
        em.setId(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis());
        userProfile.userInfo.addEmployment(em);
    }
    for (int i = 0; i < userProfile.userInfo.getEmployments().size(); i++){
        em = (employment)userProfile.userInfo.getEmployments().get(i);
        if (request.getParameter("deleteEm" + i) != null && request.getParameter("deleteEm" + i).equals("true")){
            userProfile.userInfo.removeEmployment(em.getId());
        }
        if (request.getParameter("institutionEm" + i) != null){
            if (!request.getParameter("institutionEm" + i).equals("")){
                String institutionEm = request.getParameter("institutionEm" + i);
                if (request.getParameter("institutionEm" + i).lastIndexOf("\n") == request.getParameter("institutionEm" + i).length() - 1){
                    institutionEm = request.getParameter("institutionEm" + i).substring(0, request.getParameter("institutionEm" + i).length() - 1);
                }
                em.setInstitution(institutionEm);
            }
        }
        if (request.getParameter("position" + i) != null){
            if (!request.getParameter("position" + i).equals("")){
                String position = request.getParameter("position" + i);
                if (request.getParameter("position" + i).lastIndexOf("\n") == request.getParameter("position" + i).length() - 1){
                    position = request.getParameter("position" + i).substring(0, request.getParameter("position" + i).length() - 1);
                }
                em.setPosition(position);
            }
        }
        if (request.getParameter("sinceEmployment" + i) != null){
            if (!request.getParameter("sinceEmployment" + i).equals("")){
                em.setStartYear(request.getParameter("sinceEmployment" + i));
            }
        }
        if (request.getParameter("toEmployment" + i) != null){
            if (!request.getParameter("toEmployment" + i).equals("")){
                em.setFinishYear(request.getParameter("toEmployment" + i));
            }
        }
    }
    userProfile.userInfo.setUsername(mainSettings.username);
    userProfile.saveUserInfo();
}else if (request.getParameter("op") != null && request.getParameter("op").equals("add")){
    if (request.getParameter("el")!= null && request.getParameter("el").equals("audioAlbum")){
        if (request.getParameter("belongsTo") != null && !request.getParameter("belongsTo").equals("")){
            if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                if (request.getParameter("title") != null && !request.getParameter("title").equals("")){
                    userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                    hasInfo = userProfile.loadUserInfo();
                    user u;
                    if (hasInfo){
                        u = new user(userProfile.profileOwner, userProfile.userInfo.getFirstName(), userProfile.userInfo.getLastName(), 0); 
                        userProfile = new profile(request.getParameter("belongsTo"), mainSettings.username, "./MyZone/");
                        audioAlbum aa = new audioAlbum(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(),
                                                       Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), 
                                                       request.getParameter("belongsTo"), 
                                                       u,
                                                       request.getParameter("title"),
                                                       request.getParameter("shareWith"),
                                                       null,
                                                       null,
                                                       null,
                                                       null);
                        userProfile.addAudioAlbum(aa, false);
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("videoAlbum")){
        if (request.getParameter("belongsTo") != null && !request.getParameter("belongsTo").equals("")){
            if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                if (request.getParameter("title") != null && !request.getParameter("title").equals("")){
                    userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                    hasInfo = userProfile.loadUserInfo();
                    user u;
                    if (hasInfo){
                        u = new user(userProfile.profileOwner, userProfile.userInfo.getFirstName(), userProfile.userInfo.getLastName(), 0); 
                        userProfile = new profile(request.getParameter("belongsTo"), mainSettings.username, "./MyZone/");
                        videoAlbum va = new videoAlbum(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(),
                                                       Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), 
                                                       request.getParameter("belongsTo"), 
                                                       u,
                                                       request.getParameter("title"),
                                                       request.getParameter("shareWith"),
                                                       null,
                                                       null,
                                                       null,
                                                       null);
                        userProfile.addVideoAlbum(va, false);
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("photoAlbum")){
        if (request.getParameter("belongsTo") != null && !request.getParameter("belongsTo").equals("")){
            if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                if (request.getParameter("title") != null && !request.getParameter("title").equals("")){
                    userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                    hasInfo = userProfile.loadUserInfo();
                    user u;
                    if (hasInfo){
                        u = new user(userProfile.profileOwner, userProfile.userInfo.getFirstName(), userProfile.userInfo.getLastName(), 0); 
                        userProfile = new profile(request.getParameter("belongsTo"), mainSettings.username, "./MyZone/");
                        photoAlbum pa = new photoAlbum(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(),
                                                       Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), 
                                                       request.getParameter("belongsTo"), 
                                                       u,
                                                       request.getParameter("title"),
                                                       request.getParameter("shareWith"),
                                                       null,
                                                       null,
                                                       null,
                                                       null);
                        userProfile.addPhotoAlbum(pa, false);
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("link")){
        if (request.getParameter("belongsTo") != null && !request.getParameter("belongsTo").equals("")){
            if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                if (request.getParameter("url") != null && !request.getParameter("url").equals("")){
                    userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                    hasInfo = userProfile.loadUserInfo();
                    user u;
                    if (hasInfo){
                        u = new user(userProfile.profileOwner, userProfile.userInfo.getFirstName(), userProfile.userInfo.getLastName(), 0); 
                        userProfile = new profile(request.getParameter("belongsTo"), mainSettings.username, "./MyZone/");
                        link l = new link(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(),
                                          Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), 
                                          request.getParameter("belongsTo"), 
                                          u,
                                          request.getParameter("url"),
                                          request.getParameter("description"),
                                          request.getParameter("shareWith"),
                                          null,
                                          null,
                                          null);
                        userProfile.addLink(l, false);
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("wallPost")){
        if (request.getParameter("belongsTo") != null && !request.getParameter("belongsTo").equals("")){
            if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                if (request.getParameter("body") != null && !request.getParameter("body").equals("")){
                    userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                    hasInfo = userProfile.loadUserInfo();
                    user u;
                    if (hasInfo){
                        u = new user(userProfile.profileOwner, userProfile.userInfo.getFirstName(), userProfile.userInfo.getLastName(), 0); 
                        userProfile = new profile(request.getParameter("belongsTo"), mainSettings.username, "./MyZone/");
                        wallPost wp = new wallPost(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(),
                                                   Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), 
                                                   request.getParameter("belongsTo"), 
                                                   u,
                                                   request.getParameter("body"),
                                                   request.getParameter("shareWith"),
                                                   null,
                                                   null,
                                                   null);
                        userProfile.addWallPost(wp, false);
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("comment")){
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
                    if (request.getParameter("type") != null && !request.getParameter("type").equals("")){
                        if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                            userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                            hasInfo = userProfile.loadUserInfo();
                            user u;
                            if (hasInfo){
                                u = new user(userProfile.profileOwner, userProfile.userInfo.getFirstName(), userProfile.userInfo.getLastName(), 0); 
                                userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                                like l = new like(request.getParameter("type"),
                                                  Long.parseLong(request.getParameter("grandParentID")), 
                                                  Long.parseLong(request.getParameter("parentID")), 
                                                  Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), 
                                                  request.getParameter("profileOwner"), 
                                                  request.getParameter("shareWith"), 
                                                  u);
                                userProfile.addlike(l, false);
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
                    if (request.getParameter("type") != null && !request.getParameter("type").equals("")){
                        if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                            userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                            hasInfo = userProfile.loadUserInfo();
                            user u;
                            if (hasInfo){
                                u = new user(userProfile.profileOwner, userProfile.userInfo.getFirstName(), userProfile.userInfo.getLastName(), 0); 
                                userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                                dislike dl = new dislike(request.getParameter("type"),
                                                         Long.parseLong(request.getParameter("grandParentID")), 
                                                         Long.parseLong(request.getParameter("parentID")), 
                                                         Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), 
                                                         request.getParameter("profileOwner"), 
                                                         request.getParameter("shareWith"), 
                                                         u);
                                userProfile.addDislike(dl, false);
                            }
                        }
                    }
                }
            }
        }
    }
}else if (request.getParameter("op") != null && request.getParameter("op").equals("remove")){
    if (request.getParameter("el")!= null && request.getParameter("el").equals("link")){
        if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
            if (request.getParameter("id") != null && !request.getParameter("id").equals("")){
                if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                    if (request.getParameter("postedBy") != null && !request.getParameter("postedBy").equals("")){
                        userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                        hasInfo = userProfile.loadUserInfo();
                        if (hasInfo){
                            userProfile.removeLink(Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                        }
                    }
                }
            }
        }
    }else if (request.getParameter("el")!= null && request.getParameter("el").equals("wallPost")){
        if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
            if (request.getParameter("id") != null && !request.getParameter("id").equals("")){
                if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                    if (request.getParameter("postedBy") != null && !request.getParameter("postedBy").equals("")){
                        userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                        hasInfo = userProfile.loadUserInfo();
                        if (hasInfo){
                            userProfile.removewallPost(Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                        }
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
                        hasInfo = userProfile.loadUserInfo();
                        if (hasInfo){
                            userProfile.removeAudioAlbum(Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                        }
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
                        hasInfo = userProfile.loadUserInfo();
                        if (hasInfo){
                            userProfile.removeVideoAlbum(Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                        }
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
                        hasInfo = userProfile.loadUserInfo();
                        if (hasInfo){
                            userProfile.removePhotoAlbum(Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                        }
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
                            hasInfo = userProfile.loadUserInfo();
                            if (hasInfo){
                                userProfile.removeAudio(Long.parseLong(request.getParameter("parentID")), Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                            }
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
                            hasInfo = userProfile.loadUserInfo();
                            if (hasInfo){
                                userProfile.removeVideo(Long.parseLong(request.getParameter("parentID")), Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                            }
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
                            hasInfo = userProfile.loadUserInfo();
                            if (hasInfo){
                                userProfile.removePhoto(Long.parseLong(request.getParameter("parentID")), Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                            }
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
                                    hasInfo = userProfile.loadUserInfo();
                                    if (hasInfo){
                                        userProfile.removeComment(request.getParameter("type"), Long.parseLong(request.getParameter("grandParentID")), Long.parseLong(request.getParameter("parentID")), Long.parseLong(request.getParameter("actualId")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                                    }
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
                                    hasInfo = userProfile.loadUserInfo();
                                    if (hasInfo){
                                        userProfile.removelike(request.getParameter("type"), Long.parseLong(request.getParameter("grandParentID")), Long.parseLong(request.getParameter("parentID")), Long.parseLong(request.getParameter("id")), request.getParameter("shareWith"), request.getParameter("postedBy"), false);
                                    }
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
                                    hasInfo = userProfile.loadUserInfo();
                                    if (hasInfo){
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
}
hasInfo = userProfile.loadUserInfo();
if (!hasInfo && userProfile.profileOwner.equals(mainSettings.username)){
    if (request.getParameter("show") == null){
        response.sendRedirect("profile.jsp?profileOwner=" + mainSettings.username + "&show=info");
	   return;
    }else if (!request.getParameter("show").equals("info")){
        response.sendRedirect("profile.jsp?profileOwner=" + mainSettings.username + "&show=info");
        return;
    }
}else{
    if ((userProfile.userInfo.getFirstName().equals("") || userProfile.userInfo.getLastName().equals("")) && userProfile.profileOwner.equals(mainSettings.username)){
        if (request.getParameter("show") != null && !request.getParameter("show").equals("info")){
            response.sendRedirect("profile.jsp?profileOwner=" + mainSettings.username + "&show=info");
            return;
        }
    }
}
%>
<!-- start header -->
<div id="header">
<div id="logo">
<h1><a href="index.jsp">MyZone</a></h1>
<p>Uninterrupted Privacy, Uncompromised Security</p>
</div>
<div id="menu">
<ul>
<li><a href="index.jsp">My Feed</a></li>
<li <% if (userProfile.profileOwner.equals(mainSettings.username)){ %>style="border-top: 5px solid #169418; padding:0 0 0 0" <%}%> ><a href="profile.jsp">My Profile</a></li>
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
<div id="profie-pic">
<img src="<%=userProfile.userInfo.getProfilePic()%>" width="190">
</img>
</div>
<div class="left-sidebar-title-row">
<div class="sidebar-title-textbox1">
<a href="?show=photos&profileOwner=<%=userProfile.profileOwner%>">Photos</a>
</div>
</div>
<div class="left-sidebar-title-row">
<div class="sidebar-title-textbox1">
<a href="?show=audios&profileOwner=<%=userProfile.profileOwner%>">Audios</a>
</div>
</div>
<div class="left-sidebar-title-row">
<div class="sidebar-title-textbox1">
<a href="?show=videos&profileOwner=<%=userProfile.profileOwner%>">Videos</a>
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
<% 
if (userProfile.profileOwner != null && userProfile.profileOwner.equals(mainSettings.username)){
    %>
    <a href="profile.jsp?profileOwner=<%=userProfile.profileOwner%>&show=info">
    <%
}else{
    %>
    <a>
    <%
}
%>
Profile Information</a>
</div>
</div>
<div class="scrollable-area">
<%if (hasInfo){
    out.println(userProfile.userInfo.generateHTML());
}%>
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
<div id="profile-header">
<h1><a href="?profileOwner=<%=userProfile.profileOwner%>"><%=userProfile.userInfo.getFirstName()%> <%=userProfile.userInfo.getLastName()%></a></h1>
</div>
<!-- start share box -->
<% 
if (hasInfo){
    
    if(request.getParameter("show") == null){
        %>
        <div id="sharebox">
        <div id="sharebox-menu">
        <div class="sharebox-menu-entry">
        <%
        if(request.getParameter("sharebox") != null){
            if (request.getParameter("sharebox").equals("wallPost")){
                %>
                <a style="color:black" href="?sharebox=wallPost&profileOwner=<%=userProfile.profileOwner%>"><% if (request.getParameter("profileOwner") != null){
                    if (!request.getParameter("profileOwner").equals(mainSettings.username)){
                        %>Post<%
                    }else{
                        %>Status<%
                    }
                }else{
                    %>Status<%
                }%>
                </a>
                <%
            }else{
                %>
                <a href="?sharebox=wallPost&profileOwner=<%=userProfile.profileOwner%>"><% if (request.getParameter("profileOwner") != null){
                    if (!request.getParameter("profileOwner").equals(mainSettings.username)){
                        %>Post<%
                    }else{
                        %>Status<%
                    }
                }else{
                    %>Status<%
                }%>
                </a>
                <%
            }
        }else{
            %>
            <a style="color:black" href="?sharebox=wallPost&profileOwner=<%=userProfile.profileOwner%>"><% if (request.getParameter("profileOwner") != null){
                if (!request.getParameter("profileOwner").equals(mainSettings.username)){
                    %>Post<%
                }else{
                    %>Status<%
                }
            }else{
                %>Status<%
            }%>
            </a>
            <%
        }
        %>
        </div>
        <div class="sharebox-menu-entry">
        <%
        if(request.getParameter("sharebox") != null){
            if (request.getParameter("sharebox").equals("photo")){
                %>
                <a style="color:black" href="?sharebox=photo&profileOwner=<%=userProfile.profileOwner%>">Photo</a>
                <%
            }else{
                %>
                <a href="?sharebox=photo&profileOwner=<%=userProfile.profileOwner%>">Photo</a>
                <%
            }
        }else{
            %>
            <a href="?sharebox=photo&profileOwner=<%=userProfile.profileOwner%>">Photo</a>
            <%
        }
        %>
        </div>
        <div class="sharebox-menu-entry">
        <%
        if(request.getParameter("sharebox") != null){
            if (request.getParameter("sharebox").equals("audio")){
                %>
                <a style="color:black" href="?sharebox=audio&profileOwner=<%=userProfile.profileOwner%>">Audio</a>
                <%
            }else{
                %>
                <a href="?sharebox=audio&profileOwner=<%=userProfile.profileOwner%>">Audio</a>
                <%
            }
        }else{
            %>
            <a href="?sharebox=audio&profileOwner=<%=userProfile.profileOwner%>">Audio</a>
            <%
        }
        %>
        </div>
        <div class="sharebox-menu-entry">
        <%
        if(request.getParameter("sharebox") != null){
            if (request.getParameter("sharebox").equals("video")){
                %>
                <a style="color:black" href="?sharebox=video&profileOwner=<%=userProfile.profileOwner%>">Video</a>
                <%
            }else{
                %>
                <a href="?sharebox=video&profileOwner=<%=userProfile.profileOwner%>">Video</a>
                <%
            }
        }else{
            %>
            <a href="?sharebox=video&profileOwner=<%=userProfile.profileOwner%>">Video</a>
            <%
        }
        %>
        </div>
        <div class="sharebox-menu-entry">
        <%
        if(request.getParameter("sharebox") != null){
            if (request.getParameter("sharebox").equals("link")){
                %>
                <a style="color:black" href="?sharebox=link&profileOwner=<%=userProfile.profileOwner%>">Link</a>
                <%
            }else{
                %>
                <a href="?sharebox=link&profileOwner=<%=userProfile.profileOwner%>">Link</a>
                <%
            }
        }else{
            %>
            <a href="?sharebox=link&profileOwner=<%=userProfile.profileOwner%>">Link</a>
            <%
        }
        %>
        </div>
        </div>
        <div id="sharebox-body">
        <%
        if(request.getParameter("sharebox") != null){
            if (request.getParameter("sharebox").equals("wallPost")){ 
                %>
                <form method="post" action="profile.jsp">
                <input type="hidden" name="belongsTo" value="<%= userProfile.profileOwner%>">
                <input type="hidden" name="op" value="add">
                <input type="hidden" name="el" value="wallPost">
                <h2>I would like to say:</h2>
                <textarea name="body"></textarea>
                <div class="shareWith">
                <p><img src="./images/share.png" width="15">
                </img alt="share with">
                <%
                if (userProfile.profileOwner.equals(mainSettings.username)){
                    %>
                    </p><select name="shareWith">
                    <% 
                    for(int i = 0; i < mainSettings.zones.size(); i++){
                        zone z = (zone)mainSettings.zones.get(i);
                        %>
                        <option value="<%= z.getName()%>"><%= z.getName()%></option>
                        <%
                    }
                    %>
                    </select>
                    <%
                }else{
                    %>
                    All</p><input type="hidden" name="shareWith" value="All">
                    <%
                }
                %>
                </div>
                <input type="submit" value="Update"/></input>
                </form>
                <%        
            }else if(request.getParameter("sharebox").equals("photo")){
                %>
                <form action="file_upload.jsp" method="post" enctype="multipart/form-data">
                <input type="hidden" name="belongsTo" value="<%= userProfile.profileOwner%>">
                <input type="hidden" name="username" value="<%= mainSettings.username %>">
                <input type="hidden" name="filetype" value="photo">
            <h2>Caption: </h2>
                <textarea name="description"></textarea>
                <div class="shareWith">
                <p><img src="./images/share.png" width="15">
                </img alt="share with"><%
                if (userProfile.profileOwner.equals(mainSettings.username)){
                    %>
                    </p><select name="shareWith">
                    <% 
                    for(int i = 0; i < mainSettings.zones.size(); i++){
                        zone z = (zone)mainSettings.zones.get(i);
                        %>
                        <option value="<%= z.getName()%>"><%= z.getName()%></option>
                        <%
                    }
                    %>
                    </select>
                    <%
                }else{
                    %>
                    All</p><input type="hidden" name="shareWith" value="All">
                    <%
                }
                %>
                </div>
                <input type="file" name="myFile"/></input>
                <input type="submit" value="Upload"/></input>
                </form>
                <%      
            }else if(request.getParameter("sharebox").equals("audio")){
                %>
                <form action="file_upload.jsp" method="post" enctype="multipart/form-data">
                <input type="hidden" name="belongsTo" value="<%= userProfile.profileOwner%>">
                <input type="hidden" name="username" value="<%= mainSettings.username %>">
                <input type="hidden" name="filetype" value="audio">
            <h2>Title: </h2>
                <input type="text" name="title">
            <h2>Artist: </h2>
                <input type="text" name="artist">
            <h2>Album: </h2>
                <input type="text" name="album">
            <h2>Caption: </h2>
                <textarea name="description"></textarea>
                <div class="shareWith">
                <p><img src="./images/share.png" width="15">
                </img alt="share with"><%
                if (userProfile.profileOwner.equals(mainSettings.username)){
                    %>
                    </p><select name="shareWith">
                    <% 
                    for(int i = 0; i < mainSettings.zones.size(); i++){
                        zone z = (zone)mainSettings.zones.get(i);
                        %>
                        <option value="<%= z.getName()%>"><%= z.getName()%></option>
                        <%
                    }
                    %>
                    </select>
                    <%
                }else{
                    %>
                    All</p><input type="hidden" name="shareWith" value="All">
                    <%
                }
                %>
                </div>
                <input type="file" name="myFile"/></input>
                <input type="submit" value="Upload"/></input>
                </form>
                <%      
            }else if(request.getParameter("sharebox").equals("video")){
                %>
                <form action="file_upload.jsp" method="post" enctype="multipart/form-data">
                <input type="hidden" name="belongsTo" value="<%= userProfile.profileOwner%>">
                <input type="hidden" name="username" value="<%= mainSettings.username %>">
                <input type="hidden" name="filetype" value="video">
            <h2>Title: </h2>
                <input type="text" name="title">
            <h2>Caption: </h2>
                <textarea name="description"></textarea>
                <div class="shareWith">
                <p><img src="./images/share.png" width="15">
                </img alt="share with"><%
                if (userProfile.profileOwner.equals(mainSettings.username)){
                    %>
                    </p><select name="shareWith">
                    <% 
                    for(int i = 0; i < mainSettings.zones.size(); i++){
                        zone z = (zone)mainSettings.zones.get(i);
                        %>
                        <option value="<%= z.getName()%>"><%= z.getName()%></option>
                        <%
                    }
                    %>
                    </select>
                    <%
                }else{
                    %>
                    All</p><input type="hidden" name="shareWith" value="All">
                    <%
                }
                %>
                </div>
                <input type="file" name="myFile"/></input>
                <input type="submit" value="Upload"/></input>
                </form>
                <%      
            }else if(request.getParameter("sharebox").equals("link")){
                %>
                <form method="post" action="profile.jsp">
                <input type="hidden" name="belongsTo" value="<%= userProfile.profileOwner%>">
                <input type="hidden" name="op" value="add">
                <input type="hidden" name="el" value="link">
                <h2>Link URL: </h2>
                <input type="text" name="url" value="http://"/> 
                <h2>I would like to share this link because: </h2>
                <textarea name="description"/></textarea>
                <div class="shareWith">
                <p><img src="./images/share.png" width="15">
                </img alt="share with"><%
                if (userProfile.profileOwner.equals(mainSettings.username)){
                    %>
                    </p><select name="shareWith">
                    <% 
                    for(int i = 0; i < mainSettings.zones.size(); i++){
                        zone z = (zone)mainSettings.zones.get(i);
                        %>
                        <option value="<%= z.getName()%>"><%= z.getName()%></option>
                        <%
                    }
                    %>
                    </select>
                    <%
                }else{
                    %>
                    All</p><input type="hidden" name="shareWith" value="All">
                    <%
                }
                %>
                </div>
                <input type="submit" value="Share"/></input>
                </form>
                <%      
            }
        }else{
            %>
            <form method="post" action="profile.jsp">
            <input type="hidden" name="belongsTo" value="<%= userProfile.profileOwner%>">
            <input type="hidden" name="op" value="add">
            <input type="hidden" name="el" value="wallPost">
            <h2>I would like to say:</h2>
            <textarea name="body"/></textarea>
            <div class="shareWith">
            <p><img src="./images/share.png" width="15">
            </img alt="share with"><%
            if (userProfile.profileOwner.equals(mainSettings.username)){
                %>
                </p><select name="shareWith">
                <% 
                for(int i = 0; i < mainSettings.zones.size(); i++){
                    zone z = (zone)mainSettings.zones.get(i);
                    %>
                    <option value="<%= z.getName()%>"><%= z.getName()%></option>
                    <%
                }
                %>
                </select>
                <%
            }else{
                %>
                All</p><input type="hidden" name="shareWith" value="All">
                <%
            }
            %>
            </div>
            <input type="submit" value="Update"/></input>
            </form>
            <%
        }
        %>
        </div>
        </div>
        <%
    }else{
        if (request.getParameter("show").equals("photos")){
            if (request.getParameter("op") == null){
                if (request.getParameter("profileOwner") != null){
                    if (request.getParameter("profileOwner").equals(mainSettings.username)){
                        %>
                        <div id="wrapper">
                        <div class="content">
                        <div class="posted-entry">
                        <a href="profile.jsp?show=photos&op=createAlbum&profileOwner=<%= userProfile.profileOwner%>" class="createAlbum">Create a new photo album</a>
                        </div>
                        </div>
                        </div>
                        <%
                    }
                }
            }else if (request.getParameter("op").equals("createAlbum")){
                %>
                <div id="wrapper">
                <div class="content">
                <div class="posted-entry">
                <form method="post" action="profile.jsp">
                <h3>Create a new photo album</h3>
                <input type="hidden" name="belongsTo" value="<%= userProfile.profileOwner%>">
                <input type="hidden" name="op" value="add">
                <input type="hidden" name="el" value="photoAlbum">
                <input type="hidden" name="show" value="photos">
                <table border="0" cellpadding="0" cellspacing="0">
                <tr>
                <td><h2>Album Title: </h2></td>
                <td><input type="text" name="title" value="" size="50"/></input></td>
                </tr>
                <tr>
                <td><h2>Share With: </h2></td>
                <td>
                <%
                if (userProfile.profileOwner.equals(mainSettings.username)){
                    %>
                    <select name="shareWith">
                    <% 
                    for(int i = 0; i < mainSettings.zones.size(); i++){
                        zone z = (zone)mainSettings.zones.get(i);
                        %>
                        <option value="<%= z.getName()%>"><%= z.getName()%></option>
                        <%
                    }
                    %>
                    </select>
                    <%
                }else{
                    %>
                    All</p><input type="hidden" name="shareWith" value="All">
                    <%
                }
                %>
                </td>
                </tr>
                <tr>
                <td></td><td><input type="submit" value="Create Album"/></input></td>
                </tr>
                </table>
                </form>
                </div>
                </div>
                </div>
                <%
            }
        }else if (request.getParameter("show").equals("audios")){
            if (request.getParameter("op") == null){
                if (request.getParameter("profileOwner") != null){
                    if (request.getParameter("profileOwner").equals(mainSettings.username)){
                        %>
                        <div id="wrapper">
                        <div class="content">
                        <div class="posted-entry">
                        <a href="profile.jsp?show=audios&op=createAlbum&profileOwner=<%= userProfile.profileOwner%>" class="createAlbum">Create a new audio album</a>
                        </div>
                        </div>
                        </div>
                        <%
                    }
                }
            }else if (request.getParameter("op").equals("createAlbum")){
                %>
                <div id="wrapper">
                <div class="content">
                <div class="posted-entry">
                <form method="post" action="profile.jsp">
                <h3>Create a new audio album</h3>
                <input type="hidden" name="belongsTo" value="<%= userProfile.profileOwner%>">
                <input type="hidden" name="op" value="add">
                <input type="hidden" name="el" value="audioAlbum">
                <input type="hidden" name="show" value="audios">
                <table border="0" cellpadding="0" cellspacing="0">
                <tr>
                <td><h2>Album Title: </h2></td>
                <td><input type="text" name="title" value="" size="50"/></input></td>
                </tr>
                <tr>
                <td><h2>Share With: </h2></td>
                <td>
                <%
                if (userProfile.profileOwner.equals(mainSettings.username)){
                    %>
                    <select name="shareWith">
                    <% 
                    for(int i = 0; i < mainSettings.zones.size(); i++){
                        zone z = (zone)mainSettings.zones.get(i);
                        %>
                        <option value="<%= z.getName()%>"><%= z.getName()%></option>
                        <%
                    }
                    %>
                    </select>
                    <%
                }else{
                    %>
                    All</p><input type="hidden" name="shareWith" value="All">
                    <%
                }
                %>
                </td>
                </tr>
                <tr>
                <td></td><td><input type="submit" value="Create Album"/></input></td>
                </tr>
                </table>
                </form>
                </div>
                </div>
                </div>
                <%
            }
        }else if (request.getParameter("show").equals("videos")){
            if (request.getParameter("op") == null){
                if (request.getParameter("profileOwner") != null){
                    if (request.getParameter("profileOwner").equals(mainSettings.username)){
                        %>
                        <div id="wrapper">
                        <div class="content">
                        <div class="posted-entry">
                        <a href="profile.jsp?show=videos&op=createAlbum&profileOwner=<%= userProfile.profileOwner%>" class="createAlbum">Create a new video album</a>
                        </div>
                        </div>
                        </div>
                        <%
                    }
                }
            }else if (request.getParameter("op").equals("createAlbum")){
                %>
                <div id="wrapper">
                <div class="content">
                <div class="posted-entry">
                <form method="post" action="profile.jsp">
                <h3>Create a new video album</h3>
                <input type="hidden" name="belongsTo" value="<%= userProfile.profileOwner%>">
                <input type="hidden" name="op" value="add">
                <input type="hidden" name="el" value="videoAlbum">
                <input type="hidden" name="show" value="videos">
                <table border="0" cellpadding="0" cellspacing="0">
                <tr>
                <td><h2>Album Title: </h2></td>
                <td><input type="text" name="title" value="" size="50"/></input></td>
                </tr>
                <tr>
                <td><h2>Share With: </h2></td>
                <td>
                <%
                if (userProfile.profileOwner.equals(mainSettings.username)){
                    %>
                    <select name="shareWith">
                    <% 
                    for(int i = 0; i < mainSettings.zones.size(); i++){
                        zone z = (zone)mainSettings.zones.get(i);
                        %>
                        <option value="<%= z.getName()%>"><%= z.getName()%></option>
                        <%
                    }
                    %>
                    </select>
                    <%
                }else{
                    %>
                    All</p><input type="hidden" name="shareWith" value="All">
                    <%
                }
                %>
                </td>
                </tr>
                <tr>
                <td></td><td><input type="submit" value="Create Album"/></input></td>
                </tr>
                </table>
                </form>
                </div>
                </div>
                </div>
                <%
            }
        }else if (request.getParameter("show").equals("addPhotoFile")){
            if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("") &&
                request.getParameter("postedBy") != null && !request.getParameter("postedBy").equals("") &&
                request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("") &&
                request.getParameter("parentAlbum") != null && !request.getParameter("parentAlbum").equals("")){
                %>
                <div id="wrapper">
                <div class="content">
                <div class="posted-entry">
                <form action="file_upload.jsp" method="post" enctype="multipart/form-data">
                <h3>Add a new photo to <%=request.getParameter("parentAlbum")%> album</h3>
                <input type="hidden" name="belongsTo" value="<%= userProfile.profileOwner%>">
                <input type="hidden" name="username" value="<%= mainSettings.username %>">
                <input type="hidden" name="filetype" value="photo">
                <input type="hidden" name="parentAlbum" value="<%=request.getParameter("parentAlbum")%>">
                <input type="hidden" name="shareWith" value="<%=request.getParameter("shareWith")%>">
                <table border="0" cellpadding="0" cellspacing="0">
                <tr>
                <td><h2>Caption: </h2></td>
                <td><textarea name="description"></textarea></td>
                </tr>
                <tr>
                <td><h2>Share With: </h2></td><td><%=request.getParameter("shareWith")%></td>
                </tr>
                </table>
                <input type="file" name="myFile"/>
                <input type="submit" value="Add to album"/>
                </form>
                </div>
                </div>
                </div>
                <%
            }
        } else if (request.getParameter("show").equals("addAudioFile")){
            if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("") &&
                request.getParameter("postedBy") != null && !request.getParameter("postedBy").equals("") &&
                request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("") &&
                request.getParameter("parentAlbum") != null && !request.getParameter("parentAlbum").equals("")){
                %>
                <div id="wrapper">
                <div class="content">
                <div class="posted-entry">
                <form action="file_upload.jsp" method="post" enctype="multipart/form-data">
                <h3>Add a new audio track to <%=request.getParameter("parentAlbum")%> album</h3>
                <input type="hidden" name="belongsTo" value="<%= userProfile.profileOwner%>">
                <input type="hidden" name="username" value="<%= mainSettings.username %>">
                <input type="hidden" name="filetype" value="audio">
                <input type="hidden" name="parentAlbum" value="<%=request.getParameter("parentAlbum")%>">
                <input type="hidden" name="shareWith" value="<%=request.getParameter("shareWith")%>">
                <table border="0" cellpadding="0" cellspacing="0">
                <tr>
                <td><h2>Title: </h2></td>
                <td><input type="text" name="title"></td>
                </tr>
                <tr>
                <td><h2>Artist: </h2></td>
                <td><input type="text" name="artist"></td>
                </tr>
                <tr>
                <td><h2>Album: </h2></td>
                <td><input type="text" name="album"></td>
                </tr>
                <tr>
                <td><h2>Caption: </h2></td>
                <td><textarea name="description"></textarea></td>
                </tr>
                <tr>
                <td><h2>Share With: </h2></td><td><%=request.getParameter("shareWith")%></td>
                </tr>
                </table>
                <input type="file" name="myFile"/>
                <input type="submit" value="Add to album"/>
                </form>
                </div>
                </div>
                </div>
                <%
            }
        } else if (request.getParameter("show").equals("addVideoFile")){
            if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("") &&
                request.getParameter("postedBy") != null && !request.getParameter("postedBy").equals("") &&
                request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("") &&
                request.getParameter("parentAlbum") != null && !request.getParameter("parentAlbum").equals("")){
                %>
                <div id="wrapper">
                <div class="content">
                <div class="posted-entry">
                <form action="file_upload.jsp" method="post" enctype="multipart/form-data">
                <h3>Add a new video file to <%=request.getParameter("parentAlbum")%> album</h3>
                <input type="hidden" name="belongsTo" value="<%= userProfile.profileOwner%>">
                <input type="hidden" name="username" value="<%= mainSettings.username %>">
                <input type="hidden" name="filetype" value="video">
                <input type="hidden" name="parentAlbum" value="<%=request.getParameter("parentAlbum")%>">
                <input type="hidden" name="shareWith" value="<%=request.getParameter("shareWith")%>">
                <table border="0" cellpadding="0" cellspacing="0">
                <tr>
                <td><h2>Title: </h2></td>
                <td><input type="text" name="title"></td>
                </tr>
                <tr>
                <td><h2>Caption: </h2></td>
                <td><textarea name="description"></textarea></td>
                </tr>
                <tr>
                <td><h2>Share With: </h2></td><td><%=request.getParameter("shareWith")%></td>
                </tr>
                </table>
                <input type="file" name="myFile"/>
                <input type="submit" value="Add to album"/>
                </form>
                </div>
                </div>
                </div>
                <%
            }
        }
    }
}
%>
<!-- end share box -->
<!-- start of contents --> 
<div id="wrapper">
<%

if(request.getParameter("show") != null){
    if (request.getParameter("show").equals("photos")){
        out.println(userProfile.displayPhotos());
    }else if (request.getParameter("show").equals("videos")){
        out.println(userProfile.displayVideos());
    }else if (request.getParameter("show").equals("audios")){
        out.println(userProfile.displayAudios());
    }else if (request.getParameter("show").equals("video")){
        if (request.getParameter("id") != null){
            if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
                userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                hasInfo = userProfile.loadUserInfo();
                out.println(userProfile.displayVideo(Long.parseLong(request.getParameter("id"))));
            }else{
                userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                hasInfo = userProfile.loadUserInfo();
                out.println(userProfile.displayVideo(Long.parseLong(request.getParameter("id"))));
            }
        }
    }else if (request.getParameter("show").equals("photo")){
        if (request.getParameter("id") != null){
            if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
                userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                hasInfo = userProfile.loadUserInfo();
                out.println(userProfile.displayPhoto(Long.parseLong(request.getParameter("id"))));
            }else{
                userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                hasInfo = userProfile.loadUserInfo();
                out.println(userProfile.displayPhoto(Long.parseLong(request.getParameter("id"))));
            }
        }
    }else if (request.getParameter("show").equals("audio")){
        if (request.getParameter("id") != null){
            if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
                userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                hasInfo = userProfile.loadUserInfo();
                out.println(userProfile.displayAudio(Long.parseLong(request.getParameter("id"))));
            }else{
                userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                hasInfo = userProfile.loadUserInfo();
                out.println(userProfile.displayAudio(Long.parseLong(request.getParameter("id"))));
            }
        }
    }else if (request.getParameter("show").equals("photoAlbum")){
        if (request.getParameter("id") != null){
            if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
                userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                hasInfo = userProfile.loadUserInfo();
                out.println(userProfile.displayPhotoAlbum(Long.parseLong(request.getParameter("id"))));
            }else{
                userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                hasInfo = userProfile.loadUserInfo();
                out.println(userProfile.displayPhotoAlbum(Long.parseLong(request.getParameter("id"))));
            }
        }
    }else if (request.getParameter("show").equals("videoAlbum")){
        if (request.getParameter("id") != null){
            if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
                userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                hasInfo = userProfile.loadUserInfo();
                out.println(userProfile.displayVideoAlbum(Long.parseLong(request.getParameter("id"))));
            }else{
                userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                hasInfo = userProfile.loadUserInfo();
                out.println(userProfile.displayVideoAlbum(Long.parseLong(request.getParameter("id"))));
            }
        }
    }else if (request.getParameter("show").equals("audioAlbum")){
        if (request.getParameter("id") != null){
            if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
                userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                hasInfo = userProfile.loadUserInfo();
                out.println(userProfile.displayAudioAlbum(Long.parseLong(request.getParameter("id"))));
            }else{
                userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                hasInfo = userProfile.loadUserInfo();
                out.println(userProfile.displayAudioAlbum(Long.parseLong(request.getParameter("id"))));
            }
        }
    }else if (request.getParameter("show").equals("link")){
        if (request.getParameter("id") != null){
            if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
                    userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                    hasInfo = userProfile.loadUserInfo();
                    out.println(userProfile.displayLink(Long.parseLong(request.getParameter("id")), request.getParameter("shareWith")));
                }else{
                    userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                    hasInfo = userProfile.loadUserInfo();
                    out.println(userProfile.displayLink(Long.parseLong(request.getParameter("id")), request.getParameter("shareWith")));
                }
            }
        }
    }else if (request.getParameter("show").equals("wallPost")){
        if (request.getParameter("id") != null){
            if (request.getParameter("shareWith") != null && !request.getParameter("shareWith").equals("")){
                if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
                    userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                    hasInfo = userProfile.loadUserInfo();
                    out.println(userProfile.displayWallPost(Long.parseLong(request.getParameter("id")), request.getParameter("shareWith")));
                }else{
                    userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                    hasInfo = userProfile.loadUserInfo();
                    out.println(userProfile.displayWallPost(Long.parseLong(request.getParameter("id")), request.getParameter("shareWith")));
                }
            }
        }
    }else if (request.getParameter("show").equals("selectProfilePicture")){
        %>
        <div class="content">
        <form action="file_upload.jsp" method="post" enctype="multipart/form-data">
        <input type="hidden" name="belongsTo" value="<%= userProfile.profileOwner%>">
        <input type="hidden" name="username" value="<%= mainSettings.username %>">
        <input type="hidden" name="filetype" value="photo">
        <input type="hidden" name="parentAlbum" value="Profile Pictures">
        <input type="hidden" name="calledFrom" value="info">
        <input type="hidden" name="shareWith" value="All">
        <table border="0" cellpadding="0" cellspacing="0">
        <tr>
        <td><h3>Browse for profile picture: </h3></td>
            <td><input type="file" name="myFile"/></input></td>
            </tr>
            <tr>
            <td><input type="submit" value="Upload"/></input></td>
            </tr>
            </table>
            </form>
            </div>
            <%
            
            }else if (request.getParameter("show").equals("info")){
                if (request.getParameter("profileOwner") != null && !request.getParameter("profileOwner").equals("")){
                    userProfile = new profile(request.getParameter("profileOwner"), mainSettings.username, "./MyZone/");
                    hasInfo = userProfile.loadUserInfo();
                    boolean editable = false;
                    if (userProfile.profileOwner.equals(mainSettings.username)){
                        editable = true;
                    }else{
                        editable = false;
                    }
                    if (editable){
                        %>
                        <div class="content">
                        <form method="post" action="profile.jsp" class="info">
                        <input type="hidden" name="op" value="saveUserInfo">
                        <table border="0" cellpadding="0" cellspacing="0">
                        <tr><td><h1>Basic Information</h1></td></tr>
                        <tr>
                        <td>Your username:</td>
                        <td><%=mainSettings.username%></td>
                        </tr>
                        <td>&nbsp;</td>
                        <tr>
                        <td>First Name: </td>
                        <td>
                        <% 
                        if (userProfile.userInfo.getFirstName() != null && !userProfile.userInfo.getFirstName().equals("")){ 
                            out.println(userProfile.userInfo.getFirstName());
                        }else{
                            %>
                            <input type="text" size="20" name="firstname" />
                            <% 
                        }
                        %>
                        </td>
                        </tr>
                        <td>&nbsp;</td>
                        <tr>
                        <td>Last Name: </td>
                        <td>
                        <% 
                        if (userProfile.userInfo.getLastName() != null && !userProfile.userInfo.getLastName().equals("")){ 
                            out.println(userProfile.userInfo.getLastName());
                        }else{
                            %>
                            <input type="text" size="20" name="lastname" />
                            <% 
                        }
                        %>
                        </td>
                        </tr>
                        <td>&nbsp;</td>
                        <tr>
                    <td>Gender: </td>
                        <td><select name="gender">
                        <option <% if (userProfile.userInfo.getGender() != null && userProfile.userInfo.getGender().equals("")){  %> selected="selected" <% } %> value=""></option>
                        <option <% if (userProfile.userInfo.getGender() != null && userProfile.userInfo.getGender().equals("Female")){  %> selected="selected" <% } %> value="Female">Female</option>
                        <option <% if (userProfile.userInfo.getGender() != null && userProfile.userInfo.getGender().equals("Male")){  %> selected="selected" <% } %> value="Male">Male</option>
                        </select>
                        </td>
                        </tr>
                        <td>&nbsp;</td>
                        <tr>
                        <td>Date of Birth: (Month/DD/YYYY)</td>
                        <td><select name="month">
                        <option <% if (userProfile.userInfo.getDateOfBirth() != null && userProfile.userInfo.getDateOfBirth().getMonth("completeAlphabetical").equals("")){  %> selected="selected" <% } %> value="0"></option>
                        <option <% if (userProfile.userInfo.getDateOfBirth() != null && userProfile.userInfo.getDateOfBirth().getMonth("completeAlphabetical").equals("January")){  %> selected="selected" <% } %> value="1">January</option>
                        <option <% if (userProfile.userInfo.getDateOfBirth() != null && userProfile.userInfo.getDateOfBirth().getMonth("completeAlphabetical").equals("February")){  %> selected="selected" <% } %> value="2">February</option>
                        <option <% if (userProfile.userInfo.getDateOfBirth() != null && userProfile.userInfo.getDateOfBirth().getMonth("completeAlphabetical").equals("March")){  %> selected="selected" <% } %> value="3">March</option>
                        <option <% if (userProfile.userInfo.getDateOfBirth() != null && userProfile.userInfo.getDateOfBirth().getMonth("completeAlphabetical").equals("April")){  %> selected="selected" <% } %> value="4">April</option>
                        <option <% if (userProfile.userInfo.getDateOfBirth() != null && userProfile.userInfo.getDateOfBirth().getMonth("completeAlphabetical").equals("May")){  %> selected="selected" <% } %> value="5">May</option>
                        <option <% if (userProfile.userInfo.getDateOfBirth() != null && userProfile.userInfo.getDateOfBirth().getMonth("completeAlphabetical").equals("June")){  %> selected="selected" <% } %> value="6">June</option>
                        <option <% if (userProfile.userInfo.getDateOfBirth() != null && userProfile.userInfo.getDateOfBirth().getMonth("completeAlphabetical").equals("July")){  %> selected="selected" <% } %> value="7">July</option>
                        <option <% if (userProfile.userInfo.getDateOfBirth() != null && userProfile.userInfo.getDateOfBirth().getMonth("completeAlphabetical").equals("August")){  %> selected="selected" <% } %> value="8">August</option>
                        <option <% if (userProfile.userInfo.getDateOfBirth() != null && userProfile.userInfo.getDateOfBirth().getMonth("completeAlphabetical").equals("September")){  %> selected="selected" <% } %> value="9">September</option>
                        <option <% if (userProfile.userInfo.getDateOfBirth() != null && userProfile.userInfo.getDateOfBirth().getMonth("completeAlphabetical").equals("October")){  %> selected="selected" <% } %> value="10">October</option>
                        <option <% if (userProfile.userInfo.getDateOfBirth() != null && userProfile.userInfo.getDateOfBirth().getMonth("completeAlphabetical").equals("November")){  %> selected="selected" <% } %> value="11">November</option>
                        <option <% if (userProfile.userInfo.getDateOfBirth() != null && userProfile.userInfo.getDateOfBirth().getMonth("completeAlphabetical").equals("December")){  %> selected="selected" <% } %> value="12">December</option>
                        </select><h3>&nbsp;&nbsp;</h3>
                        <input type="text" size="2" name="day"
                        <% if (userProfile.userInfo.getDateOfBirth() != null && userProfile.userInfo.getDateOfBirth().getDay() > 0){  %> value='<%=userProfile.userInfo.getDateOfBirth().getDay()%>' <% } %>/>
                        <h3>&nbsp;&nbsp;</h3>
                        <input type="text" size="4" name="year"
                        <% if (userProfile.userInfo.getDateOfBirth() != null && userProfile.userInfo.getDateOfBirth().getYear() > 0){  %> value='<%=userProfile.userInfo.getDateOfBirth().getYear()%>' <% } %>/>
                        </td>
                        </tr>
                        <td>&nbsp;</td>
                        <tr>
                        <td>Relationship Status: </td>
                        <td><input type="text" size="20" name="relationshipStatus" value="<% if (userProfile.userInfo.getRelationshipStatus() != null){ out.println(userProfile.userInfo.getRelationshipStatus()); } %>" />
                        </td>
                        </tr>
                        <td>&nbsp;</td>
                        <tr>
                        <td>About Me:</td>
                        <td><textarea name="aboutMe" ><% if (userProfile.userInfo.getAboutMe() != null){ out.println(userProfile.userInfo.getAboutMe()); } %></textarea></td>
                        </tr>
                        <tr>
                        <td>
                        <% 
                        if (userProfile.userInfo.getFirstName() != null && !userProfile.userInfo.getFirstName().equals("") && 
                            userProfile.userInfo.getLastName() != null && !userProfile.userInfo.getLastName().equals("")){
                            %>
                            <a href="profile.jsp?profileOwner=<%=userProfile.profileOwner%>&show=selectProfilePicture"><h2>Select profile picture</h2></a>
                            <input type="hidden" name="profilePic" value="<%=request.getParameter("profilePic")%>">
                            <%
                        }
                        %>
                        </td>
                        <td><%
                        if (request.getParameter("profilePic") != null && !request.getParameter("profilePic").equals("")){
                            %>
                            <p><%=request.getParameter("profilePic")%> Selected.</p>
                            <%
                        }
                        %></td>
                        </tr>
                        <tr><td><h1>Education History: <h1></td><td><a href="profile.jsp?profileOwner=<%=userProfile.profileOwner%>&show=info&new=education">Add Education Entry</a></td></tr>
                        <%
                        if (request.getParameter("new") != null && request.getParameter("new").equals("education")){
                            %>
                            <tr><td><h2>New Entry</h2></td></tr>
                            <tr>
                        <td>Institution:</td>
                            <td><textarea name="institutionEd<%=userProfile.userInfo.getEducations().size()%>" ></textarea></td>
                            </tr>
                            <tr>
                        <td>Level:</td>
                            <td><textarea name="level<%=userProfile.userInfo.getEducations().size()%>"></textarea></td>
                            </tr>
                        <td>Degree:</td>
                            <td><textarea name="degree<%=userProfile.userInfo.getEducations().size()%>" /></textarea></td>
                            </tr>
                        <td>Major:</td>
                            <td><textarea name="major<%=userProfile.userInfo.getEducations().size()%>" /></textarea></td>
                            </tr>
                        <td>Duration:</td>
                            <td><input type="text" size="5" name="sinceEducation<%=userProfile.userInfo.getEducations().size()%>" /><p> &mdash; </p>
                            <input type="text" size="5" name="toEducation<%=userProfile.userInfo.getEducations().size()%>" /></td>
                            </tr>
                            <td>&nbsp;</td>
                            <%
                        }
                        for (int i = 0; i < userProfile.userInfo.getEducations().size(); i++){
                            education ed = (education)userProfile.userInfo.getEducations().get(i);
                            %>
                            <tr><td><h2>Entry <%=(i+1)%></h2></td><td>
                            <input type="checkbox" name="deleteEd<%=i%>" value="true" /> Delete this entry
                            </td></tr>
                            <tr>
                        <td>Institution:</td>
                            <td><textarea name="institutionEd<%=i%>"><% if (ed.getInstitution() != null){  out.println(ed.getInstitution()); } %></textarea></td>
                            </tr>
                            <tr>
                        <td>Level:</td>
                            <td><textarea name="level<%=i%>"><% if (ed.getLevel() != null){  out.println(ed.getLevel()); } %></textarea></td>
                            </tr>
                        <td>Degree:</td>
                            <td><textarea name="degree<%=i%>"><% if (ed.getDegree() != null){  out.println(ed.getDegree());} %></textarea></td>
                            </tr>
                        <td>Major:</td>
                            <td><textarea name="major<%=i%>"><% if (ed.getMajor() != null){ out.println(ed.getMajor()); } %></textarea></td>
                            </tr>
                        <td>Duration:</td>
                            <td><input type="text" size="5" name="sinceEducation<%=i%>" <% if (ed.getStartYear() != null){  %> value='<%=ed.getStartYear()%>' <% } %>/><p> &mdash; </p>
                            <input type="text" size="5" name="toEducation<%=i%>" <% if (ed.getFinishYear() != null){  %> value='<%=ed.getFinishYear()%>' <% } %>/></td>
                            </tr>
                            <td>&nbsp;</td>
                            <%
                        }
                        %>
                        
                        <tr><td><h1>Employment History: <h1></td><td><a href="profile.jsp?profileOwner=<%=userProfile.profileOwner%>&show=info&new=employment">Add Employment Entry</a></td></tr>
                        <%
                        if (request.getParameter("new") != null && request.getParameter("new").equals("employment")){
                            %>
                            <tr><td><h2>New Entry</h2></td></tr>
                            <tr>
                        <td>Institution:</td>
                            <td><textarea name="institutionEm<%=userProfile.userInfo.getEmployments().size()%>"></textarea></td>
                            </tr>
                            <tr>
                        <td>Position:</td>
                            <td><textarea name="position<%=userProfile.userInfo.getEmployments().size()%>" ></textarea></td>
                            </tr>
                        <td>Duration:</td>
                            <td><input type="text" size="5" name="sinceEmployment<%=userProfile.userInfo.getEmployments().size()%>" /><p> &mdash; </p>
                            <input type="text" size="5" name="toEmployment<%=userProfile.userInfo.getEmployments().size()%>" /></td>
                            </tr>
                            <td>&nbsp;</td>
                            <%
                        }
                        for (int i = 0; i < userProfile.userInfo.getEmployments().size(); i++){
                            employment em = (employment)userProfile.userInfo.getEmployments().get(i);
                            %>
                            <tr><td><h2>Entry <%=(i+1)%></h2></td><td><input type="checkbox" name="deleteEm<%=i%>" value="true" /> Delete this entry</td></tr>
                            <tr>
                        <td>Institution:</td>
                            <td><textarea name="institutionEm<%=i%>"><% if (em.getInstitution() != null){ out.println(em.getInstitution()); } %></textarea></td>
                            </tr>
                            <tr>
                        <td>Position:</td>
                            <td><textarea name="position<%=i%>"><% if (em.getPosition() != null){ out.println(em.getPosition());} %></textarea></td>
                            </tr>
                        <td>Duration:</td>
                            <td><input type="text" size="5" name="sinceEmployment<%=i%>" <% if (em.getStartYear() != null){  %> value='<%=em.getStartYear()%>' <% } %>/><p> &mdash; </p>
                            <input type="text" size="5" name="toEmployment<%=i%>" <% if (em.getFinishYear() != null){  %> value='<%=em.getFinishYear()%>' <% } %>/></td>
                            </tr>
                            <td>&nbsp;</td>
                            <%
                        }
                        %>
                        </tr>
                        <tr><td>&nbsp;</td><td><input type="submit" value="Update"/></input></td></tr>
                        </table>
                        </form>
                        </div>
                        <%
                    }
                }
            }
}else{
    
    if (request.getParameter("numberOfEntries") != null && !request.getParameter("numberOfEntries").equals("")){
        
        out.println(userProfile.displayWall(Integer.valueOf(request.getParameter("numberOfEntries"))));
        
        out.println("<div class=\"content\">\n<div class=\"header\">\n<div class=\"posted-entry\">\n<a href=\"profile.jsp?profileOwner=" + userProfile.profileOwner + "&numberOfEntries=" + (Integer.valueOf(request.getParameter("numberOfEntries")) + 20) + "\">Display older posts</a>\n</div>\n</div>\n</div>");
        
    }else{
        
        out.println(userProfile.displayWall(20));
        
        out.println("<div class=\"content\">\n<div class=\"header\">\n<div class=\"posted-entry\">\n<a href=\"profile.jsp?profileOwner=" + userProfile.profileOwner + "&numberOfEntries=40" + "\">Display older posts</a>\n</div>\n</div>\n</div>");
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