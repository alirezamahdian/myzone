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

/*
 This file contains the implementation of the changeEntry element of a profile.
 Please refer to change.xsd for detailed description of its elements and 
 attributes. changeEntry is used in modifiedFiles.xml to track the modified
 files based on the time of their last modification.
 */

public class changeEntry {
    
    private long id;
    private String filename;
    
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
    
	public changeEntry(long id, String filename){
        this.id = id;
        this.filename = filename;
    }
    
    public changeEntry(){}
    
    public changeEntry(changeEntry orig){
        id = orig.getId();
        filename = new String(orig.getFilename());
    }
    
    public long getId(){
        return id;
    }
    
    public void setId(long id){
        this.id = id;
    }
    
    public String getFilename(){
        return filename;
    }
    
    public void setFilename(String filename){
        this.filename = filename;
    }
    
	public void create(Element el) {
        id = Long.parseLong(el.getAttribute("id"));
		filename = el.getAttribute("filename");
    }
    
    public Element createDOMElement(Document dom){
        
        Element el = dom.createElement("changeEntry");
		
        el.setAttribute("id", String.valueOf(id));
        el.setAttribute("filename", filename);
        
        return el;
    }
    
    public String generateHTML(String type){
        String res = "";
        return res;
    }
    
	public String toString() {
		
        StringBuffer sb = new StringBuffer();
		sb.append("Change Entry \n");
		sb.append("id:" + id);
		sb.append("\n");
        sb.append("filename:" + filename);
		sb.append("\n");
		
		return sb.toString();
	}
}
