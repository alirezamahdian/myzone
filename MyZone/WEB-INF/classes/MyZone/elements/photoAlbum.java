/* 
 =========================================================================
 |Copyright (C) 2012  Alireza Mahdian email:alireza.mahdian@colorado.edu |
 |This program is free software: you can redistribute it but NOT modify  |
 |it under the terms of the GNU General Public License as published by   |
 |the Free Software Foundation, either version 3 of the License, or      |
 |(at your option) any later version. Alireza Mahdian reserves all the   |
 |commit rights of this code.                                            |
 |                                                                       |
 |This program is distributed in the hope that it will be useful,        |
 |but WITHOUT ANY WARRANTY; without even the implied warranty of         |
 |MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          |
 |GNU General Public License for more details.                           |
 |                                                                       |
 |You should have received a copy of the GNU General Public License      |
 |along with this program.  If not, see <http://www.gnu.org/licenses/>.  |
 =========================================================================
 */

package MyZone.elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Calendar;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Text;

import MyZone.*;
import MyZone.elements.comment;
import MyZone.elements.like;
import MyZone.elements.dislike;
import MyZone.elements.photo;

/*
 This file contains the implementation of the photoAlbum element of a profile.
 Please refer to photos.xsd for detailed description of its elements and
 attributes.
 */

public class photoAlbum {
    
    private final static boolean DEBUG = false;
    private long id;
    private long lastUpdateTime;
	private String title;
	private String shareWith;
    private user postedBy;
    private String belongsTo;
    private Settings mainSettings;
    
    private List<comment> comments = new ArrayList();    
    private List<like> likes = new ArrayList();
    private List<dislike> dislikes = new ArrayList();
    private List<photo> photoFiles = new ArrayList();
	
    private String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
            if (el.getFirstChild() != null)
                textVal = el.getFirstChild().getNodeValue();
		}
		return textVal;
	}
    
	public photoAlbum(long id, 
                      long lastUpdateTime,
                      String belongsTo,
                      user postedBy,
                      String title, 
                      String shareWith,
                      List<photo> photoFiles,
                      List<like> likes,
                      List<dislike> dislikes,
                      List<comment> comments
                      )
    {
        mainSettings = new Settings("./MyZone/");
        mainSettings.refresh(mainSettings.BASIC_INFO);
        if (mainSettings.username == null){
            mainSettings = new Settings("../../");
            mainSettings.refresh(mainSettings.BASIC_INFO);
        }
        this.id = id;
        this.lastUpdateTime = lastUpdateTime;
        this.belongsTo = belongsTo;
        this.postedBy = new user();
        if (postedBy != null){
            this.postedBy.setUsername(postedBy.getUsername());
            this.postedBy.setFirstName(postedBy.getFirstName());
            this.postedBy.setLastName(postedBy.getLastName());
        }
        this.title = title;
        this.shareWith = shareWith;
        if (photoFiles != null){
            this.photoFiles.clear();
            for (int i = 0; i < photoFiles.size(); i++){
                photo p = new photo(photoFiles.get(i));
                this.photoFiles.add(p);
            }
        }
        if (likes != null){
            this.likes.clear();
            for (int i = 0; i < likes.size(); i++){
                like li = new like(likes.get(i));
                this.likes.add(li);
            }
        }
        if (dislikes != null){
            this.dislikes.clear();
            for (int i = 0; i < dislikes.size(); i++){
                dislike dli = new dislike(dislikes.get(i));
                this.dislikes.add(dli);
            }
        }
        if (comments != null){
            this.comments.clear();
            for (int i = 0; i < comments.size(); i++){
                comment c = new comment(comments.get(i));
                this.comments.add(c);
            }
        }
    }
	
    public photoAlbum(){
        mainSettings = new Settings("./MyZone/");
        mainSettings.refresh(mainSettings.BASIC_INFO);
        if (mainSettings.username == null){
            mainSettings = new Settings("../../");
            mainSettings.refresh(mainSettings.BASIC_INFO);
        }
        postedBy = new user();
    }
    
    public photoAlbum(photoAlbum orig){
        mainSettings = new Settings("./MyZone/");
        mainSettings.refresh(mainSettings.BASIC_INFO);
        if (mainSettings.username == null){
            mainSettings = new Settings("../../");
            mainSettings.refresh(mainSettings.BASIC_INFO);
        }
        this.id = orig.getId();
        this.lastUpdateTime = orig.getLastUpdateTime();
        this.belongsTo = orig.getBelongsTo();
        this.postedBy = new user();
        if (orig.getPostedBy() != null){
            this.postedBy.setUsername(orig.getPostedBy().getUsername());
            this.postedBy.setFirstName(orig.getPostedBy().getFirstName());
            this.postedBy.setLastName(orig.getPostedBy().getLastName());
        }
        this.title = orig.getTitle();
        this.shareWith = orig.getShareWith();
        if (orig.getPhotoFiles() != null){
            this.photoFiles.clear();
            List<photo> tempPhotoFiles = orig.getPhotoFiles();
            for (int i = 0; i < tempPhotoFiles.size(); i++){
                photo p = new photo(tempPhotoFiles.get(i));
                this.photoFiles.add(p);
            }
        }
        if (orig.getLikes() != null){
            this.likes.clear();
            List<like> tempLikes = orig.getLikes();
            for (int i = 0; i < tempLikes.size(); i++){
                like li = new like(tempLikes.get(i));
                this.likes.add(li);
            }
        }
        if (orig.getDislikes() != null){
            this.dislikes.clear();
            List<dislike> tempDislikes = orig.getDislikes();
            for (int i = 0; i < tempDislikes.size(); i++){
                dislike dli = new dislike(tempDislikes.get(i));
                this.dislikes.add(dli);
            }
        }
        if (orig.getComments() != null){
            this.comments.clear();
            List<comment> tempComments = orig.getComments();
            for (int i = 0; i < tempComments.size(); i++){
                comment c = new comment(tempComments.get(i));
                this.comments.add(c);
            }
        }
    }
	
    public long getId(){
        return id;
    }
    
    public void setId(long id){
        this.id = id;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public long getLastUpdateTime(){
        return lastUpdateTime;
    }
    
    public void setLastUpdateTime(long lastUpdateTime){
        this.lastUpdateTime = lastUpdateTime;
    }
    
    public String getBelongsTo(){
        return belongsTo;
    }
    
    public void setBelongsTo(String belongsTo){
        this.belongsTo = belongsTo;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public void setPostedBy(user postedBy){
        this.postedBy.setUsername(postedBy.getUsername());
        this.postedBy.setFirstName(postedBy.getFirstName());
        this.postedBy.setLastName(postedBy.getLastName());
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public user getPostedBy(){
        return postedBy;
    }
    
    public String getTitle(){
        return title;
    }
    
    public void setTitle(String title){
        this.title = title;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public String getShareWith(){
        return shareWith;
    }
    
    public void setShareWith(String shareWith){
        this.shareWith = shareWith;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public List getPhotoFiles(){
        return photoFiles;
    }
    
    public void setPhotos(List<photo> photoFiles){
        if (photoFiles != null){
            this.photoFiles.clear();
            for (int i = 0; i < photoFiles.size(); i++){
                photo p = new photo(photoFiles.get(i));
                this.photoFiles.add(p);
            }
        }
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public void addPhoto(photo p){
        for (int i = 0; i < photoFiles.size(); i++){
            photo pFile = (photo)photoFiles.get(i);
            if (p.getId() >= pFile.getId()){
                if (p.getId() == pFile.getId()){
                    if (p.getLastUpdateTime() <= pFile.getLastUpdateTime()){
                        return;
                    }else{
                        photoFiles.remove(i);
                    }
                }
                photoFiles.add(i, p);
                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                lastUpdateTime = now.getTimeInMillis();
                return;
            }
		}
        photoFiles.add(p);
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public boolean removePhoto(long id){
        for (int i = 0; i < photoFiles.size(); i++){
            photo p = (photo)photoFiles.get(i);
            if (p.getId() == id){
                photoFiles.remove(i);
                try{
                    File f = new File(p.getPath() + p.getFilename());
                    if (f.exists() && f.canWrite() && !f.isDirectory()){
                        f.delete();
                    }
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                lastUpdateTime = now.getTimeInMillis();
                return true;
            }
		}
        return false;
    }
    
    public photo getPhoto(long id){
        for (int i = 0; i < photoFiles.size(); i++){
            photo p = (photo)photoFiles.get(i);
            if (p.getId() == id){
                return p;
            }
		}
        return null;
    }
    
    public List getDislikes(){
        return dislikes;
    }
    
    public void setDislikes(List<dislike> dislikes){
        if (dislikes != null){
            this.dislikes.clear();
            for (int i = 0; i < dislikes.size(); i++){
                dislike dli = new dislike(dislikes.get(i));
                this.dislikes.add(dli);
            }
        }
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public void addDislike(dislike d){
        for (int i = 0; i < dislikes.size(); i++){
            dislike dl = (dislike)dislikes.get(i);
            if (dl.getUser().getUsername().equals(d.getUser().getUsername())){
                return;
            }
        }
        for (int i = 0; i < dislikes.size(); i++){
            dislike dl = (dislike)dislikes.get(i);
            if (d.getId() < dl.getId()){
                dislikes.add(i, d);
                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                lastUpdateTime = now.getTimeInMillis();
                return;
            }
		}
        dislikes.add(d);
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public boolean removeDislike(long id){
        for (int i = 0; i < dislikes.size(); i++){
            dislike d = (dislike)dislikes.get(i);
            if (d.getId() == id){
                dislikes.remove(i);
                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                lastUpdateTime = now.getTimeInMillis();
                return true;
            }
		}
        return false;
    }
    
    public List getLikes(){
        return likes;
    }
    
    public void setLikes(List<like> likes){
        if (likes != null){
            this.likes.clear();
            for (int i = 0; i < likes.size(); i++){
                like li = new like(likes.get(i));
                this.likes.add(li);
            }
        }
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public void addlike(like l){
        for (int i = 0; i < likes.size(); i++){
            like ll = (like)likes.get(i);
            if (ll.getUser().getUsername().equals(l.getUser().getUsername())){
                return;
            }
        }
        for (int i = 0; i < likes.size(); i++){
            like ll = (like)likes.get(i);
            if (l.getId() < ll.getId()){
                likes.add(i, l);
                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                lastUpdateTime = now.getTimeInMillis();
                return;
            }
		}
        likes.add(l);
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public boolean removelike(long id){
        for (int i = 0; i < likes.size(); i++){
            like l = (like)likes.get(i);
            if (l.getId() == id){
                likes.remove(i);
                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                lastUpdateTime = now.getTimeInMillis();
                return true;
            }
		}
        return false;
    }
    
    public List getComments(){
        return comments;
    }
    
    public void setComments(List<comment> comments){
        if (comments != null){
            this.comments.clear();
            for (int i = 0; i < comments.size(); i++){
                comment c = new comment(comments.get(i));
                this.comments.add(c);
            }
        }
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public void addComment(comment c){
        for (int i = 0; i < comments.size(); i++){
            comment com = (comment)comments.get(i);
            if (c.getId() <= com.getId()){
                if (c.getId() == com.getId())
                    return;
                comments.add(i, c);
                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                lastUpdateTime = now.getTimeInMillis();
                return;
            }
		}
        comments.add(c);
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public boolean removeComment(long id){
        for (int i = 0; i < comments.size(); i++){
            comment c = (comment)comments.get(i);
            if (c.getId() == id){
                comments.remove(i);
                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                lastUpdateTime = now.getTimeInMillis();
                return true;
            }
		}
        return false;
    }
    
	public void create(Element el) {
		
		id = Long.parseLong(el.getAttribute("id"));
        lastUpdateTime = Long.parseLong(el.getAttribute("lastUpdateTime"));
        belongsTo = el.getAttribute("belongsTo");
        NodeList postedByList = el.getElementsByTagName("postedBy");
		if(postedByList != null && postedByList.getLength() > 0) {
            Element postedByEl = (Element)postedByList.item(0);
            postedBy.create(postedByEl);
        }
        title = getTextValue(el, "title");
        shareWith = getTextValue(el, "shareWith");
        
        NodeList photoFilesList = el.getElementsByTagName("photo");
		if(photoFilesList != null && photoFilesList.getLength() > 0) {
			for(int i = 0 ; i < photoFilesList.getLength();i++) {
				Element photoEl = (Element)photoFilesList.item(i);
				photo p = new photo();
                p.create(photoEl);
				photoFiles.add(p);
            }
        }
        NodeList commentList = el.getElementsByTagName("comment");
		if(commentList != null && commentList.getLength() > 0) {
			for(int i = 0 ; i < commentList.getLength();i++) {
				Element comEl = (Element)commentList.item(i);
				comment c = new comment();
                c.create(comEl);
				if (c.getGrandParent() == 0)
                    comments.add(c);
            }
        }
        NodeList likeList = el.getElementsByTagName("like");
		if(likeList != null && likeList.getLength() > 0) {
			for(int i = 0 ; i < likeList.getLength();i++) {
				Element likeEl = (Element)likeList.item(i);
				like l = new like();
                l.create(likeEl);
				if (l.getGrandParent() == 0)
                    likes.add(l);
            }
        }
        NodeList dislikeList = el.getElementsByTagName("dislike");
		if(dislikeList != null && dislikeList.getLength() > 0) {
			for(int i = 0 ; i < dislikeList.getLength();i++) {
				Element dislikeEl = (Element)dislikeList.item(i);
				dislike dl = new dislike();
                dl.create(dislikeEl);
				if (dl.getGrandParent() == 0)
                    dislikes.add(dl);
            }
        }
        
	}
    
    public Element createDOMElement(Document dom){
        Element el = dom.createElement("photoAlbum");
		
        el.setAttribute("id", String.valueOf(id));
        el.setAttribute("lastUpdateTime", String.valueOf(lastUpdateTime));
        if (belongsTo != null)
            el.setAttribute("belongsTo", belongsTo);
        if (postedBy != null){
            Element postedByEl = dom.createElement("postedBy"); //change
            postedByEl.appendChild(postedBy.createDOMElement(dom)); //change
            el.appendChild(postedByEl); //changes
		}
        if (title != null){
            Element titleEl = dom.createElement("title");
            Text titleText = dom.createTextNode(title);
            titleEl.appendChild(titleText);
            el.appendChild(titleEl);
        }
        if (shareWith != null){
            Element shareEl = dom.createElement("shareWith");
            Text shareText = dom.createTextNode(shareWith);
            shareEl.appendChild(shareText);
            el.appendChild(shareEl);
        }
        
        Iterator it = photoFiles.iterator();
        while(it.hasNext()) {
			photo p = (photo)it.next();
			el.appendChild(p.createDOMElement(dom));
		}
        
        it  = likes.iterator();
		while(it.hasNext()) {
			like l = (like)it.next();
			el.appendChild(l.createDOMElement(dom));
		}
        
        it  = dislikes.iterator();
		while(it.hasNext()) {
			dislike d = (dislike)it.next();
			el.appendChild(d.createDOMElement(dom));
		}
        
        it  = comments.iterator();
		while(it.hasNext()) {
			comment c = (comment)it.next();
			el.appendChild(c.createDOMElement(dom));
		}
        
		return el;
    }
    
    public String generateHTML(){
        if (title == null){
            title = "";
        }
        mainSettings.refresh(mainSettings.BASIC_INFO);
        String res = "";
        res += "<div class=\"content\">\n";
        res += "<div class=\"header\">\n";
        res += "<div class=\"posted-img\">\n";
        res += "<img src=\"" + postedBy.getThumbnail() + "\" width=\"50\" height=\"50\"></img>\n";
        res += "</div>\n";
        res += "<div class=\"posted-entry\">\n";
        res += "<a href=\"profile.jsp?profileOwner=" + postedBy.getUsername() + "\">" + postedBy.getFirstName() + " " + postedBy.getLastName() + "</a><p>posted a photo album</p>\n";
        Calendar now = Calendar.getInstance();
        TimeZone local = now.getTimeZone();
        now.setTimeZone(TimeZone.getTimeZone("UTC"));
        now.setTimeInMillis(id);
        now.setTimeZone(local);
        res += "<br /><h3>submitted on: " + now.getTime() + "</h3>\n";
        res += "</div>\n";
        res += "<div class=\"remove-entry\">\n";
        if (belongsTo.equals(postedBy.getUsername()) && mainSettings.username.equals(belongsTo)){
            res += "<a href=\"" + "?show=addPhotoFile&profileOwner=" + belongsTo + "&postedBy=" + postedBy.getUsername() + "&parentAlbum=" + title + "&shareWith=" + shareWith + "\"><img src=\"images/add_file.png\" alt=\"Add a new file to this album\" width=\"20\" height=\"20\">\n";
            res += "</img>\n";
            res += "</a>\n";
        }
        if (postedBy.getUsername().equals(mainSettings.username)){
            res += "<a href=\"" + "?show=photos&op=remove&profileOwner=" + belongsTo + "&postedBy=" + postedBy.getUsername() + "&el=photoAlbum" + "&id=" + id + "&shareWith=" + shareWith + "\"><img src=\"images/delete.png\" alt=\"Delete this album\" width=\"15\" height=\"15\">\n";
            res += "</img>\n";
            res += "</a>\n";
        }
        res += "</div>\n";
        res += "</div>\n";
        res += "<div class=\"caption\">\n";
        res += "<h3>Title:&nbsp;" + title + "</h3><br />\n";
        res += "</div>\n";
        res += "<div class=\"entry\">\n";
        Iterator it  = photoFiles.iterator();
        while(it.hasNext()) {
            photo p = (photo)it.next();
            res += p.generateHTML("abstract", null);
        }
        res += "</div>\n";
        res += "<div class=\"shareWith\">\n";
        res += "<p><img src=\"./images/share.png\" width=\"15\">\n";
        res += "</img alt=\"share with\">" + shareWith + "</p>\n";
        res += "</div>\n";
        res += "<div class=\"like-box\">\n";
        res += "<div class=\"like-icon\">\n";
        res += "<a href=\"?show=photos&op=add&el=like&profileOwner=" + belongsTo + "&type=photoAlbum&grandParentID=0" + "&parentID=" + id + "&shareWith=" + shareWith +"\"><img src=\"images/thumbs_up.gif\" width=\"18\" height=\"18\">\n";
        res += "</img></a>\n";
        res += "</div>\n";
        res += "<div class=\"like-entries\">\n";
        for (int i = 0; i < likes.size(); i++){
            like l = (like)likes.get(i);
            res += l.generateHTML();
            if (likes.size() > 1 && i < (likes.size() - 1))
                res += ",&nbsp;";
        }
        res += "</div>\n";
        res += "</div>\n";
        res += "<div class=\"like-box\">\n";
        res += "<div class=\"like-icon\">\n";
        res += "<a href=\"?show=photos&op=add&el=dislike&profileOwner=" + belongsTo + "&type=photoAlbum&grandParentID=0" + "&parentID=" + id + "&shareWith=" + shareWith +"\"><img src=\"images/thumbs_down.gif\" width=\"18\" height=\"18\">\n";
        res += "</img></a>\n";
        res += "</div>\n";
        res += "<div class=\"like-entries\">\n";
        for (int i = 0; i < dislikes.size(); i++){
            dislike dl = (dislike)dislikes.get(i);
            res += dl.generateHTML();
            if (dislikes.size() > 1 && i < (dislikes.size() - 1))
                res += ",&nbsp;";
        }
        res += "</div>\n";
        res += "</div>\n";
        
        it  = comments.iterator();
        while(it.hasNext()) {
            comment c = (comment)it.next();
            res += c.generateHTML("photos");
        }
        profile userProfile = new profile(mainSettings.username, mainSettings.username, "./MyZone/");
        boolean hasInfo = userProfile.loadUserInfo();
        String firstname = "";
        String lastname = "";
        if (hasInfo){
            firstname = userProfile.userInfo.getFirstName();
            lastname = userProfile.userInfo.getLastName();
        }
        String path = mainSettings.username + "/thumbnails";
        File dir = new File("./MyZone/" + path);;
        
        String[] children = dir.list();
        String thumbnail = null;
        if (children != null) {
            for (int i=0; i<children.length; i++) {
                String filename = children[i];
                if (filename.contains(mainSettings.username)){
                    thumbnail = path + "/" + filename;
                    break;
                }
            }
        }
        if (thumbnail == null){
            thumbnail = "images/person.png";
        }
        res += "<div class=\"comment-box\">\n";
        res += "<div class=\"comment-thumbnail\">\n";
        res += "<img src=\"" + thumbnail + "\" alt=\"" + firstname + " " + lastname + "\" width=\"40\" height=\"40\">\n";
        res += "</img>\n";
        res += "</div>\n";
        res += "<div class=\"comment-form\">\n";
        res += "<p><a href=\"profile.jsp?profileOwner=" + userProfile.profileOwner + "\">" + firstname + " " + lastname + "</a>:&nbsp;</p>\n";
        res += "<form method=\"get\">\n";
        res += "<input type=\"hidden\" name=\"profileOwner\" value=\"" + belongsTo + "\">\n";
        res += "<input type=\"hidden\" name=\"show\" value=\"photos\">\n";
        res += "<input type=\"hidden\" name=\"op\" value=\"add\">\n";
        res += "<input type=\"hidden\" name=\"el\" value=\"comment\">\n";
        res += "<input type=\"hidden\" name=\"grandParentID\" value=\"" + 0 + "\">\n";
        res += "<input type=\"hidden\" name=\"type\" value=\"photoAlbum\">\n";
        res += "<input type=\"hidden\" name=\"parentID\" value=\"" + id + "\">\n";
        res += "<input type=\"hidden\" name=\"shareWith\" value=\"" + shareWith + "\">\n";
        res += "<textarea name=\"body\"/></textarea>\n";
        res += "<input type=\"submit\" value=\"send\"/></input>\n";
        res += "</form>\n";
        res += "</div>\n";
        res += "</div>\n";
        res += "</div>\n";
        return res;
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Photo Album Info \n");
		sb.append("id:" + id);
		sb.append("\n");
        sb.append("lastUpdateTime:" + lastUpdateTime);
		sb.append("\n");
        sb.append("belongsTo:" + belongsTo);
        sb.append("\n");
        sb.append("Postedby:" + postedBy); //change
        sb.append("\n"); //change
        sb.append("title:" + title);
		sb.append("\n");
		sb.append("shareWith:" + shareWith);
		sb.append("\n");
        Iterator it  = photoFiles.iterator();
		while(it.hasNext()) {
			photo p = (photo)it.next();
			sb.append(p.toString());
		}
        it = likes.iterator();
        while(it.hasNext()) {
			like l = (like)it.next();
			sb.append(l.toString());
		}
        it = dislikes.iterator();
		while(it.hasNext()) {
			dislike d = (dislike)it.next();
			sb.append(d.toString());
		}
        it = comments.iterator();
		while(it.hasNext()) {
			comment c = (comment)it.next();
			sb.append(c.toString());
		}
        
		return sb.toString();
	}
}
