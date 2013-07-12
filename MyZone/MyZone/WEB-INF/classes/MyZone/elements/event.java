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
import MyZone.elements.user;
import MyZone.*;

/*
 This file contains the implementation of the event element.
 Please refer to events.xsd for detailed description of its elements and attributes.
 Note that pendingNotification element of event contains the list of all users that 
 are invited to the invited but are yet to be notified and is used in postingThread
 of the clientThread class.
 */

public class event {
    
    private long id;
    private long lastUpdateTime;
    private String title;
    private String location;
    private String decision;
    private String description;
	private user creator;
	private date startDate;
    private date endDate;
    private List<user> invitees = new ArrayList();
    private List<user> accepted = new ArrayList();
    private List<user> declined = new ArrayList();
    private List<user> pendingNotification = new ArrayList();
    private Settings settings;
    
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
    
    public event(){
        creator = new user();
        startDate = new date();
        endDate = new date();
        settings = new Settings("./MyZone/");
        settings.refresh(settings.BASIC_INFO);
        if (settings.username == null){
            settings = new Settings("../../");
            settings.refresh(settings.BASIC_INFO);
        }
    }
    
	public event(long id, long lastUpdateTime, String title, String description, String location, String decision, user creator, date startDate, date endDate, List<user> invitees, List<user> accepted, List<user> declined){
        settings = new Settings("./MyZone/");
        settings.refresh(settings.BASIC_INFO);
        if (settings.username == null){
            settings = new Settings("../../");
            settings.refresh(settings.BASIC_INFO);
        }
        this.id = id;
        this.lastUpdateTime = lastUpdateTime;
        this.title = title;
        this.location = location;
        this.decision = decision;
        this.description = description;
        this.creator = new user();
        if (creator != null){
            this.creator.setUsername(creator.getUsername());
            this.creator.setFirstName(creator.getFirstName());
            this.creator.setLastName(creator.getLastName());
        }
        if (startDate != null)
            this.startDate = new date(startDate);
        if (endDate != null)
            this.endDate = new date(endDate);
        if (invitees != null){
            this.invitees.clear();
            for (int i = 0; i < invitees.size(); i++){
                user u = new user(invitees.get(i));
                this.invitees.add(u);
                pendingNotification.add(u);
            }
        }
        if (accepted != null){
            this.accepted.clear();
            for (int i = 0; i < accepted.size(); i++){
                user u = new user(accepted.get(i));
                this.accepted.add(u);
            }
        }
        if (declined != null){
            this.declined.clear();
            for (int i = 0; i < declined.size(); i++){
                user u = new user(declined.get(i));
                this.declined.add(u);
            }
        }
    }
	
    public event(event orig){
        settings = new Settings("./MyZone/");
        settings.refresh(settings.BASIC_INFO);
        if (settings.username == null){
            settings = new Settings("../../");
            settings.refresh(settings.BASIC_INFO);
        }
        this.id = orig.getId();
        this.lastUpdateTime = orig.getLastUpdateTime();
        this.title = orig.getTitle();
        this.location = orig.getLocation();
        this.decision = orig.getDecision();
        this.description = orig.getDescription();
        if (orig.getCreator() != null){
            this.creator = new user(orig.getCreator());
        }
        if (orig.getStartDate() != null){
            this.startDate = new date(orig.getStartDate());
        }
        if (orig.getEndDate() != null){
            this.endDate = new date(orig.getEndDate());
        }
        if (orig.getInvitees() != null){
            this.invitees.clear();
            List<user> tempInvitees = orig.getInvitees();
            for (int i = 0; i < tempInvitees.size(); i++){
                user u = new user(tempInvitees.get(i));
                this.invitees.add(u);
            }
        }
        if (orig.getPendingNotification() != null){
            this.pendingNotification.clear();
            List<user> tempPendingNotification = orig.getPendingNotification();
            for (int i = 0; i < tempPendingNotification.size(); i++){
                user u = new user(tempPendingNotification.get(i));
                this.pendingNotification.add(u);
            }
        }
        if (orig.getAccepted() != null){
            this.accepted.clear();
            List<user> tempAccepted = orig.getAccepted();
            for (int i = 0; i < tempAccepted.size(); i++){
                user u = new user(tempAccepted.get(i));
                this.accepted.add(u);
            }
        }
        if (orig.getDeclined() != null){
            this.declined.clear();
            List<user> tempDeclined = orig.getDeclined();
            for (int i = 0; i < tempDeclined.size(); i++){
                user u = new user(tempDeclined.get(i));
                this.declined.add(u);
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
    
    public String getTitle(){
        return title;
    }
    
    public void setTitle(String title){
        this.title = title;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public String getLocation(){
        return location;
    }
    
    public void setLocation(String location){
        this.location = location;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public String getDescription(){
        return description;
    }
    
    public void setDescription(String description){
        this.description = description;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public String getDecision(){
        return decision;
    }
    
    public void setDecision(String decision){
        this.decision = decision;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    public user getCreator(){
        return creator;
    }
    
    public void setCreator(user creator){
        this.creator.setUsername(creator.getUsername());
        this.creator.setFirstName(creator.getFirstName());
        this.creator.setLastName(creator.getLastName());
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public date getStartDate(){
        return startDate;
    }
    
    public void setStartDate(date startDate){
        this.startDate.setYear(startDate.getYear());
        this.startDate.setMonth(Integer.parseInt(startDate.getMonth("numerical")));
        this.startDate.setDay(startDate.getDay());
        this.startDate.setHour(startDate.getHour("24"));
        this.startDate.setMinute(startDate.getMinute());
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public date getEndDate(){
        return endDate;
    }
    
    public void setEndDate(date endDate){
        this.endDate.setYear(endDate.getYear());
        this.endDate.setMonth(Integer.parseInt(endDate.getMonth("numerical")));
        this.endDate.setDay(endDate.getDay());
        this.endDate.setHour(endDate.getHour("24"));
        this.endDate.setMinute(endDate.getMinute());
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public List getInvitees(){
        return invitees;
    }
    
    public void setInvitees(List<user> invitees){
        if (invitees != null){
            this.invitees.clear();
            for (int i = 0; i < invitees.size(); i++){
                user u = new user(invitees.get(i));
                this.invitees.add(u);
            }
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            lastUpdateTime = now.getTimeInMillis();
        }
    }
    
    public List getPendingNotification(){
        return pendingNotification;
    }
    
    public void setPendingNotification(List<user> pendingNotification){
        if (pendingNotification != null){
            this.pendingNotification.clear();
            for (int i = 0; i < pendingNotification.size(); i++){
                user u = new user(pendingNotification.get(i));
                this.pendingNotification.add(u);
            }
        }
    }
    
    public void addInvitee(user u){
        for (int i = 0; i < invitees.size(); i++){
            user in = (user)invitees.get(i);
            if (in.getUsername().equals(u.getUsername())){
                return;
            }
        }
        invitees.add(u);
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public boolean removeInvitee(String username){
        Iterator it  = invitees.iterator();
        for (int i = 0; i < invitees.size(); i++){
            user u = (user)it.next();
            if (u.getUsername().equals(username)){
                invitees.remove(i);
                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                lastUpdateTime = now.getTimeInMillis();
                return true;
            }
		}
        return false;
    }
    
    public List getAccepted(){
        return accepted;
    }
    
    public void setAccepted(List<user> accepted){
        if (accepted != null){
            this.accepted.clear();
            for (int i = 0; i < accepted.size(); i++){
                user u = new user(accepted.get(i));
                this.accepted.add(u);
            }
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            lastUpdateTime = now.getTimeInMillis();
        }
    }
    
    public void addAccepted(user u){
        for (int i = 0; i < accepted.size(); i++){
            user ac = (user)accepted.get(i);
            if (ac.getUsername().equals(u.getUsername())){
                return;
            }
        }
        accepted.add(u);
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public boolean removeAccepted(String username){
        Iterator it  = accepted.iterator();
        for (int i = 0; i < accepted.size(); i++){
            user u = (user)it.next();
            if (u.getUsername().equals(username)){
                accepted.remove(i);
                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                lastUpdateTime = now.getTimeInMillis();
                return true;
            }
		}
        return false;
    }
    
    public List getDeclined(){
        return declined;
    }
    
    public void setDeclined(List<user> declined){
        if (declined != null){
            this.declined.clear();
            for (int i = 0; i < declined.size(); i++){
                user u = new user(declined.get(i));
                this.declined.add(u);
            }
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            lastUpdateTime = now.getTimeInMillis();
        }
    }
    
    public void addDeclined(user u){
        for (int i = 0; i < declined.size(); i++){
            user de = (user)declined.get(i);
            if (de.getUsername().equals(u.getUsername())){
                return;
            }
        }
        declined.add(u);
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUpdateTime = now.getTimeInMillis();
    }
    
    public boolean removeDeclined(String username){
        Iterator it  = declined.iterator();
        for (int i = 0; i < declined.size(); i++){
            user u = (user)it.next();
            if (u.getUsername().equals(username)){
                declined.remove(i);
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
        title = getTextValue(el, "title");
        location = getTextValue(el, "location");
        decision = getTextValue(el, "decision");
        description = getTextValue(el, "description");
        
        NodeList creatorList = el.getElementsByTagName("creator");
		if(creatorList != null && creatorList.getLength() > 0) {
            Element creatorEl = (Element)creatorList.item(0);
            NodeList userList = creatorEl.getElementsByTagName("user");
            if(userList != null && userList.getLength() > 0) {
                Element userEl = (Element)userList.item(0);
                creator.create(userEl);
            }
        }
        
        NodeList startDateList = el.getElementsByTagName("startDate");
		if(startDateList != null && startDateList.getLength() > 0) {
			Element startDateEl = (Element)startDateList.item(0);
            startDate.create(startDateEl);
        }
        
        NodeList endDateList = el.getElementsByTagName("endDate");
		if(endDateList != null && endDateList.getLength() > 0) {
			Element endDateEl = (Element)endDateList.item(0);
            endDate.create(endDateEl);
        }
        settings.refresh(settings.FRIENDS);
        
        NodeList pendingNotificationList = el.getElementsByTagName("pendingNotification");
		if(pendingNotificationList != null && pendingNotificationList.getLength() > 0) {
            Element pendingNotificationEl = (Element)pendingNotificationList.item(0);
            NodeList userList = pendingNotificationEl.getElementsByTagName("user");
			for(int i = 0 ; i < userList.getLength();i++) {
				Element usrEl = (Element)userList.item(i);
				user usr = new user();
                usr.create(usrEl);
                if (settings.username.equals(creator.getUsername())){
                    if (settings.friends != null){
                        for (int j = 0; j < settings.friends.size(); j++){
                            friend f = new friend(settings.friends.get(j));
                            if (usr.getUsername().equals(f.getUser().getUsername())){
                                pendingNotification.add(usr);
                            }
                        }
                    }
                }else{
                    pendingNotification.add(usr);
                }
            }
        }
        
        NodeList inviteesList = el.getElementsByTagName("invitees");
		if(inviteesList != null && inviteesList.getLength() > 0) {
            Element inviteesEl = (Element)inviteesList.item(0);
            NodeList userList = inviteesEl.getElementsByTagName("user");
			for(int i = 0 ; i < userList.getLength();i++) {
				Element usrEl = (Element)userList.item(i);
				user usr = new user();
                usr.create(usrEl);
                if (settings.username.equals(creator.getUsername())){
                    if (settings.friends != null){
                        for (int j = 0; j < settings.friends.size(); j++){
                            friend f = new friend(settings.friends.get(j));
                            if (usr.getUsername().equals(f.getUser().getUsername())){
                                invitees.add(usr);
                            }
                        }
                    }
                }else{
                    invitees.add(usr);
                }
            }
        }
        
        NodeList acceptedList = el.getElementsByTagName("accepted");
		if(acceptedList != null && acceptedList.getLength() > 0) {
            Element acceptedEl = (Element)acceptedList.item(0);
            NodeList userList = acceptedEl.getElementsByTagName("user");
			for(int i = 0 ; i < userList.getLength();i++) {
				Element usrEl = (Element)userList.item(i);
				user usr = new user();
                usr.create(usrEl);
				if (settings.username.equals(creator.getUsername())){
                    if (settings.friends != null){
                        for (int j = 0; j < settings.friends.size(); j++){
                            friend f = new friend(settings.friends.get(j));
                            if (usr.getUsername().equals(f.getUser().getUsername())){
                                accepted.add(usr);
                            }
                        }
                    }
                }else{
                    accepted.add(usr);
                }
            }
        }
        
        NodeList declinedList = el.getElementsByTagName("declined");
		if(declinedList != null && declinedList.getLength() > 0) {
            Element declinedEl = (Element)declinedList.item(0);
            NodeList userList = declinedEl.getElementsByTagName("user");
			for(int i = 0 ; i < userList.getLength();i++) {
				Element usrEl = (Element)userList.item(i);
				user usr = new user();
                usr.create(usrEl);
				if (settings.username.equals(creator.getUsername())){
                    if (settings.friends != null){
                        for (int j = 0; j < settings.friends.size(); j++){
                            friend f = new friend(settings.friends.get(j));
                            if (usr.getUsername().equals(f.getUser().getUsername())){
                                declined.add(usr);
                            }
                        }
                    }
                }else{
                    declined.add(usr);
                }
            }
        }
	}
    
    public Element createDOMElement(Document dom){
        Element el = dom.createElement("event");
		
        el.setAttribute("id", String.valueOf(id));
        el.setAttribute("lastUpdateTime", String.valueOf(lastUpdateTime));
		
        if (title != null){
            Element titleEl = dom.createElement("title");
            Text titleText = dom.createTextNode(title);
            titleEl.appendChild(titleText);
            el.appendChild(titleEl);
        }
        if (description != null){
            Element descEl = dom.createElement("description");
            Text descText = dom.createTextNode(description);
            descEl.appendChild(descText);
            el.appendChild(descEl);
        }
        if (location != null){
            Element locEl = dom.createElement("location");
            Text locText = dom.createTextNode(location);
            locEl.appendChild(locText);
            el.appendChild(locEl);
        }
        if (decision != null){
            Element desEl = dom.createElement("decision");
            Text desText = dom.createTextNode(decision);
            desEl.appendChild(desText);
            el.appendChild(desEl);
        }
        if (creator != null){
            Element creatorEl = dom.createElement("creator");
            creatorEl.appendChild(creator.createDOMElement(dom));
            el.appendChild(creatorEl);
        }
        if (startDate != null){
            Element startDateEl = dom.createElement("startDate");
            startDateEl.appendChild(startDate.createDOMElement(dom));
            el.appendChild(startDateEl);
        }
        if (endDate != null){
            Element endDateEl = dom.createElement("endDate");
            endDateEl.appendChild(endDate.createDOMElement(dom));
            el.appendChild(endDateEl);
        }
        
        Element pendingNotificationEl = dom.createElement("pendingNotification");
        Iterator it  = pendingNotification.iterator();
		while(it.hasNext()) {
			user usr = (user)it.next();
			pendingNotificationEl.appendChild(usr.createDOMElement(dom));
		}
        el.appendChild(pendingNotificationEl);
        
        Element inviteesEl = dom.createElement("invitees");
        it  = invitees.iterator();
		while(it.hasNext()) {
			user usr = (user)it.next();
			inviteesEl.appendChild(usr.createDOMElement(dom));
		}
        el.appendChild(inviteesEl);
        
        Element acceptedEl = dom.createElement("accepted");
        it  = accepted.iterator();
		while(it.hasNext()) {
			user usr = (user)it.next();
			acceptedEl.appendChild(usr.createDOMElement(dom));
		}
        el.appendChild(acceptedEl);
        
        Element declinedEl = dom.createElement("declined");
        it  = declined.iterator();
		while(it.hasNext()) {
			user usr = (user)it.next();
			declinedEl.appendChild(usr.createDOMElement(dom));
		}
        el.appendChild(declinedEl);
        
		return el;
    }
    
    public String generateHTML(String type){
        String res = "";
        if (type.equals("rightSideBar")){
            res += "<div class=\"right-sidebar-entry-row\">\n";
            res += "<a href=\"events.jsp?show=event&id=" + id + "\">\n";
            res += "<div class=\"event-abstract\">\n";
            res += "<div class=\"event-abstract-sign\">\n";
            res += "<h3>" + startDate.getMonth("abstractAlphabetical") + "</h3>\n";
            res += "<h2>" + startDate.getDay() + "</h2>\n";
            res += "</div>\n";
            res += "<div class=\"event-abstract-header\">\n";
            res += "<h1>Created By: " + creator.getFirstName() + " " + creator.getLastName() + "</h1>\n";
            res += "<h1>Title: " + title + "</h1>\n";
            res += "<h1>Location: " + location + "</h1>\n";
            res += "<h1>Start Date: " + startDate.getMonth("numerical") + "/" + startDate.getDay() + "/" + startDate.getYear() + "&nbsp;&nbsp;" + startDate.getHour("12") + ":" + startDate.getMinuteStr() + "&nbsp;" + endDate.getAMPM() + "</h1>\n";
            res += "<h1>End Date: " + endDate.getMonth("numerical") + "/" + endDate.getDay() + "/" + endDate.getYear() + "&nbsp;&nbsp;" + endDate.getHour("12") + ":" + endDate.getMinuteStr() + "&nbsp;" + endDate.getAMPM() + "</h1>\n";
            res += "<h1>My Response: " + decision + "</h1>\n";
            res += "</div>\n";
            res += "</div>\n";
            res += "</a>\n";
            res += "</div>\n";
        }else if (type.equals("complete")){
            res += "<div class=\"content\">\n";
            res += "<div class=\"event-sign\">\n";
            res += "<h3>" + startDate.getMonth("completeAlphabetical") + "</h3>\n";
            res += "<h2>" + startDate.getDay() + "</h2>\n";
            res += "</div>\n";
            res += "<div class=\"event-header\">\n";
            res += "<h1>Created By: <a href=\"profile.jsp?profileOwner=" + creator.getUsername() + "\">" + creator.getFirstName() + " " + creator.getLastName() + "</a></h1>\n";
            res += "<h1>Title: " + title + "</h1>\n";
            res += "<h1>Location: " + location + "</h1>\n";
            res += "<h1>Start Date: " + startDate.getMonth("numerical") + "/" + startDate.getDay() + "/" + startDate.getYear() + "&nbsp;&nbsp;" + startDate.getHour("12") + ":" + startDate.getMinuteStr() + "&nbsp;" + endDate.getAMPM() + "</h1>\n";
            res += "<h1>End Date: " + endDate.getMonth("numerical") + "/" + endDate.getDay() + "/" + endDate.getYear() + "&nbsp;&nbsp;" + endDate.getHour("12") + ":" + endDate.getMinuteStr() + "&nbsp;" + endDate.getAMPM() + "</h1>\n";
            res += "</div>\n";
            res += "<div class=\"event-body\">\n";
            if (description == null){
                description = "";
            }
            res += "<h3>Details: <p>" + description + "</p>\n";
            res += "</div>\n";
            res += "<div class=\"attendance-box\">\n";
            res += "<h3>My Response: " + decision + "</h3>\n";
            res += "<div class=\"attendance-button\">\n";
            res += "<a href=\"events.jsp?show=event&id=" + id + "&decision=Not Attending\">Don't think so</a>\n";
            res += "</div>\n";
            res += "<div class=\"attendance-button\">\n";
            res += "<a href=\"events.jsp?show=event&id=" + id + "&decision=Attending\">Sure, I'll be there</a>\n";
            res += "</div>\n";
            res += "</div>\n";
            res += "</div>\n";
        }else{
            res += "<div class=\"event-entry\">\n";
            res += "<a href=\"events.jsp?show=event&id=" + id + "\">\n";
            res += "<div class=\"event-abstract\">\n";
            res += "<div class=\"event-abstract-sign\">\n";
            res += "<h3>" + startDate.getMonth("abstractAlphabetical") + "</h3>\n";
            res += "<h2>" + startDate.getDay() + "</h2>\n";
            res += "</div>\n";
            res += "<div class=\"event-abstract-header-left\">\n";
            res += "<h1>Created By: " + creator.getFirstName() + " " + creator.getLastName() + "</h1>\n";
            res += "<h1>Title: " + title + "</h1>\n";
            res += "<h1>Location: " + location + "</h1>\n";
            res += "</div>\n";
            res += "<div class=\"event-abstract-header-right\">\n";
            res += "<h1>Start Date: " + startDate.getMonth("numerical") + "/" + startDate.getDay() + "/" + startDate.getYear() + "&nbsp;&nbsp;" + startDate.getHour("12") + ":" + startDate.getMinuteStr() + "&nbsp;" + endDate.getAMPM() + "</h1>\n";
            res += "<h1>End Date: " + endDate.getMonth("numerical") + "/" + endDate.getDay() + "/" + endDate.getYear() + "&nbsp;&nbsp;" + endDate.getHour("12") + ":" + endDate.getMinuteStr() + "&nbsp;" + endDate.getAMPM() + "</h1>\n";
            res += "<h1>My Response: " + decision + "</h1>\n";
            res += "</div>\n";
            res += "<div class=\"event-abstract-row\">\n";
            if (description == null){
                description = "";
            }
            res += "<h1>Details: " + description + "</h1>\n";
            res += "</div>\n";
            res += "</div>\n";
            res += "</a>\n";
            res += "</div>\n";
        }
        return res;
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Event info \n");
		sb.append("id:" + id);
		sb.append("\n");
        sb.append("lastUpdateTime:" + lastUpdateTime);
		sb.append("\n");
        sb.append("title:" + title);
		sb.append("\n");
        sb.append("description:" + description);
        sb.append("\n");
        sb.append("location:" + location);
        sb.append("\n");
        sb.append("decision:" + decision);
        sb.append("\n");
		sb.append(creator.toString());
		sb.append("start date:\n");
        sb.append(startDate.toString());
		sb.append("end date:\n");
        sb.append(endDate.toString());
		sb.append("Pending Notification:\n");
        Iterator it  = pendingNotification.iterator();
		while(it.hasNext()) {
			user usr = (user)it.next();
			sb.append(usr.toString());
		}
        sb.append("Invited:\n");
        it  = invitees.iterator();
		while(it.hasNext()) {
			user usr = (user)it.next();
			sb.append(usr.toString());
		}
        sb.append("Attending:\n");
        it  = accepted.iterator();
		while(it.hasNext()) {
			user usr = (user)it.next();
			sb.append(usr.toString());
		}
        sb.append("Not Attending:\n");
        it  = declined.iterator();
		while(it.hasNext()) {
			user usr = (user)it.next();
			sb.append(usr.toString());
		}
        
		return sb.toString();
	}
}
