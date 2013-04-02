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

import MyZone.elements.user;

/*
 This file contains the implementation of the zone element.
 Please refer to zones.xsd for detailed description of its elements
 and attributes.
 */

public class zone {
    
    private long id;
    private long lastUpdateTime;
    private String name;
    private long refreshInterval;
	private List<user> members = new ArrayList<user>();
	
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
    
    public zone(){}
    
    public zone(long id,
                long lastUpdateTime,
                String name,
                long refreshInterval,
                List<user> members)
    {
        this.id = id;
        this.lastUpdateTime = lastUpdateTime;
        this.name = name;
        this.refreshInterval = refreshInterval;
        if (members != null){
            this.members.clear();
            for (int i = 0; i < members.size(); i++){
                user u = new user(members.get(i));
                this.members.add(u);
            }
        }
    }
    
    public zone(zone orig)
    {
        this.id = orig.getId();
        this.lastUpdateTime = orig.getLastUpdateTime();
        this.name = orig.getName();
        this.refreshInterval = orig.getRefreshInterval();
        if (orig.getMembers() != null){
            this.members.clear();
            List<user> tempMembers = orig.getMembers();
            for (int i = 0; i < tempMembers.size(); i++){
                user u = new user(tempMembers.get(i));
                this.members.add(u);
            }
        }
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
    
    public String getName(){
        return name;
    }
    
    public void setName(String name){
        this.name = name;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public long getRefreshInterval(){
        return refreshInterval;
    }
    
    public void setRefreshInterval(long refreshInterval){
        this.refreshInterval = refreshInterval;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public List getMembers(){
        return members;
    }
    
    public user isMember(String username){
        for (int i = 0; i < members.size(); i++){
            user u = (user)members.get(i);
            if (u.getUsername().equals(username)){
                return u;
            }
        }
        return null;
    }
    
    public void setMembers(List<user> members){
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        if (members == null)
            return;
        for (int i = 0; i < members.size(); i++){
            user u1 = members.get(i);
            int j = 0;
            for (j = 0; j < this.members.size(); j++){
                user u2 = this.members.get(j);
                if (u1.getUsername().equals(u2.getUsername())){
                    break;
                }
            }
            if (j == this.members.size()){
                u1.setMemberSince(now.getTimeInMillis());
                members.add(u1);
                members.remove(i + 1);
            }
        }
        this.members.clear();
        for (int i = 0; i < members.size(); i++){
            user u = new user(members.get(i));
            this.members.add(u);
        }
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public void addMember(user u){
        for (int i = 0; i < members.size(); i++){
            user usr = (user)members.get(i);
            if (u.getUsername().equals(usr.getUsername())){
                return;
            }
        }
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        u.setMemberSince(now.getTimeInMillis());
        members.add(u);
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public boolean removeMember(String username){
        for (int i = 0; i < members.size(); i++){
            user u = (user)members.get(i);
            if (u.getUsername().equals(username)){
                members.remove(i);
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
        name = el.getAttribute("name");
        if (!el.getAttribute("refreshInterval").equals(""))
            refreshInterval = Long.parseLong(el.getAttribute("refreshInterval"));
        
        NodeList membersList = el.getElementsByTagName("members");
		if(membersList != null && membersList.getLength() > 0) {
            Element membersEl = (Element)membersList.item(0);
            NodeList userList = membersEl.getElementsByTagName("user");
			for(int i = 0 ; i < userList.getLength();i++) {
				Element usrEl = (Element)userList.item(i);
				user usr = new user();
                usr.create(usrEl);
				members.add(usr);
            }
        }
        
	}
    
    public Element createDOMElement(Document dom){
        Element el = dom.createElement("zone");
		
        el.setAttribute("id", String.valueOf(id));
        el.setAttribute("lastUpdateTime", String.valueOf(lastUpdateTime));
		if (name != null)
            el.setAttribute("name", name);
        el.setAttribute("refreshInterval", String.valueOf(refreshInterval));
        
        Element membersEl = dom.createElement("members");
        Iterator it  = members.iterator();
		while(it.hasNext()) {
			user usr = (user)it.next();
			membersEl.appendChild(usr.createDOMElement(dom));
		}
        el.appendChild(membersEl);
        
		return el;
    }
    
    public String generateHTML(String type){
        String res = "";
        return res;
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Zone info \n");
		sb.append("id:" + id);
		sb.append("\n");
        sb.append("lastUpdateTime:" + lastUpdateTime);
		sb.append("\n");
        sb.append("name:" + name);
		sb.append("\n");
        sb.append("refreshIntrval:" + refreshInterval);
		sb.append("\n");
		Iterator it  = members.iterator();
		while(it.hasNext()) {
			user usr = (user)it.next();
			sb.append(usr.toString());
		}
		return sb.toString();
	}
}
