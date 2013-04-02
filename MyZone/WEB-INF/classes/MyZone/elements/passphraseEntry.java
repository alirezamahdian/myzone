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
 This file contains the implementation of the passphrase element.
 Please refer to passphrase.xsd for detailed description of its 
 elements and attributes.
 */

public class passphraseEntry {
    
    private String passphrase;
    private String username;
	
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
    
    public passphraseEntry(){
    }
    
	public passphraseEntry(String passphrase, String username){
        this.passphrase = passphrase;
        this.username = username;
    }
	
    public passphraseEntry(passphraseEntry orig){
        this(orig.getPassphrase(), orig.getUsername());
    }
    
    public String getPassphrase(){
        return passphrase;
    }
    
    public void setPassphrase(String passphrase){
        this.passphrase = passphrase;
    }
    
    public String getUsername(){
        return username;
    }
    
    public void setUsername(String username){
        this.username = username;
    }
    
	public void create(Element el) {
		
		passphrase = el.getAttribute("passphrase");
        username = el.getAttribute("username");;
	}
    
    public Element createDOMElement(Document dom){
        
        Element el = dom.createElement("passphraseEntry");
		if (passphrase != null)
            el.setAttribute("passphrase", passphrase);
        if (username != null)
            el.setAttribute("username", username);
        
		return el;
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Passphrase Entry info \n");
		sb.append("passphrase:" + passphrase);
        sb.append("\n");
        sb.append("username:" + username);
        sb.append("\n");
        return sb.toString();
	}
}
