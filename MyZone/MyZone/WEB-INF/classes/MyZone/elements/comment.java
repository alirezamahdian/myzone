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

import MyZone.elements.user;

/*
 This file contains the implementation of the comment element of a profile.
 Please refer to audios.xsd, videos.xsd, photos.xsd, links.xsd, or wallPosts.xsd 
 for detailed description of its elements and attributes.
 */

public class comment {
    
    private long id;
    private user usr;
	private String body;
    private long parent;
    private long grandParent;
    private String shareWith;
    private String belongsTo;
    private String type;
    
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
    
    public comment(){
        id = 0;
        parent = 0;
        grandParent = 0;
        usr = new user(); //change
    }
    
	public comment(String type, long grandParent, long parent, long id, String belongsTo, String shareWith, user u, String body){
        this.type = type;
        this.grandParent = grandParent;
        this.parent = parent;
        this.id = id;
        this.belongsTo = belongsTo;
        this.shareWith = shareWith;
        this.usr = new user();
        if (u != null){
            this.usr.setUsername(u.getUsername());
            this.usr.setFirstName(u.getFirstName());
            this.usr.setLastName(u.getLastName());
        }
        this.body = body;
    }
    
    public comment(comment orig){
        this(orig.getType(), orig.getGrandParent(), orig.getParent(), orig.getId(), orig.getBelongsTo(), orig.getShareWith(), orig.getUser(), orig.getBody());
    }
	
    public String getType(){
        return type;
    }
    
    public void setType(String type){
        this.type = type;
    }
    
    public long getParent(){
        return parent;
    }
    
    public void setParent(long parent){
        this.parent = parent;
    }
    
    public long getGrandParent(){
        return grandParent;
    }
    
    public void setGrandParent(long grandParent){
        this.grandParent = grandParent;
    }
    
    public long getId(){
        return id; 
    }
    
    public void setId(long id){
        this.id = id;
    }
    
    public String getShareWith(){
        return shareWith;
    }
    
    public void setSharedWith(String shareWith){
        this.shareWith = shareWith;
    }
    
    public String getBelongsTo(){
        return belongsTo;
    }
    
    public void setBelongsTo(String belongsTo){
        this.belongsTo = belongsTo;
    }
    
    public user getUser(){
        return usr;
    }
    
    public void setUser(user u){
        usr = new user(u);
    }
    
    public String getBody(){
        return body;
    }
    
    public void setBody(String body){
        this.body = body;
    }
    
	public void create(Element el) {
        type = el.getAttribute("type");
		grandParent = Long.parseLong(el.getAttribute("grandParent"));
        parent = Long.parseLong(el.getAttribute("parent"));
		id = Long.parseLong(el.getAttribute("id"));
        belongsTo = el.getAttribute("belongsTo");
        shareWith = getTextValue(el, "shareWith");
        NodeList userList = el.getElementsByTagName("user");
		if(userList != null && userList.getLength() > 0) {
            Element usrEl = (Element)userList.item(0);
            usr.create(usrEl);
        }
        body = getTextValue(el, "body");
	}
    
    public Element createDOMElement(Document dom){
        Element el = dom.createElement("comment");
        el.setAttribute("grandParent", String.valueOf(grandParent));
        el.setAttribute("parent", String.valueOf(parent));
        el.setAttribute("id", String.valueOf(id));
		if (type != null)
            el.setAttribute("type", type);
        if (belongsTo != null)
            el.setAttribute("belongsTo", belongsTo);
        
        if (shareWith != null){
            Element shareEl = dom.createElement("shareWith");
            Text shareText = dom.createTextNode(shareWith);
            shareEl.appendChild(shareText);
            el.appendChild(shareEl);
        }
        
        if (usr != null)
            el.appendChild(usr.createDOMElement(dom));
        if (body != null){
            Element bodyEl = dom.createElement("body");
            Text bodyText = dom.createTextNode(body);
            bodyEl.appendChild(bodyText);
            el.appendChild(bodyEl);
        }
        
		return el;
    }
    
    public String generateHTML(String show){
        
        String res = "";
        res += "<div class=\"comment-box\">\n";
        res += "<div class=\"comment-thumbnail\">\n";
        res += "<img src=\"" + usr.getThumbnail() + "\" alt=\"" + usr.getFirstName() + " " + usr.getLastName() + "\" width=\"40\" height=\"40\">\n";
        res += "</img>\n";
        res += "</div>\n";
        res += "<div class=\"comment-entry\">\n";
        Calendar now = Calendar.getInstance();
        TimeZone local = now.getTimeZone();
        now.setTimeZone(TimeZone.getTimeZone("UTC"));
        now.setTimeInMillis(id);
        now.setTimeZone(local);
        res += "<h3>" + now.getTime() + "</h3><br />\n";
        res += "<p><a href=\"profile.jsp?profileOwner=" + usr.getUsername() + "\">" + usr.getFirstName() + " " + usr.getLastName() + ":&nbsp;</a>" + body.replaceAll("\n", "<br />") + "</p>\n";
        int index = 0;
        int vIndex = 0;
        int yIndex = 0;
        while (vIndex > -1 && yIndex > -1){
            vIndex = body.indexOf("vimeo.com/", index);
            yIndex = body.indexOf("www.youtube.com/watch?", index);
            Calendar t = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            if ((yIndex < vIndex && yIndex > -1) || (yIndex > -1 && vIndex < 0)){
                int nextIndex = body.indexOf("&", body.indexOf("www.youtube.com/watch?", index));
                if (nextIndex < 0){
                    nextIndex = body.indexOf(" ", body.indexOf("www.youtube.com/watch?", index));
                }
                if (nextIndex < 0){
                    nextIndex = body.indexOf("\n", body.indexOf("www.youtube.com/watch?", index));
                }
                if (nextIndex < 0){
                    nextIndex = body.length();
                }
                if (nextIndex > body.indexOf("\n", body.indexOf("www.youtube.com/watch?", index)) && body.indexOf("\n", body.indexOf("www.youtube.com/watch?", index)) > -1){
                    nextIndex = body.indexOf("\n", body.indexOf("www.youtube.com/watch?", index));
                }
                String url = body.substring(body.indexOf("www.youtube.com/watch?", index), nextIndex);
                index = nextIndex;
                
                res += "<div class=\"comment-entry\">\n";
                res += "<div id=\"container" + (t.getTimeInMillis() + index) + "\">Loading the player ...</div>\n";
                res += "<script type=\"text/javascript\">\n";
                res += "jwplayer(\"container" + (t.getTimeInMillis() + index) + "\").setup({\n";
                res += "flashplayer: \"media_player/player.swf\",\n";
                res += "file: \"" + url + "\",\n";
                res += "height: 180,\n";
                res += "width: 330\n";
                res += "});\n";
                res += "</script>\n";
                res += "</div>\n";
            }else if (vIndex > -1){
                int nextIndex = body.indexOf("vimeo.com/", index) + 18;
                if (nextIndex > body.length()){
                    break;
                }
                String url = body.substring(body.indexOf("vimeo.com/", index), nextIndex);
                index = nextIndex;
                res += "<div class=\"comment-entry\">\n";
                res += "<div id=\"container" + (t.getTimeInMillis() + index) + "\">Loading the player ...</div>\n";
                res += "<script type=\"text/javascript\">\n";
                res += "jwplayer(\"container" + (t.getTimeInMillis() + index) + "\").setup({\n";
                res += "flashplayer: \"media_player/player.swf\",\n";
                res += "type: \"media_player/vimeo.swf\",\n";
                res += "file: \"" + url + "\",\n";
                res += "height: 180,\n";
                res += "width: 330\n";
                res += "});\n";
                res += "</script>\n";
                res += "</div>\n";
            }
        }
        res += "</div>\n";
        res += "<div class=\"remove-comment\">\n";
        if (show != null){
            res += "<a href=\"" + "?show=" + show + "&op=remove&profileOwner=" + belongsTo + "&postedBy=" + usr.getUsername() + "&el=comment&grandParentID=" + grandParent + "&parentID=" + parent + "&id=" + parent + "&actualId=" + id + "&type=" + type + "&shareWith=" + shareWith + "\"><img src=\"images/delete.png\" width=\"15\" height=\"15\">\n";
        }else{
            res += "<a href=\"" + "?op=remove&profileOwner=" + belongsTo + "&postedBy=" + usr.getUsername() + "&el=comment&grandParentID=" + grandParent + "&parentID=" + parent + "&actualId=" + id + "&type=" + type + "&shareWith=" + shareWith + "\"><img src=\"images/delete.png\" width=\"15\" height=\"15\">\n";
        }
        res += "</img>\n";
        res += "</a>\n";
        res += "</div>\n";
        res += "</div>\n";
        
        return res;
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Comment info \n");
        sb.append("type:" + type);
        sb.append("\n");
		sb.append("grandParent:" + grandParent);
        sb.append("\n");
        sb.append("parent:" + parent);
        sb.append("\n");
        sb.append("id:" + id);
        sb.append("\n");
        sb.append("belongsTo" + belongsTo);
        sb.append("\n");
        sb.append(usr.toString());
		sb.append("body:" + body);
		sb.append("\n");
		
		return sb.toString();
	}
}
