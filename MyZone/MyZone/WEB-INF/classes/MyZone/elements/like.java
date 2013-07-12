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
 This file contains the implementation of the like element of a profile.
 Please refer to audios.xsd, videos.xsd, photos.xsd, links.xsd, or wallPosts.xsd 
 for detailed description of its elements and attributes.
 */

public class like {
    
    private long id;
    private user usr;
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
    
	public like(String type, long grandParent, long parent, long id, String belongsTo, String shareWith, user u){
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
    }
	
    public like(){
        id = 0;
        parent = 0;
        grandParent = 0;
        usr = new user();
    }
    
    public like(like orig){
        this(orig.getType(), orig.getGrandParent(), orig.getParent(), orig.getId(), orig.getBelongsTo(), orig.getShareWith(), orig.getUser());
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
    
    public String getBelongsTo(){
        return belongsTo;
    }
    
    public void setBelongsTo(String belongsTo){
        this.belongsTo = belongsTo;
    }
    
    public void setSharedWith(String shareWith){
        this.shareWith = shareWith;
    }
    
    public user getUser(){
        return usr;
    }
    
    public void setUser(user u){
        usr = new user(u);
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
	}
    
    public Element createDOMElement(Document dom){
        Element el = dom.createElement("like");
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
        
		return el;
    }
    
    public String generateHTML(){
        
        String res = "";
        
        res += "<a href=\"?profileOwner=" + usr.getUsername() + "\">" + usr.getFirstName() + " " + usr.getLastName() + "</a>";
        
        return res;
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("like info \n");
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
		
		return sb.toString();
	}
}
