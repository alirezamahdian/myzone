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
import MyZone.elements.date;
import MyZone.*;

/*
 This file contains the implementation of the info element of a profile.
 Please refer to info.xsd for detailed description of its elements and
 attributes.
 */

public class info {
    
    private long id;
    private long lastUpdateTime;
    private String username;
	private String firstName;
	private String lastName;
    private String gender;
    private date dateOfBirth;
    private String relationshipStatus;
    private String aboutMe;
    private String profilePic;
    private Settings mainSettings;
    
    private List<education> educations = new ArrayList();
    private List<employment> employments = new ArrayList();
    
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
    
	public info(){
        mainSettings = new Settings("./MyZone/");
        mainSettings.refresh(mainSettings.BASIC_INFO);
        if (mainSettings.username == null){
            mainSettings = new Settings("../../");
            mainSettings.refresh(mainSettings.BASIC_INFO);
        }
        id = 0;
        lastUpdateTime = 0;
        dateOfBirth = new date();
    }
	
    public info(long id,
                long lastUpdateTime,
                String username,
                String firstName,
                String lastName,
                String gender,
                date dateOfBirth,
                String relationshipStatus,
                String aboutMe,
                String profilePic,
                List<education> educations,
                List<employment> employments)
    {
        mainSettings = new Settings("./MyZone/");
        mainSettings.refresh(mainSettings.BASIC_INFO);
        if (mainSettings.username == null){
            mainSettings = new Settings("../../");
            mainSettings.refresh(mainSettings.BASIC_INFO);
        }
        this.id = id;
        this.lastUpdateTime = lastUpdateTime;
        this.username = username;
        this.lastName = lastName;
        this.firstName = firstName;
        this.gender = gender;
        if (dateOfBirth != null)
            this.dateOfBirth = new date(dateOfBirth);
        this.relationshipStatus = relationshipStatus;
        this.aboutMe = aboutMe;
        this.profilePic = profilePic;
        if (educations != null){
            this.educations.clear();
            for (int i = 0; i < educations.size(); i++){
                education ed = new education(educations.get(i));
                this.educations.add(ed);
            }
        }
        if (employments != null){
            this.employments.clear();
            for (int i = 0; i < employments.size(); i++){
                employment em = new employment(employments.get(i));
                this.employments.add(em);
            }
        }
    }
    
    public info(info orig){
        mainSettings = new Settings("./MyZone/");
        mainSettings.refresh(mainSettings.BASIC_INFO);
        if (mainSettings.username == null){
            mainSettings = new Settings("../../");
            mainSettings.refresh(mainSettings.BASIC_INFO);
        }
        this.id = orig.getId();
        this.lastUpdateTime = orig.getLastUpdateTime();
        this.username = orig.getUsername();
        this.firstName = orig.getFirstName();
        this.lastName = orig.getLastName();
        this.gender = orig.getGender();
        if (orig.getDateOfBirth() != null)
            this.dateOfBirth = new date(orig.getDateOfBirth());
        this.relationshipStatus = orig.getRelationshipStatus();
        this.aboutMe = orig.getAboutMe();
        if (orig.getEducations() != null){
            List<education> tempEdu = orig.getEducations();
            this.educations.clear();
            for (int i = 0; i < tempEdu.size(); i++){
                education ed = new education(tempEdu.get(i));
                this.educations.add(ed);
            }
        }
        if (orig.getEmployments() != null){
            this.employments.clear();
            List<employment> tempEmp = orig.getEmployments();
            this.employments.clear();
            for (int i = 0; i < tempEmp.size(); i++){
                employment em = new employment(tempEmp.get(i));
                this.employments.add(em);
            }
        }
    }
    
    public long getId(){
        return id;
    }
    
    public void setId(long id){
        this.id = id;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public long getLastUpdateTime(){
        return lastUpdateTime;
    }
    
    public void setLastUpdateTime(long lastUpdateTime){
        this.lastUpdateTime = lastUpdateTime;
    }
    
    public String getUsername(){
        if (username == null){
            return "";
        }
        return username;
    }
    
    public void setUsername(String username){
        this.username = username;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public String getFirstName(){
        if (firstName == null){
            return "";
        }
        return firstName;
    }
    
    public void setFirstName(String firstName){
        this.firstName = firstName;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public String getLastName(){
        if (lastName == null){
            return "";
        }
        return lastName;
    }
    
    public void setLastName(String lastName){
        this.lastName = lastName;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public String getGender(){
        return gender;
    }
    
    public void setGender(String gender){
        this.gender = gender;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public date getDateOfBirth(){
        return dateOfBirth;
    }
    
    public void setDateOfBirth(date dateOfBirth){
        this.dateOfBirth.setYear(dateOfBirth.getYear());
        this.dateOfBirth.setMonth(Integer.parseInt(dateOfBirth.getMonth("numerical")));
        this.dateOfBirth.setDay(dateOfBirth.getDay());
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public String getRelationshipStatus(){
        return relationshipStatus;
    }
    
    public void setRelationshipStatus(String relationshipStatus){
        this.relationshipStatus = relationshipStatus;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public String getAboutMe(){
        return aboutMe;
    }
    
    public void setAboutMe(String aboutMe){
        this.aboutMe = aboutMe;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
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
    
    public String getProfilePic(){
        mainSettings.refresh(mainSettings.BASIC_INFO);
        String path = "";
        if (profilePic == null){
            return "images/person.png";
        }
        if (profilePic.equals("")){
            return "images/person.png";
        }
        if (mainSettings.username.equals(username)){
            path = mainSettings.username + "/photos/";
        }else{
            path = mainSettings.username + "/friends/" + username + "/photos/";
        }
        return path + profilePic;
    }
    
    public void setProfilePic(String profilePic){
        this.profilePic = profilePic;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public List getEmployments(){
        return employments;
    }
    
    public void setEmployments(List<employment> employments){
        this.employments.clear();
        if (employments != null){
            for (int i = 0; i < employments.size(); i++){
                employment em = new employment(employments.get(i));
                this.employments.add(em);
            }
        }
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public void addEmployment(employment e){
        employments.add(e);
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public boolean removeEmployment(long id){
        Iterator it  = employments.iterator();
        for (int i = 0; i < employments.size(); i++){
            employment e = (employment)it.next();
            if (e.getId() == id){
                employments.remove(i);
                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                lastUpdateTime = now.getTimeInMillis();
                return true;
            }
		}
        return false;
    }
    
    public List getEducations(){
        return educations;
    }
    
    public void setEducations(List<education> educations){
        this.educations.clear();
        if (educations != null){
            for (int i = 0; i < educations.size(); i++){
                education ed = new education(educations.get(i));
                this.educations.add(ed);
            }
        }
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public void addEducation(education e){
        educations.add(e);
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public boolean removeEducation(long id){
        Iterator it  = educations.iterator();
        for (int i = 0; i < educations.size(); i++){
            education e = (education)it.next();
            if (e.getId() == id){
                educations.remove(i);
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
        NodeList basicList = el.getElementsByTagName("basic");
        if(basicList != null && basicList.getLength() > 0) {
            Element basicEl = (Element)basicList.item(0);
            username = getTextValue(basicEl, "username");
            firstName = getTextValue(basicEl, "firstName");
            lastName = getTextValue(basicEl, "lastName");
            gender = getTextValue(basicEl, "gender");
            NodeList dateOfBirthList = basicEl.getElementsByTagName("dateOfBirth");
            if(dateOfBirthList != null && dateOfBirthList.getLength() > 0) {
                Element dateOfBirthEl = (Element)dateOfBirthList.item(0);
                dateOfBirth = new date(Integer.parseInt(getTextValue(dateOfBirthEl, "year")), Integer.parseInt(getTextValue(dateOfBirthEl, "month")), Integer.parseInt(getTextValue(dateOfBirthEl, "day")), 0, 0);
            }
            relationshipStatus = getTextValue(basicEl, "relationshipStatus");
            aboutMe = getTextValue(basicEl, "aboutMe");
            profilePic = getTextValue(basicEl, "profilePic");
        }
        educations.clear();
        NodeList eduList = el.getElementsByTagName("education");
        if(eduList != null && eduList.getLength() > 0) {
            for(int i = 0; i < eduList.getLength(); i++){
                Element eduEl = (Element)eduList.item(i);
                education edu = new education();
                edu.create(eduEl);
                educations.add(edu);
            }
        }
        employments.clear();
        NodeList empList = el.getElementsByTagName("employment");
        if(empList != null && empList.getLength() > 0) {
            for(int i = 0; i < empList.getLength(); i++){
                Element empEl = (Element)empList.item(i);
                employment emp = new employment();
                emp.create(empEl);
                employments.add(emp);
            }
        }
	}
    
    public Element createDOMElement(Document dom){
        Element el = dom.createElement("info");
		
        el.setAttribute("id", String.valueOf(id));
        el.setAttribute("lastUpdateTime", String.valueOf(lastUpdateTime));
		
        Element basicEl = dom.createElement("basic");
        
        if (username != null){
            Element usernameEl = dom.createElement("username");
            Text usernameText = dom.createTextNode(username);
            usernameEl.appendChild(usernameText);
            basicEl.appendChild(usernameEl);
        }
        if (firstName != null){
            Element firstNameEl = dom.createElement("firstName");
            Text firstNameText = dom.createTextNode(firstName);
            firstNameEl.appendChild(firstNameText);
            basicEl.appendChild(firstNameEl);
        }
        if (lastName != null){
            Element lastNameEl = dom.createElement("lastName");
            Text lastNameText = dom.createTextNode(lastName);
            lastNameEl.appendChild(lastNameText);
            basicEl.appendChild(lastNameEl);
        }
        if (gender != null){
            Element genderEl = dom.createElement("gender");
            Text genderText = dom.createTextNode(gender);
            genderEl.appendChild(genderText);
            basicEl.appendChild(genderEl);
        }
        if (dateOfBirth != null){
            Element dateOfBirthEl = dom.createElement("dateOfBirth");
            
            Element yearEl = dom.createElement("year");
            Text yearText = dom.createTextNode(String.valueOf(dateOfBirth.getYear()));
            yearEl.appendChild(yearText);
            dateOfBirthEl.appendChild(yearEl);
            
            Element monthEl = dom.createElement("month");
            Text monthText = dom.createTextNode(dateOfBirth.getMonth("numerical"));
            monthEl.appendChild(monthText);
            dateOfBirthEl.appendChild(monthEl);
            
            Element dayEl = dom.createElement("day");
            Text dayText = dom.createTextNode(String.valueOf(dateOfBirth.getDay()));
            dayEl.appendChild(dayText);
            dateOfBirthEl.appendChild(dayEl);
            basicEl.appendChild(dateOfBirthEl);
        }
        if (relationshipStatus != null){
            Element relationshipStatusEl = dom.createElement("relationshipStatus");
            Text relationshipStatusText = dom.createTextNode(relationshipStatus);
            relationshipStatusEl.appendChild(relationshipStatusText);
            basicEl.appendChild(relationshipStatusEl);
        }
        if (aboutMe != null){
            Element aboutMeEl = dom.createElement("aboutMe");
            Text aboutMeText = dom.createTextNode(aboutMe);
            aboutMeEl.appendChild(aboutMeText);
            basicEl.appendChild(aboutMeEl);
        }
        if (profilePic != null){
            Element profilePicEl = dom.createElement("profilePic");
            Text profilePicText = dom.createTextNode(profilePic);
            profilePicEl.appendChild(profilePicText);
            basicEl.appendChild(profilePicEl);
        }
        el.appendChild(basicEl);
        
        Iterator it  = educations.iterator();
		while(it.hasNext()) {
			education ed = (education)it.next();
			el.appendChild(ed.createDOMElement(dom));
		}
        
        it  = employments.iterator();
		while(it.hasNext()) {
			employment em = (employment)it.next();
			el.appendChild(em.createDOMElement(dom));
		}
        
		return el;
    }
    
    public String generateHTML(){
        if (lastName == null){
            lastName = "";
        }
        if (firstName == null){
            firstName = "";
        }
        String res = "";
        mainSettings.refresh(mainSettings.BASIC_INFO);
        if (gender == null){
            gender = "";
        }
        String path = mainSettings.username + "/thumbnails";
        File dir = new File("./MyZone/" + path);
        String[] children = dir.list();
        String thumbnail = null;
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
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
        res += "<div class=\"right-sidebar-entry-row\">\n";
        res += "<div class=\"sidebar-thumbnail\">\n";
        res += "<img src=\"" + thumbnail + "\" width=\"50\" height=\"50\">\n";
        res += "</img>\n";
        res += "</div>\n";
        res += "<div class=\"sidebar-entry-textbox3\">\n";
        res += "<p><h3>First Name:&nbsp;</h3>&nbsp;" + firstName + "</p>\n";
        res += "<p><h3>Last Name:&nbsp;</h3>&nbsp;" + lastName + "</p>\n";
        res += "<p><h3>Gender:&nbsp;</h3>&nbsp;" + gender + "</p>\n";
        String day = " ";
        if (dateOfBirth.getDay() != 0){
            day = String.valueOf(dateOfBirth.getDay()) + "&nbsp; of &nbsp;";
        }
        String year = " ";
        if (dateOfBirth.getYear() != 0){
            year = String.valueOf(dateOfBirth.getYear());
        }
        res += "<p><h3>Date Of Birth:&nbsp;</h3>&nbsp;" + day + dateOfBirth.getMonth("completeAlphabetical") + "&nbsp;" + year + "</p>\n";
        if (relationshipStatus == null){
            relationshipStatus = "&nbsp;";
        }
        res += "<p><h3>Relationship Status:&nbsp;</h3>&nbsp;" + relationshipStatus.replaceAll("\n", "<br />") + "</p>\n";
        if (aboutMe == null){
            aboutMe = "&nbsp;";
        }
        res += "<p><h3>About Me:&nbsp;</h3>&nbsp;" + aboutMe.replaceAll("\n", "<br />") + "</p>\n<br />\n<hr />\n";
        res += "<h2>Education History:</h2><br /><hr />\n";
        Iterator it  = educations.iterator();
		while(it.hasNext()) {
			education ed = (education)it.next();
			res += ed.generateHTML();
		}
        res += "<h2>Employment History:</h2><br /><hr />\n";
        it  = employments.iterator();
		while(it.hasNext()) {
			employment em = (employment)it.next();
			res += em.generateHTML();
		}
        res += "</div>\n";
        res += "</div>\n";
        return res;
        
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("User info \n");
		sb.append("id:" + id);
		sb.append("\n");
        sb.append("lastUpdateTime:" + lastUpdateTime);
		sb.append("\n");
        sb.append("first name:" + firstName);
		sb.append("\n");
		sb.append("last name:" + lastName);
		sb.append("\n");
		sb.append("gender:" + gender);
		sb.append("\n");
		sb.append("date of birth:" + dateOfBirth.toString());
		sb.append("\n");
        sb.append("relationship status:" + relationshipStatus);
		sb.append("\n");
        sb.append("about me:" + aboutMe);
		sb.append("\n");
		Iterator it  = educations.iterator();
		while(it.hasNext()) {
			education ed = (education)it.next();
			sb.append(ed.toString());
		}
        it  = employments.iterator();
		while(it.hasNext()) {
			employment em = (employment)it.next();
			sb.append(em.toString());
		}
        
		return sb.toString();
	}
}
