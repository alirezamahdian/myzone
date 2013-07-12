<%@ page import="java.util.*, MyZone.*, MyZone.elements.*" %>
<%@ page import="java.io.*" %>
<%@ page import="org.apache.commons.fileupload.*, org.apache.commons.fileupload.servlet.ServletFileUpload, org.apache.commons.fileupload.disk.DiskFileItemFactory, org.apache.commons.io.FilenameUtils, java.util.*, java.io.File, java.lang.Exception" %>
<%
if (ServletFileUpload.isMultipartContent(request)){
    ServletFileUpload servletFileUpload = new ServletFileUpload(new DiskFileItemFactory());
    List fileItemsList = servletFileUpload.parseRequest(request);
    FileItem fileItem = null;
    String filetype = "";
    String thumbnail = "";
    String shareWith = "";
    String belongsTo = "";
    String username = "";
    String description = "";
    String artist = "";
    String album = "";
    String title = "";
    String fileName = "";
    long fileSize = 0;
    String dirName = "";
    String parentAlbum = "Wall";
    String calledFrom = "";
    Iterator it = fileItemsList.iterator();
    Settings mainSettings = new Settings("./MyZone/");
    mainSettings.refresh(mainSettings.ALL);
    while (it.hasNext()){
        FileItem fileItemTemp = (FileItem)it.next();
        if (fileItemTemp.isFormField()) {
            String name = fileItemTemp.getFieldName();
            if (name.equalsIgnoreCase("calledFrom")) {
                calledFrom = fileItemTemp.getString();
            }
            if (name.equalsIgnoreCase("filetype")) {
                filetype = fileItemTemp.getString();
            }
            if (name.equalsIgnoreCase("parentAlbum")) {
                parentAlbum = fileItemTemp.getString();
            }
            if (name.equalsIgnoreCase("shareWith")) {
                shareWith = fileItemTemp.getString();
            }
            if (name.equalsIgnoreCase("belongsTo")) {
                belongsTo = fileItemTemp.getString();
            }
            if (name.equalsIgnoreCase("username")) {
                username = fileItemTemp.getString();
            }
            if (name.equalsIgnoreCase("description") && fileItemTemp.getString() != null && !fileItemTemp.getString().equals("")) {
                description = fileItemTemp.getString();
            }
            if (name.equalsIgnoreCase("artist") && fileItemTemp.getString() != null && !fileItemTemp.getString().equals("")) {
                artist = fileItemTemp.getString();
            }
            if (name.equalsIgnoreCase("album") && fileItemTemp.getString() != null && !fileItemTemp.getString().equals("")) {
                album = fileItemTemp.getString();
            }
            if (name.equalsIgnoreCase("title") && fileItemTemp.getString() != null && !fileItemTemp.getString().equals("")) {
                title = fileItemTemp.getString();
            }
        }else{
            fileItem = fileItemTemp;
        }
        if (fileItem != null){
            if ((filetype.equals("photo") && fileItem.getContentType().contains("image")) ||
                (filetype.equals("video") && fileItem.getContentType().contains("video")) ||
                (filetype.equals("audio") && fileItem.getContentType().contains("audio")) ||
                filetype.equals("CAKey") || filetype.equals("MyCert")){
                fileName = fileItem.getName();
                fileSize = fileItem.getSize();
                if (!fileName.equals("")){
                    if (fileItem.getSize() > 0){
                        fileName = FilenameUtils.getName(fileName);
                        if (!filetype.equals("")){
                            if (filetype.equals("CAKey") && 
                                mainSettings.username != null && !mainSettings.username.equals("")){
                                mainSettings.CAServerName = fileName.substring(fileName.lastIndexOf("@") + 1, fileName.lastIndexOf("."));
                                File saveTo = new File("./MyZone/" + "/CAs/" + fileName);
                                try {
                                    fileItem.write(saveTo);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                                mainSettings.save(mainSettings.BASIC_INFO);
                            }else if (filetype.equals("MyCert") && 
                                      mainSettings.username != null && !mainSettings.username.equals("")){
                                File saveTo = new File("./MyZone/" + mainSettings.username + "/cert/" + mainSettings.username + ".cert");
                                try {
                                    fileItem.write(saveTo);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                            if (!shareWith.equals("") 
                                && !belongsTo.equals("")
                                && !username.equals("")){
                                fileName = String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()) + "." + fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
                                if (belongsTo.equals(username)){
                                    if (filetype.equals("photo")){
                                        dirName = username + "/photos/";
                                    }
                                    if (filetype.equals("audio")){
                                        dirName = username + "/audios/";
                                    }
                                    if (filetype.equals("video")){
                                        dirName = username + "/videos/";
                                    }
                                }else{
                                    if (filetype.equals("photo")){
                                        dirName = username + "/friends/" + belongsTo + "/photos/";
                                    }
                                    if (filetype.equals("audio")){
                                        dirName = username + "/friends/" + belongsTo + "/audios/";
                                    }
                                    if (filetype.equals("video")){
                                        dirName = username + "/friends/" + belongsTo + "/videos/";
                                    }
                                }
                                profile userProfile = new profile(belongsTo, mainSettings.username, "./MyZone/");
                                boolean hasInfo = userProfile.loadUserInfo();
                                if (hasInfo){
                                    out.println("hasInfo = " + hasInfo);
                                    File saveTo = new File("./MyZone/" + dirName + fileName);
                                    try {
                                        fileItem.write(saveTo);
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
                                    userProfile.loadUserInfo();
                                    fileListEntry fle = new fileListEntry(belongsTo, shareWith, "./MyZone/" + dirName, fileName, fileSize);
                                    userProfile.updateImage("correctImage", "add", fle);
                                    userProfile.updateImage("existingImage", "add", fle);
                                    if (!dirName.contains("/friends/")){
                                        userProfile.updateModifiedFiles("add", Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "./MyZone/" + dirName + fileName, shareWith);
                                    }
                                    if (calledFrom.equals("info")){
                                        thumbnail = username + ".jpg";
                                        imageUtils iu = new imageUtils(saveTo);
                                        iu.resizeImageWithHint("./MyZone/" + username + "/thumbnails/" + thumbnail, 80, 80);
                                        userProfile.updateModifiedFiles("add", Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "./MyZone/" + username + "/thumbnails/" + thumbnail, "public");
                                    }
                                    user u;
                                    user x = new user();
                                    u = new user(userProfile.profileOwner, userProfile.userInfo.getFirstName(), userProfile.userInfo.getLastName(), 0); 
                                    if (parentAlbum.equals("Wall")){
                                        if (belongsTo.equals(mainSettings.username)){
                                            x = u;
                                        }else{
                                            for (int i = 0; i < mainSettings.friends.size(); i++){
                                                friend f = (friend)mainSettings.friends.get(i);
                                                if (f.getUser().getUsername().equals(belongsTo)){
                                                    x = f.getUser();
                                                    break;
                                                }
                                            }
                                        }
                                    }else{
                                        x = u;
                                    }
                                    
                                    userProfile = new profile(belongsTo, mainSettings.username, "./MyZone/");
                                    userProfile.loadUserInfo();
                                    if (filetype.equals("audio")){
                                        userProfile.loadAudios();
                                        audioAlbum aa = new audioAlbum();
                                        int i = 0;
                                        for (i = 0; i < userProfile.audioAlbums.size(); i++){
                                            aa = (audioAlbum)userProfile.audioAlbums.get(i);
                                            if (aa.getTitle().equals(parentAlbum)){
                                                break;
                                            }
                                        }
                                        // album not found. create the album.
                                        if (i == userProfile.audioAlbums.size()){
                                            aa = new audioAlbum(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(),
                                                                Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(),
                                                                belongsTo,
                                                                x,
                                                                parentAlbum,
                                                                "All",
                                                                null,
                                                                null,
                                                                null,
                                                                null);
                                            userProfile.addAudioAlbum(aa, false);
                                            audio a = new audio(aa.getTitle(),
                                                                aa.getId(),
                                                                (Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() + 1),
                                                                (Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() + 1), 
                                                                belongsTo,
                                                                u,
                                                                title,
                                                                artist,
                                                                album,
                                                                fileName,
                                                                description,
                                                                shareWith,
                                                                null,
                                                                null,
                                                                null
                                                                );
                                            userProfile.addAudio(a, false);
                                        }else{
                                            audio a = new audio(aa.getTitle(),
                                                                aa.getId(),
                                                                Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(),
                                                                Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), 
                                                                belongsTo,
                                                                u,
                                                                title,
                                                                artist,
                                                                album,
                                                                fileName,
                                                                description,
                                                                shareWith,
                                                                null,
                                                                null,
                                                                null
                                                                );
                                            userProfile.addAudio(a, false);
                                        }
                                    }
                                    if (filetype.equals("video")){
                                        userProfile.loadVideos();
                                        videoAlbum va = new videoAlbum();
                                        int i = 0;
                                        for (i = 0; i < userProfile.videoAlbums.size(); i++){
                                            va = (videoAlbum)userProfile.videoAlbums.get(i);
                                            if (va.getTitle().equals(parentAlbum)){
                                                break;
                                            }
                                        }
                                        if (i == userProfile.videoAlbums.size()){
                                            va = new videoAlbum(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(),
                                                                Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(),
                                                                belongsTo,
                                                                x,
                                                                parentAlbum,
                                                                "All",
                                                                null,
                                                                null,
                                                                null,
                                                                null);
                                            userProfile.addVideoAlbum(va, false);
                                            video v = new video(va.getTitle(),
                                                                va.getId(),
                                                                (Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() + 1),
                                                                (Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() + 1), 
                                                                belongsTo,
                                                                u,
                                                                title,
                                                                fileName,
                                                                description,
                                                                shareWith,
                                                                null,
                                                                null,
                                                                null
                                                                );
                                            userProfile.addVideo(v, false);
                                            
                                        }else{
                                            video v = new video(va.getTitle(),
                                                                va.getId(),
                                                                Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(),
                                                                Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), 
                                                                belongsTo,
                                                                u,
                                                                title,
                                                                fileName,
                                                                description,
                                                                shareWith,
                                                                null,
                                                                null,
                                                                null
                                                                );
                                            userProfile.addVideo(v, false);
                                        }
                                    }
                                    if (filetype.equals("photo")){
                                        userProfile.loadPhotos();
                                        photoAlbum pa = new photoAlbum();
                                        int i = 0;
                                        for (i = 0; i < userProfile.photoAlbums.size(); i++){
                                            pa = (photoAlbum)userProfile.photoAlbums.get(i);
                                            if (pa.getTitle().equals(parentAlbum)){
                                                break;
                                            }
                                        }
                                        if (i == userProfile.photoAlbums.size()){
                                            pa = new photoAlbum(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(),
                                                                Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(),
                                                                belongsTo,
                                                                x,
                                                                parentAlbum,
                                                                "All",
                                                                null,
                                                                null,
                                                                null,
                                                                null);
                                            userProfile.addPhotoAlbum(pa, false);
                                            photo p = new photo(pa.getTitle(),
                                                                pa.getId(),
                                                                (Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() + 1),
                                                                (Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() + 1), 
                                                                belongsTo,
                                                                u,
                                                                fileName,
                                                                description,
                                                                shareWith,
                                                                null,
                                                                null,
                                                                null
                                                                );
                                            userProfile.addPhoto(p, false);
                                        }else{
                                            photo p = new photo(pa.getTitle(),
                                                                pa.getId(),
                                                                Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(),
                                                                Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), 
                                                                belongsTo,
                                                                u,
                                                                fileName,
                                                                description,
                                                                shareWith,
                                                                null,
                                                                null,
                                                                null
                                                                );
                                            userProfile.addPhoto(p, false);
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
    String redirectURL;
    
    if (calledFrom.equals("info")){
        redirectURL = "profile.jsp?profileOwner=" + belongsTo + "&show=info&profilePic=" + fileName;
    }else if (calledFrom.equals("settings")){
        redirectURL = "settings.jsp?settings=basic";
    }else{
        if (!parentAlbum.equals("Wall") && !parentAlbum.equals("Profile Pictures")){
            redirectURL = "profile.jsp?profileOwner=" + belongsTo + "&show=" + filetype + "s";
        }else{
            redirectURL = "profile.jsp?profileOwner=" + belongsTo;
        }
    }
    response.sendRedirect(redirectURL);
}
%>