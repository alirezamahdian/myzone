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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Text;
import MyZone.*;

public class user {
    
    private String username;
    private String firstName;
	private String lastName;
    private long memberSince;
    private Settings mainSettings = new Settings("./MyZone/");
    
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
    
    public user(){
        username = null;
        firstName = null;
        lastName = null;
    }
    
	public user(String username, String firstName, String lastName, long memberSince){
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.memberSince = memberSince;
    }
	
    public user(user orig){
        this.username = orig.getUsername();
        this.firstName = orig.getFirstName();
        this.lastName = orig.getLastName();
        this.memberSince = orig.getMemberSince();
    }
    
    public void setUsername(String username){
        this.username = username;
    }
    
    public void setFirstName(String firstName){
        this.firstName = firstName;
    }
    
    public void setLastName(String lastName){
        this.lastName = lastName;
    }
    
    public void setMemberSince(long memberSince){
        this.memberSince = memberSince;
    }
    
	public void create(Element el) {
		username = getTextValue(el, "username");
        firstName = getTextValue(el, "firstName");
        lastName = getTextValue(el, "lastName");
        if (getTextValue(el, "memberSince") != null){
            memberSince = Long.parseLong(getTextValue(el, "memberSince"));
        }else{
            memberSince = 0;
        }
    }
    
    public Element createDOMElement(Document dom){
        Element el = dom.createElement("user");
		
        if (username != null){
            Element usernameEl = dom.createElement("username");
            Text usernameText = dom.createTextNode(username);
            usernameEl.appendChild(usernameText);
            el.appendChild(usernameEl);
        }
        if (firstName != null){
            Element firstNameEl = dom.createElement("firstName");
            Text firstNameText = dom.createTextNode(firstName);
            firstNameEl.appendChild(firstNameText);
            el.appendChild(firstNameEl);
        }
        if (lastName != null){
            Element lastNameEl = dom.createElement("lastName");
            Text lastNameText = dom.createTextNode(lastName);
            lastNameEl.appendChild(lastNameText);
            el.appendChild(lastNameEl);
        }
        Element memberSinceEl = dom.createElement("memberSince");
        Text memberSinceText = dom.createTextNode(String.valueOf(memberSince));
        memberSinceEl.appendChild(memberSinceText);
        el.appendChild(memberSinceEl);
		return el;
    }
    
    public String getUsername(){
        return username; 
    }
    public String getFirstName(){
        if (firstName == null){
            return username;
        }
        return firstName; 
    }
    public String getLastName(){
        if (lastName == null){
            return "";
        }
        return lastName;  
    }
    public long getMemberSince(){
        return memberSince;  
    }
    public String getThumbnail(){ 
        mainSettings.refresh(mainSettings.BASIC_INFO);
        String path = mainSettings.username + "/thumbnails";
        File dir = new File("./MyZone/" + path);;
        
        String[] children = dir.list();
        String thumbnail = null;
        if (children != null) {
            for (int i=0; i<children.length; i++) {
                String filename = children[i];
                if (filename.contains(username)){
                    thumbnail = path + "/" + filename;
                    break;
                }
            }
        }
        if (thumbnail == null){
            thumbnail = "images/person.png";
        }
        return thumbnail; 
    }
    
    public String generateHTML(){
        String res = "";
        return res;
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("user info \n");
		sb.append("username:" + username);
		sb.append("\n");
        sb.append("first name:" + firstName);
		sb.append("\n");
        sb.append("last name:" + lastName);
		sb.append("\n");
        sb.append("member since:" + memberSince);
		sb.append("\n");
        return sb.toString();
	}
}
