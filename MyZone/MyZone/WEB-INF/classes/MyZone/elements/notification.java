/*
 =============================================================================
 |Copyright (C) 2012  Alireza Mahdian email:alireza.mahdian@colorado.edu     |
 |This program is free software: you can redistribute and modify 	         |
 |it under the terms of the GNU General Public License as published by       |
 |the Free Software Foundation, either version 3 of the License, or          |
 |(at your option) any later version.                                        |
 |                                                                           |
 |This program is distributed in the hope that it will be useful,            |
 |but WITHOUT ANY WARRANTY; without even the implied warranty of             |
 |MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              |
 |GNU General Public License for more details.                               |
 |                                                                           |
 |You should have received a copy of the GNU General Public License          |
 |along with this program.  If not, see <http://www.gnu.org/licenses/>.      |
 =============================================================================
 */

package MyZone.elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

/*
 This file contains the implementation of the notification element.
 Please refer to notification.xsd for detailed description of its elements
 and attributes.
 */

public class notification {
    
    private long id;
    private String action;
	private String type;
    private long elementId;
    private String shareWith;
    private user postedBy;
    private Settings mainSettings;
    
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
    
	public notification(long id,
                        String action,
                        String type,
                        long elementId,
                        String shareWith,
                        user postedBy){
        mainSettings = new Settings("./MyZone/");
        mainSettings.refresh(mainSettings.BASIC_INFO);
        if (mainSettings.username == null){
            mainSettings = new Settings("../../");
            mainSettings.refresh(mainSettings.BASIC_INFO);
        }
        this.id = id;
        this.action = action;
        this.type = type;
        this.elementId = elementId;
        this.shareWith = shareWith;
        this.postedBy = new user();
        if (postedBy != null){
            this.postedBy.setUsername(postedBy.getUsername());
            this.postedBy.setFirstName(postedBy.getFirstName());
            this.postedBy.setLastName(postedBy.getLastName());
        }
    }
	
    public notification() {
        mainSettings = new Settings("./MyZone/");
        mainSettings.refresh(mainSettings.BASIC_INFO);
        if (mainSettings.username == null){
            mainSettings = new Settings("../../");
            mainSettings.refresh(mainSettings.BASIC_INFO);
        }
        postedBy = new user(); // change
    }
    
    public notification(notification orig){
        mainSettings = new Settings("./MyZone/");
        mainSettings.refresh(mainSettings.BASIC_INFO);
        if (mainSettings.username == null){
            mainSettings = new Settings("../../");
            mainSettings.refresh(mainSettings.BASIC_INFO);
        }
        this.id = orig.getId();
        this.action = orig.getAction();
        this.type = orig.getType();
        this.elementId = orig.getElementID();
        this.shareWith = orig.getShareWith();
        this.postedBy = new user(); // change
        if (orig.getPostedBy() != null){
            this.postedBy.setUsername(orig.getPostedBy().getUsername());
            this.postedBy.setFirstName(orig.getPostedBy().getFirstName());
            this.postedBy.setLastName(orig.getPostedBy().getLastName());
        }
    }
    
    public long getId(){
        return id;
    }
    
    public void setId(long id){
        this.id = id;
    }
    
    public long getElementID(){
        return elementId;
    }
    
    public void setElementID(long elementId){
        this.elementId = elementId;
    }
    
    public String getType(){
        return type;
    }
    
    public void setType(String type){
        this.type = type;
    }
    
    public void setPostedBy(user postedBy){
        this.postedBy.setUsername(postedBy.getUsername());
        this.postedBy.setFirstName(postedBy.getFirstName());
        this.postedBy.setLastName(postedBy.getLastName());
    }
    
    public user getPostedBy(){
        return postedBy;
    }
    
    public String getAction(){
        return action;
    }
    
    public void setAction(String action){
        this.action = action;
    }
    
    public String getShareWith(){
        return shareWith;
    }
    
    public void setShareWith(String shareWith){
        this.shareWith = shareWith;
    }
    
	public void create(Element el) {
		
		id = Long.parseLong(el.getAttribute("id"));
        elementId = Long.parseLong(el.getAttribute("elementId"));
        type = el.getAttribute("type");
        action = el.getAttribute("action");
        shareWith = el.getAttribute("shareWith");
        
        NodeList postedByList = el.getElementsByTagName("postedBy");
		if(postedByList != null && postedByList.getLength() > 0) 
        {
            Element postedByEl = (Element)postedByList.item(0);
            postedBy.create(postedByEl);
        }
	}
    
    public Element createDOMElement(Document dom){
        Element el = dom.createElement("notification");
		
        el.setAttribute("id", String.valueOf(id));
        el.setAttribute("elementId", String.valueOf(elementId));
        if (type != null)
            el.setAttribute("type", type);
        if (action != null)
            el.setAttribute("action", action);
		if (shareWith != null)
            el.setAttribute("shareWith", shareWith);
        if (postedBy != null){
            Element postedByEl = dom.createElement("postedBy"); //change
            postedByEl.appendChild(postedBy.createDOMElement(dom)); //change
            el.appendChild(postedByEl); //changes
        }
		return el;
    }
    
    public String generateHTML(String type){
        String res = "";
        String verb = "";
        String object = "";
        String objectType = "";
        if (action.equals("add")){
            if (this.type.contains("linkComment")){
                verb = "commented on";
                object = "a link post";
                objectType = "link";
            }else if (this.type.contains("linkLike")){
                verb = "liked";
                object = "a link post";
                objectType = "link";
            }else if (this.type.contains("linkDislike")){
                verb = "disliked";
                object = "a link post";
                objectType = "link";
            }else if (this.type.contains("link")){
                verb = "posted";
                object = "a link";
                objectType = "link";
            }
            if (this.type.contains("wallPostComment")){
                verb = "commented on";
                object = "a wall post";
                objectType = "wallPost";
            }else if (this.type.contains("wallPostLike")){
                verb = "liked";
                object = "a wall post";
                objectType = "wallPost";
            }else if (this.type.contains("wallPostDislike")){
                verb = "disliked";
                object = "a wall post";
                objectType = "wallPost";
            }else if (this.type.contains("wallPost")){
                verb = "posted";
                object = "a wall post";
                objectType = "wallPost";
            }
            if (this.type.contains("audioComment")){
                verb = "commented on";
                object = "an audio post";
                objectType = "audio";
            }else if (this.type.contains("audioLike")){
                verb = "liked";
                object = "an audio post";
                objectType = "audio";
            }else if (this.type.contains("audioDislike")){
                verb = "disliked";
                object = "an audio post";
                objectType = "audio";
            }else if (this.type.contains("audio")){
                verb = "posted";
                object = "an audio file";
                objectType = "audio";
            }
            if (this.type.contains("audioAlbumComment")){
                verb = "commented on";
                object = "an audio album post";
                objectType = "audioAlbum";
            }else if (this.type.contains("audioAlbumLike")){
                verb = "liked";
                object = "an audio album post";
                objectType = "audioAlbum";
            }else if (this.type.contains("audioAlbumDislike")){
                verb = "disliked";
                object = "an audio album post";
                objectType = "audioAlbum";
            }else if (this.type.contains("audioAlbum")){
                verb = "posted";
                object = "an audio album";
                objectType = "audioAlbum";
            }
            if (this.type.contains("videoComment")){
                verb = "commented on";
                object = "a video post";
                objectType = "video";
            }else if (this.type.contains("videoLike")){
                verb = "liked";
                object = "a video post";
                objectType = "video";
            }else if (this.type.contains("videoDislike")){
                verb = "disliked";
                object = "a video post";
                objectType = "video";
            }else if (this.type.contains("video")){
                verb = "posted";
                object = "a video";
                objectType = "video";
            }
            if (this.type.contains("videoAlbumComment")){
                verb = "commented on";
                object = "a video album post";
                objectType = "videoAlbum";
            }else if (this.type.contains("videoAlbumLike")){
                verb = "liked";
                object = "a video album post";
                objectType = "videoAlbum";
            }else if (this.type.contains("videoAlbumDislike")){
                verb = "disliked";
                object = "a video album post";
                objectType = "videoAlbum";
            }else if (this.type.contains("videoAlbum")){
                verb = "posted";
                object = "a video album";
                objectType = "videoAlbum";
            }
            if (this.type.contains("photoComment")){
                verb = "commented on";
                object = "a photo post";
                objectType = "photo";
            }else if (this.type.contains("photoLike")){
                verb = "liked";
                object = "a photo post";
                objectType = "photo";
            }else if (this.type.contains("photoDislike")){
                verb = "disliked";
                object = "a photo post";
                objectType = "photo";
            }else if (this.type.contains("photo")){
                verb = "posted";
                object = "a photo";
                objectType = "photo";
            }
            if (this.type.contains("photoAlbumComment")){
                verb = "commented on";
                object = "a photo album post";
                objectType = "photoAlbum";
            }else if (this.type.contains("photoAlbumLike")){
                verb = "liked";
                object = "a photo album post";
                objectType = "photoAlbum";
            }else if (this.type.contains("photoAlbumDislike")){
                verb = "disliked";
                object = "a photo album post";
                objectType = "photoAlbum";
            }else if (this.type.contains("photoAlbum")){
                verb = "posted";
                object = "a photo album";
                objectType = "photoAlbum";
            }
        }
        
        if (type.equals("sidebarAbstract")){
            res += "<div class=\"right-sidebar-entry-row\">\n";
            res += "<div class=\"sidebar-thumbnail\">\n";
            res += "<img src=\"" + postedBy.getThumbnail() + "\" alt=\"" + postedBy.getFirstName() + " " + postedBy.getLastName() + "\" width=\"50\" height=\"50\">\n";
            res += "</img>\n";
            res += "</div>\n";
            res += "<div class=\"sidebar-entry-textbox3\"><p><h3><a href=profile.jsp?profileOwner=" + postedBy.getUsername() + ">" + postedBy.getFirstName() + " " + postedBy.getLastName() + "</a></h3>&nbsp;" + verb + "<a href=profile.jsp?show=" + objectType + "&id=" + elementId + "&shareWith=" + shareWith +">" + " "  + object + "</a> on your profile</p>\n";
            res += "</div>\n";
            res += "</div>\n";
        }
        return res;
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("notification info \n");
		sb.append("id:" + id);
		sb.append("\n");
        sb.append("elementId:" + elementId);
		sb.append("\n");
        sb.append("type:" + type);
        sb.append("\n");
        sb.append("Postedby:" + postedBy);
        sb.append("\n");
        sb.append("action:" + action);
		sb.append("\n");
		sb.append("shareWith:" + shareWith);
		sb.append("\n");
		return sb.toString();
	}
}
