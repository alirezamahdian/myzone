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

package MyZone;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Text;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import MyZone.elements.*;

/*
 This file contains the implementation of profile class.
 The profile class implements the main functionalities of MyZone.
 These include:
 1. modifying user information.
 2. creating/deleting photo/video/audio albums.
 3. posting and removing photo/video/audio/link/status updates.
 4. commenting on/liking/disliking any posted item.
 5. creating/updating/cancelling events.
 6. composing and viweing private messages.
 For detailed definitions of profile elements please refer to XML Schema definition files.
 profile class is considered as the main part of the web interface of MyZone.
 */

class map{
    public long old_id;
    public long new_id;
    public map(long old_id, long new_id){
        this.old_id = old_id;
        this.new_id = new_id;
    }
    public map(map orig){
        old_id = orig.old_id;
        new_id = orig.new_id;
    }
}

public class profile {
    
    private final static boolean DEBUG = false;
    Document wallDOM;
    public String profileOwner;   
    public List<timelineEntry> wall;
    public List<audioAlbum> audioAlbums;
    public List<videoAlbum> videoAlbums;
    public List<photoAlbum> photoAlbums;
    public List<wallPost> wallPosts;
    public List<link> links;
    public List<notification> notifications;
    public List<event> events;
    public List<message> sentMsgs;
    public List<message> receivedMsgs;
    public List<fileListEntry> fileList;
    public info userInfo;
    public List<map> changeIdMap;
    private String prefix;
    private String accountHolder;
    private boolean executeAsProfileOwner = false;
    Settings mainSettings;
    
    public profile(String profileOwner, String accountHolder, String prefix){
        this.profileOwner = profileOwner;
        this.prefix = prefix;
        this.accountHolder = accountHolder;
        audioAlbums = new ArrayList<audioAlbum>();
        videoAlbums = new ArrayList<videoAlbum>();
        photoAlbums = new ArrayList<photoAlbum>();
        events = new ArrayList<event>();
        receivedMsgs = new ArrayList<message>();
        sentMsgs = new ArrayList<message>();
        wall = new ArrayList<timelineEntry>();
        wallPosts = new ArrayList<wallPost>();
        links = new ArrayList<link>();
        notifications = new ArrayList<notification>();
        fileList = new ArrayList<fileListEntry>();
        mainSettings = new Settings(prefix);
        mainSettings.refresh(mainSettings.ALL);
        if (!accountHolder.equals(mainSettings.username)){
            this.prefix = prefix + "mirroring/";
        }
        userInfo = new info();
        changeIdMap = new ArrayList<map>();
    }
    
    public profile(String profileOwner, String accountHolder, boolean executeAsProfileOwner, String prefix){
        this.executeAsProfileOwner = executeAsProfileOwner;
        this.profileOwner = profileOwner;
        this.prefix = prefix;
        this.accountHolder = accountHolder;
        audioAlbums = new ArrayList<audioAlbum>();
        videoAlbums = new ArrayList<videoAlbum>();
        photoAlbums = new ArrayList<photoAlbum>();
        events = new ArrayList<event>();
        receivedMsgs = new ArrayList<message>();
        sentMsgs = new ArrayList<message>();
        wall = new ArrayList<timelineEntry>();
        wallPosts = new ArrayList<wallPost>();
        links = new ArrayList<link>();
        notifications = new ArrayList<notification>();
        fileList = new ArrayList<fileListEntry>();
        mainSettings = new Settings(prefix);
        mainSettings.refresh(mainSettings.ALL);
        if (!accountHolder.equals(mainSettings.username)){
            this.prefix = prefix + "mirroring/";
        }
        userInfo = new info();
        changeIdMap = new ArrayList<map>();
    }
    
    private long getMapEntry(long old_id){
        for (int i = 0; i < changeIdMap.size(); i++){
            if(old_id == changeIdMap.get(i).old_id){
                return changeIdMap.get(i).new_id;
            }
        }
        return old_id;
    }
    
    private void addToMap(long old_id, long new_id){
        map temp = new map(old_id, new_id);
        changeIdMap.add(temp);
    }
    
    public void printMapTable(){
        System.out.println("old ID\tNewId");
        for (int i = 0; i < changeIdMap.size(); i++){
            map m = new map(changeIdMap.get(i));
            System.out.println(m.old_id + "\t" + m.new_id);
        }
    }
    public boolean loadUserInfo(){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            String path;
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                path = prefix + accountHolder + "/friends/" + profileOwner;
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            File file = new File(path + "/info.xml");
            boolean exists = file.exists();
            if (!exists){
                return false;
            }
            byte[] readIn = null;
            while((readIn = readXML(path + "/info.xml")) == null){
                Thread.sleep(100);
            }
            dom = db.parse(new ByteArrayInputStream(readIn));
            Element docEle = dom.getDocumentElement();
            NodeList nl = docEle.getElementsByTagName("info");
            if(nl != null && nl.getLength() > 0) {
                Element el = (Element)nl.item(0);
                userInfo.create(el);
            }
            if (userInfo.getFirstName() == null || userInfo.getFirstName().equals("") || userInfo.getLastName() == null || userInfo.getLastName().equals("")){
                return false;
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean saveUserInfo(){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        String path;
        try{
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                return false;
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            dom = db.newDocument();
            
            Element rootEle = dom.createElement("userInformation");
            dom.appendChild(rootEle);
            rootEle.appendChild(userInfo.createDOMElement(dom));
            
            while (!saveXML(path + "/info.xml", dom)){
                Thread.sleep(100);
            }
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            return updateModifiedFiles("add", now.getTimeInMillis(), path + "/info.xml", "public");
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
    }
    
    public void updateImage(String imageName, String action, fileListEntry fle){
        if (profileOwner == null || profileOwner.equals(""))
            return;
        fileList.clear();
        parseDocument(prefix + accountHolder + "/" + imageName + ".xml", "fileListEntry");
        if (action.equals("add")){
            int i = 0;
            for (i = 0; i < fileList.size(); i++){
                fileListEntry f = new fileListEntry(fileList.get(i));
                if (f.getFilename().equals(fle.getFilename()) &&
                    f.getOwner().equals(fle.getOwner()) &&
                    f.getFilesize() == fle.getFilesize()){
                    break;
                }
            }
            if (i < fileList.size()){
                return;
            }
            fileList.add(0, fle);
        }else{
            for (int i = 0; i < fileList.size(); i++){
                fileListEntry f = new fileListEntry(fileList.get(i));
                if (f.getFilename().equals(fle.getFilename()) &&
                    f.getOwner().equals(fle.getOwner())){
                    fileList.remove(i);
                }
            }
        }
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document fileListDOM = db.newDocument();
            Element rootEle = fileListDOM.createElement(imageName);
            fileListDOM.appendChild(rootEle);
            for (int i = 0; i < fileList.size(); i++){
                fileListEntry f = new fileListEntry(fileList.get(i));
                rootEle.appendChild(f.createDOMElement(fileListDOM));
            }
            while (!saveXML(prefix + accountHolder + "/" + imageName + ".xml", fileListDOM)){
                Thread.sleep(100);
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    public boolean parseDocument(String filename, String type){
        if (profileOwner == null || profileOwner.equals("")){
            return false;
        }
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            
            File file = null;
            if (!new File(filename).exists()){
                return false;
            }
            file = new File(filename);
            byte[] readIn = null;
            while((readIn = readXML(filename)) == null){
                Thread.sleep(100);
            }
            dom = db.parse(new ByteArrayInputStream(readIn));
            Element docEle = dom.getDocumentElement();
            
            if (type.equals("fileListEntry")){
                NodeList nl = docEle.getElementsByTagName("fileListEntry");
                if(nl != null && nl.getLength() > 0) {
                    for(int i = 0 ; i < nl.getLength(); i++) {
                        Element el = (Element)nl.item(i);
                        fileListEntry fle = new fileListEntry();
                        fle.create(el);
                        fileList.add(fle);
                    }
                }
            }
            
            if (type.equals("event")){
                NodeList nl = docEle.getElementsByTagName("event");
                if(nl != null && nl.getLength() > 0) {
                    for(int i = 0 ; i < nl.getLength(); i++) {
                        Element el = (Element)nl.item(i);
                        event e = new event();
                        e.create(el);
                        events.add(e);
                    }
                }
            }
            
            if (type.equals("message")){
                NodeList nl = docEle.getElementsByTagName("message");
                if(nl != null && nl.getLength() > 0) {
                    for(int i = 0 ; i < nl.getLength(); i++) {
                        Element el = (Element)nl.item(i);
                        message m = new message();
                        m.create(el);
                        if (filename.contains("outbox.xml")){
                            sentMsgs.add(m);
                        }else{
                            receivedMsgs.add(m);
                        }
                    }
                }
            }
            
            if (type.equals("audioAlbum")){
                NodeList nl = docEle.getElementsByTagName("audioAlbum");
                if(nl != null && nl.getLength() > 0) {
                    for(int i = 0 ; i < nl.getLength(); i++) {
                        Element el = (Element)nl.item(i);
                        audioAlbum aa = new audioAlbum();
                        aa.create(el);
                        insertAudioAlbum(aa);
                    }
                }
            }
            
            if (type.equals("photoAlbum")){
                NodeList nl = docEle.getElementsByTagName("photoAlbum");
                if(nl != null && nl.getLength() > 0) {
                    for(int i = 0 ; i < nl.getLength(); i++) {
                        Element el = (Element)nl.item(i);
                        photoAlbum pa = new photoAlbum();
                        pa.create(el);
                        insertPhotoAlbum(pa);
                    }
                }
            }
            
            if (type.equals("videoAlbum")){
                NodeList nl = docEle.getElementsByTagName("videoAlbum");
                if(nl != null && nl.getLength() > 0) {
                    for(int i = 0 ; i < nl.getLength(); i++) {
                        Element el = (Element)nl.item(i);
                        videoAlbum va = new videoAlbum();
                        va.create(el);
                        insertVideoAlbum(va);
                    }
                }
            }
            
            if (type.equals("notification")){
                NodeList nl = docEle.getElementsByTagName("notification");
                if(nl != null && nl.getLength() > 0) {
                    for(int i = 0 ; i < nl.getLength(); i++) {
                        Element el = (Element)nl.item(i);
                        notification n = new notification();
                        n.create(el);
                        notifications.add(n);
                    }
                }
            }
        }catch (OverlappingFileLockException e) {
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
            // File is already locked in this thread or virtual machine
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
		return true;
	}
    
    public boolean loadNotifications(){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        String path;
        mainSettings.refresh(mainSettings.BASIC_INFO);
        notifications.clear();
        try{
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                return false;
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return parseDocument(path + "/notifications.xml", "notification");
    }
    
    private void saveNotifications(){
        if (profileOwner == null || profileOwner.equals(""))
            return;
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            dom = db.newDocument();
            
            Element rootEle = dom.createElement("Notifications");
            dom.appendChild(rootEle);
            
            Iterator it  = notifications.iterator();
            while(it.hasNext()) {
                notification n = new notification((notification)it.next());
                rootEle.appendChild(n.createDOMElement(dom));
            }
            String path;
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                return; 
            }
            while (!saveXML(path + "/notifications.xml", dom)){
                Thread.sleep(100);    
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    public void insertNotificaton(notification n){
        try{
            if (!profileOwner.equals(accountHolder)) {
                return;
            }
            if (profileOwner.equals(n.getPostedBy().getUsername())){
                return;
            }
            if (notifications.size() == 0){
                loadNotifications();
            }
            int i = 0;
            for (i = 0; i < notifications.size(); i++){
                notification tmp = notifications.get(i);
                if (n.getId() >= tmp.getId()){
                    if (n.getId() == tmp.getId()){
                        return;
                    }
                    notifications.add(i, n);
                    saveNotifications();
                    return;
                }
            }
            notifications.add(n);
            saveNotifications();
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    private String getWallFileName(long timestamp, String shareWith){
        if (profileOwner == null || profileOwner.equals(""))
            return null;
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            String path;
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner + "/zones/" + shareWith;
            }else{
                path = prefix + accountHolder + "/friends/" + profileOwner;
            }
            
            File file = new File(path + "/history.xml");
            boolean exists = file.exists();
            if (!exists){
                return null;
            }
            
            byte[] readIn = null;
            while((readIn = readXML(path + "/history.xml")) == null){
                Thread.sleep(100);
            }
            dom = db.parse(new ByteArrayInputStream(readIn));
            Element docEle = dom.getDocumentElement();
            
            NodeList nl = docEle.getElementsByTagName("historyLogEntry");
            if(nl != null && nl.getLength() > 0) {
                for(int i = 0 ; i < nl.getLength();i++) {
                    Element el = (Element)nl.item(i);
                    historyLogEntry le = new historyLogEntry();
                    le.create(el);
                    if (le.getStart() <= timestamp && le.getEnd() >= timestamp){
                        return path + "/wall/" + String.valueOf(le.getStart()) + ".xml";
                    }
                }
            }
            return null;
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
    }
    
    public boolean updatePendingChanges(String action, String type, timelineEntry tle){
        if (profileOwner == null || profileOwner.equals("")){
            return false;
        }
        long timespent = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
        try{
            if (profileOwner.equals(accountHolder) && !type.equals("event") && !type.equals("message") && !type.equals("mirror")){
                return false;
            }
            if (tle.type.equals("link") && type.equals("link")){
                if (!accountHolder.equals(tle.l.getPostedBy().getUsername())){
                    return false;
                }
            }else if (tle.type.equals("wallPost") && type.equals("wallPost")){
                if (!accountHolder.equals(tle.s.getPostedBy().getUsername())){
                    if (DEBUG){
                        System.out.println("DEBUG: line 558 of profile.java. returned false");
                    }
                    return false;
                }
            }else if (tle.type.equals("audio") && type.equals("audio")){
                if (!accountHolder.equals(tle.a.getPostedBy().getUsername())){
                    if (DEBUG){
                        System.out.println("DEBUG: line 565 of profile.java. returned false");
                    }
                    return false;
                }
            }else if (tle.type.equals("audioAlbum") && type.equals("audioAlbum")){
                if (!accountHolder.equals(tle.aa.getPostedBy().getUsername())){
                    if (DEBUG){
                        System.out.println("DEBUG: line 572 of profile.java. returned false");
                    }
                    return false;
                }
            }else if (tle.type.equals("video") && type.equals("video")){
                if (!accountHolder.equals(tle.v.getPostedBy().getUsername())){
                    if (DEBUG){
                        System.out.println("DEBUG: line 579 of profile.java. returned false");
                    }
                    return false;
                }
            }else if (tle.type.equals("videoAlbum") && type.equals("videoAlbum")){
                if (!accountHolder.equals(tle.va.getPostedBy().getUsername())){
                    if (DEBUG){
                        System.out.println("DEBUG: line 586 of profile.java. returned false");
                    }
                    return false;
                }
            }else if (tle.type.equals("photo") && type.equals("photo")){
                if (!accountHolder.equals(tle.p.getPostedBy().getUsername())){
                    if (DEBUG){
                        System.out.println("DEBUG: line 593 of profile.java. returned false");
                    }
                    return false;
                }
            }else if (tle.type.equals("photoAlbum") && type.equals("photoAlbum")){
                if (!accountHolder.equals(tle.pa.getPostedBy().getUsername())){
                    if (DEBUG){
                        System.out.println("DEBUG: line 600 of profile.java. returned false");
                    }
                    return false;
                }
            }else if (tle.type.equals("deletedEntry")){
                if (!accountHolder.equals(tle.de.getPostedBy())){
                    if (DEBUG){
                        System.out.println("DEBUG: line 607 of profile.java. returned false");
                    }
                    return false;
                }
            }
            
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            String path = prefix + accountHolder;
            File file = new File(path + "/pendingChanges.xml");
            boolean exists = file.exists();
            if (!exists){
                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                pendingChange pc = new pendingChange(now.getTimeInMillis(), action, type, tle);
                dom = db.newDocument();
                Element rootEle = dom.createElement("pendingChanges");
                dom.appendChild(rootEle);
                
                rootEle.appendChild(pc.createDOMElement(dom));
                while (!saveXML(path + "/pendingChanges.xml", dom)){
                    Thread.sleep(100);
                }
                if (DEBUG){
                    System.out.println("DEBUG: line 631 of profile.java. returned true");
                }
                return true;
            }else{
                List<pendingChange> pendingChanges = new ArrayList<pendingChange>();
                byte[] readIn = null;
                while((readIn = readXML(path + "/pendingChanges.xml")) == null){
                    Thread.sleep(100);
                }
                dom = db.parse(new ByteArrayInputStream(readIn));
                Element docEle = dom.getDocumentElement();
                NodeList nl = docEle.getElementsByTagName("pendingChange");
                if(nl != null && nl.getLength() > 0) {
                    for(int i = 0 ; i < nl.getLength();i++) {
                        Element el = (Element)nl.item(i);
                        pendingChange pc = new pendingChange();
                        pc.create(el);
                        pendingChanges.add(pc);
                    }
                }
                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                pendingChange pc = new pendingChange(now.getTimeInMillis(), action, type, tle);
                pendingChanges.add(0, pc);
                dom = db.newDocument();
                
                Element rootEle = dom.createElement("pendingChanges");
                dom.appendChild(rootEle);
                
                Iterator it  = pendingChanges.iterator();
                while(it.hasNext()) {
                    pendingChange p = new pendingChange((pendingChange)it.next());
                    rootEle.appendChild(p.createDOMElement(dom));
                }
                while (!saveXML(path + "/pendingChanges.xml", dom)){
                    Thread.sleep(100);
                }
                long passed = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() - timespent;
                if (DEBUG){
                    System.out.println("DEBUG: line 669 of profile.java. time passed in updatePendingChanges = " + passed);
                }
                return true;
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
    }
    
    
    
    private String updateHistory(long timestamp, String shareWith){
        if (profileOwner == null || profileOwner.equals(""))
            return null;
        try{
            Settings settings = new Settings(prefix);
            settings.refresh(settings.ALL);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            String path;
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner + "/zones/" + shareWith;
            }else{
                path = prefix + accountHolder + "/friends/" + profileOwner;
            }
            
            List<historyLogEntry> historyLogEntries = new ArrayList<historyLogEntry>();
            historyLogEntries.clear();
            File file = new File(path + "/history.xml");
            boolean exists = file.exists();
            if (!exists){
                historyLogEntry newLog = new historyLogEntry(timestamp, timestamp, 1);
                dom = db.newDocument();
                Element rootEle = dom.createElement("history");
                dom.appendChild(rootEle);
                
                rootEle.appendChild(newLog.createDOMElement(dom));
                while (!saveXML(path + "/history.xml", dom)){
                    Thread.sleep(100);
                }
                String res = path + "/wall/" + String.valueOf(newLog.getStart()) + ".xml";
                return res;
            }else{
                byte[] readIn = null;
                while((readIn = readXML(path + "/history.xml")) == null){
                    Thread.sleep(100);
                }
                dom = db.parse(new ByteArrayInputStream(readIn));
                Element docEle = dom.getDocumentElement();
                // load all log entries
                NodeList nl = docEle.getElementsByTagName("historyLogEntry");
                if(nl != null && nl.getLength() > 0) {
                    for(int i = 0 ; i < nl.getLength();i++) {
                        Element el = (Element)nl.item(i);
                        historyLogEntry le = new historyLogEntry();
                        le.create(el);
                        historyLogEntries.add(le);
                    }
                }
                // find the correct log entry and update it.
                if (historyLogEntries.size() > 0){
                    for (int i = 0; i < historyLogEntries.size(); i++){
                        historyLogEntry le = new historyLogEntry(historyLogEntries.get(i));
                        if (le.getStart() <= timestamp && le.getEnd() >= timestamp){
                            String res = path + "/wall/" + String.valueOf(le.getStart()) + ".xml";
                            return res;
                        }
                    }
                    historyLogEntry first = new historyLogEntry(historyLogEntries.get(historyLogEntries.size() - 1));
                    historyLogEntry latest = new historyLogEntry(historyLogEntries.get(0));
                    
                    // the entry timestamp is before all the recoreded history.
                    if (first.getStart() > timestamp){
                        return null;
                    }
                    
                    // at this point the entry's timestamp is definitely bigger than the latest history log entry's end timestamp.
                    if (latest.getNumberOfChanges() >= settings.maxNumberOfChanges)
                    {
                        if (timestamp <= latest.getEnd()){
                            String res = path + "/wall/" + String.valueOf(latest.getStart()) + ".xml";
                            return res; 
                        }
                        historyLogEntry newLog = new historyLogEntry(timestamp, timestamp, 1);
                        historyLogEntries.add(0, newLog);
                    }else{
                        latest.setNumberOfChanges(latest.getNumberOfChanges() + 1);
                        if (latest.getEnd() < timestamp){
                            latest.setEnd(timestamp);
                        }
                        historyLogEntries.add(0, latest);
                        historyLogEntries.remove(1);
                    }
                }else{
                    historyLogEntry newLog = new historyLogEntry(timestamp, timestamp, 1);
                    historyLogEntries.add(newLog);
                }
                historyLogEntry result = new historyLogEntry(historyLogEntries.get(0));
                
                dom = db.newDocument();
                
                Element rootEle = dom.createElement("history");
                dom.appendChild(rootEle);
                
                Iterator it  = historyLogEntries.iterator();
                while(it.hasNext()) {
                    historyLogEntry log = new historyLogEntry((historyLogEntry)it.next());
                    rootEle.appendChild(log.createDOMElement(dom));
                }
                while (!saveXML(path + "/history.xml", dom)){
                    Thread.sleep(100);
                }
                String res = path + "/wall/" + String.valueOf(result.getStart()) + ".xml";
                return res;
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
    }
    
    public boolean updateModifiedFiles(String type, long timestamp, String filename, String shareWith){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            String path;
            List<changeEntry> changeEntries = new ArrayList<changeEntry>();
            changeEntries.clear();
            if (profileOwner.equals(accountHolder)){
                if (shareWith.equals("public")){
                    List<String> zoneNames = getZoneNames();
                    if (zoneNames == null)
                        return false;
                    for (int i = 0; i < zoneNames.size(); i++){
                        String z = new String(zoneNames.get(i));
                        updateModifiedFiles("add", timestamp, filename, z);
                    }
                    return true;
                }else{
                    path = prefix + profileOwner + "/zones/" + shareWith;
                }
            }else{
                return false;
            }
            File file = new File(path + "/modifiedFiles.xml");
            boolean exists = file.exists();
            if (!exists){
                if (type.equals("add")){
                    changeEntry c = new changeEntry(timestamp, filename);
                    dom = db.newDocument();
                    Element rootEle = dom.createElement("changeHistory");
                    dom.appendChild(rootEle);
                    rootEle.appendChild(c.createDOMElement(dom));
                    while (!saveXML(path + "/modifiedFiles.xml", dom)){
                        Thread.sleep(100);
                    }
                    return true;
                }else{
                    return false;
                }
            }
            
            byte[] readIn = null;
            while((readIn = readXML(path + "/modifiedFiles.xml")) == null){
                Thread.sleep(100);
            }
            dom = db.parse(new ByteArrayInputStream(readIn));
            Element docEle = dom.getDocumentElement();
            
            
            // load all log entries
            NodeList nl = docEle.getElementsByTagName("changeEntry");
            if(nl != null && nl.getLength() > 0) {
                for(int i = 0 ; i < nl.getLength();i++) {
                    Element el = (Element)nl.item(i);
                    changeEntry ce = new changeEntry();
                    ce.create(el);
                    changeEntries.add(ce);
                }
            }
            // find the correct place of change entry.
            if (type.equals("add")){
                if (changeEntries.size() > 0){
                    int i = 0;
                    for (i = 0; i < changeEntries.size(); i++){
                        changeEntry ce = new changeEntry(changeEntries.get(i));
                        if (ce.getFilename().equals(filename)){
                            changeEntries.remove(i);
                        }
                    }
                    changeEntry c = new changeEntry();
                    for (i = 0; i < changeEntries.size(); i++){
                        changeEntry ce = new changeEntry(changeEntries.get(i));
                        if (ce.getId() < timestamp){
                            c = new changeEntry(timestamp, filename);
                            changeEntries.add(i, c);
                            break;
                        }
                    }
                    if (i == changeEntries.size())
                        changeEntries.add(c);
                }else{
                    changeEntry c = new changeEntry(timestamp, filename);
                    changeEntries.add(c);
                }
            }else{
                for (int i = 0; i < changeEntries.size(); i++){
                    changeEntry ce = new changeEntry(changeEntries.get(i));
                    if (ce.getFilename().equals(filename)){
                        changeEntries.remove(i);
                    }
                }
            }
            dom = db.newDocument();
            
            Element rootEle = dom.createElement("changeHistory");
            dom.appendChild(rootEle);
            
            for (int i = 0; i < changeEntries.size(); i++){
                changeEntry ce = new changeEntry(changeEntries.get(i));
                rootEle.appendChild(ce.createDOMElement(dom));
            }
            while (!saveXML(path + "/modifiedFiles.xml", dom)){
                Thread.sleep(100);    
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean loadWall(String filename){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            
            File file = new File(filename);
            boolean exists = file.exists();
            if (!exists){
                wallDOM = db.newDocument();
                return false;
            }
            byte[] readIn = null;
            while((readIn = readXML(filename)) == null){
                Thread.sleep(100);
            }
            wallDOM = db.parse(new ByteArrayInputStream(readIn));
            Element docEle = wallDOM.getDocumentElement();
            
            NodeList nl = docEle.getElementsByTagName("timeLineEntry");
            if(nl != null && nl.getLength() > 0) {
                for(int i = 0 ; i < nl.getLength(); i++) {
                    Element el = (Element)nl.item(i);
                    timelineEntry tle = new timelineEntry();
                    tle.create(el);
                    int j = 0;
                    for (j = 0; j < wall.size(); j++){
                        timelineEntry temp = (timelineEntry)wall.get(j);
                        if (tle.id <= temp.id){
                            if (tle.id == temp.id && tle.lastUpdateTime > temp.lastUpdateTime){
                                wall.add(j, tle);
                                wall.remove(j + 1);
                                break;
                            }
                        }else{
                            wall.add(j, tle);
                            break;
                        }
                    }
                    if (j == wall.size()){
                        wall.add(tle);
                    }
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }finally{
            if (DEBUG){
                System.out.println("DEBUG: line 965 of profile.java. time passed to load the wall = " + (Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() - now.getTimeInMillis()));
            }
        }
        return true;
    }
    
    public byte[] readXML(String filename){
        byte[] readIn = null;
        FileChannel channel = null;
        FileLock lock = null;
        FileInputStream fis = null;
        ByteArrayOutputStream baos = null;
        try{
            File file = new File(filename);
            if (!file.exists()){
                return null;
            }
            fis = new FileInputStream(file);
            channel = fis.getChannel();
            while ((lock = channel.tryLock(0L, Long.MAX_VALUE, true)) == null){
                Thread.yield();
            }
            baos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            ByteBuffer buf = ByteBuffer.wrap(b);
            int count = 0;
            long fileLength = file.length();
            while(fileLength > 0){
                count = channel.read(buf);
                if (count >= 0){
                    fileLength -= count;
                    baos.write(b, 0, count);
                    buf.rewind();
                }
            }
            readIn = baos.toByteArray();
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            readIn = null;
        }finally{
            try{
                if (lock != null){
                    lock.release();
                }
                if (channel != null){
                    channel.close();
                }
                if (fis != null){
                    fis.close();
                }
                if (baos != null){
                    baos.close();
                }
            }catch(Exception e){
                if (DEBUG){
                    e.printStackTrace();
                }
                readIn = null;
            }
        }
        return readIn;
    }
    
    public boolean saveXML(String filename, Document dom){
        File file = null;
        FileChannel channel = null;
        FileLock lock = null;
        FileOutputStream toWrite = null;
        try{
            if (!new File(filename).exists()){
                String dirName = filename.substring(0, filename.lastIndexOf("/"));
                boolean success = (new File(dirName)).mkdirs();
                if (!success && !(new File(dirName)).exists()){
                    return false;
                }
                OutputFormat format = new OutputFormat(dom);
                format.setIndenting(true);
                file = new File(filename);
                toWrite = new FileOutputStream(file, false);
                XMLSerializer serializer = new XMLSerializer(toWrite, format);
                serializer.serialize(dom);
            }else{
                file = new File(filename);
                toWrite = new FileOutputStream(file, false);
                channel = toWrite.getChannel();
                while ((lock = channel.tryLock()) == null){
                    Thread.yield();
                }
                OutputFormat format = new OutputFormat(dom);
                format.setIndenting(true);
                XMLSerializer serializer = new XMLSerializer(toWrite, format);
                serializer.serialize(dom);
                return true;
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }finally{
            try{
                if (lock != null){
                    lock.release();
                }
                if (channel != null){
                    channel.close();
                }
                if (toWrite != null){
                    toWrite.flush();
                    toWrite.close();
                }
            }catch(Exception e){
                if (DEBUG){
                    e.printStackTrace();
                }
                return false;
            }
        }
        return false;
    }
    
    public boolean mergeWall(String file){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            wallDOM = db.newDocument();
            Element rootEle = wallDOM.createElement("wall");
            wallDOM.appendChild(rootEle);
            if (DEBUG){
                System.out.println("DEBUG: line 1098 of profile.java. in merge wall... wall size = " + wall.size());
            }
            for (int i = 0; i < wall.size(); i++){
                timelineEntry tle = new timelineEntry(wall.get(i));
                rootEle.appendChild(tle.createDOMElement(wallDOM));
            }
            while (!saveXML(file, wallDOM)){
                Thread.sleep(100);
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean removeUncommittedChanges(long since){
        try{
            if (DEBUG){
                System.out.println("DEBUG: line 1119 of profile.java. in removeUncommittedChanges with " + since);
            }
            if (profileOwner.equals(accountHolder)){
                return false;
            }
            String path = prefix + accountHolder + "/friends/" + profileOwner;
            File file = new File(path + "/history.xml");
            boolean exists = file.exists();
            if (!exists){
                return false;
            }
            byte[] readIn = null;
            while((readIn = readXML(path + "/history.xml")) == null){
                Thread.sleep(100);
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom = db.parse(new ByteArrayInputStream(readIn));
            Element docEle = dom.getDocumentElement();
            NodeList nl = docEle.getElementsByTagName("historyLogEntry");
            if(nl != null && nl.getLength() > 0) {
                for(int i = 0 ; i < nl.getLength();i++) {
                    Element el = (Element)nl.item(i);
                    historyLogEntry le = new historyLogEntry();
                    le.create(el);
                    if ( (le.getStart() <= since && le.getEnd() >= since)
                        || le.getStart() >= since){
                        wall.clear();
                        String filename = path + "/wall/" + String.valueOf(le.getStart()) + ".xml";
                        loadWall(filename);
                        for (int j = wall.size() - 1; j >= 0; j--){
                            timelineEntry tle = new timelineEntry(wall.get(j));
                            if (tle.type.equals("link")){
                                if (tle.l.getLastUpdateTime() > since){
                                    if (tle.l.getId() > since){
                                        if (!removeFromWall(tle.l.getId(), null)){
                                            return false;
                                        }
                                        if (!saveWall(filename, tle.l.getShareWith())){
                                            return false;
                                        }
                                    }else{
                                        List<like> likes = tle.l.getLikes();
                                        List<dislike> dislikes = tle.l.getDislikes();
                                        List<comment> comments = tle.l.getComments();
                                        for (int k = 0; k < likes.size(); k++){
                                            like li = new like(likes.get(k));
                                            if (li.getId() > since){
                                                if (!tle.l.removelike(li.getId())){
                                                    return false;
                                                }
                                                if (!removeFromWall(tle.l.getId(), null)){
                                                    return false;
                                                }
                                                if (!insertLinkIntoWall(tle.l.getId(), tle.l)){
                                                    return false;
                                                }
                                                if (!saveWall(filename, tle.l.getShareWith())){
                                                    return false;
                                                }
                                            }
                                        }
                                        for (int k = 0; k < dislikes.size(); k++){
                                            dislike dli = new dislike(dislikes.get(k));
                                            if (dli.getId() > since){
                                                if (!tle.l.removeDislike(dli.getId())){
                                                    return false;
                                                }
                                                if (!removeFromWall(tle.l.getId(), null)){
                                                    return false;
                                                }
                                                if (!insertLinkIntoWall(tle.l.getId(), tle.l)){
                                                    return false;
                                                }
                                                if (!saveWall(filename, tle.l.getShareWith())){
                                                    return false;
                                                }
                                            }
                                        }
                                        for (int k = 0; k < comments.size(); k++){
                                            comment c = new comment(comments.get(k));
                                            if (c.getId() > since){
                                                if (!tle.l.removeComment(c.getId())){
                                                    return false;
                                                }
                                                if (!removeFromWall(tle.l.getId(), null)){
                                                    return false;
                                                }
                                                if (!insertLinkIntoWall(tle.l.getId(), tle.l)){
                                                    return false;
                                                }
                                                if (!saveWall(filename, tle.l.getShareWith())){
                                                    return false;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (tle.type.equals("wallPost")){
                                if (tle.s.getLastUpdateTime() > since){
                                    if (tle.s.getId() > since){
                                        if (!removeFromWall(tle.s.getId(), null))
                                            return false;
                                        if (!saveWall(filename, tle.s.getShareWith()))
                                            return false;
                                    }else{
                                        List<like> likes = tle.s.getLikes();
                                        List<dislike> dislikes = tle.s.getDislikes();
                                        List<comment> comments = tle.s.getComments();
                                        for (int k = 0; k < likes.size(); k++){
                                            like li = new like(likes.get(k));
                                            if (li.getId() > since){
                                                if (!tle.s.removelike(li.getId()))
                                                    return false;
                                                if (!removeFromWall(tle.s.getId(), null))
                                                    return false;
                                                if (!insertwallPostIntoWall(tle.s.getId(), tle.s))
                                                    return false;
                                                if (!saveWall(filename, tle.s.getShareWith()))
                                                    return false;
                                            }
                                        }
                                        for (int k = 0; k < dislikes.size(); k++){
                                            dislike dli = new dislike(dislikes.get(k));
                                            if (dli.getId() > since){
                                                if (!tle.s.removeDislike(dli.getId()))
                                                    return false;
                                                if (!removeFromWall(tle.s.getId(), null))
                                                    return false;
                                                if (!insertwallPostIntoWall(tle.s.getId(), tle.s))
                                                    return false;
                                                if (!saveWall(filename, tle.s.getShareWith()))
                                                    return false;
                                            }
                                        }
                                        for (int k = 0; k < comments.size(); k++){
                                            comment c = new comment(comments.get(k));
                                            if (c.getId() > since){
                                                if (!tle.s.removeComment(c.getId()))
                                                    return false;
                                                if (!removeFromWall(tle.s.getId(), null))
                                                    return false;
                                                if (!insertwallPostIntoWall(tle.s.getId(), tle.s))
                                                    return false;
                                                if (!saveWall(filename, tle.s.getShareWith()))
                                                    return false;
                                            }
                                        }
                                    }
                                }
                            }
                            if (tle.type.equals("audioAlbum")){
                                if (tle.aa.getLastUpdateTime() > since){
                                    if (tle.aa.getId() > since){
                                        if (!removeFromWall(tle.aa.getId(), null)){
                                            return false;
                                        }
                                        if (!saveWall(filename, tle.aa.getShareWith())){
                                            return false;
                                        }
                                        loadAudios();
                                        deleteAudioAlbum(tle.aa.getId());
                                        saveAudios();
                                    }else{
                                        List<like> likes = tle.aa.getLikes();
                                        List<dislike> dislikes = tle.aa.getDislikes();
                                        List<comment> comments = tle.aa.getComments();
                                        for (int k = 0; k < likes.size(); k++){
                                            like li = likes.get(k);
                                            if (li.getId() > since){
                                                if (!tle.aa.removelike(li.getId())){
                                                    return false;
                                                }
                                                if (!removeFromWall(tle.aa.getId(), null)){
                                                    return false;
                                                }
                                                if (!insertAudioAlbumIntoWall(tle.aa.getId(), tle.aa)){
                                                    return false;
                                                }
                                                if (!saveWall(filename, tle.aa.getShareWith())){
                                                    return false;
                                                }
                                                loadAudios();
                                                deleteAudioAlbum(tle.aa.getId());
                                                insertAudioAlbum(tle.aa);
                                                saveAudios();
                                            }
                                        }
                                        for (int k = 0; k < dislikes.size(); k++){
                                            dislike dli = dislikes.get(k);
                                            if (dli.getId() > since){
                                                if (!tle.aa.removeDislike(dli.getId())){
                                                    return false;
                                                }
                                                if (!removeFromWall(tle.aa.getId(), null)){
                                                    return false;
                                                }
                                                if (!insertAudioAlbumIntoWall(tle.aa.getId(), tle.aa)){
                                                    return false;
                                                }
                                                if (!saveWall(filename, tle.aa.getShareWith())){
                                                    return false;
                                                }
                                                loadAudios();
                                                deleteAudioAlbum(tle.aa.getId());
                                                insertAudioAlbum(tle.aa);
                                                saveAudios();
                                            }
                                        }
                                        for (int k = 0; k < comments.size(); k++){
                                            comment c = comments.get(k);
                                            if (c.getId() > since){
                                                if (!tle.aa.removeComment(c.getId())){
                                                    return false;
                                                }
                                                if (!removeFromWall(tle.aa.getId(), null)){
                                                    return false;
                                                }
                                                if (!insertAudioAlbumIntoWall(tle.aa.getId(), tle.aa)){
                                                    return false;
                                                }
                                                if (!saveWall(filename, tle.aa.getShareWith())){
                                                    return false;
                                                }
                                                loadAudios();
                                                deleteAudioAlbum(tle.aa.getId());
                                                insertAudioAlbum(tle.aa);
                                                saveAudios();
                                            }
                                        }
                                    }
                                }
                            }
                            if (tle.type.equals("audio")){
                                if (tle.a.getLastUpdateTime() > since){
                                    if (tle.a.getId() > since){
                                        if (!removeFromWall(tle.a.getId(), null)){
                                            return false;
                                        }
                                        if (!saveWall(filename, tle.a.getShareWith())){
                                            return false;
                                        }
                                        loadAudios();
                                        deleteAudio(tle.a.getParent(), tle.a.getId());
                                        saveAudios();
                                    }else{
                                        List<like> likes = tle.a.getLikes();
                                        List<dislike> dislikes = tle.a.getDislikes();
                                        List<comment> comments = tle.a.getComments();
                                        for (int k = 0; k < likes.size(); k++){
                                            like li = new like(likes.get(k));
                                            if (li.getId() > since){
                                                if (!tle.a.removelike(li.getId())){
                                                    return false;
                                                }
                                                if (!removeFromWall(tle.a.getId(), null)){
                                                    return false;
                                                }
                                                if (!insertAudioIntoWall(tle.a.getId(), tle.a)){
                                                    return false;
                                                }
                                                if (!saveWall(filename, tle.a.getShareWith())){
                                                    return false;
                                                }
                                                loadAudios();
                                                deleteAudio(tle.a.getParent(), tle.a.getId());
                                                insertAudio(tle.a);
                                                saveAudios();
                                            }
                                        }
                                        for (int k = 0; k < dislikes.size(); k++){
                                            dislike dli = new dislike(dislikes.get(k));
                                            if (dli.getId() > since){
                                                if (!tle.a.removeDislike(dli.getId())){
                                                    return false;
                                                }
                                                if (!removeFromWall(tle.a.getId(), null)){
                                                    return false;
                                                }
                                                if (!insertAudioIntoWall(tle.a.getId(), tle.a)){
                                                    return false;
                                                }
                                                if (!saveWall(filename, tle.a.getShareWith())){
                                                    return false;
                                                }
                                                loadAudios();
                                                deleteAudio(tle.a.getParent(), tle.a.getId());
                                                insertAudio(tle.a);
                                                saveAudios();
                                            }
                                        }
                                        for (int k = 0; k < comments.size(); k++){
                                            comment c = new comment(comments.get(k));
                                            if (c.getId() > since){
                                                if (!tle.a.removeComment(c.getId())){
                                                    return false;
                                                }
                                                if (!removeFromWall(tle.a.getId(), null)){
                                                    return false;
                                                }
                                                if (!insertAudioIntoWall(tle.a.getId(), tle.a)){
                                                    return false;
                                                }
                                                if (!saveWall(filename, tle.a.getShareWith())){
                                                    return false;
                                                }
                                                loadAudios();
                                                deleteAudio(tle.a.getParent(), tle.a.getId());
                                                insertAudio(tle.a);
                                                saveAudios();
                                            }
                                        }
                                    }
                                }
                            }
                            if (tle.type.equals("videoAlbum")){
                                if (tle.va.getLastUpdateTime() > since){
                                    if (tle.va.getId() > since){
                                        if (!removeFromWall(tle.va.getId(), null))
                                            return false;
                                        if (!saveWall(filename, tle.va.getShareWith()))
                                            return false;
                                        loadVideos();
                                        deleteVideoAlbum(tle.va.getId());
                                        saveVideos();
                                    }else{
                                        List<like> likes = tle.va.getLikes();
                                        List<dislike> dislikes = tle.va.getDislikes();
                                        List<comment> comments = tle.va.getComments();
                                        for (int k = 0; k < likes.size(); k++){
                                            like li = likes.get(k);
                                            if (li.getId() > since){
                                                if (!tle.va.removelike(li.getId()))
                                                    return false;
                                                if (!removeFromWall(tle.va.getId(), null))
                                                    return false;
                                                if (!insertVideoAlbumIntoWall(tle.va.getId(), tle.va))
                                                    return false;
                                                if (!saveWall(filename, tle.va.getShareWith()))
                                                    return false;
                                                loadVideos();
                                                deleteVideoAlbum(tle.va.getId());
                                                insertVideoAlbum(tle.va);
                                                saveVideos();
                                            }
                                        }
                                        for (int k = 0; k < dislikes.size(); k++){
                                            dislike dli = dislikes.get(k);
                                            if (dli.getId() > since){
                                                if (!tle.va.removeDislike(dli.getId()))
                                                    return false;
                                                if (!removeFromWall(tle.va.getId(), null))
                                                    return false;
                                                if (!insertVideoAlbumIntoWall(tle.va.getId(), tle.va))
                                                    return false;
                                                if (!saveWall(filename, tle.va.getShareWith()))
                                                    return false;
                                                loadVideos();
                                                deleteVideoAlbum(tle.va.getId());
                                                insertVideoAlbum(tle.va);
                                                saveVideos();
                                            }
                                        }
                                        for (int k = 0; k < comments.size(); k++){
                                            comment c = comments.get(k);
                                            if (c.getId() > since){
                                                if (!tle.va.removeComment(c.getId()))
                                                    return false;
                                                if (!removeFromWall(tle.va.getId(), null))
                                                    return false;
                                                if (!insertVideoAlbumIntoWall(tle.va.getId(), tle.va))
                                                    return false;
                                                if (!saveWall(filename, tle.va.getShareWith()))
                                                    return false;
                                                loadVideos();
                                                deleteVideoAlbum(tle.va.getId());
                                                insertVideoAlbum(tle.va);
                                                saveVideos();
                                            }
                                        }
                                    }
                                }
                            }
                            if (tle.type.equals("video")){
                                if (tle.v.getLastUpdateTime() > since){
                                    if (tle.v.getId() > since){
                                        if (!removeFromWall(tle.v.getId(), null))
                                            return false;
                                        if (!saveWall(filename, tle.v.getShareWith()))
                                            return false;
                                        loadVideos();
                                        deleteVideo(tle.v.getParent(), tle.v.getId());
                                        saveVideos();
                                    }else{
                                        List<like> likes = tle.v.getLikes();
                                        List<dislike> dislikes = tle.v.getDislikes();
                                        List<comment> comments = tle.v.getComments();
                                        for (int k = 0; k < likes.size(); k++){
                                            like li = new like(likes.get(k));
                                            if (li.getId() > since){
                                                if (!tle.v.removelike(li.getId()))
                                                    return false;
                                                if (!removeFromWall(tle.v.getId(), null))
                                                    return false;
                                                if (!insertVideoIntoWall(tle.v.getId(), tle.v))
                                                    return false;
                                                if (!saveWall(filename, tle.v.getShareWith()))
                                                    return false;
                                                loadVideos();
                                                deleteVideo(tle.v.getParent(), tle.v.getId());
                                                insertVideo(tle.v);
                                                saveVideos();
                                            }
                                        }
                                        for (int k = 0; k < dislikes.size(); k++){
                                            dislike dli = new dislike(dislikes.get(k));
                                            if (dli.getId() > since){
                                                if (!tle.v.removeDislike(dli.getId()))
                                                    return false;
                                                if (!removeFromWall(tle.v.getId(), null))
                                                    return false;
                                                if (!insertVideoIntoWall(tle.v.getId(), tle.v))
                                                    return false;
                                                if (!saveWall(filename, tle.v.getShareWith()))
                                                    return false;
                                                loadVideos();
                                                deleteVideo(tle.v.getParent(), tle.v.getId());
                                                insertVideo(tle.v);
                                                saveVideos();
                                            }
                                        }
                                        for (int k = 0; k < comments.size(); k++){
                                            comment c = new comment(comments.get(k));
                                            if (c.getId() > since){
                                                if (!tle.v.removeComment(c.getId()))
                                                    return false;
                                                if (!removeFromWall(tle.v.getId(), null))
                                                    return false;
                                                if (!insertVideoIntoWall(tle.v.getId(), tle.v))
                                                    return false;
                                                if (!saveWall(filename, tle.v.getShareWith()))
                                                    return false;
                                                loadVideos();
                                                deleteVideo(tle.v.getParent(), tle.v.getId());
                                                insertVideo(tle.v);
                                                saveVideos();
                                            }
                                        }
                                    }
                                }
                            }
                            if (tle.type.equals("photoAlbum")){
                                if (tle.pa.getLastUpdateTime() > since){
                                    if (tle.pa.getId() > since){
                                        if (!removeFromWall(tle.pa.getId(), null)){
                                            return false;
                                        }
                                        if (!saveWall(filename, tle.pa.getShareWith())){
                                            return false;
                                        }
                                        loadPhotos();
                                        deletePhotoAlbum(tle.pa.getId());
                                        savePhotos();
                                    }else{
                                        List<like> likes = tle.pa.getLikes();
                                        List<dislike> dislikes = tle.pa.getDislikes();
                                        List<comment> comments = tle.pa.getComments();
                                        for (int k = 0; k < likes.size(); k++){
                                            like li = likes.get(k);
                                            if (li.getId() > since){
                                                if (!tle.pa.removelike(li.getId()))
                                                    return false;
                                                if (!removeFromWall(tle.pa.getId(), null))
                                                    return false;
                                                if (!insertPhotoAlbumIntoWall(tle.pa.getId(), tle.pa))
                                                    return false;
                                                if (!saveWall(filename, tle.pa.getShareWith()))
                                                    return false;
                                                loadPhotos();
                                                deletePhotoAlbum(tle.pa.getId());
                                                insertPhotoAlbum(tle.pa);
                                                savePhotos();
                                            }
                                        }
                                        for (int k = 0; k < dislikes.size(); k++){
                                            dislike dli = dislikes.get(k);
                                            if (dli.getId() > since){
                                                if (!tle.pa.removeDislike(dli.getId()))
                                                    return false;
                                                if (!removeFromWall(tle.pa.getId(), null))
                                                    return false;
                                                if (!insertPhotoAlbumIntoWall(tle.pa.getId(), tle.pa))
                                                    return false;
                                                if (!saveWall(filename, tle.pa.getShareWith()))
                                                    return false;
                                                loadPhotos();
                                                deletePhotoAlbum(tle.pa.getId());
                                                insertPhotoAlbum(tle.pa);
                                                savePhotos();
                                            }
                                        }
                                        for (int k = 0; k < comments.size(); k++){
                                            comment c = comments.get(k);
                                            if (c.getId() > since){
                                                if (!tle.pa.removeComment(c.getId()))
                                                    return false;
                                                if (!removeFromWall(tle.pa.getId(), null))
                                                    return false;
                                                if (!insertPhotoAlbumIntoWall(tle.pa.getId(), tle.pa))
                                                    return false;
                                                if (!saveWall(filename, tle.pa.getShareWith()))
                                                    return false;
                                                loadPhotos();
                                                deletePhotoAlbum(tle.pa.getId());
                                                insertPhotoAlbum(tle.pa);
                                                savePhotos();
                                            }
                                        }
                                    }
                                }
                            }
                            if (tle.type.equals("photo")){
                                if (tle.p.getLastUpdateTime() > since){
                                    if (tle.p.getId() > since){
                                        if (!removeFromWall(tle.p.getId(), null)){
                                            return false;
                                        }
                                        if (!saveWall(filename, tle.p.getShareWith())){
                                            return false;
                                        }
                                        loadPhotos();
                                        deletePhoto(tle.p.getParent(), tle.p.getId());
                                        savePhotos();
                                    }else{
                                        List<like> likes = tle.p.getLikes();
                                        List<dislike> dislikes = tle.p.getDislikes();
                                        List<comment> comments = tle.p.getComments();
                                        for (int k = 0; k < likes.size(); k++){
                                            like li = new like(likes.get(k));
                                            if (li.getId() > since){
                                                if (!tle.p.removelike(li.getId()))
                                                    return false;
                                                if (!removeFromWall(tle.p.getId(), null))
                                                    return false;
                                                if (!insertPhotoIntoWall(tle.p.getId(), tle.p))
                                                    return false;
                                                if (!saveWall(filename, tle.p.getShareWith()))
                                                    return false;
                                                loadPhotos();
                                                deletePhoto(tle.p.getParent(), tle.p.getId());
                                                insertPhoto(tle.p);
                                                savePhotos();
                                            }
                                        }
                                        for (int k = 0; k < dislikes.size(); k++){
                                            dislike dli = new dislike(dislikes.get(k));
                                            if (dli.getId() > since){
                                                if (!tle.p.removeDislike(dli.getId()))
                                                    return false;
                                                if (!removeFromWall(tle.p.getId(), null))
                                                    return false;
                                                if (!insertPhotoIntoWall(tle.p.getId(), tle.p))
                                                    return false;
                                                if (!saveWall(filename, tle.p.getShareWith()))
                                                    return false;
                                                loadPhotos();
                                                deletePhoto(tle.p.getParent(), tle.p.getId());
                                                insertPhoto(tle.p);
                                                savePhotos();
                                            }
                                        }
                                        for (int k = 0; k < comments.size(); k++){
                                            comment c = new comment(comments.get(k));
                                            if (c.getId() > since){
                                                if (!tle.p.removeComment(c.getId()))
                                                    return false;
                                                if (!removeFromWall(tle.p.getId(), null))
                                                    return false;
                                                if (!insertPhotoIntoWall(tle.p.getId(), tle.p))
                                                    return false;
                                                if (!saveWall(filename, tle.p.getShareWith()))
                                                    return false;
                                                loadPhotos();
                                                deletePhoto(tle.p.getParent(), tle.p.getId());
                                                insertPhoto(tle.p);
                                                savePhotos();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean trimWall(long timestamp){
        if (DEBUG){
            System.out.println("DEBUG: line 1726 of profile.java. in trimWall with " + timestamp);
            System.out.println("DEBUG: line 1727 of profile.java. wall size at the start of trimWall = " + wall.size());
        }
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            for (int i = wall.size() - 1; i >= 0 ; i--){
                timelineEntry tle = new timelineEntry(wall.get(i));
                if (tle.lastUpdateTime < timestamp){
                    wall.remove(i);
                }
            }
            if (DEBUG){
                System.out.println("DEBUG: line 1739 of profile.java. wall size at the end of trimWall = " + wall.size());
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    // needs to update modifiedfiles.xml
    private boolean saveWall(String file, String shareWith){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            wallDOM = db.newDocument();
            Element rootEle = wallDOM.createElement("wall");
            wallDOM.appendChild(rootEle);
            for (int i = 0; i < wall.size(); i++){
                timelineEntry tle = new timelineEntry(wall.get(i));
                rootEle.appendChild(tle.createDOMElement(wallDOM));
            }
            while (!saveXML(file, wallDOM)){
                Thread.sleep(100);
            }
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            updateModifiedFiles("add", now.getTimeInMillis(), file, shareWith);
            if (profileOwner.equals(accountHolder)){
                updateModifiedFiles("add", now.getTimeInMillis(), prefix + accountHolder + "/zones/" + shareWith + "/history.xml", shareWith); 
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean insertLinkIntoWall(long timestamp, link l){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            if (wall.size() == 0){
                timelineEntry toBeAdded = new timelineEntry(timestamp, "link", l);
                wall.add(toBeAdded);
                return true;
            }
            int i = 0;
            for (i = 0; i < wall.size(); i++){
                timelineEntry tle = new timelineEntry(wall.get(i));
                if (timestamp >= tle.id){
                    if (timestamp == tle.id && tle.type.equals("link")){
                        if (l.getLastUpdateTime() <= tle.l.getLastUpdateTime()){
                            return false;
                        }else{
                            timelineEntry toBeAdded = new timelineEntry(timestamp, "link", l);
                            wall.add(i, toBeAdded);
                            wall.remove(i + 1);
                            return true;
                        }
                    }
                    break;
                }
            }
            timelineEntry toBeAdded = new timelineEntry(timestamp, "link", l);
            if(i == wall.size())
                wall.add(toBeAdded);
            else
                wall.add(i, toBeAdded);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean insertwallPostIntoWall(long timestamp, wallPost s){
        if (profileOwner == null || profileOwner.equals("")){
            return false;
        }
        try{
            if (wall.size() == 0){
                timelineEntry toBeAdded = new timelineEntry(timestamp, "wallPost", s);
                wall.add(toBeAdded);
                return true;
            }
            int i = 0;
            for (i = 0; i < wall.size(); i++){
                timelineEntry tle = new timelineEntry(wall.get(i));
                if (timestamp >= tle.id){
                    if (timestamp == tle.id && tle.type.equals("wallPost")){
                        if (s.getLastUpdateTime() <= tle.s.getLastUpdateTime()){
                            return false;
                        }else{
                            timelineEntry toBeAdded = new timelineEntry(timestamp, "wallPost", s);
                            wall.add(i, toBeAdded);
                            wall.remove(i + 1);
                            return true;
                        }
                    }
                    break;
                }
            }
            timelineEntry toBeAdded = new timelineEntry(timestamp, "wallPost", s);
            if(i == wall.size())
                wall.add(toBeAdded);
            else
                wall.add(i, toBeAdded);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean insertAudioAlbumIntoWall(long timestamp, audioAlbum aa){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            if (getMapEntry(aa.getId()) != aa.getId())
                aa.setId(getMapEntry(aa.getId()));
            if (wall.size() == 0){
                timelineEntry toBeAdded = new timelineEntry(timestamp, "audioAlbum", aa);
                wall.add(toBeAdded);
                return true;
            }
            int i = 0;
            for (i = 0; i < wall.size(); i++){
                timelineEntry tle = (timelineEntry)wall.get(i);
                if (timestamp >= tle.id){
                    if (timestamp == tle.id && tle.type.equals("audioAlbum")){
                        if (aa.getLastUpdateTime() <= tle.aa.getLastUpdateTime()){
                            return false;
                        }else{
                            timelineEntry toBeAdded = new timelineEntry(timestamp, "audioAlbum", aa);
                            wall.add(i, toBeAdded);
                            wall.remove(i + 1);
                            return true;
                        }
                    }
                    break;
                }
            }
            timelineEntry toBeAdded = new timelineEntry(timestamp, "audioAlbum", aa);
            if(i == wall.size())
                wall.add(toBeAdded);
            else
                wall.add(i, toBeAdded);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean insertAudioIntoWall(long timestamp, audio a){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            a.setParent(getMapEntry(a.getParent()));
            if (wall.size() == 0){
                timelineEntry toBeAdded = new timelineEntry(timestamp, "audio", a);
                wall.add(toBeAdded);
                return true;
            }
            int i = 0;
            for (i = 0; i < wall.size(); i++){
                timelineEntry tle = new timelineEntry(wall.get(i));
                if (timestamp >= tle.id){
                    if (timestamp == tle.id && tle.type.equals("audio")){
                        if (a.getLastUpdateTime() <= tle.a.getLastUpdateTime()){
                            return false;
                        }else{
                            timelineEntry toBeAdded = new timelineEntry(timestamp, "audio", a);
                            wall.add(i, toBeAdded);
                            wall.remove(i + 1);
                            return true;
                        }
                    }
                    break;
                }
            }
            timelineEntry toBeAdded = new timelineEntry(timestamp, "audio", a);
            if(i == wall.size())
                wall.add(toBeAdded);
            else
                wall.add(i, toBeAdded);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean insertVideoAlbumIntoWall(long timestamp, videoAlbum va){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            if (getMapEntry(va.getId()) != va.getId())
                va.setId(getMapEntry(va.getId()));
            if (wall.size() == 0){
                timelineEntry toBeAdded = new timelineEntry(timestamp, "videoAlbum", va);
                wall.add(toBeAdded);
                return true;
            }
            int i = 0;
            for (i = 0; i < wall.size(); i++){
                timelineEntry tle = (timelineEntry)wall.get(i);
                if (timestamp >= tle.id){
                    if (timestamp == tle.id && tle.type.equals("videoAlbum")){
                        if (va.getLastUpdateTime() <= tle.va.getLastUpdateTime()){
                            return false;
                        }else{
                            timelineEntry toBeAdded = new timelineEntry(timestamp, "videoAlbum", va);
                            wall.add(i, toBeAdded);
                            wall.remove(i + 1);
                            return true;
                        }
                    }
                    break;
                }
            }
            timelineEntry toBeAdded = new timelineEntry(timestamp, "videoAlbum", va);
            if(i == wall.size())
                wall.add(toBeAdded);
            else
                wall.add(i, toBeAdded);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean insertVideoIntoWall(long timestamp, video v){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            v.setParent(getMapEntry(v.getParent()));
            if (wall.size() == 0){
                timelineEntry toBeAdded = new timelineEntry(timestamp, "video", v);
                wall.add(toBeAdded);
                return true;
            }
            int i = 0;
            for (i = 0; i < wall.size(); i++){
                timelineEntry tle = new timelineEntry(wall.get(i));
                if (timestamp >= tle.id){
                    if (timestamp == tle.id && tle.type.equals("video")){
                        if (v.getLastUpdateTime() <= tle.v.getLastUpdateTime()){
                            return false;
                        }else{
                            timelineEntry toBeAdded = new timelineEntry(timestamp, "video", v);
                            wall.add(i, toBeAdded);
                            wall.remove(i + 1);
                            return true;
                        }
                    }
                    break;
                }
            }
            timelineEntry toBeAdded = new timelineEntry(timestamp, "video", v);
            if(i == wall.size())
                wall.add(toBeAdded);
            else
                wall.add(i, toBeAdded);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean insertPhotoAlbumIntoWall(long timestamp, photoAlbum pa){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            if (getMapEntry(pa.getId()) != pa.getId())
                pa.setId(getMapEntry(pa.getId()));
            if (wall.size() == 0){
                timelineEntry toBeAdded = new timelineEntry(timestamp, "photoAlbum", pa);
                wall.add(toBeAdded);
                return true;
            }
            int i = 0;
            for (i = 0; i < wall.size(); i++){
                timelineEntry tle = (timelineEntry)wall.get(i);
                if (timestamp >= tle.id){
                    if (timestamp == tle.id && tle.type.equals("photoAlbum")){
                        if (pa.getLastUpdateTime() <= tle.pa.getLastUpdateTime()){
                            return false;
                        }else{
                            timelineEntry toBeAdded = new timelineEntry(timestamp, "photoAlbum", pa);
                            wall.add(i, toBeAdded);
                            wall.remove(i + 1);
                            return true;
                        }
                    }
                    break;
                }
            }
            timelineEntry toBeAdded = new timelineEntry(timestamp, "photoAlbum", pa);
            if(i == wall.size())
                wall.add(toBeAdded);
            else
                wall.add(i, toBeAdded);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean insertPhotoIntoWall(long timestamp, photo p){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            p.setParent(getMapEntry(p.getParent()));
            if (wall.size() == 0){
                timelineEntry toBeAdded = new timelineEntry(timestamp, "photo", p);
                wall.add(toBeAdded);
                return true;
            }
            int i = 0;
            for (i = 0; i < wall.size(); i++){
                timelineEntry tle = new timelineEntry(wall.get(i));
                if (timestamp >= tle.id){
                    if (timestamp == tle.id && tle.type.equals("photo")){
                        if (p.getLastUpdateTime() <= tle.p.getLastUpdateTime()){
                            return false;
                        }else{
                            timelineEntry toBeAdded = new timelineEntry(timestamp, "photo", p);
                            wall.add(i, toBeAdded);
                            wall.remove(i + 1);
                            return true;
                        }
                    }
                    break;
                }
            }
            timelineEntry toBeAdded = new timelineEntry(timestamp, "photo", p);
            if(i == wall.size())
                wall.add(toBeAdded);
            else
                wall.add(i, toBeAdded);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean insertDeletedEntryIntoWall(long timestamp, deletedEntry de){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            if (getMapEntry(de.getId()) != de.getId() || getMapEntry(de.getParent()) != de.getParent()){
                de.setParent(getMapEntry(de.getParent()));
                de.setId(getMapEntry(de.getId()));
            }
            if (wall.size() == 0){
                timelineEntry toBeAdded = new timelineEntry(timestamp, "deletedEntry", de);
                wall.add(toBeAdded);
                return true;
            }
            int i = 0;
            for (i = 0; i < wall.size(); i++){
                timelineEntry tle = new timelineEntry(wall.get(i));
                if (de.getId() >= tle.id){
                    if (de.getId() == tle.id && tle.type.equals("deletedEntry")){
                        return true;
                    }else{
                        break;
                    }
                }
            }
            timelineEntry toBeAdded = new timelineEntry(timestamp, "deletedEntry", de);
            if(i == wall.size())
                wall.add(toBeAdded);
            else
                wall.add(i, toBeAdded);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean removeFromWall(long timestamp, deletedEntry de){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            if (wall.size() == 0){
                return false;
            }
            for (int i = 0; i < wall.size(); i++){
                timelineEntry tle = new timelineEntry(wall.get(i));
                if (timestamp == tle.id && !tle.type.equals("deletedEntry")){
                    wall.remove(i);
                    if (de != null){
                        tle = new timelineEntry(de.getId(), "deletedEntry", de);
                        insertDeletedEntryIntoWall(de.getId(), de);
                    }
                    return true;
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return false;
    }
    
    public boolean addLink(link l, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: al1");
            }
            return false;
        }
        try{
            if (!profileOwner.equals(accountHolder) && !committed){
                if (DEBUG){
                    System.out.println("DEBUG: al2");
                }
                updatePendingChanges("add", "link", new timelineEntry(l.getId(), "link", l));
            }
            String filename = updateHistory(l.getId(), l.getShareWith());
            if (filename == null){
                if (DEBUG){
                    System.out.println("DEBUG: al3");
                }
                return false;
            }
            wall.clear();
            loadWall(filename);
            if (!insertLinkIntoWall(l.getId(), l)){
                if (DEBUG){
                    System.out.println("DEBUG: al4");
                }
                return false;
            }
            if (!saveWall(filename, l.getShareWith())){
                if (DEBUG){
                    System.out.println("DEBUG: al5");
                }
                return false;
            }
            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "link", l.getId(), l.getShareWith(), l.getPostedBy());
            insertNotificaton(n);
            if (DEBUG){
                System.out.println("DEBUG: al success");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean removeLink(long id, String shareWith, String postedBy, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: rl1");
            }
            return false;
        }
        try{
            if (!profileOwner.equals(accountHolder) && !postedBy.equals(accountHolder) && !executeAsProfileOwner){
                if (DEBUG){
                    System.out.println("DEBUG: rl2");
                }
                return false;
            }
            if (!profileOwner.equals(accountHolder) && !committed){
                if (DEBUG){
                    System.out.println("DEBUG: rl3");
                }
                user u = new user(postedBy, null, null, 0);
                link l = new link(id, 0, profileOwner, u, null, null, shareWith, null, null, null);
                updatePendingChanges("remove", "link", new timelineEntry(l.getId(), "link", l));
            }
            String filename = getWallFileName(id, shareWith);
            if (filename == null){
                if (DEBUG){
                    System.out.println("DEBUG: rl4");
                }
                return false;
            }
            wall.clear();
            if (!loadWall(filename)){
                if (DEBUG){
                    System.out.println("DEBUG: rl5");
                }
                return false;
            }
            deletedEntry de = new deletedEntry(0, id, Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), shareWith, postedBy, "link");
            if (!removeFromWall(id, de)){
                if (DEBUG){
                    System.out.println("DEBUG: rl6");
                }
                return false;
            }
            if (!saveWall(filename, shareWith)){
                if (DEBUG){
                    System.out.println("DEBUG: rl7");
                }
                return false;
            }
            if (DEBUG){
                System.out.println("DEBUG: rl success");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean addWallPost(wallPost s, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: awp1");
            }
            return false;
        }
        try{
            if (!profileOwner.equals(accountHolder) && !committed){
                if (DEBUG){
                    System.out.println("DEBUG: awp2");
                }
                updatePendingChanges("add", "wallPost", new timelineEntry(s.getId(), "wallPost", s));
            }
            String filename = updateHistory(s.getId(), s.getShareWith());
            if (filename == null){
                if (DEBUG){
                    System.out.println("DEBUG: awp3");
                }
                return false;
            }
            wall.clear();
            loadWall(filename);
            if (!insertwallPostIntoWall(s.getId(), s)){
                if (DEBUG){
                    System.out.println("DEBUG: awp4");
                }
                return false;
            }
            if (!saveWall(filename, s.getShareWith())){
                if (DEBUG){
                    System.out.println("DEBUG: awp5");
                }
                return false;
            }
            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "wallPost", s.getId(), s.getShareWith(), s.getPostedBy());
            insertNotificaton(n);
            if (DEBUG){
                System.out.println("DEBUG: awp success");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean removewallPost(long id, String shareWith, String postedBy, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: rwp1");
            }
            return false;
        }
        try{
            if (!profileOwner.equals(accountHolder) && !postedBy.equals(accountHolder) && !executeAsProfileOwner){
                if (DEBUG){
                    System.out.println("DEBUG: rwp2");
                }
                return false;
            }
            if (!profileOwner.equals(accountHolder) && !committed){
                if (DEBUG){
                    System.out.println("DEBUG: rwp3");
                }
                user u = new user(postedBy, null, null, 0);
                wallPost s = new wallPost(id, 0, profileOwner, u, null, shareWith, null, null, null);
                updatePendingChanges("remove", "wallPost", new timelineEntry(s.getId(), "wallPost", s));
            }
            String filename = getWallFileName(id, shareWith);
            if (filename == null){
                if (DEBUG){
                    System.out.println("DEBUG: rwp4");
                }
                return false;
            }
            wall.clear();
            if (!loadWall(filename)){
                if (DEBUG){
                    System.out.println("DEBUG: rwp5");
                }
                return false;
            }
            deletedEntry de = new deletedEntry(0, id, Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), shareWith, postedBy, "wallPost");
            if (!removeFromWall(id, de)){
                if (DEBUG){
                    System.out.println("DEBUG: rwp6");
                }
                return false;
            }
            if (!saveWall(filename, shareWith)){
                if (DEBUG){
                    System.out.println("DEBUG: rwp7");
                }
                return false;
            }
            if (DEBUG){
                System.out.println("DEBUG: rwp success");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean loadAudios(){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            String path;
            mainSettings.refresh(mainSettings.BASIC_INFO);
            audioAlbums.clear();
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                path = prefix + accountHolder + "/friends/" + profileOwner;
            }
            return parseDocument(path + "/audios.xml", "audioAlbum");
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
    }
    
    public boolean loadVideos(){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            String path;
            mainSettings.refresh(mainSettings.BASIC_INFO);
            videoAlbums.clear();
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                path = prefix + accountHolder + "/friends/" + profileOwner;
            }
            return parseDocument(path + "/videos.xml", "videoAlbum");
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
    }
    
    public boolean loadPhotos(){
        if (profileOwner == null || profileOwner.equals("")){
            return false;
        }
        try{
            String path;
            mainSettings.refresh(mainSettings.BASIC_INFO);
            photoAlbums.clear();
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                path = prefix + accountHolder + "/friends/" + profileOwner;
            }
            return parseDocument(path + "/photos.xml", "photoAlbum");
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
    }
    
    // can only be called from the user's own profile.
    private void saveAudios(){
        if (profileOwner == null || profileOwner.equals(""))
            return;
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            dom = db.newDocument();
            
            Element rootEle = dom.createElement("allAudios");
            dom.appendChild(rootEle);
            
            Iterator it  = audioAlbums.iterator();
            while(it.hasNext()) {
                audioAlbum aa = new audioAlbum((audioAlbum)it.next());
                rootEle.appendChild(aa.createDOMElement(dom));
            }
            String path;
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                path = prefix + accountHolder + "/friends/" + profileOwner; 
            }
            while (!saveXML(path + "/audios.xml", dom)){
                Thread.sleep(100);    
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    // can only be called from the user's own profile.
    private void saveVideos(){
        if (profileOwner == null || profileOwner.equals(""))
            return;
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            dom = db.newDocument();
            
            Element rootEle = dom.createElement("allVideos");
            dom.appendChild(rootEle);
            
            Iterator it  = videoAlbums.iterator();
            while(it.hasNext()) {
                videoAlbum va = new videoAlbum((videoAlbum)it.next());
                rootEle.appendChild(va.createDOMElement(dom));
            }
            String path;
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                path = prefix + accountHolder + "/friends/" + profileOwner; 
            }
            while (!saveXML(path + "/videos.xml", dom)){
                Thread.sleep(100);
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    private void savePhotos(){
        if (profileOwner == null || profileOwner.equals(""))
            return;
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            dom = db.newDocument();
            
            Element rootEle = dom.createElement("allPhotos");
            dom.appendChild(rootEle);
            
            Iterator it  = photoAlbums.iterator();
            while(it.hasNext()) {
                photoAlbum pa = new photoAlbum((photoAlbum)it.next());
                rootEle.appendChild(pa.createDOMElement(dom));
            }
            String path;
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                path = prefix + accountHolder + "/friends/" + profileOwner; 
            }
            while (!saveXML(path + "/photos.xml", dom)){
                Thread.sleep(100);
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    private void insertAudioAlbum(audioAlbum aa){
        if (profileOwner == null || profileOwner.equals(""))
            return;
        try{
            for (int i = 0; i < audioAlbums.size(); i++){
                audioAlbum x = new audioAlbum(audioAlbums.get(i));
                if (aa.getId() >= x.getId()){
                    if (aa.getId() == x.getId()){
                        if (aa.getLastUpdateTime() <= x.getLastUpdateTime()){
                            return;
                        }else{
                            aa.setAudioFiles(x.getAudioFiles());
                            audioAlbums.add(i, aa);
                            audioAlbums.remove(i + 1);
                        }
                    }else{
                        audioAlbums.add(i, aa);
                    }
                    return;
                }
            }
            audioAlbums.add(aa);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    private void insertVideoAlbum(videoAlbum va){
        if (profileOwner == null || profileOwner.equals(""))
            return;
        try{
            for (int i = 0; i < videoAlbums.size(); i++){
                videoAlbum x = new videoAlbum(videoAlbums.get(i));
                if (va.getId() >= x.getId()){
                    if (va.getId() == x.getId()){
                        if (va.getLastUpdateTime() <= x.getLastUpdateTime()){
                            return;
                        }else{
                            va.setVideos(x.getVideoFiles());
                            videoAlbums.add(i, va);
                            videoAlbums.remove(i + 1);
                        }
                    }else{
                        videoAlbums.add(i, va);
                    }
                    return;
                }
            }
            videoAlbums.add(va);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    private void insertPhotoAlbum(photoAlbum pa){
        if (profileOwner == null || profileOwner.equals(""))
            return;
        try{
            for (int i = 0; i < photoAlbums.size(); i++){
                photoAlbum x = new photoAlbum(photoAlbums.get(i));
                if (pa.getId() >= x.getId()){
                    if (pa.getId() == x.getId()){
                        if (pa.getLastUpdateTime() <= x.getLastUpdateTime()){
                            return;
                        }else{
                            pa.setPhotos(x.getPhotoFiles());
                            photoAlbums.add(i, pa);
                            photoAlbums.remove(i + 1);
                        }
                    }else{
                        photoAlbums.add(i, pa);
                    }
                    return;
                }
            }
            photoAlbums.add(pa);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    public boolean addAudioAlbum(audioAlbum aa, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: aaa1");
            }
            return false;
        }
        try{
            if (!profileOwner.equals(accountHolder) && !committed){
                if (DEBUG){
                    System.out.println("DEBUG: aaa2");
                }
                updatePendingChanges("add", "audioAlbum", new timelineEntry(aa.getId(), "audioAlbum", aa));
            }
            String filename = updateHistory(aa.getId(), aa.getShareWith());
            if (filename == null){
                if (DEBUG){
                    System.out.println("DEBUG: aaa3");
                }
                return false;
            }
            wall.clear();
            loadWall(filename);
            if (!insertAudioAlbumIntoWall(aa.getId(),aa)){
                if (DEBUG){
                    System.out.println("DEBUG: aaa4");
                }
                return false;
            }
            if (!saveWall(filename, aa.getShareWith())){
                if (DEBUG){
                    System.out.println("DEBUG: aaa5");
                }
                return false;
            }
            loadAudios();
            insertAudioAlbum(aa);
            saveAudios();
            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "audioAlbum", aa.getId(), aa.getShareWith(), aa.getPostedBy());
            insertNotificaton(n);
            if (DEBUG){
                System.out.println("DEBUG: aaa success");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean removeAudioAlbum(long id, String shareWith, String postedBy, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: raa1");
            }
            return false;
        }
        try{
            id = getMapEntry(id);
            if (!profileOwner.equals(accountHolder) && !postedBy.equals(accountHolder) && !executeAsProfileOwner){
                if (DEBUG){
                    System.out.println("DEBUG: raa2");
                }
                return false;
            }
            if (!profileOwner.equals(accountHolder) && !committed){
                if (DEBUG){
                    System.out.println("DEBUG: raa3");
                }
                user u = new user(postedBy, null, null, 0);
                audioAlbum aa = new audioAlbum(id, 0, profileOwner, u, null, shareWith, null, null, null, null);
                updatePendingChanges("remove", "audioAlbum", new timelineEntry(0, "audioAlbum", aa));
            }
            if (!loadAudios()){
                if (DEBUG){
                    System.out.println("DEBUG: raa4");
                }
                return false;
            }
            audioAlbum aa = new audioAlbum();
            for (int i = 0; i < audioAlbums.size(); i++){
                aa = new audioAlbum(audioAlbums.get(i));
                if (aa.getId() == id)
                    break;
            }
            List<audio> audios = new ArrayList<audio>();
            audios.addAll(aa.getAudioFiles());
            if (!deleteAudioAlbum(id))
            {
                if (DEBUG){
                    System.out.println("DEBUG: raa5");
                }
                return false;
            }
            saveAudios();
            String filename = getWallFileName(id, shareWith);
            if (filename != null){
                wall.clear();
                loadWall(filename);
                deletedEntry de = new deletedEntry(0, id, Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), shareWith, postedBy, "audioAlbum");
                removeFromWall(id, de);
                saveWall(filename, shareWith);
            }
            for (int i = 0; i < audios.size(); i++){
                audio a = (audio)audios.get(i);
                filename = getWallFileName(a.getId(), a.getShareWith());
                if (filename != null){
                    wall.clear();
                    loadWall(filename);
                    deletedEntry de = new deletedEntry(a.getParent(), a.getId(), Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), shareWith, a.getPostedBy().getUsername(), "audio");
                    removeFromWall(a.getId(), de);
                    saveWall(filename, a.getShareWith());
                }
            }
            if (DEBUG){
                System.out.println("DEBUG: raa success");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean deleteAudioAlbum(long id){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            for (int i = 0; i < audioAlbums.size(); i++){
                audioAlbum x = new audioAlbum(audioAlbums.get(i));
                if (id == x.getId()){
                    List<audio> audioFiles = x.getAudioFiles();
                    for (int j = 0; j < audioFiles.size(); j++){
                        audio a = new audio(audioFiles.get(j));
                        deleteAudio(id, a.getId());
                    }
                    audioAlbums.remove(i);
                    return true;
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public boolean addVideoAlbum(videoAlbum va, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: ava1");
            }
            return false;
        }
        try{
            if (!profileOwner.equals(accountHolder) && !committed){
                if (DEBUG){
                    System.out.println("DEBUG: ava2");
                }
                updatePendingChanges("add", "videoAlbum", new timelineEntry(va.getId(), "videoAlbum", va));
            }
            String filename = updateHistory(va.getId(), va.getShareWith());
            if (filename == null){
                if (DEBUG){
                    System.out.println("DEBUG: ava3");
                }
                return false;
            }
            wall.clear();
            loadWall(filename);
            if (!insertVideoAlbumIntoWall(va.getId(), va)){
                if (DEBUG){
                    System.out.println("DEBUG: ava4");
                }
                return false;
            }
            if (!saveWall(filename, va.getShareWith())){
                if (DEBUG){
                    System.out.println("DEBUG: ava5");
                }
                return false;
            }
            loadVideos();
            insertVideoAlbum(va);
            saveVideos();
            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "videoAlbum", va.getId(), va.getShareWith(), va.getPostedBy());
            insertNotificaton(n);
            if (DEBUG){
                System.out.println("DEBUG: ava success");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean removeVideoAlbum(long id, String shareWith, String postedBy, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: rva1");
            }
            return false;
        }
        try{
            id = getMapEntry(id);
            if (!profileOwner.equals(accountHolder) && !postedBy.equals(accountHolder) && !executeAsProfileOwner){
                if (DEBUG){
                    System.out.println("DEBUG: rva2");
                }
                return false;
            }
            if (!profileOwner.equals(accountHolder) && !committed){
                if (DEBUG){
                    System.out.println("DEBUG: rva3");
                }
                user u = new user(postedBy, null, null, 0);
                videoAlbum va = new videoAlbum(id, 0, profileOwner, u, null, shareWith, null, null, null, null);
                updatePendingChanges("remove", "videoAlbum", new timelineEntry(va.getId(), "videoAlbum", va));
            }
            if (!loadVideos()){
                if (DEBUG){
                    System.out.println("DEBUG: rva4");
                }
                return false;
            }
            videoAlbum va = new videoAlbum();
            for (int i = 0; i < videoAlbums.size(); i++){
                va = new videoAlbum(videoAlbums.get(i));
                if (va.getId() == id)
                    break;
            }
            List<video> videos = new ArrayList<video>();
            videos.addAll(va.getVideoFiles());
            if (!deleteVideoAlbum(id))
            {
                if (DEBUG){
                    System.out.println("DEBUG: rva5");
                }
                return false;
            }
            saveVideos();
            String filename = getWallFileName(id, shareWith);
            if (filename != null){
                wall.clear();
                loadWall(filename);
                deletedEntry de = new deletedEntry(0, id, Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), shareWith, postedBy, "videoAlbum");
                removeFromWall(id, de);
                saveWall(filename, shareWith);
            }
            for (int i = 0; i < videos.size(); i++){
                video v = new video(videos.get(i));
                filename = getWallFileName(v.getId(), v.getShareWith());
                if (filename != null){
                    wall.clear();
                    loadWall(filename);
                    deletedEntry de = new deletedEntry(v.getParent(), v.getId(), Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), shareWith, v.getPostedBy().getUsername(), "video");
                    removeFromWall(v.getId(), de);
                    saveWall(filename, v.getShareWith());
                }
            }
            if (DEBUG){
                System.out.println("DEBUG: rva success");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean deleteVideoAlbum(long id){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            for ( int i = 0; i < videoAlbums.size(); i++){
                videoAlbum x = new videoAlbum(videoAlbums.get(i));
                if (id == x.getId()){
                    List<video> videoFiles = x.getVideoFiles();
                    for (int j = 0; j < videoFiles.size(); j++){
                        video v = new video(videoFiles.get(j));
                        deletePhoto(id, v.getId());
                    }
                    videoAlbums.remove(i);
                    return true;
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public boolean addPhotoAlbum(photoAlbum pa, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: apa1");
            }
            return false;
        }
        try{
            if (!profileOwner.equals(accountHolder) && !committed){
                if (DEBUG){
                    System.out.println("DEBUG: apa2");
                }
                updatePendingChanges("add", "photoAlbum", new timelineEntry(pa.getId(), "photoAlbum", pa));
            }
            String filename = updateHistory(pa.getId(), pa.getShareWith());
            if (filename == null){
                if (DEBUG){
                    System.out.println("DEBUG: apa3");
                }
                return false;
            }
            wall.clear();
            loadWall(filename);
            if (!insertPhotoAlbumIntoWall(pa.getId(), pa)){
                if (DEBUG){
                    System.out.println("DEBUG: apa4");
                }
                return false;
            }
            if (!saveWall(filename, pa.getShareWith())){
                if (DEBUG){
                    System.out.println("DEBUG: apa5");
                }
                return false;
            }
            loadPhotos();
            insertPhotoAlbum(pa);
            savePhotos();
            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "photoAlbum", pa.getId(), pa.getShareWith(), pa.getPostedBy());
            insertNotificaton(n);
            if (DEBUG){
                System.out.println("DEBUG: apa success");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean removePhotoAlbum(long id, String shareWith, String postedBy, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: rpa1");
            }
            return false;
        }
        try{
            id = getMapEntry(id);
            if (!profileOwner.equals(accountHolder) && !postedBy.equals(accountHolder) && !executeAsProfileOwner){
                if (DEBUG){
                    System.out.println("DEBUG: rpa2");
                }
                return false;
            }
            if (!profileOwner.equals(accountHolder) && !committed){
                if (DEBUG){
                    System.out.println("DEBUG: rpa3");
                }
                user u = new user(postedBy, null, null, 0);
                photoAlbum pa = new photoAlbum(id, 0, profileOwner, u, null, shareWith, null, null, null, null);
                updatePendingChanges("remove", "photoAlbum", new timelineEntry(pa.getId(), "photoAlbum", pa));
            }
            if (!loadPhotos()){
                if (DEBUG){
                    System.out.println("DEBUG: rpa4");
                }
                return false;
            }
            photoAlbum pa = new photoAlbum();
            for (int i = 0; i < photoAlbums.size(); i++){
                pa = new photoAlbum(photoAlbums.get(i));
                if (pa.getId() == id)
                    break;
            }
            List<photo> photos = new ArrayList<photo>();
            photos.addAll(pa.getPhotoFiles());
            if (!deletePhotoAlbum(id))
            {
                if (DEBUG){
                    System.out.println("DEBUG: rpa5");
                }
                return false;
            }
            savePhotos();
            String filename = getWallFileName(id, shareWith);
            if (filename != null){
                wall.clear();
                loadWall(filename);
                deletedEntry de = new deletedEntry(0, id, Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), shareWith, postedBy, "photoAlbum");
                removeFromWall(id, de);
                saveWall(filename, shareWith);
            }
            for (int i = 0; i < photos.size(); i++){
                photo p = new photo(photos.get(i));
                filename = getWallFileName(p.getId(), p.getShareWith());
                if (filename != null){
                    wall.clear();
                    loadWall(filename);
                    deletedEntry de = new deletedEntry(p.getParent(), p.getId(), Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), shareWith, p.getPostedBy().getUsername(), "photo");
                    removeFromWall(p.getId(), de);
                    saveWall(filename, p.getShareWith());
                }
            }
            if (DEBUG){
                System.out.println("DEBUG: rpa success");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean deletePhotoAlbum(long id){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            for ( int i = 0; i < photoAlbums.size(); i++){
                photoAlbum x = new photoAlbum(photoAlbums.get(i));
                if (id == x.getId()){
                    List<photo> photoFiles = x.getPhotoFiles();
                    for (int j = 0; j < photoFiles.size(); j++){
                        photo p = new photo(photoFiles.get(j));
                        deletePhoto(id, p.getId());
                    }
                    photoAlbums.remove(i);
                    return true;
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
    }
    
    private boolean insertAudio(audio a){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            for (int i = 0; i < audioAlbums.size(); i++){
                audioAlbum x = new audioAlbum(audioAlbums.get(i));
                if (a.getParent() == x.getId()){
                    x.addAudio(a);
                    audioAlbums.add(i, x);
                    audioAlbums.remove(i + 1);
                    return true;
                }
                if (a.getParentAlbum().equals(x.getTitle())){
                    if (a.getBelongsTo().equals(accountHolder)){
                        addToMap(a.getParent(), x.getId());
                        a.setParent(x.getId());
                    }else{
                        addToMap(x.getId(), a.getParent());
                        x.setId(a.getParent());
                    }
                    x.addAudio(a);
                    audioAlbums.add(i, x);
                    audioAlbums.remove(i + 1);
                    return true;
                }
            }
            loadUserInfo();
            user u = new user(userInfo.getUsername(), userInfo.getFirstName(), userInfo.getLastName(), 0);
            
            audioAlbum aa = null;
            if (a.getParentAlbum().equals("Wall")){
                aa = new audioAlbum(a.getId() - 1,
                                    a.getId() - 1,
                                    profileOwner,
                                    u,
                                    a.getParentAlbum(),
                                    "All",
                                    null,
                                    null,
                                    null,
                                    null);
            }else{
                aa = new audioAlbum(a.getId() - 1,
                                    a.getId() - 1,
                                    profileOwner,
                                    u,
                                    a.getParentAlbum(),
                                    a.getShareWith(),
                                    null,
                                    null,
                                    null,
                                    null);
            }
            addToMap(a.getParent(), aa.getId());
            String filename = updateHistory(aa.getId(), aa.getShareWith());
            if (filename != null){
                wall.clear();
                loadWall(filename);
                insertAudioAlbumIntoWall(aa.getId(), aa);
                saveWall(filename, aa.getShareWith());
            }
            a.setParent(a.getId() - 1);
            aa.addAudio(a);
            audioAlbums.add(aa);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    // the audio file belongs to an already existing audio album.
    public boolean addAudio(audio a, boolean committed){
        if (DEBUG){
            System.out.println("DEBUG: aa0");
        }
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: aa1");
            }
            return false;
        }
        try{
            if (!profileOwner.equals(accountHolder) && !committed){
                if (DEBUG){
                    System.out.println("DEBUG: aa2");
                }
                updatePendingChanges("add", "audio", new timelineEntry(a.getId(), "audio", a));
            }
            loadAudios();
            if (!insertAudio(a)){
                if (DEBUG){
                    System.out.println("DEBUG: aa3");
                }
            }
            saveAudios();
            String filename = updateHistory(a.getId(), a.getShareWith());
            if (filename == null){
                if (DEBUG){
                    System.out.println("DEBUG: aa4");
                }
                return false;
            }
            wall.clear();
            loadWall(filename);
            if (!insertAudioIntoWall(a.getId(), a)){
                if (DEBUG){
                    System.out.println("DEBUG: aa5");
                }
                return false;
            }
            if (!saveWall(filename, a.getShareWith())){
                if (DEBUG){
                    System.out.println("DEBUG: aa6");
                }
                return false;
            }
            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "audio", a.getId(), a.getShareWith(), a.getPostedBy());
            insertNotificaton(n);
            if (DEBUG){
                System.out.println("DEBUG: aa success");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    // the audio file belongs to an already existing album.
    private boolean deleteAudio(long parentID, long id){
        if (profileOwner == null || profileOwner.equals("")){
            return false;
        }
        try{
            int i = 0;
            audioAlbum x = new audioAlbum();
            for (i = 0; i < audioAlbums.size(); i++){
                x = new audioAlbum(audioAlbums.get(i));
                if (x.getId() == parentID){
                    audio a = new audio(x.getAudio(id));
                    if (a != null){
                        if (a.getBelongsTo().equals(accountHolder)){
                            updateModifiedFiles("remove", id, prefix + accountHolder + "/audios/" + a.getFilename(), a.getShareWith());
                            fileListEntry fle = new fileListEntry(a.getBelongsTo(), a.getShareWith(), prefix + accountHolder + "/audios/", a.getFilename(), 0);
                            updateImage("correctImage", "remove", fle);
                        }else{
                            fileListEntry fle = new fileListEntry(a.getBelongsTo(), a.getShareWith(), prefix + accountHolder + "/friends/" + a.getBelongsTo() + "/audios/", a.getFilename(), 0);
                            updateImage("correctImage", "remove", fle);
                        }
                    }
                    x.removeAudio(id);
                    break;
                }
            }
            if (i < audioAlbums.size()){
                audioAlbums.add(i, x);
                audioAlbums.remove(i + 1);
                return true;
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public boolean removeAudio(long parentID, long id, String shareWith, String postedBy, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: ra1");
            }
            return false;
        }
        try{
            parentID = getMapEntry(parentID);
            id = getMapEntry(id);
            if (!profileOwner.equals(accountHolder) && !postedBy.equals(accountHolder) && !executeAsProfileOwner){
                if (DEBUG){
                    System.out.println("DEBUG: ra2");
                }
                return false;
            }
            if (!profileOwner.equals(accountHolder) && !committed){
                if (DEBUG){
                    System.out.println("DEBUG: ra3");
                }
                user u = new user(postedBy, null, null, 0);
                audio a = new audio(null, parentID, id, 0, profileOwner, u, null, null, null, null, null, shareWith, null, null, null);
                updatePendingChanges("remove", "audio", new timelineEntry(a.getId(), "audio", a));
            }
            String filename = getWallFileName(id, shareWith);
            if (filename == null){
                if (DEBUG){
                    System.out.println("DEBUG: ra4");
                }
                return false;
            }
            wall.clear();
            if (!loadWall(filename)){
                if (DEBUG){
                    System.out.println("DEBUG: ra5");
                }
                return false;
            }
            deletedEntry de = new deletedEntry(parentID, id, Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), shareWith, postedBy, "audio");
            if (!removeFromWall(id, de)){
                if (DEBUG){
                    System.out.println("DEBUG: ra6");
                }
                return false;
            }
            if (!saveWall(filename, shareWith)){
                if (DEBUG){
                    System.out.println("DEBUG: ra7");
                }
                return false;
            }
            if (!loadAudios()){
                if (DEBUG){
                    System.out.println("DEBUG: ra8");
                }
                return false;
            }
            if (!deleteAudio(parentID, id))
            {
                if (DEBUG){
                    System.out.println("DEBUG: ra9");
                }
                return false;
            }
            saveAudios();
            if (DEBUG){
                System.out.println("DEBUG: ra success");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean insertVideo(video v){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            for (int i = 0; i < videoAlbums.size(); i++){
                videoAlbum x = new videoAlbum(videoAlbums.get(i));
                if (v.getParent() == x.getId()){
                    x.addVideo(v);
                    videoAlbums.add(i, x);
                    videoAlbums.remove(i + 1);
                    return true;
                }
                if (v.getParentAlbum().equals(x.getTitle())){
                    if (v.getBelongsTo().equals(accountHolder)){
                        addToMap(v.getParent(), x.getId());
                        v.setParent(x.getId());
                    }else{
                        addToMap(x.getId(), v.getParent());
                        x.setId(v.getParent());
                    }
                    x.addVideo(v);
                    videoAlbums.add(i, x);
                    videoAlbums.remove(i + 1);
                    return true;
                }
            }
            loadUserInfo();
            user u = new user(userInfo.getUsername(), userInfo.getFirstName(), userInfo.getLastName(), 0);
            
            videoAlbum va = null;
            if (v.getParentAlbum().equals("Wall")){
                va = new videoAlbum(v.getId() - 1,
                                    v.getId() - 1,
                                    profileOwner,
                                    u,
                                    v.getParentAlbum(),
                                    "All",
                                    null,
                                    null,
                                    null,
                                    null);
            }else{
                va = new videoAlbum(v.getId() - 1,
                                    v.getId() - 1,
                                    profileOwner,
                                    u,
                                    v.getParentAlbum(),
                                    v.getShareWith(),
                                    null,
                                    null,
                                    null,
                                    null);
            }
            addToMap(v.getParent(), va.getId());
            String filename = updateHistory(va.getId(), va.getShareWith());
            if (filename != null){
                wall.clear();
                loadWall(filename);
                insertVideoAlbumIntoWall(va.getId(), va);
                saveWall(filename, va.getShareWith());
            }
            v.setParent(v.getId() - 1);
            va.addVideo(v);
            videoAlbums.add(va);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    // the video file belongs to an already existing album.
    public boolean addVideo(video v, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: av1");
            }
            return false;
        }
        try{
            if (!profileOwner.equals(accountHolder) && !committed){
                if (DEBUG){
                    System.out.println("DEBUG: av2");
                }
                updatePendingChanges("add", "video", new timelineEntry(v.getId(), "video", v));
            }
            loadVideos();
            if (!insertVideo(v)){
                if (DEBUG){
                    System.out.println("DEBUG: av6");
                }
            }
            saveVideos();
            String filename = updateHistory(v.getId(), v.getShareWith());
            if (filename == null){
                if (DEBUG){
                    System.out.println("DEBUG: av3");
                }
                return false;
            }
            wall.clear();
            loadWall(filename);
            if (!insertVideoIntoWall(v.getId(), v)){
                if (DEBUG){
                    System.out.println("DEBUG: av4");
                }
                return false;
            }
            if (!saveWall(filename, v.getShareWith())){
                if (DEBUG){
                    System.out.println("DEBUG: av5");
                }
                return false;
            }
            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "video", v.getId(), v.getShareWith(), v.getPostedBy());
            insertNotificaton(n);
            if (DEBUG){
                System.out.println("DEBUG: av success");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean deleteVideo(long parentID, long id){
        if (profileOwner == null || profileOwner.equals("")){
            return false;
        }
        try{
            int i = 0;
            videoAlbum x = new videoAlbum();
            for (i = 0; i < videoAlbums.size(); i++){
                x = new videoAlbum(videoAlbums.get(i));
                if (x.getId() == parentID){
                    video v = new video(x.getVideo(id));
                    if (v != null){
                        if (v.getBelongsTo().equals(accountHolder)){
                            updateModifiedFiles("remove", id, prefix + accountHolder + "/videos/" + v.getFilename(), v.getShareWith());
                            fileListEntry fle = new fileListEntry(v.getBelongsTo(), v.getShareWith(), prefix + accountHolder + "/videos/", v.getFilename(), 0);
                            updateImage("correctImage", "remove", fle);
                        }else{
                            fileListEntry fle = new fileListEntry(v.getBelongsTo(), v.getShareWith(), prefix + accountHolder + "/friends/" + v.getBelongsTo() + "/videos/", v.getFilename(), 0);
                            updateImage("correctImage", "remove", fle);
                        }
                    }
                    x.removeVideo(id);
                    break;
                }
            }
            if (i < videoAlbums.size()){
                videoAlbums.add(i, x);
                videoAlbums.remove(i + 1);
                return true;
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
    }
    // the video file belongs to an already existing album
    public boolean removeVideo(long parentID, long id, String shareWith, String postedBy, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: rv1");
            }
            return false;
        }
        try{
            parentID = getMapEntry(parentID);
            id = getMapEntry(id);
            if (!profileOwner.equals(accountHolder) && !postedBy.equals(accountHolder) && !executeAsProfileOwner){
                if (DEBUG){
                    System.out.println("DEBUG: rv2");
                }
                return false;
            }
            if (!profileOwner.equals(accountHolder) && !committed){
                if (DEBUG){
                    System.out.println("DEBUG: rv3");
                }
                user u = new user(postedBy, null, null, 0);
                video v = new video(null, parentID, id, 0, profileOwner, u, null, null, null, shareWith, null, null, null);
                updatePendingChanges("remove", "video", new timelineEntry(v.getId(), "video", v));
            }
            String filename = getWallFileName(id, shareWith);
            if (filename == null){
                if (DEBUG){
                    System.out.println("DEBUG: rv4");
                }
                return false;
            }
            wall.clear();
            if (!loadWall(filename)){
                if (DEBUG){
                    System.out.println("DEBUG: rv5");
                }
                return false;
            }
            deletedEntry de = new deletedEntry(parentID, id, Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), shareWith, postedBy, "video");
            if (!removeFromWall(id, de)){
                if (DEBUG){
                    System.out.println("DEBUG: rv6");
                }
                return false;
            }
            if (!saveWall(filename, shareWith)){
                if (DEBUG){
                    System.out.println("DEBUG: rv7");
                }
                return false;
            }
            if (!loadVideos()){
                if (DEBUG){
                    System.out.println("DEBUG: rv8");
                }
                return false;
            }
            if (!deleteVideo(parentID, id))
            {
                if (DEBUG){
                    System.out.println("DEBUG: rv9");
                }
                return false;
            }
            saveVideos();
            if (DEBUG){
                System.out.println("DEBUG: rv success");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean insertPhoto(photo p){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            for (int i = 0; i < photoAlbums.size(); i++){
                photoAlbum x = new photoAlbum(photoAlbums.get(i));
                if (p.getParent() == x.getId()){
                    x.addPhoto(p);
                    photoAlbums.add(i, x);
                    photoAlbums.remove(i + 1);
                    return true;
                }
                if (p.getParentAlbum().equals(x.getTitle())){
                    if (p.getBelongsTo().equals(accountHolder)){
                        addToMap(p.getParent(), x.getId());
                        p.setParent(x.getId());
                    }else{
                        addToMap(x.getId(), p.getParent());
                        x.setId(p.getParent());
                    }
                    x.addPhoto(p);
                    photoAlbums.add(i, x);
                    photoAlbums.remove(i + 1);
                    return true;
                }
            }
            loadUserInfo();
            user u = new user(userInfo.getUsername(), userInfo.getFirstName(), userInfo.getLastName(), 0);
            
            photoAlbum pa = null;
            if (p.getParentAlbum().equals("Wall")){
                pa = new photoAlbum(p.getId() - 1,
                                    p.getId() - 1,
                                    profileOwner,
                                    u,
                                    p.getParentAlbum(),
                                    "All",
                                    null,
                                    null,
                                    null,
                                    null);
            }else{
                pa = new photoAlbum(p.getId() - 1,
                                    p.getId() - 1,
                                    profileOwner,
                                    u,
                                    p.getParentAlbum(),
                                    p.getShareWith(),
                                    null,
                                    null,
                                    null,
                                    null);
            }
            addToMap(p.getParent(), pa.getId());
            String filename = updateHistory(pa.getId(), pa.getShareWith());
            if (filename != null){
                wall.clear();
                loadWall(filename);
                insertPhotoAlbumIntoWall(pa.getId(), pa);
                saveWall(filename, pa.getShareWith());
            }
            p.setParent(p.getId() - 1);
            pa.addPhoto(p);
            photoAlbums.add(pa);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean addPhoto(photo p, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: ap1");
            }
            return false;
        }
        try{
            if (!profileOwner.equals(accountHolder) && !committed){
                if (DEBUG){
                    System.out.println("DEBUG: ap2");
                }
                updatePendingChanges("add", "photo", new timelineEntry(p.getId(), "photo", p));
            }
            loadPhotos();
            if (!insertPhoto(p)){
                if (DEBUG){
                    System.out.println("DEBUG: ap3");
                }
            }
            savePhotos();
            String filename = updateHistory(p.getId(), p.getShareWith());
            if (filename == null){
                if (DEBUG){
                    System.out.println("DEBUG: ap4");
                }
                return false;
            }
            wall.clear();
            loadWall(filename);
            if (!insertPhotoIntoWall(p.getId(), p)){
                if (DEBUG){
                    System.out.println("DEBUG: ap5");
                }
                return false;
            }
            if (!saveWall(filename, p.getShareWith())){
                if (DEBUG){
                    System.out.println("DEBUG: ap6");
                }
                return false;
            }
            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "photo", p.getId(), p.getShareWith(), p.getPostedBy());
            insertNotificaton(n);
            if (DEBUG){
                System.out.println("DEBUG: ap success");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private boolean deletePhoto(long parentID, long id){
        if (profileOwner == null || profileOwner.equals("")){
            return false;
        }
        try{
            int i = 0;
            photoAlbum x = new photoAlbum();
            for (i = 0; i < photoAlbums.size(); i++){
                x = new photoAlbum(photoAlbums.get(i));
                if (x.getId() == parentID){
                    photo p = new photo(x.getPhoto(id));
                    if (p != null){
                        if (p.getBelongsTo().equals(accountHolder)){
                            updateModifiedFiles("remove", id, prefix + accountHolder + "/photos/" + p.getFilename(), p.getShareWith());
                            fileListEntry fle = new fileListEntry(p.getBelongsTo(), p.getShareWith(), prefix + accountHolder + "/photos/", p.getFilename(), 0);
                            updateImage("correctImage", "remove", fle);
                        }else{
                            fileListEntry fle = new fileListEntry(p.getBelongsTo(), p.getShareWith(), prefix + accountHolder + "/friends/" + p.getBelongsTo() + "/photos/", p.getFilename(), 0);
                            updateImage("correctImage", "remove", fle);
                        }
                    }
                    x.removePhoto(id);
                    break;
                }
            }
            if (i < photoAlbums.size()){
                photoAlbums.add(i, x);
                photoAlbums.remove(i + 1);
                return true;
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public boolean removePhoto(long parentID, long id, String shareWith, String postedBy, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: rp1");
            }
            return false;
        }
        try{
            parentID = getMapEntry(parentID);
            id = getMapEntry(id);
            if (!profileOwner.equals(accountHolder) && !postedBy.equals(accountHolder) && !executeAsProfileOwner){
                if (DEBUG){
                    System.out.println("DEBUG: rp2");
                }
                return false;
            }
            if (!profileOwner.equals(accountHolder) && !committed){
                if (DEBUG){
                    System.out.println("DEBUG: rp3");
                }
                user u = new user(postedBy, null, null, 0);
                photo p = new photo(null, parentID, id, 0, profileOwner, u, null, null, shareWith, null, null, null);
                updatePendingChanges("remove", "photo", new timelineEntry(p.getId(), "photo", p));
            }
            String filename = getWallFileName(id, shareWith);
            if (filename == null){
                if (DEBUG){
                    System.out.println("DEBUG: rp4");
                }
                return false;
            }
            wall.clear();
            if (!loadWall(filename)){
                if (DEBUG){
                    System.out.println("DEBUG: rp5");
                }
                return false;
            }
            deletedEntry de = new deletedEntry(parentID, id, Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), shareWith, postedBy, "photo");
            if (!removeFromWall(id, de)){
                if (DEBUG){
                    System.out.println("DEBUG: rp6");
                }
                return false;
            }
            if (!saveWall(filename, shareWith)){
                if (DEBUG){
                    System.out.println("DEBUG: rp7");
                }
                return false;
            }
            if (!loadPhotos()){
                if (DEBUG){
                    System.out.println("DEBUG: rp8");
                }
                return false;
            }
            if (!deletePhoto(parentID, id))
            {
                if (DEBUG){
                    System.out.println("DEBUG: rp9");
                }
                return false;
            }
            savePhotos();
            if (DEBUG){
                System.out.println("DEBUG: rp success");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean addComment(comment c, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: ac1");
            }
            return false;
        }
        try{
            c.setGrandParent(getMapEntry(c.getGrandParent()));
            c.setParent(getMapEntry(c.getParent()));
            long time = 0;
            if (c.getType().equals("link")){
                if (DEBUG){
                    System.out.println("DEBUG: ac2");
                }
                if (!profileOwner.equals(accountHolder) && !committed){
                    if (DEBUG){
                        System.out.println("DEBUG: ac3");
                    }
                    link l = new link();
                    l.addComment(c);
                    updatePendingChanges("add", "linkComment", new timelineEntry(0, "link", l));
                }
                String filename = getWallFileName(c.getParent(), c.getShareWith());
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: ac4");
                    }
                    return false;
                }
                wall.clear();
                if (!loadWall(filename)){
                    if (DEBUG){
                        System.out.println("DEBUG: ac5");
                    }
                    return false;
                }
                if (wall.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: ac6");
                    }
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = new timelineEntry(wall.get(i));
                    if (tle.id == c.getParent()){
                        if (DEBUG){
                            System.out.println("DEBUG: ac7");
                        }
                        link l = new link(tle.l);
                        l.addComment(c);
                        if (!removeFromWall(l.getId(), null)){
                            if (DEBUG){
                                System.out.println("DEBUG: ac8");
                            }
                            return false;
                        }
                        if (!insertLinkIntoWall(l.getId(), l)){
                            if (DEBUG){
                                System.out.println("DEBUG: ac9");
                            }
                            return false;
                        }
                        if (!saveWall(filename, c.getShareWith())){
                            if (DEBUG){
                                System.out.println("DEBUG: ac10");
                            }
                            return false;
                        }
                        notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "linkComment", l.getId(), l.getShareWith(), c.getUser());
                        insertNotificaton(n);
                        if (DEBUG){
                            System.out.println("DEBUG: ac success");
                        }
                        return true;
                    }
                }
                return false;
            }
            
            if (c.getType().equals("wallPost")){
                if (DEBUG){
                    System.out.println("DEBUG: ac11");
                }
                if (!profileOwner.equals(accountHolder) && !committed){
                    if (DEBUG){
                        System.out.println("DEBUG: ac12");
                    }
                    wallPost s = new wallPost();
                    s.addComment(c);
                    updatePendingChanges("add", "wallPostComment", new timelineEntry(0, "wallPost", s));
                    if (DEBUG){
                        System.out.println("DEBUG: ac12.5");
                    }
                }
                String filename = getWallFileName(c.getParent(), c.getShareWith());
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: ac13");
                    }
                    return false;
                }
                wall.clear();
                if (!loadWall(filename)){
                    if (DEBUG){
                        System.out.println("DEBUG: ac14");
                    }
                    return false;
                }
                if (wall.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: ac15");
                    }
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = new timelineEntry(wall.get(i));
                    if (tle.id == c.getParent()){
                        wallPost s = new wallPost(tle.s);
                        s.addComment(c);
                        if (!removeFromWall(s.getId(), null)){
                            if (DEBUG){
                                System.out.println("DEBUG: ac16");
                            }
                            return false;
                        }
                        if (!insertwallPostIntoWall(s.getId(), s)){
                            if (DEBUG){
                                System.out.println("DEBUG: ac17");
                            }
                            return false;
                        }
                        if (!saveWall(filename, c.getShareWith())){
                            if (DEBUG){
                                System.out.println("DEBUG: ac18");
                            }
                            return false;
                        }
                        notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "wallPostComment", s.getId(), s.getShareWith(), c.getUser());
                        insertNotificaton(n);
                        if (DEBUG){
                            System.out.println("DEBUG: ac success");
                        }
                        return true;
                    }
                }
                if (DEBUG){
                    System.out.println("DEBUG: ac19");
                }
                return false;
            }
            
            if (c.getType().equals("audioAlbum")){
                if (DEBUG){
                    System.out.println("DEBUG: ac20");
                }
                if (!profileOwner.equals(accountHolder) && !committed){
                    if (DEBUG){
                        System.out.println("DEBUG: ac21");
                    }
                    audioAlbum aa = new audioAlbum();
                    aa.addComment(c);
                    updatePendingChanges("add", "audioAlbumComment", new timelineEntry(0, "audioAlbum", aa));
                }
                if (!loadAudios()){
                    if (DEBUG){
                        System.out.println("DEBUG: ac22");
                    }
                    return false;
                }
                if (audioAlbums.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: ac23");
                    }
                    return false;
                }
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = new audioAlbum(audioAlbums.get(i));
                    if (aa.getId() == c.getParent()){
                        if (DEBUG){
                            System.out.println("DEBUG: ac24");
                        }
                        aa.addComment(c);
                        audioAlbums.add(i, aa);
                        audioAlbums.remove(i + 1);
                        saveAudios();
                        break;
                    }
                }
                String filename = getWallFileName(c.getParent(), c.getShareWith());
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: ac25");
                    }
                    return false;
                }
                wall.clear();
                if (!loadWall(filename)){
                    if (DEBUG){
                        System.out.println("DEBUG: ac26");
                    }
                    return false;
                }
                if (wall.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: ac27");
                    }
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == c.getParent()){
                        if (DEBUG){
                            System.out.println("DEBUG: ac28");
                        }
                        audioAlbum aa = tle.aa;
                        aa.addComment(c);
                        if (!removeFromWall(aa.getId(), null)){
                            if (DEBUG){
                                System.out.println("DEBUG: ac29");
                            }
                            return false;
                        }
                        if (!insertAudioAlbumIntoWall(aa.getId(), aa)){
                            if (DEBUG){
                                System.out.println("DEBUG: ac30");
                            }
                            return false;
                        }
                        if (!saveWall(filename, c.getShareWith())){
                            if (DEBUG){
                                System.out.println("DEBUG: ac31");
                            }
                            return false;
                        }
                        notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "audioAlbumComment", aa.getId(), aa.getShareWith(), c.getUser());
                        insertNotificaton(n);
                        if (DEBUG){
                            System.out.println("DEBUG: ac success");
                        }
                        return true;
                    }
                }
                if (DEBUG){
                    System.out.println("DEBUG: ac32");
                }
                return false;
            }
            
            if (c.getType().equals("videoAlbum")){
                if (DEBUG){
                    System.out.println("DEBUG: ac33");
                }
                if (!profileOwner.equals(accountHolder) && !committed){
                    if (DEBUG){
                        System.out.println("DEBUG: ac34");
                    }
                    videoAlbum va = new videoAlbum();
                    va.addComment(c);
                    updatePendingChanges("add", "videoAlbumComment", new timelineEntry(0, "videoAlbum", va));
                }
                if (!loadVideos()){
                    if (DEBUG){
                        System.out.println("DEBUG: ac35");
                    }
                    return false;
                }
                if (videoAlbums.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: ac36");
                    }
                    return false;
                }
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = new videoAlbum(videoAlbums.get(i));
                    if (va.getId() == c.getParent()){
                        if (DEBUG){
                            System.out.println("DEBUG: ac37");
                        }
                        va.addComment(c);
                        videoAlbums.add(i, va);
                        videoAlbums.remove(i + 1);
                        saveVideos();
                        break;
                    }
                }
                String filename = getWallFileName(c.getParent(), c.getShareWith());
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: ac38");
                    }
                    return false;
                }
                wall.clear();
                if (!loadWall(filename)){
                    if (DEBUG){
                        System.out.println("DEBUG: ac39");
                    }
                    return false;
                }
                if (wall.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: ac40");
                    }
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == c.getParent()){
                        if (DEBUG){
                            System.out.println("DEBUG: ac41");
                        }
                        videoAlbum va = tle.va;
                        va.addComment(c);
                        if (!removeFromWall(va.getId(), null)){
                            if (DEBUG){
                                System.out.println("DEBUG: ac42");
                            }
                            return false;
                        }
                        if (!insertVideoAlbumIntoWall(va.getId(), va)){
                            if (DEBUG){
                                System.out.println("DEBUG: ac43");
                            }
                            return false;
                        }
                        if (!saveWall(filename, c.getShareWith())){
                            if (DEBUG){
                                System.out.println("DEBUG: ac44");
                            }
                            return false;
                        }
                        notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "videoAlbumComment", va.getId(), va.getShareWith(), c.getUser());
                        insertNotificaton(n);
                        if (DEBUG){
                            System.out.println("DEBUG: ac success");
                        }
                        return true;
                    }
                }
                if (DEBUG){
                    System.out.println("DEBUG: ac45");
                }
                return false;
            }
            
            if (c.getType().equals("photoAlbum")){
                if (DEBUG){
                    System.out.println("DEBUG: ac46");
                }
                if (!profileOwner.equals(accountHolder) && !committed){
                    if (DEBUG){
                        System.out.println("DEBUG: ac47");
                    }
                    photoAlbum pa = new photoAlbum();
                    pa.addComment(c);
                    updatePendingChanges("add", "photoAlbumComment", new timelineEntry(0, "photoAlbum", pa));
                }
                if (!loadPhotos()){
                    if (DEBUG){
                        System.out.println("DEBUG: ac48");
                    }
                    return false;
                }
                if (photoAlbums.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: ac49");
                    }
                    return false;
                }
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = new photoAlbum(photoAlbums.get(i));
                    if (pa.getId() == c.getParent()){
                        if (DEBUG){
                            System.out.println("DEBUG: ac50");
                        }
                        pa.addComment(c);
                        photoAlbums.add(i, pa);
                        photoAlbums.remove(i + 1);
                        savePhotos();
                        break;
                    }
                }
                String filename = getWallFileName(c.getParent(), c.getShareWith());
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: ac51");
                    }
                    return false;
                }
                wall.clear();
                if (!loadWall(filename)){
                    if (DEBUG){
                        System.out.println("DEBUG: ac52");
                    }
                    return false;
                }
                if (wall.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: ac53");
                    }
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == c.getParent()){
                        if (DEBUG){
                            System.out.println("DEBUG: ac54");
                        }
                        photoAlbum pa = tle.pa;
                        pa.addComment(c);
                        if (!removeFromWall(pa.getId(), null)){
                            if (DEBUG){
                                System.out.println("DEBUG: ac55");
                            }
                            return false;
                        }
                        if (!insertPhotoAlbumIntoWall(pa.getId(), pa)){
                            if (DEBUG){
                                System.out.println("DEBUG: ac56");
                            }
                            return false;
                        }
                        if (!saveWall(filename, c.getShareWith())){
                            if (DEBUG){
                                System.out.println("DEBUG: ac57");
                            }
                            return false;
                        }
                        notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "photoAlbumComment", pa.getId(), pa.getShareWith(), c.getUser());
                        insertNotificaton(n);
                        if (DEBUG){
                            System.out.println("DEBUG: ac success");
                        }
                        return true;
                    }
                }
                if (DEBUG){
                    System.out.println("DEBUG: ac58");
                }
                return false;
            }
            
            if (c.getType().equals("audio")){
                if (DEBUG){
                    System.out.println("DEBUG: ac59");
                }
                if (!profileOwner.equals(accountHolder) && !committed){
                    if (DEBUG){
                        System.out.println("DEBUG: ac60");
                    }
                    audio a = new audio();
                    a.addComment(c);
                    updatePendingChanges("add", "audioComment", new timelineEntry(0, "audio", a));
                }
                if (!loadAudios()){
                    if (DEBUG){
                        System.out.println("DEBUG: ac61");
                    }
                    return false;
                }
                if (audioAlbums.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: ac62");
                    }
                    return false;
                }
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = new audioAlbum(audioAlbums.get(i));
                    if (c.getGrandParent() == aa.getId()){
                        audio a = new audio(aa.getAudio(c.getParent()));
                        if (a == null){
                            if (DEBUG){
                                System.out.println("DEBUG: ac63");
                            }
                            return false;
                        }
                        a.addComment(c);
                        if (!aa.removeAudio(a.getId())){
                            if (DEBUG){
                                System.out.println("DEBUG: ac64");
                            }
                            return false;
                        }
                        aa.addAudio(a);
                        audioAlbums.add(i, aa);
                        audioAlbums.remove(i + 1);
                        saveAudios();
                        break;
                    }
                }
                String filename = getWallFileName(c.getParent(), c.getShareWith());
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: ac65");
                    }
                    return false;
                }
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = new timelineEntry(wall.get(i));
                        if (tle.id == c.getParent()){
                            if (DEBUG){
                                System.out.println("DEBUG: ac66");
                            }
                            audio a = new audio(tle.a);
                            if (DEBUG){
                                System.out.println("DEBUG: ac66.1");
                            }
                            a.addComment(c);
                            if (DEBUG){
                                System.out.println("DEBUG: ac66.2");
                            }
                            if (!removeFromWall(a.getId(), null)){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac67");
                                }
                                return false;
                            }
                            if (DEBUG){
                                System.out.println("DEBUG: ac66.3");
                            }
                            if (!insertAudioIntoWall(a.getId(), a)){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac68");
                                }
                                return false;
                            }
                            if (DEBUG){
                                System.out.println("DEBUG: ac66.4");
                            }
                            if (!saveWall(filename, c.getShareWith())){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac69");
                                }
                                return false;
                            }
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "audioComment", a.getId(), a.getShareWith(), c.getUser());
                            insertNotificaton(n);
                            return true;
                        }
                    }
                }
                filename = getWallFileName(c.getGrandParent(), c.getShareWith());
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: ac70");
                    }
                    return false;
                }
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == c.getGrandParent()){
                            if (DEBUG){
                                System.out.println("DEBUG: ac71");
                            }
                            audioAlbum aa = tle.aa;
                            audio a = aa.getAudio(c.getParent());
                            if (a == null){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac72");
                                }
                                return false;
                            }
                            a.addComment(c);
                            if (!aa.removeAudio(a.getId())){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac73");
                                }
                                return false;
                            }
                            aa.addAudio(a);
                            if (!removeFromWall(aa.getId(), null)){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac74");
                                }
                                return false;
                            }
                            if (!insertAudioAlbumIntoWall(aa.getId(), aa)){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac75");
                                }
                                return false;
                            }
                            if (!saveWall(filename, c.getShareWith())){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac76");
                                }
                                return false;
                            }
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "audioComment", a.getId(), a.getShareWith(), c.getUser());
                            insertNotificaton(n);
                            if (DEBUG){
                                System.out.println("DEBUG: ac success");
                            }
                            return true;
                        }
                    }
                }
                return false;
            }
            if (c.getType().equals("video")){
                if (DEBUG){
                    System.out.println("DEBUG: ac77");
                }
                if (!profileOwner.equals(accountHolder) && !committed){
                    if (DEBUG){
                        System.out.println("DEBUG: ac78");
                    }
                    video v = new video();
                    v.addComment(c);
                    updatePendingChanges("add", "videoComment", new timelineEntry(0, "video", v));
                }
                if (!loadVideos()){
                    if (DEBUG){
                        System.out.println("DEBUG: ac79");
                    }
                    return false;
                }
                if (videoAlbums.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: ac80");
                    }
                    return false;
                }
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = new videoAlbum(videoAlbums.get(i));
                    if (c.getGrandParent() == va.getId()){
                        if (DEBUG){
                            System.out.println("DEBUG: ac81");
                        }
                        video v = new video(va.getVideo(c.getParent()));
                        if (v == null){
                            if (DEBUG){
                                System.out.println("DEBUG: ac82");
                            }
                            return false;
                        }
                        v.addComment(c);
                        if (!va.removeVideo(v.getId())){
                            if (DEBUG){
                                System.out.println("DEBUG: ac83");
                            }
                            return false;
                        }
                        va.addVideo(v);
                        videoAlbums.add(i, va);
                        videoAlbums.remove(i + 1);
                        saveVideos();
                        break;
                    }
                }
                String filename = getWallFileName(c.getParent(), c.getShareWith());
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: ac84");
                    }
                    return false;
                }
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = new timelineEntry(wall.get(i));
                        if (tle.id == c.getParent()){
                            if (DEBUG){
                                System.out.println("DEBUG: ac85");
                            }
                            video v = new video(tle.v);
                            v.addComment(c);
                            if (!removeFromWall(v.getId(), null)){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac86");
                                }
                                return false;
                            }
                            if (!insertVideoIntoWall(v.getId(), v)){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac87");
                                }
                                return false;
                            }
                            if (!saveWall(filename, c.getShareWith())){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac88");
                                }
                                return false;
                            }
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "videoComment", v.getId(), v.getShareWith(), c.getUser());
                            insertNotificaton(n);
                            return true;
                        }
                    }
                }
                filename = getWallFileName(c.getGrandParent(), c.getShareWith());
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: ac89");
                    }
                    return false;
                }
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == c.getGrandParent()){
                            if (DEBUG){
                                System.out.println("DEBUG: ac90");
                            }
                            videoAlbum va = tle.va;
                            video v = va.getVideo(c.getParent());
                            if (v == null){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac91");
                                }
                                return false;
                            }
                            v.addComment(c);
                            if (!va.removeVideo(v.getId())){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac92");
                                }
                                return false;
                            }
                            va.addVideo(v);
                            if (!removeFromWall(va.getId(), null)){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac93");
                                }
                                return false;
                            }
                            if (!insertVideoAlbumIntoWall(va.getId(), va)){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac94");
                                }
                                return false;
                            }
                            if (!saveWall(filename, c.getShareWith())){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac95");
                                }
                                return false;
                            }
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "videoComment", v.getId(), v.getShareWith(), c.getUser());
                            insertNotificaton(n);
                            if (DEBUG){
                                System.out.println("DEBUG: ac success");
                            }
                            return true;
                        }
                    }
                }
                return false;
            }
            
            if (c.getType().equals("photo")){
                if (DEBUG){
                    System.out.println("DEBUG: ac97");
                }
                if (!profileOwner.equals(accountHolder) && !committed){
                    if (DEBUG){
                        System.out.println("DEBUG: ac98");
                    }
                    photo p = new photo();
                    p.addComment(c);
                    updatePendingChanges("add", "photoComment", new timelineEntry(0, "photo", p));
                }
                if (!loadPhotos()){
                    if (DEBUG){
                        System.out.println("DEBUG: ac99");
                    }
                    return false;
                }
                if (photoAlbums.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: ac100");
                    }
                    return false;
                }
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = new photoAlbum(photoAlbums.get(i));
                    if (c.getGrandParent() == pa.getId()){
                        if (DEBUG){
                            System.out.println("DEBUG: ac101");
                        }
                        photo p = new photo(pa.getPhoto(c.getParent()));
                        if (p == null){
                            if (DEBUG){
                                System.out.println("DEBUG: ac102");
                            }
                            return false;
                        }
                        p.addComment(c);
                        if (!pa.removePhoto(p.getId())){
                            if (DEBUG){
                                System.out.println("DEBUG: ac103");
                            }
                            return false;
                        }
                        pa.addPhoto(p);
                        photoAlbums.add(i, pa);
                        photoAlbums.remove(i + 1);
                        savePhotos();
                        break;
                    }
                }
                String filename = getWallFileName(c.getParent(), c.getShareWith());
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: ac104");
                    }
                    return false;
                }
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = new timelineEntry(wall.get(i));
                        if (tle.id == c.getParent()){
                            if (DEBUG){
                                System.out.println("DEBUG: ac105");
                            }
                            photo p = new photo(tle.p);
                            p.addComment(c);
                            if (!removeFromWall(p.getId(), null)){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac106");
                                }
                                return false;
                            }
                            if (!insertPhotoIntoWall(p.getId(), p)){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac107");
                                }
                                return false;
                            }
                            if (!saveWall(filename, c.getShareWith())){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac108");
                                }
                                return false;
                            }
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "photoComment", p.getId(), p.getShareWith(), c.getUser());
                            insertNotificaton(n);
                            return true;
                        }
                    }
                }
                filename = getWallFileName(c.getGrandParent(), c.getShareWith());
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: ac109");
                    }
                    return false;
                }
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == c.getGrandParent()){
                            if (DEBUG){
                                System.out.println("DEBUG: ac110");
                            }
                            photoAlbum pa = tle.pa;
                            photo p = pa.getPhoto(c.getParent());
                            if (p == null){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac111");
                                }
                                return false;
                            }
                            p.addComment(c);
                            if (!pa.removePhoto(p.getId())){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac112");
                                }
                                return false;
                            }
                            pa.addPhoto(p);
                            if (!removeFromWall(pa.getId(), null)){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac113");
                                }
                                return false;
                            }
                            if (!insertPhotoAlbumIntoWall(pa.getId(), pa)){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac114");
                                }
                                return false;
                            }
                            if (!saveWall(filename, c.getShareWith())){
                                if (DEBUG){
                                    System.out.println("DEBUG: ac115");
                                }
                                return false;
                            }
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "photoComment", p.getId(), p.getShareWith(), c.getUser());
                            insertNotificaton(n);
                            if (DEBUG){
                                System.out.println("DEBUG: ac success");
                            }
                            return true;
                        }
                    }
                }
                return false;
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean addlike(like li, boolean committed){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            li.setGrandParent(getMapEntry(li.getGrandParent()));
            li.setParent(getMapEntry(li.getParent()));
            if (li.getType().equals("link")){
                if (!profileOwner.equals(accountHolder) && !committed){
                    link l = new link();
                    l.addlike(li);
                    updatePendingChanges("add", "linkLike", new timelineEntry(0, "link", l));
                }
                String filename = getWallFileName(li.getParent(), li.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = new timelineEntry(wall.get(i));
                    if (tle.id == li.getParent()){
                        link l = new link(tle.l);
                        l.addlike(li);
                        if (!removeFromWall(l.getId(), null))
                            return false;
                        if (!insertLinkIntoWall(l.getId(), l))
                            return false;
                        if (!saveWall(filename, li.getShareWith()))
                            return false;
                        notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "linkLike", l.getId(), l.getShareWith(), li.getUser());
                        insertNotificaton(n);
                        return true;
                    }
                }
                return false;
            }
            
            if (li.getType().equals("wallPost")){
                if (!profileOwner.equals(accountHolder) && !committed){
                    wallPost s = new wallPost();
                    s.addlike(li);
                    updatePendingChanges("add", "wallPostLike", new timelineEntry(0, "wallPost", s));
                }
                String filename = getWallFileName(li.getParent(), li.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = new timelineEntry(wall.get(i));
                    if (tle.id == li.getParent()){
                        wallPost s = new wallPost(tle.s);
                        s.addlike(li);
                        if (!removeFromWall(s.getId(), null))
                            return false;
                        if (!insertwallPostIntoWall(s.getId(), s))
                            return false;
                        if (!saveWall(filename, li.getShareWith()))
                            return false;
                        notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "wallPostLike", s.getId(), s.getShareWith(), li.getUser());
                        insertNotificaton(n);
                        return true;
                    }
                }
                return false;
            }
            
            if (li.getType().equals("audioAlbum")){
                if (!profileOwner.equals(accountHolder) && !committed){
                    audioAlbum aa = new audioAlbum();
                    aa.addlike(li);
                    updatePendingChanges("add", "audioAlbumLike", new timelineEntry(0, "audioAlbum", aa));
                }
                if (!loadAudios()){
                    return false;
                }
                if (audioAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = new audioAlbum(audioAlbums.get(i));
                    if (aa.getId() == li.getParent()){
                        aa.addlike(li);
                        audioAlbums.add(i, aa);
                        audioAlbums.remove(i + 1);
                        saveAudios();
                        break;
                    }
                }
                String filename = getWallFileName(li.getParent(), li.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == li.getParent()){
                        audioAlbum aa = tle.aa;
                        aa.addlike(li);
                        if (!removeFromWall(aa.getId(), null))
                            return false;
                        if (!insertAudioAlbumIntoWall(aa.getId(), aa))
                            return false;
                        if (!saveWall(filename, li.getShareWith()))
                            return false;
                        notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "audioAlbumLike", aa.getId(), aa.getShareWith(), li.getUser());
                        insertNotificaton(n);
                        return true;
                    }
                }
                return false;
            }
            
            if (li.getType().equals("videoAlbum")){
                if (!profileOwner.equals(accountHolder) && !committed){
                    videoAlbum va = new videoAlbum();
                    va.addlike(li);
                    updatePendingChanges("add", "videoAlbumLike", new timelineEntry(0, "videoAlbum", va));
                }
                if (!loadVideos()){
                    return false;
                }
                if (videoAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = new videoAlbum(videoAlbums.get(i));
                    if (va.getId() == li.getParent()){
                        va.addlike(li);
                        videoAlbums.add(i, va);
                        videoAlbums.remove(i + 1);
                        saveVideos();
                        break;
                    }
                }
                String filename = getWallFileName(li.getParent(), li.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == li.getParent()){
                        videoAlbum va = tle.va;
                        va.addlike(li);
                        if (!removeFromWall(va.getId(), null))
                            return false;
                        if (!insertVideoAlbumIntoWall(va.getId(), va))
                            return false;
                        if (!saveWall(filename, li.getShareWith()))
                            return false;
                        notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "videoAlbumLike", va.getId(), va.getShareWith(), li.getUser());
                        insertNotificaton(n);
                        return true;
                    }
                }
                return false;
            }
            
            if (li.getType().equals("photoAlbum")){
                if (!profileOwner.equals(accountHolder) && !committed){
                    photoAlbum pa = new photoAlbum();
                    pa.addlike(li);
                    updatePendingChanges("add", "photoAlbumLike", new timelineEntry(0, "photoAlbum", pa));
                }
                if (!loadPhotos()){
                    return false;
                }
                if (photoAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = new photoAlbum(photoAlbums.get(i));
                    if (pa.getId() == li.getParent()){
                        pa.addlike(li);
                        photoAlbums.add(i, pa);
                        photoAlbums.remove(i + 1);
                        savePhotos();
                        break;
                    }
                }
                String filename = getWallFileName(li.getParent(), li.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == li.getParent()){
                        photoAlbum pa = tle.pa;
                        pa.addlike(li);
                        if (!removeFromWall(pa.getId(), null))
                            return false;
                        if (!insertPhotoAlbumIntoWall(pa.getId(), pa))
                            return false;
                        if (!saveWall(filename, li.getShareWith()))
                            return false;
                        notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "photoAlbumLike", pa.getId(), pa.getShareWith(), li.getUser());
                        insertNotificaton(n);
                        return true;
                    }
                }
                return false;
            }
            
            if (li.getType().equals("audio")){
                if (!profileOwner.equals(accountHolder) && !committed){
                    audio a = new audio();
                    a.addlike(li);
                    updatePendingChanges("add", "audioLike", new timelineEntry(0, "audio", a));
                }
                if (!loadAudios()){
                    return false;
                }
                if (audioAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = (audioAlbum)audioAlbums.get(i);
                    if (li.getGrandParent() == aa.getId()){
                        audio a = aa.getAudio(li.getParent());
                        if (a == null)
                            return false;
                        a.addlike(li);
                        if (!aa.removeAudio(a.getId()))
                            return false;
                        aa.addAudio(a);
                        audioAlbums.remove(i);
                        audioAlbums.add(i, aa);
                        saveAudios();
                        break;
                    }
                }
                String filename = getWallFileName(li.getParent(), li.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == li.getParent()){
                            audio a = tle.a;
                            a.addlike(li);
                            if (!removeFromWall(a.getId(), null))
                                return false;
                            if (!insertAudioIntoWall(a.getId(), a))
                                return false;
                            if (!saveWall(filename, li.getShareWith()))
                                return false;
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "audioLike", a.getId(), a.getShareWith(), li.getUser());
                            insertNotificaton(n);
                            return true;
                        }
                    }
                }
                filename = getWallFileName(li.getGrandParent(), li.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == li.getGrandParent()){
                            audioAlbum aa = tle.aa;
                            audio a = aa.getAudio(li.getParent());
                            if (a == null)
                                return false;
                            a.addlike(li);
                            if (!aa.removeAudio(a.getId()))
                                return false;
                            aa.addAudio(a);
                            if (!removeFromWall(aa.getId(), null))
                                return false;
                            if (!insertAudioAlbumIntoWall(aa.getId(), aa))
                                return false;
                            if (!saveWall(filename, li.getShareWith()))
                                return false;
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "audioLike", aa.getId(), aa.getShareWith(), li.getUser());
                            insertNotificaton(n);
                            return true;
                        }
                    }
                }
                return false;
            }
            
            if (li.getType().equals("video")){
                if (!profileOwner.equals(accountHolder) && !committed){
                    video v = new video();
                    v.addlike(li);
                    updatePendingChanges("add", "videoLike", new timelineEntry(0, "video", v));
                }
                if (!loadVideos()){
                    return false;
                }
                if (videoAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = (videoAlbum)videoAlbums.get(i);
                    if (li.getGrandParent() == va.getId()){
                        video v = va.getVideo(li.getParent());
                        if (v == null)
                            return false;
                        v.addlike(li);
                        if (!va.removeVideo(v.getId()))
                            return false;
                        va.addVideo(v);
                        videoAlbums.remove(i);
                        videoAlbums.add(i, va);
                        saveVideos();
                        break;
                    }
                }
                String filename = getWallFileName(li.getParent(), li.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == li.getParent()){
                            video v = tle.v;
                            v.addlike(li);
                            if (!removeFromWall(v.getId(), null))
                                return false;
                            if (!insertVideoIntoWall(v.getId(), v))
                                return false;
                            if (!saveWall(filename, li.getShareWith()))
                                return false;
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "videoLike", v.getId(), v.getShareWith(), li.getUser());
                            insertNotificaton(n);
                            return true;
                        }
                    }
                }
                filename = getWallFileName(li.getGrandParent(), li.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == li.getGrandParent()){
                            videoAlbum va = tle.va;
                            video v = va.getVideo(li.getParent());
                            if (v == null)
                                return false;
                            v.addlike(li);
                            if (!va.removeVideo(v.getId()))
                                return false;
                            va.addVideo(v);
                            if (!removeFromWall(va.getId(), null))
                                return false;
                            if (!insertVideoAlbumIntoWall(va.getId(), va))
                                return false;
                            if (!saveWall(filename, li.getShareWith()))
                                return false;
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "videoLike", va.getId(), va.getShareWith(), li.getUser());
                            insertNotificaton(n);
                            return true;
                        }
                    }
                }
                return false;
            }
            
            if (li.getType().equals("photo")){
                if (!profileOwner.equals(accountHolder) && !committed){
                    photo p = new photo();
                    p.addlike(li);
                    updatePendingChanges("add", "photoLike", new timelineEntry(0, "photo", p));
                }
                if (!loadPhotos()){
                    return false;
                }
                if (photoAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = (photoAlbum)photoAlbums.get(i);
                    if (li.getGrandParent() == pa.getId()){
                        photo p = pa.getPhoto(li.getParent());
                        if (p == null)
                            return false;
                        p.addlike(li);
                        if (!pa.removePhoto(p.getId()))
                            return false;
                        pa.addPhoto(p);
                        photoAlbums.remove(i);
                        photoAlbums.add(i, pa);
                        savePhotos();
                        break;
                    }
                }
                String filename = getWallFileName(li.getParent(), li.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == li.getParent()){
                            photo p = tle.p;
                            p.addlike(li);
                            if (!removeFromWall(p.getId(), null))
                                return false;
                            if (!insertPhotoIntoWall(p.getId(), p))
                                return false;
                            if (!saveWall(filename, li.getShareWith()))
                                return false;
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "photoLike", p.getId(), p.getShareWith(), li.getUser());
                            insertNotificaton(n);
                            return true;
                        }
                    }
                }
                filename = getWallFileName(li.getGrandParent(), li.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == li.getGrandParent()){
                            photoAlbum pa = tle.pa;
                            photo p = pa.getPhoto(li.getParent());
                            if (p == null)
                                return false;
                            p.addlike(li);
                            if (!pa.removePhoto(p.getId()))
                                return false;
                            pa.addPhoto(p);
                            if (!removeFromWall(pa.getId(), null))
                                return false;
                            if (!insertPhotoAlbumIntoWall(pa.getId(), pa))
                                return false;
                            if (!saveWall(filename, li.getShareWith()))
                                return false;
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "photoLike", pa.getId(), pa.getShareWith(), li.getUser());
                            insertNotificaton(n);
                            return true;
                        }
                    }
                }
                return false;
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean addDislike(dislike dl, boolean committed){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            dl.setGrandParent(getMapEntry(dl.getGrandParent()));
            dl.setParent(getMapEntry(dl.getParent()));
            if (dl.getType().equals("link")){
                if (!profileOwner.equals(accountHolder) && !committed){
                    link l = new link();
                    l.addDislike(dl);
                    updatePendingChanges("add", "linkDislike", new timelineEntry(0, "link", l));
                }
                String filename = getWallFileName(dl.getParent(), dl.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = new timelineEntry(wall.get(i));
                    if (tle.id == dl.getParent()){
                        link l = new link(tle.l);
                        l.addDislike(dl);
                        if (!removeFromWall(l.getId(), null))
                            return false;
                        if (!insertLinkIntoWall(l.getId(), l))
                            return false;
                        if (!saveWall(filename, dl.getShareWith()))
                            return false;
                        notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "linkDislike", l.getId(), l.getShareWith(), dl.getUser());
                        insertNotificaton(n);
                        return true;
                    }
                }
                return false;
            }
            
            if (dl.getType().equals("wallPost")){
                if (!profileOwner.equals(accountHolder) && !committed){
                    wallPost s = new wallPost();
                    s.addDislike(dl);
                    updatePendingChanges("add", "wallPostDislike", new timelineEntry(0, "wallPost", s));
                }
                String filename = getWallFileName(dl.getParent(), dl.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = new timelineEntry(wall.get(i));
                    if (tle.id == dl.getParent()){
                        wallPost s = new wallPost(tle.s);
                        s.addDislike(dl);
                        if (!removeFromWall(s.getId(), null))
                            return false;
                        if (!insertwallPostIntoWall(s.getId(), s))
                            return false;
                        if (!saveWall(filename, dl.getShareWith()))
                            return false;
                        notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "wallPostDislike", s.getId(), s.getShareWith(), dl.getUser());
                        insertNotificaton(n);
                        return true;
                    }
                }
                return false;
            }
            
            if (dl.getType().equals("audioAlbum")){
                if (!profileOwner.equals(accountHolder) && !committed){
                    audioAlbum aa = new audioAlbum();
                    aa.addDislike(dl);
                    updatePendingChanges("add", "audioAlbumDislike", new timelineEntry(0, "audioAlbum", aa));
                }
                if (!loadAudios()){
                    return false;
                }
                if (audioAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = (audioAlbum)audioAlbums.get(i);
                    if (aa.getId() == dl.getParent()){
                        aa.addDislike(dl);
                        audioAlbums.remove(i);
                        audioAlbums.add(i, aa);
                        saveAudios();
                        break;
                    }
                }
                String filename = getWallFileName(dl.getParent(), dl.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == dl.getParent()){
                        audioAlbum aa = tle.aa;
                        aa.addDislike(dl);
                        if (!removeFromWall(aa.getId(), null))
                            return false;
                        if (!insertAudioAlbumIntoWall(aa.getId(), aa))
                            return false;
                        if (!saveWall(filename, dl.getShareWith()))
                            return false;
                        notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "audioAlbumDislike", aa.getId(), aa.getShareWith(), dl.getUser());
                        insertNotificaton(n);
                        return true;
                    }
                }
                return false;
            }
            
            if (dl.getType().equals("videoAlbum")){
                if (!profileOwner.equals(accountHolder) && !committed){
                    videoAlbum va = new videoAlbum();
                    va.addDislike(dl);
                    updatePendingChanges("add", "videoAlbumDislike", new timelineEntry(0, "videoAlbum", va));
                }
                if (!loadVideos()){
                    return false;
                }
                if (videoAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = (videoAlbum)videoAlbums.get(i);
                    if (va.getId() == dl.getParent()){
                        va.addDislike(dl);
                        videoAlbums.remove(i);
                        videoAlbums.add(i, va);
                        saveVideos();
                        break;
                    }
                }
                String filename = getWallFileName(dl.getParent(), dl.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == dl.getParent()){
                        videoAlbum va = tle.va;
                        va.addDislike(dl);
                        if (!removeFromWall(va.getId(), null))
                            return false;
                        if (!insertVideoAlbumIntoWall(va.getId(), va))
                            return false;
                        if (!saveWall(filename, dl.getShareWith()))
                            return false;
                        notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "videoAlbumDislike", va.getId(), va.getShareWith(), dl.getUser());
                        insertNotificaton(n);
                        return true;
                    }
                }
                return false;
            }
            
            if (dl.getType().equals("photoAlbum")){
                if (!profileOwner.equals(accountHolder) && !committed){
                    photoAlbum pa = new photoAlbum();
                    pa.addDislike(dl);
                    updatePendingChanges("add", "photoAlbumDislike", new timelineEntry(0, "photoAlbum", pa));
                }
                if (!loadPhotos()){
                    return false;
                }
                if (photoAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = (photoAlbum)photoAlbums.get(i);
                    if (pa.getId() == dl.getParent()){
                        pa.addDislike(dl);
                        photoAlbums.remove(i);
                        photoAlbums.add(i, pa);
                        savePhotos();
                        break;
                    }
                }
                String filename = getWallFileName(dl.getParent(), dl.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == dl.getParent()){
                        photoAlbum pa = tle.pa;
                        pa.addDislike(dl);
                        if (!removeFromWall(pa.getId(), null))
                            return false;
                        if (!insertPhotoAlbumIntoWall(pa.getId(), pa))
                            return false;
                        if (!saveWall(filename, dl.getShareWith()))
                            return false;
                        notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "photoAlbumDislike", pa.getId(), pa.getShareWith(), dl.getUser());
                        insertNotificaton(n);
                        return true;
                    }
                }
                return false;
            }
            
            if (dl.getType().equals("audio")){
                if (!profileOwner.equals(accountHolder) && !committed){
                    audio a = new audio();
                    a.addDislike(dl);
                    updatePendingChanges("add", "audioDislike", new timelineEntry(0, "audio", a));
                }
                if (!loadAudios()){
                    return false;
                }
                if (audioAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = (audioAlbum)audioAlbums.get(i);
                    if (dl.getGrandParent() == aa.getId()){
                        audio a = aa.getAudio(dl.getParent());
                        if (a == null)
                            return false;
                        a.addDislike(dl);
                        if (!aa.removeAudio(a.getId()))
                            return false;
                        aa.addAudio(a);
                        audioAlbums.remove(i);
                        audioAlbums.add(i, aa);
                        saveAudios();
                        break;
                    }
                }
                String filename = getWallFileName(dl.getParent(), dl.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == dl.getParent()){
                            audio a = tle.a;
                            a.addDislike(dl);
                            if (!removeFromWall(a.getId(), null))
                                return false;
                            if (!insertAudioIntoWall(a.getId(), a))
                                return false;
                            if (!saveWall(filename, dl.getShareWith()))
                                return false;
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "audioDislike", a.getId(), a.getShareWith(), dl.getUser());
                            insertNotificaton(n);
                            return true;
                        }
                    }
                }
                filename = getWallFileName(dl.getGrandParent(), dl.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == dl.getGrandParent()){
                            audioAlbum aa = tle.aa;
                            audio a = aa.getAudio(dl.getParent());
                            if (a == null)
                                return false;
                            a.addDislike(dl);
                            if (!aa.removeAudio(a.getId()))
                                return false;
                            aa.addAudio(a);
                            if (!removeFromWall(aa.getId(), null))
                                return false;
                            if (!insertAudioAlbumIntoWall(aa.getId(), aa))
                                return false;
                            if (!saveWall(filename, dl.getShareWith()))
                                return false;
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "audioDislike", a.getId(), a.getShareWith(), dl.getUser());
                            insertNotificaton(n);
                            return true;
                        }
                    }
                }
                return false;
            }
            
            if (dl.getType().equals("video")){
                if (!profileOwner.equals(accountHolder) && !committed){
                    video v = new video();
                    v.addDislike(dl);
                    updatePendingChanges("add", "videoDislike", new timelineEntry(0, "video", v));
                }
                if (!loadVideos()){
                    return false;
                }
                if (videoAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = (videoAlbum)videoAlbums.get(i);
                    if (dl.getGrandParent() == va.getId()){
                        video v = va.getVideo(dl.getParent());
                        if (v == null)
                            return false;
                        v.addDislike(dl);
                        if (!va.removeVideo(v.getId()))
                            return false;
                        va.addVideo(v);
                        videoAlbums.remove(i);
                        videoAlbums.add(i, va);
                        saveVideos();
                        break;
                    }
                }
                String filename = getWallFileName(dl.getParent(), dl.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == dl.getParent()){
                            video v = tle.v;
                            v.addDislike(dl);
                            if (!removeFromWall(v.getId(), null))
                                return false;
                            if (!insertVideoIntoWall(v.getId(), v))
                                return false;
                            if (!saveWall(filename, dl.getShareWith()))
                                return false;
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "videoDislike", v.getId(), v.getShareWith(), dl.getUser());
                            insertNotificaton(n);
                            return true;
                        }
                    }
                }
                filename = getWallFileName(dl.getGrandParent(), dl.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == dl.getGrandParent()){
                            videoAlbum va = tle.va;
                            video v = va.getVideo(dl.getParent());
                            if (v == null)
                                return false;
                            v.addDislike(dl);
                            if (!va.removeVideo(v.getId()))
                                return false;
                            va.addVideo(v);
                            if (!removeFromWall(va.getId(), null))
                                return false;
                            if (!insertVideoAlbumIntoWall(va.getId(), va))
                                return false;
                            if (!saveWall(filename, dl.getShareWith()))
                                return false;
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "videoDislike", v.getId(), v.getShareWith(), dl.getUser());
                            insertNotificaton(n);
                            return true;
                        }
                    }
                }
                return false;
            }
            
            if (dl.getType().equals("photo")){
                if (!profileOwner.equals(accountHolder) && !committed){
                    photo p = new photo();
                    p.addDislike(dl);
                    updatePendingChanges("add", "photoDislike", new timelineEntry(0, "photo", p));
                }
                if (!loadPhotos()){
                    return false;
                }
                if (photoAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = (photoAlbum)photoAlbums.get(i);
                    if (dl.getGrandParent() == pa.getId()){
                        photo p = pa.getPhoto(dl.getParent());
                        if (p == null)
                            return false;
                        p.addDislike(dl);
                        if (!pa.removePhoto(p.getId()))
                            return false;
                        pa.addPhoto(p);
                        photoAlbums.remove(i);
                        photoAlbums.add(i, pa);
                        savePhotos();
                        break;
                    }
                }
                String filename = getWallFileName(dl.getParent(), dl.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == dl.getParent()){
                            photo p = tle.p;
                            p.addDislike(dl);
                            if (!removeFromWall(p.getId(), null))
                                return false;
                            if (!insertPhotoIntoWall(p.getId(), p))
                                return false;
                            if (!saveWall(filename, dl.getShareWith()))
                                return false;
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "photoDislike", p.getId(), p.getShareWith(), dl.getUser());
                            insertNotificaton(n);
                            return true;
                        }
                    }
                }
                filename = getWallFileName(dl.getGrandParent(), dl.getShareWith());
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == dl.getGrandParent()){
                            photoAlbum pa = tle.pa;
                            photo p = pa.getPhoto(dl.getParent());
                            if (p == null)
                                return false;
                            p.addDislike(dl);
                            if (!pa.removePhoto(p.getId()))
                                return false;
                            pa.addPhoto(p);
                            if (!removeFromWall(pa.getId(), null))
                                return false;
                            if (!insertPhotoAlbumIntoWall(pa.getId(), pa))
                                return false;
                            if (!saveWall(filename, dl.getShareWith()))
                                return false;
                            notification n = new notification(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "add", "photoDislike", p.getId(), p.getShareWith(), dl.getUser());
                            insertNotificaton(n);
                            return true;
                        }
                    }
                }
                return false;
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean removeComment(String type, long grandParent, long parent, long id, String shareWith, String postedBy, boolean committed){
        if (profileOwner == null || profileOwner.equals("")){
            if (DEBUG){
                System.out.println("DEBUG: rc1");
            }
            return false;
        }
        try{
            grandParent = getMapEntry(grandParent);
            parent = getMapEntry(parent);
            if (type.equals("link") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (DEBUG){
                    System.out.println("DEBUG: rc2");
                }
                if (!profileOwner.equals(accountHolder) && !committed){
                    if (DEBUG){
                        System.out.println("DEBUG: rc3");
                    }
                    link l = new link();
                    user u = new user(postedBy, null, null, 0);
                    comment c = new comment(type, grandParent, parent, id, profileOwner, shareWith, u, null);
                    l.addComment(c);
                    updatePendingChanges("remove", "linkComment", new timelineEntry(0, "link", l));
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: rc4");
                    }
                    return false;
                }
                wall.clear();
                if (!loadWall(filename)){
                    if (DEBUG){
                        System.out.println("DEBUG: rc5");
                    }
                    return false;
                }
                if (wall.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: rc6");
                    }
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = new timelineEntry(wall.get(i));
                    if (tle.id == parent){
                        if (DEBUG){
                            System.out.println("DEBUG: rc7");
                        }
                        link l = new link(tle.l);
                        if (!l.removeComment(id)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc8");
                            }
                            return false;
                        }
                        if (!removeFromWall(l.getId(), null)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc9");
                            }
                            return false;
                        }
                        if (!insertLinkIntoWall(l.getId(), l)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc10");
                            }
                            return false;
                        }
                        if (!saveWall(filename, shareWith)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc11");
                            }
                            return false;
                        }
                        if (DEBUG){
                            System.out.println("DEBUG: rc success");
                        }
                        return true;
                    }
                }
                if (DEBUG){
                    System.out.println("DEBUG: rc12");
                }
                return false;
            }
            
            if (type.equals("wallPost") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (DEBUG){
                    System.out.println("DEBUG: rc13");
                }
                if (!profileOwner.equals(accountHolder) && !committed){
                    if (DEBUG){
                        System.out.println("DEBUG: rc14");
                    }
                    wallPost s = new wallPost();
                    user u = new user(postedBy, null, null, 0);
                    comment c = new comment(type, grandParent, parent, id, profileOwner, shareWith, u, null);
                    s.addComment(c);
                    updatePendingChanges("remove", "wallPostComment", new timelineEntry(0, "wallPost", s));
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: rc15");
                    }
                    return false;
                }
                wall.clear();
                if (!loadWall(filename)){
                    if (DEBUG){
                        System.out.println("DEBUG: rc16");
                    }
                    return false;
                }
                if (wall.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: rc17");
                    }
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = new timelineEntry(wall.get(i));
                    if (tle.id == parent){
                        if (DEBUG){
                            System.out.println("DEBUG: rc18");
                        }
                        wallPost s = new wallPost(tle.s);
                        if (!s.removeComment(id)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc19");
                            }
                            return false;
                        }
                        if (!removeFromWall(s.getId(), null)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc20");
                            }
                            return false;
                        }
                        if (!insertwallPostIntoWall(s.getId(), s)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc21");
                            }
                            return false;
                        }
                        if (!saveWall(filename, shareWith)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc22");
                            }
                            return false;
                        }
                        if (DEBUG){
                            System.out.println("DEBUG: rc success");
                        }
                        return true;
                    }
                }
                if (DEBUG){
                    System.out.println("DEBUG: rc23");
                }
                return false;
            }
            
            if (type.equals("audioAlbum") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (DEBUG){
                    System.out.println("DEBUG: rc23");
                }
                if (!profileOwner.equals(accountHolder) && !committed){
                    if (DEBUG){
                        System.out.println("DEBUG: rc24");
                    }
                    audioAlbum aa = new audioAlbum();
                    user u = new user(postedBy, null, null, 0);
                    comment c = new comment(type, grandParent, parent, id, profileOwner, shareWith, u, null);
                    aa.addComment(c);
                    updatePendingChanges("remove", "audioAlbumComment", new timelineEntry(0, "audioAlbum", aa));
                }
                if (!loadAudios()){
                    if (DEBUG){
                        System.out.println("DEBUG: rc25");
                    }
                    return false;
                }
                if (audioAlbums.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: rc26");
                    }
                    return false;
                }
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = (audioAlbum)audioAlbums.get(i);
                    if (aa.getId() == parent){
                        if (DEBUG){
                            System.out.println("DEBUG: rc27");
                        }
                        if (!aa.removeComment(id)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc28");
                            }
                            return false;
                        }
                        audioAlbums.remove(i);
                        audioAlbums.add(i, aa);
                        saveAudios();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: rc29");
                    }
                    return false;
                }
                wall.clear();
                if (!loadWall(filename)){
                    if (DEBUG){
                        System.out.println("DEBUG: rc30");
                    }
                    return false;
                }
                if (wall.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: rc31");
                    }
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == parent){
                        if (DEBUG){
                            System.out.println("DEBUG: rc32");
                        }
                        audioAlbum aa = tle.aa;
                        if (!aa.removeComment(id)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc33");
                            }
                            return false;
                        }
                        if (!removeFromWall(aa.getId(), null)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc34");
                            }
                            return false;
                        }
                        if (!insertAudioAlbumIntoWall(aa.getId(), aa)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc35");
                            }
                            return false;
                        }
                        if (!saveWall(filename, shareWith)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc36");
                            }
                            return false;
                        }
                        if (DEBUG){
                            System.out.println("DEBUG: rc success");
                        }
                        return true;
                    }
                }
                if (DEBUG){
                    System.out.println("DEBUG: rc37");
                }
                return false;
            }
            if (type.equals("videoAlbum") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (DEBUG){
                    System.out.println("DEBUG: rc38");
                }
                if (!profileOwner.equals(accountHolder) && !committed){
                    if (DEBUG){
                        System.out.println("DEBUG: rc39");
                    }
                    videoAlbum va = new videoAlbum();
                    user u = new user(postedBy, null, null, 0);
                    comment c = new comment(type, grandParent, parent, id, profileOwner, shareWith, u, null);
                    va.addComment(c);
                    updatePendingChanges("remove", "videoAlbumComment", new timelineEntry(0, "videoAlbum", va));
                }
                if (!loadVideos()){
                    if (DEBUG){
                        System.out.println("DEBUG: rc40");
                    }
                    return false;
                }
                if (videoAlbums.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: rc41");
                    }
                    return false;
                }
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = (videoAlbum)videoAlbums.get(i);
                    if (va.getId() == parent){
                        if (DEBUG){
                            System.out.println("DEBUG: rc42");
                        }
                        if (!va.removeComment(id)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc43");
                            }
                            return false;
                        }
                        videoAlbums.remove(i);
                        videoAlbums.add(i, va);
                        saveVideos();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: rc44");
                    }
                    return false;
                }
                wall.clear();
                if (!loadWall(filename)){
                    if (DEBUG){
                        System.out.println("DEBUG: rc45");
                    }
                    return false;
                }
                if (wall.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: rc46");
                    }
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == parent){
                        if (DEBUG){
                            System.out.println("DEBUG: rc47");
                        }
                        videoAlbum va = tle.va;
                        if (!va.removeComment(id)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc48");
                            }
                            return false;
                        }
                        if (!removeFromWall(va.getId(), null)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc49");
                            }
                            return false;
                        }
                        if (!insertVideoAlbumIntoWall(va.getId(), va)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc50");
                            }
                            return false;
                        }
                        if (!saveWall(filename, shareWith)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc51");
                            }
                            return false;
                        }
                        if (DEBUG){
                            System.out.println("DEBUG: rc success");
                        }
                        return true;
                    }
                }
                if (DEBUG){
                    System.out.println("DEBUG: rc52");
                }
                return false;
            }
            if (type.equals("photoAlbum") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (DEBUG){
                    System.out.println("DEBUG: rc53");
                }
                if (!profileOwner.equals(accountHolder) && !committed){
                    if (DEBUG){
                        System.out.println("DEBUG: rc54");
                    }
                    photoAlbum pa = new photoAlbum();
                    user u = new user(postedBy, null, null, 0);
                    comment c = new comment(type, grandParent, parent, id, profileOwner, shareWith, u, null);
                    pa.addComment(c);
                    updatePendingChanges("remove", "photoAlbumComment", new timelineEntry(0, "photoAlbum", pa));
                }
                if (!loadPhotos()){
                    if (DEBUG){
                        System.out.println("DEBUG: rc55");
                    }
                    return false;
                }
                if (photoAlbums.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: rc56");
                    }
                    return false;
                }
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = (photoAlbum)photoAlbums.get(i);
                    if (pa.getId() == parent){
                        if (DEBUG){
                            System.out.println("DEBUG: rc57");
                        }
                        if (!pa.removeComment(id)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc58");
                            }
                            return false;
                        }
                        photoAlbums.remove(i);
                        photoAlbums.add(i, pa);
                        savePhotos();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: rc59");
                    }
                    return false;
                }
                wall.clear();
                if (!loadWall(filename)){
                    if (DEBUG){
                        System.out.println("DEBUG: rc60");
                    }
                    return false;
                }
                if (wall.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: rc61");
                    }
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == parent){
                        if (DEBUG){
                            System.out.println("DEBUG: rc62");
                        }
                        photoAlbum pa = tle.pa;
                        if (!pa.removeComment(id)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc63");
                            }
                            return false;
                        }
                        if (!removeFromWall(pa.getId(), null)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc64");
                            }
                            return false;
                        }
                        if (!insertPhotoAlbumIntoWall(pa.getId(), pa)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc65");
                            }
                            return false;
                        }
                        if (!saveWall(filename, shareWith)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc66");
                            }
                            return false;
                        }
                        if (DEBUG){
                            System.out.println("DEBUG: rc success");
                        }
                        return true;
                    }
                }
                if (DEBUG){
                    System.out.println("DEBUG: rc67");
                }
                return false;
            }
            
            if (type.equals("audio") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (DEBUG){
                    System.out.println("DEBUG: rc68");
                }
                if (!profileOwner.equals(accountHolder) && !committed){
                    if (DEBUG){
                        System.out.println("DEBUG: rc69");
                    }
                    audio a = new audio();
                    user u = new user(postedBy, null, null, 0);
                    comment c = new comment(type, grandParent, parent, id, profileOwner, shareWith, u, null);
                    a.addComment(c);
                    updatePendingChanges("remove", "audioComment", new timelineEntry(0, "audio", a));
                }
                if (!loadAudios()){
                    if (DEBUG){
                        System.out.println("DEBUG: rc70");
                    }
                    return false;
                }
                if (audioAlbums.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: rc71");
                    }
                    return false;
                }
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = (audioAlbum)audioAlbums.get(i);
                    if (grandParent == aa.getId()){
                        if (DEBUG){
                            System.out.println("DEBUG: rc72");
                        }
                        audio a = aa.getAudio(parent);
                        if (a == null){
                            if (DEBUG){
                                System.out.println("DEBUG: rc73");
                            }
                            return false;
                        }
                        if (!a.removeComment(id)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc74");
                            }
                            return false;
                        }
                        if (!aa.removeAudio(a.getId())){
                            if (DEBUG){
                                System.out.println("DEBUG: rc75");
                            }
                            return false;
                        }
                        aa.addAudio(a);
                        audioAlbums.remove(i);
                        audioAlbums.add(i, aa);
                        saveAudios();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: rc76");
                    }
                    return false;
                }
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == parent){
                            if (DEBUG){
                                System.out.println("DEBUG: rc77");
                            }
                            audio a = tle.a;
                            if (!a.removeComment(id)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc78");
                                }
                                return false;
                            }
                            if (!removeFromWall(a.getId(), null)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc79");
                                }
                                return false;
                            }
                            if (!insertAudioIntoWall(a.getId(), a)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc80");
                                }
                                return false;
                            }
                            if (!saveWall(filename, shareWith)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc81");
                                }
                                return false;
                            }
                            if (DEBUG){
                                System.out.println("DEBUG: rc success");
                            }
                            return true;
                        }
                    }
                }
                filename = getWallFileName(grandParent, shareWith);
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: rc82");
                    }
                    return false;
                }
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == grandParent){
                            if (DEBUG){
                                System.out.println("DEBUG: rc83");
                            }
                            audioAlbum aa = tle.aa;
                            audio a = aa.getAudio(parent);
                            if (a == null){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc84");
                                }
                                return false;
                            }
                            if (!a.removeComment(id)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc85");
                                }
                                return false;
                            }
                            if (!aa.removeAudio(a.getId())){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc86");
                                }
                                return false;
                            }
                            aa.addAudio(a);
                            if (!removeFromWall(aa.getId(), null)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc87");
                                }
                                return false;
                            }
                            if (!insertAudioAlbumIntoWall(aa.getId(), aa)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc88");
                                }
                                return false;
                            }
                            if (!saveWall(filename, shareWith)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc89");
                                }
                                return false;
                            }
                            if (DEBUG){
                                System.out.println("DEBUG: rc success");
                            }
                            return true;
                        }
                    }
                }
                if (DEBUG){
                    System.out.println("DEBUG: rc90");
                }
                return false;
            }
            
            if (type.equals("video") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (DEBUG){
                    System.out.println("DEBUG: rc91");
                }
                if (!profileOwner.equals(accountHolder) && !committed){
                    if (DEBUG){
                        System.out.println("DEBUG: rc92");
                    }
                    video v = new video();
                    user u = new user(postedBy, null, null, 0);
                    comment c = new comment(type, grandParent, parent, id, profileOwner, shareWith, u, null);
                    v.addComment(c);
                    updatePendingChanges("remove", "videoComment", new timelineEntry(0, "video", v));
                }
                if (!loadVideos()){
                    if (DEBUG){
                        System.out.println("DEBUG: rc93");
                    }
                    return false;
                }
                if (videoAlbums.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: rc94");
                    }
                    return false;
                }
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = (videoAlbum)videoAlbums.get(i);
                    if (grandParent == va.getId()){
                        if (DEBUG){
                            System.out.println("DEBUG: rc95");
                        }
                        video v = va.getVideo(parent);
                        if (v == null){
                            if (DEBUG){
                                System.out.println("DEBUG: rc96");
                            }
                            return false;
                        }
                        if (!v.removeComment(id)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc97");
                            }
                            return false;
                        }
                        if (!va.removeVideo(v.getId())){
                            if (DEBUG){
                                System.out.println("DEBUG: rc98");
                            }
                            return false;
                        }
                        va.addVideo(v);
                        videoAlbums.remove(i);
                        videoAlbums.add(i, va);
                        saveVideos();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: rc99");
                    }
                    return false;
                }
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == parent){
                            if (DEBUG){
                                System.out.println("DEBUG: rc100");
                            }
                            video v = tle.v;
                            if (!v.removeComment(id)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc101");
                                }
                                return false;
                            }
                            if (!removeFromWall(v.getId(), null)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc102");
                                }
                                return false;
                            }
                            if (!insertVideoIntoWall(v.getId(), v)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc103");
                                }
                                return false;
                            }
                            if (!saveWall(filename, shareWith)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc104");
                                }
                                return false;
                            }
                            if (DEBUG){
                                System.out.println("DEBUG: rc succes");
                            }
                            return true;
                        }
                    }
                }
                filename = getWallFileName(grandParent, shareWith);
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: rc105");
                    }
                    return false;
                }
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == grandParent){
                            if (DEBUG){
                                System.out.println("DEBUG: rc106");
                            }
                            videoAlbum va = tle.va;
                            video v = va.getVideo(parent);
                            if (v == null){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc107");
                                }
                                return false;
                            }
                            if (!v.removeComment(id)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc108");
                                }
                                return false;
                            }
                            if (!va.removeVideo(v.getId())){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc109");
                                }
                                return false;
                            }
                            va.addVideo(v);
                            if (!removeFromWall(va.getId(), null)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc110");
                                }
                                return false;
                            }
                            if (!insertVideoAlbumIntoWall(va.getId(), va)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc111");
                                }
                                return false;
                            }
                            if (!saveWall(filename, shareWith)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc112");
                                }
                                return false;
                            }
                            if (DEBUG){
                                System.out.println("DEBUG: rc success");
                            }
                            return true;
                        }
                    }
                }
                if (DEBUG){
                    System.out.println("DEBUG: rc113");
                }
                return false;
            }
            
            if (type.equals("photo") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (DEBUG){
                    System.out.println("DEBUG: rc114");
                }
                if (!profileOwner.equals(accountHolder) && !committed){
                    if (DEBUG){
                        System.out.println("DEBUG: rc115");
                    }
                    photo p = new photo();
                    user u = new user(postedBy, null, null, 0);
                    comment c = new comment(type, grandParent, parent, id, profileOwner, shareWith, u, null);
                    p.addComment(c);
                    updatePendingChanges("remove", "photoComment", new timelineEntry(0, "photo", p));
                }
                if (!loadPhotos()){
                    if (DEBUG){
                        System.out.println("DEBUG: rc116");
                    }
                    return false;
                }
                if (photoAlbums.size() == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: rc117");
                    }
                    return false;
                }
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = (photoAlbum)photoAlbums.get(i);
                    if (grandParent == pa.getId()){
                        if (DEBUG){
                            System.out.println("DEBUG: rc118");
                        }
                        photo p = pa.getPhoto(parent);
                        if (p == null){
                            if (DEBUG){
                                System.out.println("DEBUG: rc119");
                            }
                            return false;
                        }
                        if (!p.removeComment(id)){
                            if (DEBUG){
                                System.out.println("DEBUG: rc120");
                            }
                            return false;
                        }
                        if (!pa.removePhoto(p.getId())){
                            if (DEBUG){
                                System.out.println("DEBUG: rc121");
                            }
                            return false;
                        }
                        pa.addPhoto(p);
                        photoAlbums.remove(i);
                        photoAlbums.add(i, pa);
                        savePhotos();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: rc122");
                    }
                    return false;
                }
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == parent){
                            if (DEBUG){
                                System.out.println("DEBUG: rc123");
                            }
                            photo p = tle.p;
                            if (!p.removeComment(id)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc124");
                                }
                                return false;
                            }
                            if (!removeFromWall(p.getId(), null)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc125");
                                }
                                return false;
                            }
                            if (!insertPhotoIntoWall(p.getId(), p)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc126");
                                }
                                return false;
                            }
                            if (!saveWall(filename, shareWith)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc127");
                                }
                                return false;
                            }
                            if (DEBUG){
                                System.out.println("DEBUG: rc success");
                            }
                            return true;
                        }
                    }
                }
                filename = getWallFileName(grandParent, shareWith);
                if (filename == null){
                    if (DEBUG){
                        System.out.println("DEBUG: rc128");
                    }
                    return false;
                }
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == grandParent){
                            if (DEBUG){
                                System.out.println("DEBUG: rc129");
                            }
                            photoAlbum pa = tle.pa;
                            photo p = pa.getPhoto(parent);
                            if (p == null){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc130");
                                }
                                return false;
                            }
                            if (!p.removeComment(id)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc131");
                                }
                                return false;
                            }
                            if (!pa.removePhoto(p.getId())){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc132");
                                }
                                return false;
                            }
                            pa.addPhoto(p);
                            if (!removeFromWall(pa.getId(), null)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc133");
                                }
                                return false;
                            }
                            if (!insertPhotoAlbumIntoWall(pa.getId(), pa)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc134");
                                }
                                return false;
                            }
                            if (!saveWall(filename, shareWith)){
                                if (DEBUG){
                                    System.out.println("DEBUG: rc135");
                                }
                                return false;
                            }
                            if (DEBUG){
                                System.out.println("DEBUG: rc success");
                            }
                            return true;
                        }
                    }
                }
                if (DEBUG){
                    System.out.println("DEBUG: rc136");
                }
                return false;
            }
            if (DEBUG){
                System.out.println("DEBUG: rc 137");
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean removelike(String type, long grandParent, long parent, long id, String shareWith, String postedBy, boolean committed){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            grandParent = getMapEntry(grandParent);
            parent = getMapEntry(parent);
            if (type.equals("link") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (!profileOwner.equals(accountHolder) && !committed){
                    link l = new link();
                    user u = new user(postedBy, null, null, 0);
                    like li = new like(type, grandParent, parent, id, profileOwner, shareWith, u);
                    l.addlike(li);
                    updatePendingChanges("remove", "linkLike", new timelineEntry(0, "link", l));
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = new timelineEntry(wall.get(i));
                    if (tle.id == parent){
                        link l = new link(tle.l);
                        if (!l.removelike(id))
                            return false;
                        if (!removeFromWall(l.getId(), null))
                            return false;
                        if (!insertLinkIntoWall(l.getId(), l))
                            return false;
                        if (!saveWall(filename, shareWith))
                            return false;
                        return true;
                    }
                }
                return false;
            }
            
            if (type.equals("wallPost") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (!profileOwner.equals(accountHolder) && !committed){
                    wallPost s = new wallPost();
                    user u = new user(postedBy, null, null, 0);
                    like li = new like(type, grandParent, parent, id, profileOwner, shareWith, u);
                    s.addlike(li);
                    updatePendingChanges("remove", "wallPostLike", new timelineEntry(0, "wallPost", s));
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = new timelineEntry(wall.get(i));
                    if (tle.id == parent){
                        wallPost s = new wallPost(tle.s);
                        if (!s.removelike(id))
                            return false;
                        if (!removeFromWall(s.getId(), null))
                            return false;
                        if (!insertwallPostIntoWall(s.getId(), s))
                            return false;
                        if (!saveWall(filename, shareWith))
                            return false;
                        return true;
                    }
                }
                return false;
            }
            
            if (type.equals("audioAlbum") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (!profileOwner.equals(accountHolder) && !committed){
                    audioAlbum aa = new audioAlbum();
                    user u = new user(postedBy, null, null, 0);
                    like li = new like(type, grandParent, parent, id, profileOwner, shareWith, u);
                    aa.addlike(li);
                    updatePendingChanges("remove", "audioAlbumLike", new timelineEntry(0, "audioAlbum", aa));
                }
                if (!loadAudios()){
                    return false;
                }
                if (audioAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = (audioAlbum)audioAlbums.get(i);
                    if (aa.getId() == parent){
                        if (!aa.removelike(id))
                            return false;
                        audioAlbums.remove(i);
                        audioAlbums.add(i, aa);
                        saveAudios();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == parent){
                        audioAlbum aa = tle.aa;
                        if (!aa.removelike(id))
                            return false;
                        if (!removeFromWall(aa.getId(), null))
                            return false;
                        if (!insertAudioAlbumIntoWall(aa.getId(), aa))
                            return false;
                        if (!saveWall(filename, shareWith))
                            return false;
                        return true;
                    }
                }
                return false;
            }
            
            if (type.equals("videoAlbum") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (!profileOwner.equals(accountHolder) && !committed){
                    videoAlbum va = new videoAlbum();
                    user u = new user(postedBy, null, null, 0);
                    like li = new like(type, grandParent, parent, id, profileOwner, shareWith, u);
                    va.addlike(li);
                    updatePendingChanges("remove", "videoAlbumLike", new timelineEntry(0, "videoAlbum", va));
                }
                if (!loadVideos()){
                    return false;
                }
                if (videoAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = (videoAlbum)videoAlbums.get(i);
                    if (va.getId() == parent){
                        if (!va.removelike(id))
                            return false;
                        videoAlbums.remove(i);
                        videoAlbums.add(i, va);
                        saveVideos();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == parent){
                        videoAlbum va = tle.va;
                        if (!va.removelike(id))
                            return false;
                        if (!removeFromWall(va.getId(), null))
                            return false;
                        if (!insertVideoAlbumIntoWall(va.getId(), va))
                            return false;
                        if (!saveWall(filename, shareWith))
                            return false;
                        return true;
                    }
                }
                return false;
            }
            
            if (type.equals("photoAlbum") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (!profileOwner.equals(accountHolder) && !committed){
                    photoAlbum pa = new photoAlbum();
                    user u = new user(postedBy, null, null, 0);
                    like li = new like(type, grandParent, parent, id, profileOwner, shareWith, u);
                    pa.addlike(li);
                    updatePendingChanges("remove", "photoAlbumLike", new timelineEntry(0, "photoAlbum", pa));
                }
                if (!loadPhotos()){
                    return false;
                }
                if (photoAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = (photoAlbum)photoAlbums.get(i);
                    if (pa.getId() == parent){
                        if (!pa.removelike(id))
                            return false;
                        photoAlbums.remove(i);
                        photoAlbums.add(i, pa);
                        savePhotos();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == parent){
                        photoAlbum pa = tle.pa;
                        if (!pa.removelike(id))
                            return false;
                        if (!removeFromWall(pa.getId(), null))
                            return false;
                        if (!insertPhotoAlbumIntoWall(pa.getId(), pa))
                            return false;
                        if (!saveWall(filename, shareWith))
                            return false;
                        return true;
                    }
                }
                return false;
            }
            
            if (type.equals("audio") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (!profileOwner.equals(accountHolder) && !committed){
                    audio a = new audio();
                    user u = new user(postedBy, null, null, 0);
                    like li = new like(type, grandParent, parent, id, profileOwner, shareWith, u);
                    a.addlike(li);
                    updatePendingChanges("remove", "audioLike", new timelineEntry(0, "audio", a));
                }
                if (!loadAudios()){
                    return false;
                }
                if (audioAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = (audioAlbum)audioAlbums.get(i);
                    if (grandParent == aa.getId()){
                        audio a = aa.getAudio(parent);
                        if (a == null)
                            return false;
                        if (!a.removelike(id))
                            return false;
                        if (!aa.removeAudio(a.getId()))
                            return false;
                        aa.addAudio(a);
                        audioAlbums.remove(i);
                        audioAlbums.add(i, aa);
                        saveAudios();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == parent){
                            audio a = tle.a;
                            if (!a.removelike(id))
                                return false;
                            if (!removeFromWall(a.getId(), null))
                                return false;
                            if (!insertAudioIntoWall(a.getId(), a))
                                return false;
                            if (!saveWall(filename, shareWith))
                                return false;
                            return true;
                        }
                    }
                }
                filename = getWallFileName(grandParent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == grandParent){
                            audioAlbum aa = tle.aa;
                            audio a = aa.getAudio(parent);
                            if (a == null)
                                return false;
                            if (!a.removelike(id))
                                return false;
                            if (!aa.removeAudio(a.getId()))
                                return false;
                            aa.addAudio(a);
                            if (!removeFromWall(aa.getId(), null))
                                return false;
                            if (!insertAudioAlbumIntoWall(aa.getId(), aa))
                                return false;
                            if (!saveWall(filename, shareWith))
                                return false;
                            return true;
                        }
                    }
                }
                return false;
            }
            
            if (type.equals("video") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (!profileOwner.equals(accountHolder) && !committed){
                    video v = new video();
                    user u = new user(postedBy, null, null, 0);
                    like li = new like(type, grandParent, parent, id, profileOwner, shareWith, u);
                    v.addlike(li);
                    updatePendingChanges("remove", "videoLike", new timelineEntry(0, "video", v));
                }
                if (!loadVideos()){
                    return false;
                }
                if (videoAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = (videoAlbum)videoAlbums.get(i);
                    if (grandParent == va.getId()){
                        video v = va.getVideo(parent);
                        if (v == null)
                            return false;
                        if (!v.removelike(id))
                            return false;
                        if (!va.removeVideo(v.getId()))
                            return false;
                        va.addVideo(v);
                        videoAlbums.remove(i);
                        videoAlbums.add(i, va);
                        saveVideos();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == parent){
                            video v = tle.v;
                            if (!v.removelike(id))
                                return false;
                            if (!removeFromWall(v.getId(), null))
                                return false;
                            if (!insertVideoIntoWall(v.getId(), v))
                                return false;
                            if (!saveWall(filename, shareWith))
                                return false;
                            return true;
                        }
                    }
                }
                filename = getWallFileName(grandParent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == grandParent){
                            videoAlbum va = tle.va;
                            video v = va.getVideo(parent);
                            if (v == null)
                                return false;
                            if (!v.removelike(id))
                                return false;
                            if (!va.removeVideo(v.getId()))
                                return false;
                            va.addVideo(v);
                            if (!removeFromWall(va.getId(), null))
                                return false;
                            if (!insertVideoAlbumIntoWall(va.getId(), va))
                                return false;
                            if (!saveWall(filename, shareWith))
                                return false;
                            return true;
                        }
                    }
                }
                return false;
            }
            
            if (type.equals("photo") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (!profileOwner.equals(accountHolder) && !committed){
                    photo p = new photo();
                    user u = new user(postedBy, null, null, 0);
                    like li = new like(type, grandParent, parent, id, profileOwner, shareWith, u);
                    p.addlike(li);
                    updatePendingChanges("remove", "photoLike", new timelineEntry(0, "photo", p));
                }
                if (!loadPhotos()){
                    return false;
                }
                if (photoAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = (photoAlbum)photoAlbums.get(i);
                    if (grandParent == pa.getId()){
                        photo p = pa.getPhoto(parent);
                        if (p == null)
                            return false;
                        if (!p.removelike(id))
                            return false;
                        if (!pa.removePhoto(p.getId()))
                            return false;
                        pa.addPhoto(p);
                        photoAlbums.remove(i);
                        photoAlbums.add(i, pa);
                        savePhotos();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == parent){
                            photo p = tle.p;
                            if (!p.removelike(id))
                                return false;
                            if (!removeFromWall(p.getId(), null))
                                return false;
                            if (!insertPhotoIntoWall(p.getId(), p))
                                return false;
                            if (!saveWall(filename, shareWith))
                                return false;
                            return true;
                        }
                    }
                }
                filename = getWallFileName(grandParent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == grandParent){
                            photoAlbum pa = tle.pa;
                            photo p = pa.getPhoto(parent);
                            if (p == null)
                                return false;
                            if (!p.removelike(id))
                                return false;
                            if (!pa.removePhoto(p.getId()))
                                return false;
                            pa.addPhoto(p);
                            if (!removeFromWall(pa.getId(), null))
                                return false;
                            if (!insertPhotoAlbumIntoWall(pa.getId(), pa))
                                return false;
                            if (!saveWall(filename, shareWith))
                                return false;
                            return true;
                        }
                    }
                }
                return false;
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean removeDislike(String type, long grandParent, long parent, long id, String shareWith, String postedBy, boolean committed){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            grandParent = getMapEntry(grandParent);
            parent = getMapEntry(parent);
            if (type.equals("link") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (!profileOwner.equals(accountHolder) && !committed){
                    link l = new link();
                    user u = new user(postedBy, null, null, 0);
                    dislike dli = new dislike(type, grandParent, parent, id, profileOwner, shareWith, u);
                    l.addDislike(dli);
                    updatePendingChanges("remove", "linkDislike", new timelineEntry(0, "link", l));
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = new timelineEntry(wall.get(i));
                    if (tle.id == parent){
                        link l = new link(tle.l);
                        if (!l.removeDislike(id))
                            return false;
                        if (!removeFromWall(l.getId(), null))
                            return false;
                        if (!insertLinkIntoWall(l.getId(), l))
                            return false;
                        if (!saveWall(filename, shareWith))
                            return false;
                        return true;
                    }
                }
                return false;
            }
            
            if (type.equals("wallPost") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (!profileOwner.equals(accountHolder) && !committed){
                    wallPost s = new wallPost();
                    user u = new user(postedBy, null, null, 0);
                    dislike dli = new dislike(type, grandParent, parent, id, profileOwner, shareWith, u);
                    s.addDislike(dli);
                    updatePendingChanges("remove", "wallPostDislike", new timelineEntry(0, "wallPost", s));
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = new timelineEntry(wall.get(i));
                    if (tle.id == parent){
                        wallPost s = new wallPost(tle.s);
                        if (!s.removeDislike(id))
                            return false;
                        if (!removeFromWall(s.getId(), null))
                            return false;
                        if (!insertwallPostIntoWall(s.getId(), s))
                            return false;
                        if (!saveWall(filename, shareWith))
                            return false;
                        return true;
                    }
                }
                return false;
            }
            
            if (type.equals("audioAlbum") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (!profileOwner.equals(accountHolder) && !committed){
                    audioAlbum aa = new audioAlbum();
                    user u = new user(postedBy, null, null, 0);
                    dislike dli = new dislike(type, grandParent, parent, id, profileOwner, shareWith, u);
                    aa.addDislike(dli);
                    updatePendingChanges("remove", "audioAlbumDislike", new timelineEntry(0, "audioAlbum", aa));
                }
                if (!loadAudios()){
                    return false;
                }
                if (audioAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = (audioAlbum)audioAlbums.get(i);
                    if (aa.getId() == parent){
                        if (!aa.removeDislike(id))
                            return false;
                        audioAlbums.remove(i);
                        audioAlbums.add(i, aa);
                        saveAudios();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == parent){
                        audioAlbum aa = tle.aa;
                        if (!aa.removeDislike(id))
                            return false;
                        if (!removeFromWall(aa.getId(), null))
                            return false;
                        if (!insertAudioAlbumIntoWall(aa.getId(), aa))
                            return false;
                        if (!saveWall(filename, shareWith))
                            return false;
                        return true;
                    }
                }
                return false;
            }
            
            if (type.equals("videoAlbum") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (!profileOwner.equals(accountHolder) && !committed){
                    videoAlbum va = new videoAlbum();
                    user u = new user(postedBy, null, null, 0);
                    dislike dli = new dislike(type, grandParent, parent, id, profileOwner, shareWith, u);
                    va.addDislike(dli);
                    updatePendingChanges("remove", "videoAlbumDislike", new timelineEntry(0, "videoAlbum", va));
                }
                if (!loadVideos()){
                    return false;
                }
                if (videoAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = (videoAlbum)videoAlbums.get(i);
                    if (va.getId() == parent){
                        if (!va.removeDislike(id))
                            return false;
                        videoAlbums.remove(i);
                        videoAlbums.add(i, va);
                        saveVideos();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == parent){
                        videoAlbum va = tle.va;
                        if (!va.removeDislike(id))
                            return false;
                        if (!removeFromWall(va.getId(), null))
                            return false;
                        if (!insertVideoAlbumIntoWall(va.getId(), va))
                            return false;
                        if (!saveWall(filename, shareWith))
                            return false;
                        return true;
                    }
                }
                return false;
            }
            
            if (type.equals("photoAlbum") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (!profileOwner.equals(accountHolder) && !committed){
                    photoAlbum pa = new photoAlbum();
                    user u = new user(postedBy, null, null, 0);
                    dislike dli = new dislike(type, grandParent, parent, id, profileOwner, shareWith, u);
                    pa.addDislike(dli);
                    updatePendingChanges("remove", "photoAlbumDislike", new timelineEntry(0, "photoAlbum", pa));
                }
                if (!loadPhotos()){
                    return false;
                }
                if (photoAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = (photoAlbum)photoAlbums.get(i);
                    if (pa.getId() == parent){
                        if (!pa.removeDislike(id))
                            return false;
                        photoAlbums.remove(i);
                        photoAlbums.add(i, pa);
                        savePhotos();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (!loadWall(filename))
                    return false;
                if (wall.size() == 0){
                    return false;
                }
                for (int i = 0; i < wall.size(); i++){
                    timelineEntry tle = (timelineEntry)wall.get(i);
                    if (tle.id == parent){
                        photoAlbum pa = tle.pa;
                        if (!pa.removeDislike(id))
                            return false;
                        if (!removeFromWall(pa.getId(), null))
                            return false;
                        if (!insertPhotoAlbumIntoWall(pa.getId(), pa))
                            return false;
                        if (!saveWall(filename, shareWith))
                            return false;
                        return true;
                    }
                }
                return false;
            }
            
            if (type.equals("audio") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (!profileOwner.equals(accountHolder) && !committed){
                    audio a = new audio();
                    user u = new user(postedBy, null, null, 0);
                    dislike dli = new dislike(type, grandParent, parent, id, profileOwner, shareWith, u);
                    a.addDislike(dli);
                    updatePendingChanges("remove", "audioDislike", new timelineEntry(0, "audio", a));
                }
                if (!loadAudios()){
                    return false;
                }
                if (audioAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = (audioAlbum)audioAlbums.get(i);
                    if (grandParent == aa.getId()){
                        audio a = aa.getAudio(parent);
                        if (a == null)
                            return false;
                        if (!a.removeDislike(id))
                            return false;
                        if (!aa.removeAudio(a.getId()))
                            return false;
                        aa.addAudio(a);
                        audioAlbums.remove(i);
                        audioAlbums.add(i, aa);
                        saveAudios();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == parent){
                            audio a = tle.a;
                            if (!a.removeDislike(id))
                                return false;
                            if (!removeFromWall(a.getId(), null))
                                return false;
                            if (!insertAudioIntoWall(a.getId(), a))
                                return false;
                            if (!saveWall(filename, shareWith))
                                return false;
                            return true;
                        }
                    }
                }
                filename = getWallFileName(grandParent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == grandParent){
                            audioAlbum aa = tle.aa;
                            audio a = aa.getAudio(parent);
                            if (a == null)
                                return false;
                            if (!a.removeDislike(id))
                                return false;
                            if (!aa.removeAudio(a.getId()))
                                return false;
                            aa.addAudio(a);
                            if (!removeFromWall(aa.getId(), null))
                                return false;
                            if (!insertAudioAlbumIntoWall(aa.getId(), aa))
                                return false;
                            if (!saveWall(filename, shareWith))
                                return false;
                            return true;
                        }
                    }
                }
                return false;
            }
            
            if (type.equals("video") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (!profileOwner.equals(accountHolder) && !committed){
                    video v = new video();
                    user u = new user(postedBy, null, null, 0);
                    dislike dli = new dislike(type, grandParent, parent, id, profileOwner, shareWith, u);
                    v.addDislike(dli);
                    updatePendingChanges("remove", "videoDislike", new timelineEntry(0, "video", v));
                }
                if (!loadVideos()){
                    return false;
                }
                if (videoAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = (videoAlbum)videoAlbums.get(i);
                    if (grandParent == va.getId()){
                        video v = va.getVideo(parent);
                        if (v == null)
                            return false;
                        if (!v.removeDislike(id))
                            return false;
                        if (!va.removeVideo(v.getId()))
                            return false;
                        va.addVideo(v);
                        videoAlbums.remove(i);
                        videoAlbums.add(i, va);
                        saveVideos();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == parent){
                            video v = tle.v;
                            if (!v.removeDislike(id))
                                return false;
                            if (!removeFromWall(v.getId(), null))
                                return false;
                            if (!insertVideoIntoWall(v.getId(), v))
                                return false;
                            if (!saveWall(filename, shareWith))
                                return false;
                            return true;
                        }
                    }
                }
                filename = getWallFileName(grandParent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == grandParent){
                            videoAlbum va = tle.va;
                            video v = va.getVideo(parent);
                            if (v == null)
                                return false;
                            if (!v.removeDislike(id))
                                return false;
                            if (!va.removeVideo(v.getId()))
                                return false;
                            va.addVideo(v);
                            if (!removeFromWall(va.getId(), null))
                                return false;
                            if (!insertVideoAlbumIntoWall(va.getId(), va))
                                return false;
                            if (!saveWall(filename, shareWith))
                                return false;
                            return true;
                        }
                    }
                }
                return false;
            }
            
            if (type.equals("photo") && ( postedBy.equals(accountHolder) || profileOwner.equals(accountHolder) )){
                if (!profileOwner.equals(accountHolder) && !committed){
                    photo p = new photo();
                    user u = new user(postedBy, null, null, 0);
                    dislike dli = new dislike(type, grandParent, parent, id, profileOwner, shareWith, u);
                    p.addDislike(dli);
                    updatePendingChanges("remove", "photoDislike", new timelineEntry(0, "photo", p));
                }
                if (!loadPhotos()){
                    return false;
                }
                if (photoAlbums.size() == 0){
                    return false;
                }
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = (photoAlbum)photoAlbums.get(i);
                    if (grandParent == pa.getId()){
                        photo p = pa.getPhoto(parent);
                        if (p == null)
                            return false;
                        if (!p.removeDislike(id))
                            return false;
                        if (!pa.removePhoto(p.getId()))
                            return false;
                        pa.addPhoto(p);
                        photoAlbums.remove(i);
                        photoAlbums.add(i, pa);
                        savePhotos();
                        break;
                    }
                }
                String filename = getWallFileName(parent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == parent){
                            photo p = tle.p;
                            if (!p.removeDislike(id))
                                return false;
                            if (!removeFromWall(p.getId(), null))
                                return false;
                            if (!insertPhotoIntoWall(p.getId(), p))
                                return false;
                            if (!saveWall(filename, shareWith))
                                return false;
                            return true;
                        }
                    }
                }
                filename = getWallFileName(grandParent, shareWith);
                if (filename == null)
                    return false;
                wall.clear();
                if (loadWall(filename)){
                    for (int i = 0; i < wall.size(); i++){
                        timelineEntry tle = (timelineEntry)wall.get(i);
                        if (tle.id == grandParent){
                            photoAlbum pa = tle.pa;
                            photo p = pa.getPhoto(parent);
                            if (p == null)
                                return false;
                            if (!p.removeDislike(id))
                                return false;
                            if (!pa.removePhoto(p.getId()))
                                return false;
                            pa.addPhoto(p);
                            if (!removeFromWall(pa.getId(), null))
                                return false;
                            if (!insertPhotoAlbumIntoWall(pa.getId(), pa))
                                return false;
                            if (!saveWall(filename, shareWith))
                                return false;
                            return true;
                        }
                    }
                }
                return false;
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public List<String> getZoneNames(){
        if (profileOwner == null || profileOwner.equals(""))
            return null;
        List<String> zoneNames = new ArrayList<String>();
        try{
            String path;
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                return null;
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            File file = new File(path + "/zones.xml");
            boolean exists = file.exists();
            if (!exists){
                return null;
            }
            byte[] readIn = null;
            while((readIn = readXML(path + "/zones.xml")) == null){
                Thread.sleep(100);
            }
            dom = db.parse(new ByteArrayInputStream(readIn));
            Element docEle = dom.getDocumentElement();
            
            NodeList nl = docEle.getElementsByTagName("zone");
            if(nl != null && nl.getLength() > 0) {
                for(int i = 0 ; i < nl.getLength();i++) {
                    Element el = (Element)nl.item(i);
                    zone z = new zone();
                    z.create(el);
                    zoneNames.add(z.getName());
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return null;
        }
        return zoneNames;
    }
    
    public List<timelineEntry> createTimeLine(long since, long to){
        if (profileOwner == null || profileOwner.equals(""))
            return null;
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            String path;
            wall.clear();
            if (profileOwner.equals(accountHolder)){
                List<String> zoneNames = getZoneNames();
                if (zoneNames == null)
                    return wall;
                for (int i = 0; i < zoneNames.size(); i++){
                    String zone = (String)zoneNames.get(i);
                    path = prefix + profileOwner + "/zones/" + zone;
                    File file = new File(path + "/history.xml");
                    boolean exists = file.exists();
                    if (!exists){
                        continue;
                    }
                    byte[] readIn = null;
                    while((readIn = readXML(path + "/history.xml")) == null){
                        Thread.sleep(100);
                    }
                    dom = db.parse(new ByteArrayInputStream(readIn));
                    Element docEle = dom.getDocumentElement();
                    NodeList nl = docEle.getElementsByTagName("historyLogEntry");
                    if(nl != null && nl.getLength() > 0) {
                        for(int j = 0 ; j < nl.getLength(); j++) {
                            Element el = (Element)nl.item(j);
                            historyLogEntry le = new historyLogEntry();
                            le.create(el);
                            if (le.getStart() <= since && le.getEnd() >= since
                                || le.getStart() <= to && le.getEnd() >= to
                                || le.getStart() >= since && le.getEnd() <= to){
                                loadWall(path + "/wall/" + String.valueOf(le.getStart()) + ".xml");
                            }
                        }
                    }
                }
            }else{
                path = prefix + accountHolder + "/friends/" + profileOwner;
                File file = new File(path + "/history.xml");
                boolean exists = file.exists();
                if (!exists){
                    return wall;
                }
                byte[] readIn = null;
                while((readIn = readXML(path + "/history.xml")) == null){
                    Thread.sleep(100);
                }
                dom = db.parse(new ByteArrayInputStream(readIn));
                Element docEle = dom.getDocumentElement();
                NodeList nl = docEle.getElementsByTagName("historyLogEntry");
                if(nl != null && nl.getLength() > 0) {
                    for(int i = 0 ; i < nl.getLength();i++) {
                        Element el = (Element)nl.item(i);
                        historyLogEntry le = new historyLogEntry();
                        le.create(el);
                        if (le.getStart() <= since && le.getEnd() >= since
                            || le.getStart() <= to && le.getEnd() >= to
                            || le.getStart() >= since && le.getEnd() <= to){
                            loadWall(path + "/wall/" + String.valueOf(le.getStart()) + ".xml");
                        }
                    }
                }
            }
            if (wall.size() == 0){
                return null;
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return null;
        }
        return wall;
    }
    
    public String displayWall(int entries){
        if (profileOwner == null || profileOwner.equals(""))
            return "";
        String result = "";
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            String path = null;
            wall.clear();
            if (profileOwner.equals(accountHolder)){
                int index = 0;
                boolean hasMore = true;
                List<String> zoneNames = getZoneNames();
                if (zoneNames == null)
                    return result;
                NodeList znl[] = new NodeList[zoneNames.size()];
                for (int i = 0; i < zoneNames.size(); i++){
                    String zone = new String(zoneNames.get(i));
                    path = prefix + profileOwner + "/zones/" + zone;
                    File file = new File(path + "/history.xml");
                    boolean exists = file.exists();
                    if (!exists){
                        continue;
                    }
                    byte[] readIn = null;
                    while((readIn = readXML(path + "/history.xml")) == null){
                        Thread.sleep(100);
                    }
                    dom = db.parse(new ByteArrayInputStream(readIn));
                    Element docEle = dom.getDocumentElement();
                    znl[i] = docEle.getElementsByTagName("historyLogEntry");
                }
                while(wall.size() < entries && hasMore){
                    for (int i = 0; i < zoneNames.size(); i++){
                        String zone = new String(zoneNames.get(i));
                        path = prefix + profileOwner + "/zones/" + zone;
                        if(znl[i] != null && znl[i].getLength() > 0) {
                            if (index < znl[i].getLength()){
                                Element el = (Element)znl[i].item(index);
                                historyLogEntry le = new historyLogEntry();
                                le.create(el);
                                loadWall(path + "/wall/" + String.valueOf(le.getStart()) + ".xml");
                                hasMore = true;
                            }else{
                            	hasMore = false;
                            }
                        }
                    }
                    if (wall.size() == 0)
                    	break;
                    index++;
                }
            }else{
                path = prefix + accountHolder + "/friends/" + profileOwner;
                File file = new File(path + "/history.xml");
                boolean exists = file.exists();
                if (!exists){
                    return result;
                }
                byte[] readIn = null;
                while((readIn = readXML(path + "/history.xml")) == null){
                    Thread.sleep(100);
                }
                dom = db.parse(new ByteArrayInputStream(readIn));
                Element docEle = dom.getDocumentElement();
                NodeList nl = docEle.getElementsByTagName("historyLogEntry");
                int index = 0;
                boolean hasMore = true;
                while(wall.size() < entries && hasMore){
                    if(nl != null && nl.getLength() > 0) {
                        if (index < nl.getLength()){
                            Element el = (Element)nl.item(index);
                            historyLogEntry le = new historyLogEntry();
                            le.create(el);
                            loadWall(path + "/wall/" + String.valueOf(le.getStart()) + ".xml");
                            hasMore = true;
                        }else{
                            hasMore = false;
                        }
                    }
                    if (wall.size() == 0)
                    	break;
                    index++;
                }
            }
            if (wall.size() == 0){
                return result;
            }
            for (int i = 0; i < wall.size(); i++){
                timelineEntry tle = (timelineEntry)wall.get(i);
                if(tle.type.equals("link")){
                    link l = new link(tle.l);
                    result += l.generateHTML("complete");
                }
                if(tle.type.equals("wallPost")){
                    wallPost s = new wallPost(tle.s);
                    result += s.generateHTML("complete");
                }
                if(tle.type.equals("audio")){
                    audio a = new audio(tle.a);
                    result += a.generateHTML("complete", null);
                }
                if(tle.type.equals("video")){
                    video v = new video(tle.v);
                    result += v.generateHTML("complete", null);
                }
                if(tle.type.equals("photo")){
                    photo p = new photo(tle.p);
                    result += p.generateHTML("complete", null);
                }
            }
            if (DEBUG){
                System.out.println("DEBUG: line 7927 of profile.java. time passsed in displayWall = " + (Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() - now.getTimeInMillis()) );
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return "";
        }
        return result;
    }
    
    public String displayPhotos(){
        if (profileOwner == null || profileOwner.equals(""))
            return "";
        try{
            String result = "";
            photoAlbums.clear();
            if (profileOwner.equals(accountHolder)){
                parseDocument(prefix + profileOwner + "/photos.xml", "photoAlbum");
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = new photoAlbum(photoAlbums.get(i));
                    result += pa.generateHTML();
                }
            }else{
                parseDocument(prefix + accountHolder + "/friends/" + profileOwner + "/photos.xml", "photoAlbum");
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = new photoAlbum(photoAlbums.get(i));
                    result += pa.generateHTML();
                }
            }
            return result;
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return "";
        }
    }
    
    public String displayPhoto(long id){
        if (profileOwner == null || profileOwner.equals(""))
            return "";
        try{
            String result = "";
            photoAlbums.clear();
            if (profileOwner.equals(accountHolder)){
                parseDocument(prefix + profileOwner + "/photos.xml", "photoAlbum");
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = new photoAlbum(photoAlbums.get(i));
                    List<photo> photoFiles = pa.getPhotoFiles();
                    if (photoFiles == null)
                        continue;
                    for (int j = 0; j < photoFiles.size(); j++){
                        photo p = new photo(photoFiles.get(j));
                        if (p.getId() == id){
                            result += p.generateHTML("complete", "photo");
                            return result;
                        }
                    }
                }
            }else{
                parseDocument(prefix + accountHolder + "/friends/" + profileOwner + "/photos.xml", "photoAlbum");
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = new photoAlbum(photoAlbums.get(i));
                    List<photo> photoFiles = pa.getPhotoFiles();
                    if (photoFiles == null)
                        continue;
                    for (int j = 0; j < photoFiles.size(); j++){
                        photo p = new photo(photoFiles.get(j));
                        if (p.getId() == id){
                            result += p.generateHTML("complete", "photo");
                            return result;
                        }
                    }
                }
            }
            return result;
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return "";
        }
    }
    
    public String displayPhotoAlbum(long id){
        if (profileOwner == null || profileOwner.equals(""))
            return "";
        try{
            String result = "";
            photoAlbums.clear();
            if (profileOwner.equals(accountHolder)){
                parseDocument(prefix + profileOwner + "/photos.xml", "photoAlbum");
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = new photoAlbum(photoAlbums.get(i));
                    if (pa.getId() == id){
                        result += pa.generateHTML();
                    }
                }
            }else{
                parseDocument(prefix + accountHolder + "/friends/" + profileOwner + "/photos.xml", "photoAlbum");
                for (int i = 0; i < photoAlbums.size(); i++){
                    photoAlbum pa = new photoAlbum(photoAlbums.get(i));
                    if (pa.getId() == id){
                        result += pa.generateHTML();
                    }
                }
            }
            return result;
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return "";
        }
    }
    
    public String displayAudios(){
        if (profileOwner == null || profileOwner.equals(""))
            return "";
        try{
            String result = "";
            audioAlbums.clear();
            if (profileOwner.equals(accountHolder)){
                parseDocument(prefix + profileOwner + "/audios.xml", "audioAlbum");
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = new audioAlbum(audioAlbums.get(i));
                    result += aa.generateHTML();
                }
            }else{
                parseDocument(prefix + accountHolder + "/friends/" + profileOwner + "/audios.xml", "audioAlbum");
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = new audioAlbum(audioAlbums.get(i));
                    result += aa.generateHTML();
                }
            }
            return result;
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return "";
        }
    }
    
    public String displayAudio(long id){
        if (profileOwner == null || profileOwner.equals(""))
            return "";
        try{
            String result = "";
            audioAlbums.clear();
            if (profileOwner.equals(accountHolder)){
                parseDocument(prefix + profileOwner + "/audios.xml", "audioAlbum");
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = new audioAlbum(audioAlbums.get(i));
                    List<audio> audioFiles = aa.getAudioFiles();
                    if (audioFiles == null)
                        continue;
                    for (int j = 0; j < audioFiles.size(); j++){
                        audio a = new audio(audioFiles.get(j));
                        if (a.getId() == id){
                            result += a.generateHTML("complete", "audio");
                            return result;
                        }
                    }
                }
            }else{
                parseDocument(prefix + accountHolder + "/friends/" + profileOwner + "/audios.xml", "audioAlbum");
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = new audioAlbum(audioAlbums.get(i));
                    List<audio> audioFiles = aa.getAudioFiles();
                    if (audioFiles == null)
                        continue;
                    for (int j = 0; j < audioFiles.size(); j++){
                        audio a = new audio(audioFiles.get(j));
                        if (a.getId() == id){
                            result += a.generateHTML("complete", "audio");
                            return result;
                        }
                    }
                }
            }
            return result;
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return "";
        }
    }
    
    public String displayAudioAlbum(long id){
        if (profileOwner == null || profileOwner.equals(""))
            return "";
        try{
            String result = "";
            audioAlbums.clear();
            if (profileOwner.equals(accountHolder)){
                parseDocument(prefix + profileOwner + "/audios.xml", "audioAlbum");
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = new audioAlbum(audioAlbums.get(i));
                    if (aa.getId() == id){
                        result += aa.generateHTML();
                    }
                }
            }else{
                parseDocument(prefix + accountHolder + "/friends/" + profileOwner + "/audios.xml", "audioAlbum");
                for (int i = 0; i < audioAlbums.size(); i++){
                    audioAlbum aa = new audioAlbum(audioAlbums.get(i));
                    if (aa.getId() == id){
                        result += aa.generateHTML();
                    }
                }
            }
            return result;
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return "";
        }
    }
    
    public String displayVideos(){
        if (profileOwner == null || profileOwner.equals(""))
            return "";
        try{
            String result = "";
            videoAlbums.clear();
            if (profileOwner.equals(accountHolder)){
                parseDocument(prefix + profileOwner + "/videos.xml", "videoAlbum");
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = new videoAlbum(videoAlbums.get(i));
                    result += va.generateHTML();
                }
            }else{
                parseDocument(prefix + accountHolder + "/friends/" + profileOwner + "/videos.xml", "videoAlbum");
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = new videoAlbum(videoAlbums.get(i));
                    result += va.generateHTML();
                }
            }
            return result;
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return "";
        }
    }
    
    public String displayVideo(long id){
        if (profileOwner == null || profileOwner.equals(""))
            return "";
        try{
            String result = "";
            videoAlbums.clear();
            if (profileOwner.equals(accountHolder)){
                parseDocument(prefix + profileOwner + "/videos.xml", "videoAlbum");
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = new videoAlbum(videoAlbums.get(i));
                    List<video> videoFiles = va.getVideoFiles();
                    if (videoFiles == null)
                        continue;
                    for (int j = 0; j < videoFiles.size(); j++){
                        video v = new video(videoFiles.get(j));
                        if (v.getId() == id){
                            result += v.generateHTML("complete", "video");
                            return result;
                        }
                    }
                }
            }else{
                parseDocument(prefix + accountHolder + "/friends/" + profileOwner + "/videos.xml", "videoAlbum");
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = new videoAlbum(videoAlbums.get(i));
                    List<video> videoFiles = va.getVideoFiles();
                    if (videoFiles == null)
                        continue;
                    for (int j = 0; j < videoFiles.size(); j++){
                        video v = new video(videoFiles.get(j));
                        if (v.getId() == id){
                            result += v.generateHTML("complete", "video");
                            return result;
                        }
                    }
                }
            }
            return result;
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return "";
        }
    }
    
    public String displayVideoAlbum(long id){
        if (profileOwner == null || profileOwner.equals(""))
            return "";
        try{
            String result = "";
            videoAlbums.clear();
            if (profileOwner.equals(accountHolder)){
                parseDocument(prefix + profileOwner + "/videos.xml", "videoAlbum");
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = new videoAlbum(videoAlbums.get(i));
                    if (va.getId() == id){
                        result += va.generateHTML();
                    }
                }
            }else{
                parseDocument(prefix + accountHolder + "/friends/" + profileOwner + "/videos.xml", "videoAlbum");
                for (int i = 0; i < videoAlbums.size(); i++){
                    videoAlbum va = new videoAlbum(videoAlbums.get(i));
                    if (va.getId() == id){
                        result += va.generateHTML();
                    }
                }
            }
            return result;
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return "";
        }
    }
    
    public String displayWallPost(long id, String shareWith){
        if (profileOwner == null || profileOwner.equals(""))
            return "";
        try{
            String result = "";
            String filename = updateHistory(id, shareWith);
            if (filename == null){
                return result;
            }
            wall.clear();
            loadWall(filename);
            for (int i = 0; i < wall.size(); i++){
                timelineEntry tle = new timelineEntry(wall.get(i));
                if (tle.type.equals("wallPost")){
                    if (tle.s.getId() == id){
                        result += tle.s.generateHTML("profile");
                        return result;
                    }
                }
            }
            return result;
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return "";
        }
    }
    
    public String displayLink(long id, String shareWith){
        if (profileOwner == null || profileOwner.equals(""))
            return "";
        try{
            String result = "";
            String filename = updateHistory(id, shareWith);
            if (filename == null){
                return result;
            }
            wall.clear();
            loadWall(filename);
            for (int i = 0; i < wall.size(); i++){
                timelineEntry tle = new timelineEntry(wall.get(i));
                if (tle.type.equals("link")){
                    if (tle.l.getId() == id){
                        result += tle.l.generateHTML("profile");
                        return result;
                    }
                }
            }
            return result;
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return "";
        }
    }
    
    public boolean loadEvents(){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            String path;
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                return false;
            }
            events.clear();
            return parseDocument(path + "/events.xml", "event");
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
    }
    
    // can only be called from the user's own profile.
    public void saveEvents(){
        if (profileOwner == null || profileOwner.equals(""))
            return;
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            dom = db.newDocument();
            
            Element rootEle = dom.createElement("Events");
            dom.appendChild(rootEle);
            
            Iterator it  = events.iterator();
            while(it.hasNext()) {
                event e = new event((event)it.next());
                rootEle.appendChild(e.createDOMElement(dom));
            }
            String path;
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                return; 
            }
            while (!saveXML(path + "/events.xml", dom)){
                Thread.sleep(100);
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    public boolean addEvent(event e, boolean merge){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            if (!profileOwner.equals(accountHolder)){
                return false;
            }
            loadEvents();
            int i = 0;
            event ev = null;
            for (i = 0; i < events.size(); i ++){
                ev = new event(events.get(i));
                if (ev.getId() <= e.getId()){
                    break;
                }
            }
            boolean update = true;
            if (i < events.size()){
                if (ev.getId() == e.getId()){
                    if (ev.getLastUpdateTime() < e.getLastUpdateTime()){
                        if (merge){
                            e.setDecision(ev.getDecision());
                        }
                        events.add(i, e);
                        events.remove(i + 1);
                    }else{
                        update = false;
                    }
                }else{
                    if (e.getCreator().getUsername().equals(accountHolder)){
                        e.setDecision("Attending");
                    }else{
                        e.setDecision("Haven't Decided Yet");
                    }
                    events.add(i, e);
                }
            }else{
                if (e.getCreator().getUsername().equals(accountHolder)){
                    e.setDecision("Attending");
                }else{
                    e.setDecision("Haven't Decided Yet");
                }
                events.add(e);
            }
            saveEvents();
            if (accountHolder.equals(e.getCreator().getUsername()) && update){
                return updatePendingChanges("add", "event", new timelineEntry(e.getId(), "event", e));
            }
        }catch(Exception ex){
            if (DEBUG){
                ex.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean updateEvent(event e){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            if (!profileOwner.equals(accountHolder)){
                return false;
            }
            loadEvents();
            int i = 0;
            event ev = new event();
            for (i = 0; i < events.size(); i ++){
                ev = new event(events.get(i));
                if (ev.getId() == e.getId()){
                    break;
                }
            }
            if (i == events.size()){
                return false;
            }
            List<user> accepted = ev.getAccepted();
            List<user> declined = ev.getDeclined();
            for (int j = 0; j < accepted.size(); j++){
                user v = new user(accepted.get(j));
                boolean keep = false;
                for (int k = 0; k < e.getInvitees().size(); k++){
                    user u = new user((user)e.getInvitees().get(k));
                    if (v.getUsername().equals(u.getUsername())){
                        keep = true;
                        break;
                    }
                }
                if (!keep && !v.getUsername().equals(profileOwner)){
                    accepted.remove(j);
                }
            }
            for (int j = 0; j < declined.size(); j++){
                user v = new user(declined.get(j));
                boolean keep = false;
                for (int k = 0; k < e.getInvitees().size(); k++){
                    user u = new user((user)e.getInvitees().get(k));
                    if (v.getUsername().equals(u.getUsername())){
                        keep = true;
                        break;
                    }
                }
                if (!keep && !v.getUsername().equals(profileOwner)){
                    declined.remove(j);
                }
            }
            e.setAccepted(accepted);
            e.setDeclined(declined);
            events.add(i, e);
            events.remove(i + 1);
            saveEvents();
            if (accountHolder.equals(e.getCreator().getUsername())){
                return updatePendingChanges("update", "event", new timelineEntry(e.getId(), "event", e));
            }
        }catch(Exception ex){
            if (DEBUG){
                ex.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    public boolean removeEvent(long id){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            if (!profileOwner.equals(accountHolder)){
                return false;
            }
            loadEvents();
            for (int i = 0; i < events.size(); i++){
                event e = new event(events.get(i));
                if (e.getId() == id){
                    events.remove(i);
                    saveEvents();
                    if (e.getCreator().getUsername().equals(profileOwner)){
                        return updatePendingChanges("remove", "event", new timelineEntry(e.getId(), "Event", e));
                    }
                    return true;
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public boolean respondToEvent(long id, String decision){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            if (!profileOwner.equals(accountHolder)){
                return false;
            }
            user u = new user(profileOwner, userInfo.getFirstName(), userInfo.getLastName(), 0); 
            loadEvents();
            int i = 0;
            event e = new event();
            for (i = 0; i < events.size(); i ++){
                e = new event(events.get(i));
                if (e.getId() == id){
                    break;
                }
            }
            if (i < events.size()){
                if (decision.equals("Attending")){
                    e.removeDeclined(u.getUsername());
                    e.addAccepted(u);
                    e.setDecision("Attending");
                }else if (decision.equals("Not Attending")){
                    e.removeAccepted(u.getUsername());
                    e.addDeclined(u);
                    e.setDecision("Not Attending");
                }
                events.add(i, e);
                events.remove(i + 1);
            }
            saveEvents();
            return updatePendingChanges("respond", "event", new timelineEntry(e.getId(), "event", e));
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
    }
    
    public boolean loadOutbox(){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            String path;
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                return false;
            }
            sentMsgs.clear();
            return parseDocument(path + "/outbox.xml", "message");
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
    }
    
    public boolean loadInbox(){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            String path;
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                return false;
            }
            receivedMsgs.clear();
            return parseDocument(path + "/inbox.xml", "message");
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
    }
    
    // can only be called from the user's own profile.
    private void saveOutbox(){
        if (profileOwner == null || profileOwner.equals(""))
            return;
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            dom = db.newDocument();
            
            Element rootEle = dom.createElement("SentMessages");
            dom.appendChild(rootEle);
            
            Iterator it  = sentMsgs.iterator();
            while(it.hasNext()) {
                message m = new message((message)it.next());
                rootEle.appendChild(m.createDOMElement(dom));
            }
            String path;
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                return; 
            }
            while (!saveXML(path + "/outbox.xml", dom)){
                Thread.sleep(100);
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    public void saveInbox(){
        if (profileOwner == null || profileOwner.equals(""))
            return;
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            dom = db.newDocument();
            
            Element rootEle = dom.createElement("ReceivedMessages");
            dom.appendChild(rootEle);
            
            Iterator it  = receivedMsgs.iterator();
            while(it.hasNext()) {
                message m = new message((message)it.next());
                rootEle.appendChild(m.createDOMElement(dom));
            }
            String path;
            if (profileOwner.equals(accountHolder)){
                path = prefix + profileOwner;
            }else{
                return; 
            }
            while (!saveXML(path + "/inbox.xml", dom)){
                Thread.sleep(100);
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    public boolean sendMessage(message m){
        if (profileOwner == null || profileOwner.equals(""))
            return false;
        try{
            int i = 0;
            loadOutbox();
            for (i = 0; i < sentMsgs.size(); i++){
                message msg = new message(sentMsgs.get(i));
                if (m.getId() >= msg.getId()){
                    if (m.getId() == msg.getId()){
                        return false; // found duplicate
                    }
                    break;
                }
            }
            if (i < sentMsgs.size()){
                sentMsgs.add(i, m);
            }else{
                sentMsgs.add(m);
            }
            saveOutbox();
            m.setType("received");
            return updatePendingChanges("add", "message", new timelineEntry(m.getId(), "message", m));
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
    }
    
    public boolean hasAccess(String username, String zoneName){
        try{
            File file = new File(prefix + accountHolder + "/zones.xml");
            List<zone> zones = new ArrayList<zone>();
            if (file.exists()){
                byte[] readIn = null;
                while((readIn = readXML(prefix + accountHolder + "/zones.xml")) == null){
                    Thread.sleep(100);
                }
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document dom;
                dom = db.parse(new ByteArrayInputStream(readIn));
                Element docEle = dom.getDocumentElement();
                NodeList zl = docEle.getElementsByTagName("zone");
                if(zl != null && zl.getLength() > 0) {
                    for (int i = 0; i < zl.getLength(); i++){
                        Element zel = (Element)zl.item(i);
                        zone z = new zone();
                        z.create(zel);
                        zones.add(z);
                    }
                }
                zone z = new zone();
                int i = 0;
                for (i = 0; i < zones.size(); i++){
                    z = new zone(zones.get(i));
                    if (z.getName().equals(zoneName)){
                        break;
                    }
                }
                if (i == zones.size()){
                    if (DEBUG){
                        System.out.println("DEBUG: line 8729 of profile.java. hasAccess success");
                    }
                    return false;
                }
                List<user> members = z.getMembers();
                for (i = 0; i < members.size(); i++){
                    user u = new user(members.get(i));
                    if (u.getUsername().equals(username)){
                        if (DEBUG){
                            System.out.println("DEBUG: line 8738 of profile.java. hasAccess success");
                        }
                        return true;
                    }
                }
                if (DEBUG){
                    System.out.println("DEBUG: line 8744 of profile.java. hasAccess failed");
                }
                return false;
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        if (DEBUG){
            System.out.println("DEBUG: line 8754 of profile.java. hasAccess failed");
        }
        return false;
    }
}
