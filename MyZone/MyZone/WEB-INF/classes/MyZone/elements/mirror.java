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
import MyZone.Settings;

/*
 This file contains the implementation of the mirror element.
 Please refer to mirrors.xsd for detailed description of its elements
 and attributes.
 */

public class mirror {
    
    private long id;
    private long lastUpdateTime;
    private String username;
    private String passphrase;
    private String status;
    private long capacity;
    private long used;
    private int priority;
    private long lastSyncTime;
    private long lastModifiedTime;
    
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
    
    public mirror(){
        capacity = 500 * 1024 * 1024; //Bytes
        used = 0;
        priority = 0;
        status = "idle";
    }
    
	public mirror(long id, String username, String passphrase, long capacity, long used, int priority, long lastSyncTime, long lastModifiedTime){
        this.id = id;
        this.passphrase = passphrase;
        this.username = username;
        this.capacity = capacity;
        this.used = used;
        this.priority = priority;
        this.lastSyncTime = lastSyncTime;
        this.lastModifiedTime = lastModifiedTime;
        lastUpdateTime = id;
        status = "idle";
    }
	
    public mirror(mirror orig){
        this(orig.getId(), 
             orig.getUsername(), 
             orig.getPassphrase(), 
             orig.getCapacity(), 
             orig.getUsed(), 
             orig.getPriority(),
             orig.getLastSyncTime(),
             orig.getLastModifiedTime());
        lastUpdateTime = orig.getLastUpdateTime();
        status = orig.getStatus();
    }
    
    public long getId(){
        return id; 
    }
    
    public void setId(long id){
        this.id = id;
    }
    
    public long getLastUpdateTime(){
        return lastUpdateTime; 
    }
    
    public void setLastUpdateTime(long lastUpdateTime){
        this.lastUpdateTime = lastUpdateTime;
    }
    
    public String getPassphrase(){
        return passphrase;
    }
    
    public void setPassphrase(String passphrase){
        this.passphrase = passphrase;
        lastUpdateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
    }
    
    public String getUsername(){
        return username;
    }
    
    public void setUsername(String usr){
        username = usr;
        lastUpdateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
    }
    
    public long getCapacity(){
        return capacity; 
    }
    
    public void setCapacity(long capacity){
        this.capacity = capacity;
        lastUpdateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
    }
    
    public long getUsed(){
        return used; 
    }
    
    public void setUsed(long used){
        this.used = used;
        lastUpdateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
    }
    
    public int getPriority(){
        return priority; 
    }
    
    public void setPriority(int priority){
        this.priority = priority;
        lastUpdateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
    }
    
    public long getLastSyncTime(){
        return lastSyncTime; 
    }
    
    public void setLastSyncTime(long lastSyncTime){
        this.lastSyncTime = lastSyncTime;
        lastUpdateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
    }
    
    public long getLastModifiedTime(){
        return lastModifiedTime; 
    }
    
    public void setLastModifiedTime(long lastModifiedTime){
        this.lastModifiedTime = lastModifiedTime;
        lastUpdateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
    }
    
    public void setStatus(String status){
        this.status = status;
        lastUpdateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
    }
    
    public String getStatus(){
        return status;
    }
    
	public void create(Element el) {
		
		id = Long.parseLong(el.getAttribute("id"));
        lastUpdateTime = Long.parseLong(el.getAttribute("lastUpdateTime"));
        username = el.getAttribute("username");
        passphrase = el.getAttribute("passphrase");
        if (!el.getAttribute("capacity").equals(""))
            capacity = Long.parseLong(el.getAttribute("capacity"));
        if (!el.getAttribute("used").equals(""))
            used = Long.parseLong(el.getAttribute("used"));
        if (!el.getAttribute("priority").equals(""))
            priority = Integer.parseInt(el.getAttribute("priority"));
        if (!el.getAttribute("lastSyncTime").equals(""))
            lastSyncTime = Long.parseLong(el.getAttribute("lastSyncTime"));
        if (!el.getAttribute("lastModifiedTime").equals(""))
            lastModifiedTime = Integer.parseInt(el.getAttribute("lastModifiedTime"));
        status = el.getAttribute("status");
    }
    
    public Element createDOMElement(Document dom){
        
        Element el = dom.createElement("mirror");
		el.setAttribute("id", String.valueOf(id));
        el.setAttribute("lastUpdateTime", String.valueOf(lastUpdateTime));
        if (username != null)
            el.setAttribute("username", username);
        if (passphrase != null)
            el.setAttribute("passphrase", passphrase);
        el.setAttribute("capacity", String.valueOf(capacity));
        el.setAttribute("used", String.valueOf(used));
        el.setAttribute("priority", String.valueOf(priority));
        el.setAttribute("lastSyncTime", String.valueOf(lastSyncTime));
        el.setAttribute("lastModifiedTime", String.valueOf(lastModifiedTime));
        el.setAttribute("status", status);
        return el;
    }
    
    public String generateHTML(String type){
        String res = "";
        Settings mainSettings = new Settings("./MyZone/");
        mainSettings.refresh(mainSettings.BASIC_INFO);
        if (mainSettings.username == null){
            mainSettings = new Settings("../../");
            mainSettings.refresh(mainSettings.BASIC_INFO);
        }
        mainSettings.refresh(mainSettings.FRIENDS);
        user usr = null;
        for (int i = 0; i < mainSettings.friends.size(); i++){
            friend f = (friend)mainSettings.friends.get(i);
            if (f.getUser().getUsername().equals(username)){
                usr = f.getUser();
                break;
            }
        }
        if (usr != null){
            if (usr.getFirstName() == null){
                usr.setFirstName(usr.getUsername());
            }
            if (usr.getLastName() == null){
                usr.setLastName(usr.getUsername());
            }
            if (usr.getFirstName().equals("") && usr.getLastName().equals("")){
                usr.setFirstName(usr.getUsername());
                usr.setLastName("");
            }
        }else{
            return "";
        }
        if (type.equals("sentRequest")){
            res += "<div class=\"right-sidebar-entry-row\">\n";
            res += "<div class=\"sidebar-entry-textbox1\"><a>" + usr.getUsername() + "</a>\n";
            res += "</div>\n";
            res += "<div class=\"sidebar-entry-textbox2\"><a href=\"?op=deleteSentMirroringRequest&username=" + usr.getUsername() + "\"><img src=\"images/delete.png\" width=\"15\" height=\"15\">\n";
            res += "</img></a>\n";
            res += "</div>\n";
            res += "</div>\n";
        }else if (type.equals("receivedRequest")){
            res += "<div class=\"right-sidebar-entry-row\">\n";
            res += "<div class=\"sidebar-entry-textbox1\"><a>" + usr.getUsername() + "</a>\n";
            res += "</div>\n";
            res += "<div class=\"sidebar-entry-textbox2\"><a href=\"?op=acceptMirroringRequest&username=" + usr.getUsername() + "\"><img src=\"images/check.png\" width=\"15\" height=\"15\"></img></a>\n";
            res += "</div>\n";
            res += "<div class=\"sidebar-entry-textbox2\"><a href=\"?op=deleteReceivedMirroringRequest&username=" + usr.getUsername() + "\"><img src=\"images/delete.png\" width=\"15\" height=\"15\">\n";
            res += "</img></a>\n";
            res += "</div>\n";
            res += "</div>\n";
        }else if (type.equals("original")){
            res += "<div class=\"content\">\n";
            res += "<form method=\"post\">\n";
            res += "<input type=\"hidden\" name=\"show\" value=\"mirrored\">\n";
            res += "<input type=\"hidden\" name=\"op\" value=\"updateOriginal\">\n";
            res += "<input type=\"hidden\" name=\"username\" value=\"" + username + "\">\n";
            res += "<div class=\"content-thumbnail\">\n";
            res += "<img src=\"" + usr.getThumbnail() + "\" alt=\"" + usr.getFirstName() + " " + usr.getLastName() + "\" width=\"50\" height=\"50\">\n";
            res += "</img>\n";
            res += "</div>\n";
            res += "<div class=\"content-entry\">\n";
            res += "<div class=\"content-header\">\n";
            res += "<div class=\"content-header-textbox1\">\n";
            res += "<a href=\"profile.jsp?profileOwner=" + username + "\">" + usr.getFirstName() + " " + usr.getLastName() + "</a>\n";
            res += "</div>\n";
            res += "<div class=\"content-header-textbox2\">\n";
            res += "<a href=\"?op=deleteOriginal&username=" + username + "\"><img src=\"images/delete.png\" width=\"15\" height=\"15\">\n";
            res += "</a></img>\n";
            res += "</div>\n";
            res += "<div class=\"content-entry\">\n";
            res += "<div class=\"content-header\">\n";
            res += "<div class=\"content-header-textbox1\">\n";
            res += "<p>Allocated Capacity: </p><input type=\"text\" size=\"20\" name=\"capacity\" value=\"" + (int)(capacity / (1024 * 1024)) + "\"/><p>&nbsp;MB</p>\n<br />\n";
            res += "<p>Used Space: " + (int)(used / (1024 * 1024)) + "&nbsp;MB</p>\n<br />\n<br />\n";
            Calendar now = Calendar.getInstance();
            TimeZone local = now.getTimeZone();
            now.setTimeZone(TimeZone.getTimeZone("UTC"));
            now.setTimeInMillis(lastSyncTime);
            now.setTimeZone(local);
            if (lastSyncTime > 0){
                res += "<p>Last Sync: " + now.getTime() + "</p>\n<br />\n";
            }else{
                res += "<p>Last Sync: Never</p>\n<br />\n";
            }
            res += "<div class=\"content-header-textbox1\">\n";
            res += "<input type=\"submit\" name=\"B1\" value=\"Update\" />\n";
            res += "</div>\n";
            res += "</div>\n";
            res += "</div>\n";
            res += "</div>\n";
            res += "</div>\n";
            res += "</div>\n";
            res += "</form>\n";
            res += "</div>\n";
        }else{
            res += "<div class=\"content\">\n";
            res += "<div class=\"content-thumbnail\">\n";
            res += "<img src=\"" + usr.getThumbnail() + "\" alt=\"" + usr.getFirstName() + " " + usr.getLastName() + "\" width=\"50\" height=\"50\">\n";
            res += "</img>\n";
            res += "</div>\n";
            res += "<div class=\"content-entry\">\n";
            res += "<div class=\"content-header\">\n";
            res += "<div class=\"content-header-textbox1\">\n";
            res += "<a href=\"profile.jsp?profileOwner=" + username + "\">" + usr.getFirstName() + " " + usr.getLastName() + "</a>\n";
            res += "</div>\n";
            res += "<div class=\"content-header-textbox2\">\n";
            res += "<a href=\"?show=mirrors&op=deleteMirror&username=" + username + "\"><img src=\"images/delete.png\" width=\"15\" height=\"15\">\n";
            res += "</a></img>\n";
            res += "</div>\n";
            res += "<div class=\"content-header-textbox2\">\n";
            res += "<a href=\"?show=mirrors&op=moveUp&username=" + username + "\"><img src=\"images/up.png\" width=\"15\" \">\n";
            res += "</a></img>\n";
            res += "</div>\n";
            res += "<div class=\"content-header-textbox2\">\n";
            res += "<a href=\"?show=mirrors&op=moveDown&username=" + username + "\"><img src=\"images/down.png\" width=\"15\" \">\n";
            res += "</a></img>\n";
            res += "</div>\n";
            res += "<div class=\"content-entry\">\n";
            res += "<div class=\"content-header\">\n";
            res += "<div class=\"content-header-textbox1\">\n";
            res += "<p>Mirror Rank: " + (priority + 1) + "</p>\n<br />\n";
            res += "<p>Allocated Capacity: " + (int)(capacity / (1024 * 1024)) + "&nbsp;MB</p>\n<br />\n";
            res += "<p>Used Space: " + (int)(used / (1024 * 1024)) + "&nbsp;MB</p>\n<br />\n";
            Calendar now = Calendar.getInstance();
            TimeZone local = now.getTimeZone();
            now.setTimeZone(TimeZone.getTimeZone("UTC"));
            now.setTimeInMillis(lastSyncTime);
            now.setTimeZone(local);
            if (lastSyncTime > 0){
                res += "<p>Last Sync: " + now.getTime() + "</p>\n<br />\n";
            }else{
                res += "<p>Last Sync: Never</p>\n<br />\n";
            }
            res += "<br />\n";
            res += "</div>\n";
            res += "</div>\n";
            res += "</div>\n";
            res += "</div>\n";
            res += "</div>\n";
            res += "</div>\n";
        }
        return res;
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Mirror info \n");
		sb.append("id:" + id);
		sb.append("\n");
        sb.append("lastUpdateTime:" + lastUpdateTime);
		sb.append("\n");
        sb.append("username:" + username);
        sb.append("\n");
        sb.append("passphrase:" + passphrase);
        sb.append("\n");
        sb.append("capacity:" + capacity);
        sb.append("\n");
        sb.append("used:" + used);
        sb.append("\n");
        sb.append("priority:" + priority);
        sb.append("\n");
        sb.append("lastSyncTime:" + lastSyncTime);
        sb.append("\n");
        sb.append("lastModifiedTime:" + lastModifiedTime);
        sb.append("\n");
        sb.append("status:" + status);
        sb.append("\n");
        return sb.toString();
	}
}
