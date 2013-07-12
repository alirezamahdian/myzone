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

/*
 This file contains the implementation of the fileListEntry element.
 Please refer to fileList.xsd for detailed description of its elements 
 and attributes. fileListEntry is used to build correctImage.xml and 
 existingImage.xml
 */

public class fileListEntry {
    
    private String owner;
    private String filename;
    private String path;
    private long filesize;
    private String zone;
    
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
    
    public fileListEntry(){
    }
    
    public fileListEntry(String owner,
                         String zone,
                         String path,
                         String filename,
                         long filesize)
    {
        this.owner = owner;
        this.path = path;
        this.filename = filename;
        this.filesize = filesize;
        this.zone = zone;
    }
    
    public fileListEntry(fileListEntry orig)
    {
        this.owner = orig.getOwner();
        this.path = orig.getPath();
        this.filename = orig.getFilename();
        this.filesize = orig.getFilesize();
        this.zone = orig.getZone();
    }
    
    public String getOwner(){
        return owner;
    }
    
    public void setOwner(String owner){
        this.owner = owner;
    }
    
    public String getZone(){
        return zone;
    }
    
    public void setZone(String zone){
        this.zone = zone;
    }
    
    public String getPath(){
        return path;
    }
    
    public void setPath(String path){
        this.path = path;
    }
    
    public String getFilename(){
        return filename;
    }
    
    public void setFilename(String filename){
        this.filename = filename;
    }
    
    public long getFilesize(){
        return filesize;
    }
    
    public void setFilesize(long filesize){
        this.filesize = filesize;
    }
    
	public void create(Element el) {
		owner = el.getAttribute("owner");
        zone = el.getAttribute("zone");
        path = el.getAttribute("path");
        filename = el.getAttribute("filename");
        filesize = Long.parseLong(el.getAttribute("filesize"));
	}
    
    public Element createDOMElement(Document dom){
        Element el = dom.createElement("fileListEntry");
		
        if (owner != null)
            el.setAttribute("owner", owner);
        if (zone != null)
            el.setAttribute("zone", zone);
        if (path != null)
            el.setAttribute("path", path);
        if (filename != null)
            el.setAttribute("filename", filename);
        el.setAttribute("filesize", String.valueOf(filesize));
		return el;
    }
    
    public String generateHTML(String type){
        String res = "";
        return res;
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("fileListEntry info \n");
		sb.append("owner:" + owner);
		sb.append("\n");
        sb.append("zone:" + zone);
		sb.append("\n");
        sb.append("path:" + path);
		sb.append("\n");
        sb.append("filename:" + filename);
		sb.append("\n");
        sb.append("filesize:" + filesize);
		sb.append("\n");
		return sb.toString();
	}
}
