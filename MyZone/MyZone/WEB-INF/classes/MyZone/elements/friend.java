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
 This file contains the implementation of the friend element.
 Please refer to friends.xsd for detailed description of its 
 elements and attributes.
 */

public class friend {
    
    private long id;
    private long lastUpdateTime;
    private user usr;
    private String passphrase;
    private String status;
    private String ipAddress;
    private int port;
    private String relayAddress;
    private int relayPort;
    private int typeOfProtocol;
    private long latestVersion;
    private String sent; // indicates whether a friendship request has been sent to the rendezvous server or not.
    private List<String> mirrors = new ArrayList();
	
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
    
    public friend(){
        usr = new user();
        mirrors = new ArrayList();
    }
    
	public friend(long id, String passphrase, user usr, String status, String ipAddress, int port, String relayAddress, int relayPort, int typeOfProtocol, long latestVersion){
        this.id = id;
        this.passphrase = passphrase;
        this.usr = new user();
        if (usr != null){
            this.usr.setUsername(usr.getUsername());
            this.usr.setFirstName(usr.getFirstName());
            this.usr.setLastName(usr.getLastName());
        }
        this.status = status;
        this.ipAddress = ipAddress;
        this.port = port;
        this.relayAddress = relayAddress;
        this.relayPort = relayPort;
        this.typeOfProtocol = typeOfProtocol;
        this.latestVersion = latestVersion;
        lastUpdateTime = id;
        sent = "false";
        status = "disconnected";
    }
	
    public friend(friend orig){
        this(orig.getId(), orig.getPassphrase(), orig.getUser(), orig.getStatus(), orig.getIPAddress(), orig.getPort(), orig.getRelayAddress(), orig.getRelayPort(), orig.getTypeOfProtocol(), orig.getLatestVersion());
        if (orig.getMirrors() != null){
            this.mirrors.clear();
            List<String> tempMirrors = orig.getMirrors();
            for (int i = 0; i < tempMirrors.size(); i++){
                String mi = new String(tempMirrors.get(i));
                this.mirrors.add(mi);
            }
        }
        lastUpdateTime = orig.getLastUpdateTime();
        sent = orig.getSent();
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
    }
    
    public user getUser(){
        return usr;
    }
    
    public void setUser(user u){
        usr = new user(u);
    }
    
    public String getStatus(){
        if (status == null){
            return "disconnected";
        }
        return status;
    }
    
    public void setStatus(String status){
        this.status = status;
    }
    
    public String getSent(){
        return sent;
    }
    
    public void setSent(String sent){
        this.sent = sent;
    }
    
    public String getIPAddress(){
        return ipAddress;
    }
    
    public void setIPAddress(String ipAddress){
        this.ipAddress = ipAddress;
    }
    
    public int getPort(){
        return port;
    }
    
    public void setPort(int port){
        this.port = port;
    }
    
    public String getRelayAddress(){
        return relayAddress;
    }
    
    public void setRelayAddress(String relayAddress){
        this.relayAddress = relayAddress;
    }
    
    public int getRelayPort(){
        return relayPort;
    }
    
    public void setRelayPort(int relayPort){
        this.relayPort = relayPort;
    }
    
    public int getTypeOfProtocol(){
        return typeOfProtocol;
    }
    
    public void setTypeOfProtocol(int typeOfProtocol){
        this.typeOfProtocol = typeOfProtocol;
    }
    
    public long getLatestVersion(){
        return latestVersion;
    }
    
    public void setLatestVersion(long latestVersion){
        this.latestVersion = latestVersion;
    }
    
    public List<String> getMirrors(){
        return mirrors;
    }
    
    
    public void setMirrors(List<String> mirrors){
        this.mirrors.clear();
        if (mirrors != null){
            for (int i = 0; i < mirrors.size(); i++){
                String mi = new String(mirrors.get(i));
                this.mirrors.add(mi);
            }
        }
    }
    
    public void addMirror(String username){
        for (int i = 0; i < mirrors.size(); i++){
            String m = (String)mirrors.get(i);
            if (username.equals(m))
                return;
        }
        mirrors.add(username);
    }
    
    public boolean removeMirror(String username){
        for (int i = 0; i < mirrors.size(); i++){
            String m = (String)mirrors.get(i);
            if (m.equals(username)){
                mirrors.remove(i);
                return true;
            }
        }
        return false;
    }
    
	public void create(Element el) {
		mirrors = new ArrayList();
		id = Long.parseLong(el.getAttribute("id"));
        lastUpdateTime = Long.parseLong(el.getAttribute("lastUpdateTime"));
        passphrase = el.getAttribute("passphrase");
        NodeList userList = el.getElementsByTagName("user");
		if(userList != null && userList.getLength() > 0) {
            Element usrEl = (Element)userList.item(0);
            usr.create(usrEl);
        }
        status = getTextValue(el, "status");
        sent = getTextValue(el, "sent");
        ipAddress = getTextValue(el, "ipAddress");
        if (getTextValue(el, "port") != null)
            port = Integer.parseInt(getTextValue(el, "port"));
        relayAddress = getTextValue(el, "relayAddress");
        if (getTextValue(el, "relayPort") != null)
            relayPort = Integer.parseInt(getTextValue(el, "relayPort"));
        if (getTextValue(el, "typeOfProtocol") != null)
            typeOfProtocol = Integer.parseInt(getTextValue(el, "typeOfProtocol"));
        if (getTextValue(el, "latestVersion") != null)
            latestVersion = Long.parseLong(getTextValue(el, "latestVersion"));
        NodeList mirrorList = el.getElementsByTagName("mirror");
		if(mirrorList != null && mirrorList.getLength() > 0) {
            for (int i = 0; i < mirrorList.getLength(); i++) {
                Element mi = (Element)mirrorList.item(i);
                String m = getTextValue(mi, "username");
                if (m != null && !m.equals("")){
                    mirrors.add(m);
                }
            }
        }
	}
    
    public Element createDOMElement(Document dom){
        
        Element el = dom.createElement("friend");
		
        el.setAttribute("id", String.valueOf(id));
        el.setAttribute("lastUpdateTime", String.valueOf(lastUpdateTime));
        if (passphrase != null)
            el.setAttribute("passphrase", passphrase);
        if (usr != null)
            el.appendChild(usr.createDOMElement(dom));
        if (status != null){
            Element statusEl = dom.createElement("status");
            Text statusText = dom.createTextNode(status);
            statusEl.appendChild(statusText);
            el.appendChild(statusEl);
        }
        if (sent != null){
            Element sentEl = dom.createElement("sent");
            Text sentText = dom.createTextNode(sent);
            sentEl.appendChild(sentText);
            el.appendChild(sentEl);
        }
        if (ipAddress != null){
            Element ipAddressEl = dom.createElement("ipAddress");
            Text ipAddressText = dom.createTextNode(ipAddress);
            ipAddressEl.appendChild(ipAddressText);
            el.appendChild(ipAddressEl);
        }
        Element portEl = dom.createElement("port");
        Text portText = dom.createTextNode(String.valueOf(port));
        portEl.appendChild(portText);
        el.appendChild(portEl);
        if (relayAddress != null){
            Element relayAddressEl = dom.createElement("relayAddress");
            Text relayAddressText = dom.createTextNode(relayAddress);
            relayAddressEl.appendChild(relayAddressText);
            el.appendChild(relayAddressEl);
        }
        Element relayPortEl = dom.createElement("relayPort");
        Text relayPortText = dom.createTextNode(String.valueOf(relayPort));
        relayPortEl.appendChild(relayPortText);
        el.appendChild(relayPortEl);
        Element typeOfProtocolEl = dom.createElement("typeOfProtocol");
        Text typeOfProtocolText = dom.createTextNode(String.valueOf(typeOfProtocol));
        typeOfProtocolEl.appendChild(typeOfProtocolText);
        el.appendChild(typeOfProtocolEl);
        Element latestVersionEl = dom.createElement("latestVersion");
        Text latestVersionText = dom.createTextNode(String.valueOf(latestVersion));
        latestVersionEl.appendChild(latestVersionText);
        el.appendChild(latestVersionEl);
        if (mirrors.size() > 0){
            for (int i = 0; i < mirrors.size(); i++){
                Element mirrorEl = dom.createElement("mirror");
                String m = (String)mirrors.get(i);
                Element usernameEl = dom.createElement("username");
                Text usernameText = dom.createTextNode(m);
                usernameEl.appendChild(usernameText);
                mirrorEl.appendChild(usernameEl);
                el.appendChild(mirrorEl);
            }
        }
		return el;
    }
    
    public String generateHTML(String type){
        String res = "";
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
            res += "<div class=\"sidebar-entry-textbox2\"><a href=\"friends.jsp?op=deletePendingFriendshipRequest&username=" + usr.getUsername() + "\"><img src=\"images/delete.png\" width=\"15\" height=\"15\">\n";
            res += "</img></a>\n";
            res += "</div>\n";
            res += "</div>\n";
        }else if (type.equals("receivedRequest")){
            res += "<div class=\"right-sidebar-entry-row\">\n";
            res += "<div class=\"sidebar-entry-textbox1\"><a>" + usr.getUsername() + "</a>\n";
            res += "</div>\n";
            res += "<div class=\"sidebar-entry-textbox2\"><a href=\"friends.jsp?op=acceptRequest&username=" + usr.getUsername() + "\"><img src=\"images/check.png\" width=\"15\" height=\"15\"></img></a>\n";
            res += "</div>\n";
            res += "<div class=\"sidebar-entry-textbox2\"><a href=\"friends.jsp?op=deleteAwaitingFriendshipRequest&username=" + usr.getUsername() + "\"><img src=\"images/delete.png\" width=\"15\" height=\"15\">\n";
            res += "</img></a>\n";
            res += "</div>\n";
            res += "</div>\n";
        }else if (type.equals("abstract")){
            res += "<div class=\"left-sidebar-entry-row\">\n";
            res += "<div class=\"sidebar-thumbnail\">\n";
            res += "<img src=\"" + usr.getThumbnail() + "\" alt=\"" + usr.getFirstName() + " " + usr.getLastName() + "\" width=\"50\" height=\"50\">\n";
            res += "</img>\n";
            res += "</div>\n";
            res += "<div class=\"sidebar-entry-textbox\"><a href=\"profile.jsp?profileOwner=" + usr.getUsername() + "\">" + usr.getFirstName() + " " + usr.getLastName() + "</a>\n";
            res += "</div>\n";
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
            res += "<a href=\"profile.jsp?profileOwner=" + usr.getUsername() + "\">" + usr.getFirstName() + " " + usr.getLastName() + "</a>\n";
            res += "</div>\n";
            res += "<div class=\"content-header-textbox2\">\n";
            res += "<a href=\"?op=delete&username=" + usr.getUsername() + "\"><img src=\"images/delete.png\" width=\"15\" height=\"15\">\n";
            res += "</a></img>\n";
            res += "</div>\n";
            res += "</div>\n";
            res += "</div>\n";
            res += "</div>\n";
        }
        return res;
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Friend info \n");
		sb.append("id:" + id);
		sb.append("\n");
        sb.append("lastUpdateTime:" + lastUpdateTime);
		sb.append("\n");
        sb.append("passphrase:" + passphrase);
        sb.append("\n");
        sb.append(usr.toString());
		sb.append("status:" + status);
		sb.append("\n");
        sb.append("sent:" + sent);
		sb.append("\n");
        sb.append("ipAddress:" + ipAddress);
		sb.append("\n");
        sb.append("port:" + port);
		sb.append("\n");
        sb.append("relayAddress:" + relayAddress);
		sb.append("\n");
        sb.append("relayPort:" + relayPort);
		sb.append("\n");
        sb.append("typeOfProtocol:" + typeOfProtocol);
		sb.append("\n");
        sb.append("latestVersion:" + latestVersion);
		sb.append("\n");
        sb.append("mirrors:");
		sb.append("\n");
        for (int i = 0; i < mirrors.size(); i++){
            sb.append("username:" + (String)mirrors.get(i));
            sb.append("\n");
        }
		return sb.toString();
	}
}
