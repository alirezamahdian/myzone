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
 This file contains the implementation of the historyLogEntry element.
 Please refer to history.xsd for detailed description of its elements 
 and attributes. historyLogEntry is used in history.xml to keep track
 of the xml files for wall content and the number of entries that each
 file contains. The maximum number of changes is defined in the settings.
 In order to find the correct xml file for a content in a wall the timestamp 
 of the content is looked up in from the history.xml using historyLogEntry
 elements.
 */

public class historyLogEntry {
    
    private long start;
    private long end;
    private int numberOfChanges;
    
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
    
	public historyLogEntry(long start, long end, int numberOfChanges){
        this.start = start;
        this.end = end;
        this.numberOfChanges = numberOfChanges;
    }
    
    public historyLogEntry(){}
    
    public historyLogEntry(historyLogEntry orig){
        start = orig.getStart();
        end = orig.getEnd();
        numberOfChanges = orig.getNumberOfChanges();
    }
    
    public long getStart(){
        return start;
    }
    
    public void setStart(long start){
        this.start = start;
    }
    
    public long getEnd(){
        return end;
    }
    
    public void setEnd(long end){
        this.end = end;
    }
    
    public int getNumberOfChanges(){
        return numberOfChanges;
    }
    
    public void setNumberOfChanges(int numberOfChanges){
        this.numberOfChanges = numberOfChanges;
    }
    
	public void create(Element el) {
		start = Long.parseLong(getTextValue(el, "start"));
        end = Long.parseLong(getTextValue(el, "end"));
        numberOfChanges = Integer.parseInt(getTextValue(el, "numberOfChanges"));
    }
    
    public Element createDOMElement(Document dom){
        
        Element el = dom.createElement("historyLogEntry");
		
        Element startEl = dom.createElement("start");
        Text startText = dom.createTextNode(String.valueOf(start));
        startEl.appendChild(startText);
        el.appendChild(startEl);
        
        Element endEl = dom.createElement("end");
        Text endText = dom.createTextNode(String.valueOf(end));
        endEl.appendChild(endText);
        el.appendChild(endEl);
        
        Element numberOfChangesEl = dom.createElement("numberOfChanges");
        Text numberOfChangesText = dom.createTextNode(String.valueOf(numberOfChanges));
        numberOfChangesEl.appendChild(numberOfChangesText);
        el.appendChild(numberOfChangesEl);
        
        
		return el;
    }
    
    public String generateHTML(String type){
        String res = "";
        return res;
    }
    
	public String toString() {
		
        StringBuffer sb = new StringBuffer();
		sb.append("History Log Entry \n");
		sb.append("start:" + start);
		sb.append("\n");
        sb.append("end:" + end);
		sb.append("\n");
        sb.append("number of changes:" + numberOfChanges);
		sb.append("\n");
		
		return sb.toString();
	}
}
