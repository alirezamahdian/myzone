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

/*
 This file contains the implementation of the audio element of a profile.
 a deletedEntry is used to represent a deleted element (audio, video, photo, 
 link, wallPost, comment, like, dislike) of the profile in wall.
 */

public class deletedEntry {
    private long parent;
    private long id;
    private long lastUpdateTime;
    private String shareWith;
    private String postedBy;
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
    
    public deletedEntry(){
        
    }
    
	public deletedEntry(long parent, long id, long lastUpdateTime, String shareWith, String postedBy, String type){
        this.parent = parent;
        this.id = id;
        this.lastUpdateTime = lastUpdateTime;
        this.postedBy = postedBy;
        this.shareWith = shareWith;
        this.type = type;
    }
	
    public deletedEntry(deletedEntry orig){
        this(orig.getParent(), orig.getId(), orig.getLastUpdateTime(), orig.getShareWith(), orig.getPostedBy(), orig.getType());
    }
    
    public long getParent(){
        return parent; 
    }
    
    public void setParent(long parent){
        this.parent = parent;
    }
    
    public long getId(){
        return id; 
    }
    
    public long getLastUpdateTime(){
        return lastUpdateTime;
    }
    
    public void setLastUpdateTime(long lastUpdateTime){
        this.lastUpdateTime = lastUpdateTime;
    }
    
    public String getShareWith(){
        return shareWith;
    }
    
    public void setSharedWith(String shareWith){
        this.shareWith = shareWith;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public String getPostedBy(){
        return postedBy;
    }
    
    public void setPostedBy(String postedBy){
        this.postedBy = postedBy;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public void setId(long id){
        this.id = id;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public void setType(String type){
        this.type = type;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
        
    }
    
    public String getType(){
        return type;
    }
    
	public void create(Element el) {
        parent = Long.parseLong(el.getAttribute("parent"));
        id = Long.parseLong(el.getAttribute("id"));
        lastUpdateTime = Long.parseLong(el.getAttribute("lastUpdateTime"));
        postedBy = el.getAttribute("postedBy");
        shareWith = el.getAttribute("shareWith");
        type = el.getAttribute("type");
	}
    
    public Element createDOMElement(Document dom){
        Element el = dom.createElement("deletedEntry");
		el.setAttribute("parent", String.valueOf(parent));
        el.setAttribute("id", String.valueOf(id));
        el.setAttribute("lastUpdateTime", String.valueOf(lastUpdateTime));
        if (postedBy != null)
            el.setAttribute("postedBy", postedBy);
        
        if (shareWith != null){
            el.setAttribute("shareWith", shareWith);
        }
        
        if (type != null){
            el.setAttribute("type", type);
        }
		return el;
    }
    
}
