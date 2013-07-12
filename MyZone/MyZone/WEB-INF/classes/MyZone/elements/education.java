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

/*
 This file contains the implementation of the education element of a profile.
 Please refer to info.xsd for detailed description of its elements and attributes.
 */

public class education {
    
    private long id;
	private String level;
    private String degree;
	private String major;
    private String institution;
    private String startYear;
    private String finishYear;
    
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
    
    public education(){}
    
    public education(long id, String level, String degree, String major, String institution, String startYear, String finishYear){
        this.id = id;
        this.level = level;
        this.degree = degree;
        this.major = major;
        this.institution = institution;
        this.startYear = startYear;
        this.finishYear = finishYear;
    }
	
    public education(education orig){
        this(orig.getId(), orig.getLevel(), orig.getDegree(), orig.getMajor(), orig.getInstitution(), orig.getStartYear(), orig.getFinishYear());
    }
	public void create(Element el) {
		level = getTextValue(el, "level");
        degree = getTextValue(el, "degree");
        major = getTextValue(el, "major");
        institution = getTextValue(el, "institution");
        startYear = getTextValue(el, "startYear");
        finishYear = getTextValue(el, "finishYear");
	}
    
    public long getId(){
        return id;
    }
    
    public void setId(long id){
        this.id = id;
    }
    
    public String getLevel(){
        return level;
    }
    
    public void setLevel(String level){
        this.level = level;
    }
    
    public String getDegree(){
        return degree;
    }
    
    public void setDegree(String degree){
        this.degree = degree;
    }
    
    public String getMajor(){
        return major;
    }
    
    public void setMajor(String major){
        this.major = major;
    }
    
    public String getInstitution(){
        return institution;
    }
    
    public void setInstitution(String institution){
        this.institution = institution;
    }
    
    public String getStartYear(){
        return startYear;
    }
    
    public void setStartYear(String startYear){
        this.startYear = startYear;
    }
    
    public String getFinishYear(){
        return finishYear;
    }
    
    public void setFinishYear(String finishYear){
        this.finishYear = finishYear;
    }
    
    public Element createDOMElement(Document dom){
        Element el = dom.createElement("education");
		
        Element idEl = dom.createElement("id");
        Text idText = dom.createTextNode(String.valueOf(id));
        idEl.appendChild(idText);
        el.appendChild(idEl);
        
        if (level != null){
            Element levelEl = dom.createElement("level");
            Text levelText = dom.createTextNode(level);
            levelEl.appendChild(levelText);
            el.appendChild(levelEl);
        }
        
        if (degree != null){
            Element degreeEl = dom.createElement("degree");
            Text degreeText = dom.createTextNode(degree);
            degreeEl.appendChild(degreeText);
            el.appendChild(degreeEl);
        }
        
        if (major != null){
            Element majorEl = dom.createElement("major");
            Text majorText = dom.createTextNode(major);
            majorEl.appendChild(majorText);
            el.appendChild(majorEl);
        }
        
        if (institution != null){
            Element instEl = dom.createElement("institution");
            Text instText = dom.createTextNode(institution);
            instEl.appendChild(instText);
            el.appendChild(instEl);
        }
        
        if (startYear != null){
            Element startYearEl = dom.createElement("startYear");
            Text startYearText = dom.createTextNode(startYear);
            startYearEl.appendChild(startYearText);
            el.appendChild(startYearEl);
        }
        
        if (finishYear != null){
            Element finishYearEl = dom.createElement("finishYear");
            Text finishYearText = dom.createTextNode(finishYear);
            finishYearEl.appendChild(finishYearText);
            el.appendChild(finishYearEl);
        }
        
		return el;
    }
    
    public String generateHTML(){
        if (institution == null){
            institution = "";
        }
        if (level == null){
            level = "";
        }
        if (degree == null){
            degree = "";
        }
        if (major == null){
            major = "";
        }
        if (startYear == null){
            startYear = "";
        }
        if (finishYear == null){
            finishYear = "";
        }
        String res = "";
        res += "<p><h3>Institution:&nbsp;</h3>&nbsp;" + institution.replaceAll("\n", "<br />") + "</p>\n";
        res += "<p><h3>Level:&nbsp;</h3>&nbsp;" + level.replaceAll("\n", "<br />") + "</p>\n";
        res += "<p><h3>Degree:&nbsp;</h3>&nbsp;" + degree.replaceAll("\n", "<br />") + "</p>\n";
        res += "<p><h3>Major:&nbsp;</h3>&nbsp;" + major.replaceAll("\n", "<br />") + "</p>\n";
        res += "<p><h3>Attended From:&nbsp;</h3>&nbsp;" + startYear + "&nbsp;To &nbsp;" + finishYear + "</p>\n<br />\n<hr />\n";
        return res;
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Education info \n");
		sb.append("institution:" + institution);
		sb.append("\n");
        sb.append("level:" + level);
		sb.append("\n");
        sb.append("degree:" + degree);
		sb.append("\n");
		sb.append("major:" + major);
		sb.append("\n");
		sb.append("start year:" + startYear);
		sb.append("\n");
		sb.append("finish year:" + finishYear);
		sb.append("\n");
        
		return sb.toString();
	}
}
