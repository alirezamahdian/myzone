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

package MyZone;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.*;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyPair;

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

import utils.security.KeyPairUtil.*;
import utils.security.CertUtil.*;
import utils.security.CertUtil.userCertificate;
import utils.attributes.*;

import MyZone.elements.*;

/*
 This file contains the implementation of the settings class.
 The setting class contains all the settings information of MyZone application.
 In addition to loading and saving all the settings attributes, settings class is 
 also responsible for all friendship, mirroring, and zone functionalities such as:
 1. sending, receiving, accepting, or declining friendship requests and mirroring requests.
 2. deleting friends, mirrors and profile copies mirrored on this machine.
 3. updating the capacity dedicated to each profile being mirrored on this machine. 
 4. creating, deleting and modifying zones.
 */

public class Settings {
    
    private final static boolean DEBUG = false;
    private globalAttributes globalProperties = new globalAttributes();
    Document dom;
    public String prefix;
    public String username;
    public boolean keyFound;
    public boolean certFound;
    public boolean CAKeyFound;
    public boolean certCorrupted;
    public boolean logUsage;
    public int maxNumberOfChanges;
    public String CAServerName;
    public String rendezvousServerAddress;
    public int rendezvousServerPort;
    public String relayServerAddress;
    public int relayServerPort;
    public String STUNServerAddress;
    public int STUNServerPort;
    public int MyZonePort;
    public int cacheSize;
    public int devPriority;
    public long lastSyncTime1;
    public long lastSyncTime2;
    public long syncPeriod;
    public List<zone> zones = new ArrayList();
    public List<friend> friends = new ArrayList();
    public List<mirror> mirrors = new ArrayList();
    public List<mirror> originals = new ArrayList();
    public List<friend> pendingFriendships = new ArrayList(); 
    public List<friend> awaitingFriendships = new ArrayList(); 
    public List<mirror> sentMirroringRequests = new ArrayList();
    public List<mirror> receivedMirroringRequests = new ArrayList();
    public List<passphraseEntry> passphrases = new ArrayList(); 
    public final int ALL = 0;
    public final int BASIC_INFO = 1;
    public final int ZONES = 2;
    public final int FRIENDS = 3;
    public final int PENDINGFRIENDSHIPS = 4;
    public final int AWAITINGFRIENDSHIPS = 5;
    public final int SENTMIRRORINGREQUESTS = 6;
    public final int RECEIVEDMIRRORINGREQUESTS = 7;
    public final int PASSPHRASES = 8;
    public final int MIRRORS = 9;
    public final int ORIGINALS = 10;
    
    
    
    private String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}
        
		return textVal;
	}
    
    public Settings(String prefix){
        this.prefix = prefix;
        maxNumberOfChanges = 100;
        username = null;
        CAServerName = "MyZone.CA";
        rendezvousServerAddress = "joinmyzone.com";
        rendezvousServerPort = 8080;
        relayServerAddress = "";
        relayServerPort = -1;
        STUNServerAddress = "joinmyzone.com";
        STUNServerPort = 20;
        MyZonePort = 1010;
        cacheSize = 500; //MB
        devPriority = 0;
        keyFound = false;
        certFound = false;
        CAKeyFound = false;
        certCorrupted = false;
        lastSyncTime1 = 0;
        lastSyncTime2 = 0;
        syncPeriod = 900;
        logUsage = false;
    }
    
    public boolean createNewUser(String username){
        globalProperties = new globalAttributes();
        try{
            if (!(new File(prefix + username).exists())){
                boolean success = (new File(prefix + username)).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + username + "/audios").exists())){
                boolean success = (new File(prefix + username + "/audios")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + username + "/videos").exists())){
                boolean success = (new File(prefix + username + "/videos")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + username + "/photos").exists())){
                boolean success = (new File(prefix + username + "/photos")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + username + "/zones").exists())){
                boolean success = (new File(prefix + username + "/zones")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + username + "/zones/All").exists())){
                boolean success = (new File(prefix + username + "/zones/All")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + username + "/zones/All/wall").exists())){
                boolean success = (new File(prefix + username + "/zones/All/wall")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + "/CAs").exists())){
                boolean success = (new File(prefix + "/CAs")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + username + "/cert").exists())){
                boolean success = (new File(prefix + username + "/cert")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + username + "/friends").exists())){
                boolean success = (new File(prefix + username + "/friends")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + username + "/keys").exists())){
                boolean success = (new File(prefix + username + "/keys")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + username + "/thumbnails").exists())){
                boolean success = (new File(prefix + username + "/thumbnails")).mkdirs();
                if (!success)
                    return false;
            }
            File file = new File(prefix + username + "/keys/" + username + ".pub");
            boolean pubKeyExists = file.exists();
            file = new File(prefix + username + "/keys/" + username + ".pri");
            boolean priKeyExists = file.exists();
            KeyPairUtil x = new KeyPairUtil();
            KeyPair keys = null;
            if (!pubKeyExists || !priKeyExists) {
                keys = x.generateKeys(prefix + username + "/keys/", username, globalProperties.keyPairAlgorithm, globalProperties.certificateKeySize);
            }
            if (keys != null)
                keyFound = true;
            certFound = false;
            if (CAServerName != null){
                File caKeyFile = new File(prefix + "/CAs/CA@" + CAServerName + ".pub");
                CAKeyFound = true;
                if (!caKeyFile.exists()){
                    CAKeyFound = false;
                }
            }else{
                CAKeyFound = false;
            }
            certCorrupted = false;
            this.username = username;
            zones.clear();
            friends.clear();
            mirrors.clear();
            originals.clear();
            pendingFriendships.clear();
            awaitingFriendships.clear();
            sentMirroringRequests.clear();
            receivedMirroringRequests.clear();
            passphrases.clear();
            zone z = new zone(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "All", 1800, null);
            zones.add(z);
            lastSyncTime1 = 0;
            lastSyncTime2 = 0;
            save(ALL);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return true;
    }
    
    private byte[] readXMLNoLock(String filename){
        byte[] readIn = null;
        FileChannel channel = null;
        FileLock lock = null;
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = null;
        try{
            File file = new File(filename);
            if (!file.exists()){
                return null;
            }
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            baos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int count = 0;
            long fileLength = file.length();
            while(fileLength > 0){
                count = bis.read(b, 0, 1024);
                if (count >= 0){
                    fileLength -= count;
                    baos.write(b, 0, count);
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
                if (bis != null){
                    bis.close();
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
    
    public synchronized void updateFriend(friend f){
        try{
            refresh(BASIC_INFO);
            refresh(FRIENDS);
            boolean duplicate = false;
            for (int i = 0; i < friends.size(); i++){
                friend fr = new friend(friends.get(i));
                if (fr.getUser().getUsername().equals(f.getUser().getUsername())){
                    if (!duplicate){
                        friends.add(i, f);
                        friends.remove(i + 1);
                    }else{
                        friends.remove(i);
                    }
                    duplicate = true;
                }
            }
            save(FRIENDS);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    public synchronized void updateOriginal(mirror o){
        try{
            refresh(BASIC_INFO);
            refresh(ORIGINALS);
            boolean duplicate = false;
            for (int i = 0; i < originals.size(); i++){
                mirror org = new mirror(originals.get(i));
                if (org.getUsername().equals(o.getUsername())){
                    if (!duplicate){
                        originals.add(i, o);
                        originals.remove(i + 1);
                    }else{
                        originals.remove(i);
                    }
                    duplicate = true;
                }
            }
            save(ORIGINALS);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    public synchronized void updateMirror(mirror m){
        try{
            refresh(BASIC_INFO);
            refresh(MIRRORS);
            boolean duplicate = false;
            for (int i = 0; i < mirrors.size(); i++){
                mirror mi = new mirror(mirrors.get(i));
                if (mi.getUsername().equals(m.getUsername())){
                    if (!duplicate){
                        mirrors.add(i, m);
                        mirrors.remove(i + 1);
                    }else{
                        mirrors.remove(i);
                    }
                    duplicate = true;
                }
                
            }
            save(MIRRORS);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    public boolean refresh(int part){
        byte[] readIn = null;
        File file = null;
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            Element docEle;
            if (part == ALL){
                file = new File(prefix + "settings.xml");
                if (!file.exists()){
                    return false;
                }
                while((readIn = readXML(prefix + "settings.xml")) == null){
                    Thread.sleep(100);
                }
                dom = db.parse(new ByteArrayInputStream(readIn));
                docEle = dom.getDocumentElement();
                username = docEle.getAttribute("username");
                if (username != null){
                    if (!(new File(prefix + username).exists())){
                        username = "";
                    }
                }
                if (!docEle.getAttribute("lastSyncTime1").equals(""))
                    lastSyncTime1 = Long.parseLong(docEle.getAttribute("lastSyncTime1"));
                if (!docEle.getAttribute("lastSyncTime2").equals(""))
                    lastSyncTime2 = Long.parseLong(docEle.getAttribute("lastSyncTime2"));
                if (!docEle.getAttribute("syncPeriod").equals(""))
                    syncPeriod = Long.parseLong(docEle.getAttribute("syncPeriod"));
                if (!docEle.getAttribute("logUsage").equals("")){
                    if (docEle.getAttribute("logUsage").equals("true")){
                        logUsage = true;
                    }else{
                        logUsage = false;
                    }
                }
                if (!docEle.getAttribute("maxNumberOfChanges").equals(""))
                    maxNumberOfChanges = Integer.parseInt(docEle.getAttribute("maxNumberOfChanges"));
                if (!docEle.getAttribute("CAServerName").equals(""))
                    CAServerName = docEle.getAttribute("CAServerName");
                if (!docEle.getAttribute("rendezvousServerAddress").equals(""))
                    rendezvousServerAddress = docEle.getAttribute("rendezvousServerAddress");
                if (!docEle.getAttribute("rendezvousServerPort").equals(""))
                    rendezvousServerPort = Integer.parseInt(docEle.getAttribute("rendezvousServerPort"));
                if (!docEle.getAttribute("relayServerAddress").equals(""))
                    relayServerAddress = docEle.getAttribute("relayServerAddress");
                if (!docEle.getAttribute("relayServerPort").equals(""))
                    relayServerPort = Integer.parseInt(docEle.getAttribute("relayServerPort"));
                if (!docEle.getAttribute("STUNServerAddress").equals(""))
                    STUNServerAddress = docEle.getAttribute("STUNServerAddress");
                if (!docEle.getAttribute("STUNServerPort").equals(""))
                    STUNServerPort = Integer.parseInt(docEle.getAttribute("STUNServerPort"));
                if (!docEle.getAttribute("MyZonePort").equals(""))
                    MyZonePort = Integer.parseInt(docEle.getAttribute("MyZonePort"));
                if (MyZonePort <= 0)
                    MyZonePort = 1010;
                if (!docEle.getAttribute("cacheSize").equals(""))
                    cacheSize = Integer.parseInt(docEle.getAttribute("cacheSize"));
                if (cacheSize < 100)
                    cacheSize = 100;
                if (!docEle.getAttribute("devPriority").equals(""))
                    devPriority = Integer.parseInt(docEle.getAttribute("devPriority"));
                if (devPriority < 0)
                    devPriority = 0;
                File keyFile = new File(prefix + username + "/keys/" + username + ".pri");
                keyFound = true;
                if (!keyFile.exists()){
                    keyFound = false;
                }
                keyFile = new File(prefix + username + "/keys/" + username + ".pub");
                if (!keyFile.exists()){
                    keyFound = false;
                }
                File certFile = new File(prefix + username + "/cert/" + username + ".cert");
                certFound = true;
                if (!certFile.exists()){
                    certFound = false;
                }
                if (certFound){
                    CertVerifier y = new CertVerifier();
                    byte[] certBytes = y.readRawFile(prefix + username + "/cert/", username + ".cert");
                    if (certBytes == null){
                        certCorrupted = true;
                    }else{
                        globalProperties.caCertPath = prefix + "/CAs/";
                        userCertificate clientCert = y.verifyCertificate(certBytes, globalProperties.caCertPath, globalProperties.keyPairAlgorithm, globalProperties.certSigAlgorithm);
                        if (clientCert != null){
                            if (clientCert.username.equals(username)){
                                certCorrupted = false;
                            }else{
                                certCorrupted = true;
                            }
                        }else{
                            certCorrupted = true;
                        }
                    }
                }
                File caKeyFile = new File(prefix + "/CAs/CA@" + CAServerName + ".pub");
                CAKeyFound = true;
                if (!caKeyFile.exists()){
                    CAKeyFound = false;
                }
                file = new File(prefix + username + "/zones.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/zones.xml")) == null){
                        Thread.sleep(100);
                    }
                    zones.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList zl = docEle.getElementsByTagName("zone");
                        if(zl != null && zl.getLength() > 0) {
                            for (int i = 0; i < zl.getLength(); i++){
                                Element zel = (Element)zl.item(i);
                                zone z = new zone();
                                z.create(zel);
                                zones.add(z);
                            }
                        }
                    }
                }
                file = new File(prefix + username + "/friends.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/friends.xml")) == null){
                        Thread.sleep(100);
                    }
                    friends.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList fl = docEle.getElementsByTagName("friend");
                        if(fl != null && fl.getLength() > 0) {
                            for (int i = 0; i < fl.getLength(); i++){
                                Element fel = (Element)fl.item(i);
                                friend f = new friend();
                                f.create(fel);
                                friends.add(f);
                            }
                        }
                    }
                }
                file = new File(prefix + username + "/pendingFriendships.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/pendingFriendships.xml")) == null){
                        Thread.sleep(100);
                    }
                    pendingFriendships.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList fl = docEle.getElementsByTagName("friend");
                        if(fl != null && fl.getLength() > 0) {
                            for (int i = 0; i < fl.getLength(); i++){
                                Element fel = (Element)fl.item(i);
                                friend f = new friend();
                                f.create(fel);
                                pendingFriendships.add(f);
                            }
                        }
                    }
                }
                file = new File(prefix + username + "/awaitingFriendships.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/awaitingFriendships.xml")) == null){
                        Thread.sleep(100);
                    }
                    awaitingFriendships.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList fl = docEle.getElementsByTagName("friend");
                        if(fl != null && fl.getLength() > 0) {
                            for (int i = 0; i < fl.getLength(); i++){
                                Element fel = (Element)fl.item(i);
                                friend f = new friend();
                                f.create(fel);
                                awaitingFriendships.add(f);
                            }
                        }
                    }
                }
                file = new File(prefix + username + "/sentMirroringRequests.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/sentMirroringRequests.xml")) == null){
                        Thread.sleep(100);
                    }
                    sentMirroringRequests.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList ml = docEle.getElementsByTagName("mirror");
                        if(ml != null && ml.getLength() > 0) {
                            for (int i = 0; i < ml.getLength(); i++){
                                Element mel = (Element)ml.item(i);
                                mirror m = new mirror();
                                m.create(mel);
                                sentMirroringRequests.add(m);
                            }
                        }
                    }
                }
                file = new File(prefix + username + "/receivedMirroringRequests.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/receivedMirroringRequests.xml")) == null){
                        Thread.sleep(100);
                    }
                    receivedMirroringRequests.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList ml = docEle.getElementsByTagName("mirror");
                        if(ml != null && ml.getLength() > 0) {
                            for (int i = 0; i < ml.getLength(); i++){
                                Element mel = (Element)ml.item(i);
                                mirror m = new mirror();
                                m.create(mel);
                                receivedMirroringRequests.add(m);
                            }
                        }
                    }
                }
                file = new File(prefix + username + "/passphrases.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/passphrases.xml")) == null){
                        Thread.sleep(100);
                    }
                    passphrases.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList pl = docEle.getElementsByTagName("passphraseEntry");
                        if(pl != null && pl.getLength() > 0) {
                            for (int i = 0; i < pl.getLength(); i++){
                                Element pel = (Element)pl.item(i);
                                passphraseEntry p = new passphraseEntry();
                                p.create(pel);
                                passphrases.add(p);
                            }
                        }
                    }
                }
                file = new File(prefix + username + "/mirrors.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/mirrors.xml")) == null){
                        Thread.sleep(100);
                    }
                    mirrors.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList ml = docEle.getElementsByTagName("mirror");
                        if(ml != null && ml.getLength() > 0) {
                            for (int i = 0; i < ml.getLength(); i++){
                                Element mel = (Element)ml.item(i);
                                mirror m = new mirror();
                                m.create(mel);
                                mirrors.add(m);
                            }
                        }
                    }
                }
                
                file = new File(prefix + username + "/originals.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/originals.xml")) == null){
                        Thread.sleep(100);
                    }
                    originals.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList ol = docEle.getElementsByTagName("mirror");
                        if(ol != null && ol.getLength() > 0) {
                            for (int i = 0; i < ol.getLength(); i++){
                                Element oel = (Element)ol.item(i);
                                mirror o = new mirror();
                                o.create(oel);
                                originals.add(o);
                            }
                        }
                    }
                }
            }else if (part == BASIC_INFO){
                file = new File(prefix + "settings.xml");
                if (!file.exists()){
                    if (DEBUG){
                        System.out.println("DEBUG: line 743 of settings.java. file does not exists: " + file.getName());
                    }
                    return false;
                }
                while((readIn = readXMLNoLock(prefix + "settings.xml")) == null){
                    Thread.sleep(100);
                }
                dom = db.parse(new ByteArrayInputStream(readIn));
                docEle = dom.getDocumentElement();
                username = docEle.getAttribute("username");
                if (username != null){
                    if (!(new File(prefix + username).exists())){
                        username = "";
                    }
                }
                if (!docEle.getAttribute("lastSyncTime1").equals(""))
                    lastSyncTime1 = Long.parseLong(docEle.getAttribute("lastSyncTime1"));
                if (!docEle.getAttribute("lastSyncTime2").equals(""))
                    lastSyncTime2 = Long.parseLong(docEle.getAttribute("lastSyncTime2"));
                if (!docEle.getAttribute("syncPeriod").equals(""))
                    syncPeriod = Long.parseLong(docEle.getAttribute("syncPeriod"));
                if (!docEle.getAttribute("logUsage").equals("")){
                    if (docEle.getAttribute("logUsage").equals("true")){
                        logUsage = true;
                    }else{
                        logUsage = false;
                    }
                }
                if (!docEle.getAttribute("maxNumberOfChanges").equals(""))
                    maxNumberOfChanges = Integer.parseInt(docEle.getAttribute("maxNumberOfChanges"));
                if (!docEle.getAttribute("CAServerName").equals(""))
                    CAServerName = docEle.getAttribute("CAServerName");
                if (!docEle.getAttribute("rendezvousServerAddress").equals(""))
                    rendezvousServerAddress = docEle.getAttribute("rendezvousServerAddress");
                if (!docEle.getAttribute("rendezvousServerPort").equals(""))
                    rendezvousServerPort = Integer.parseInt(docEle.getAttribute("rendezvousServerPort"));
                if (!docEle.getAttribute("relayServerAddress").equals(""))
                    relayServerAddress = docEle.getAttribute("relayServerAddress");
                if (!docEle.getAttribute("relayServerPort").equals(""))
                    relayServerPort = Integer.parseInt(docEle.getAttribute("relayServerPort"));
                if (!docEle.getAttribute("STUNServerAddress").equals(""))
                    STUNServerAddress = docEle.getAttribute("STUNServerAddress");
                if (!docEle.getAttribute("STUNServerPort").equals(""))
                    STUNServerPort = Integer.parseInt(docEle.getAttribute("STUNServerPort"));
                if (!docEle.getAttribute("MyZonePort").equals(""))
                    MyZonePort = Integer.parseInt(docEle.getAttribute("MyZonePort"));
                if (MyZonePort <= 0)
                    MyZonePort = 1010;
                if (!docEle.getAttribute("cacheSize").equals(""))
                    cacheSize = Integer.parseInt(docEle.getAttribute("cacheSize"));
                if (cacheSize < 100)
                    cacheSize = 100;
                if (!docEle.getAttribute("devPriority").equals(""))
                    devPriority = Integer.parseInt(docEle.getAttribute("devPriority"));
                if (devPriority < 0)
                    devPriority = 0;
                File keyFile = new File(prefix + username + "/keys/" + username + ".pri");
                keyFound = true;
                if (!keyFile.exists()){
                    keyFound = false;
                }
                keyFile = new File(prefix + username + "/keys/" + username + ".pub");
                if (!keyFile.exists()){
                    keyFound = false;
                }
                File certFile = new File(prefix + username + "/cert/" + username + ".cert");
                certFound = true;
                if (!certFile.exists()){
                    certFound = false;
                }
                if (certFound){
                    CertVerifier y = new CertVerifier();
                    byte[] certBytes = y.readRawFile(prefix + username + "/cert/", username + ".cert");
                    if (certBytes == null){
                        certCorrupted = true;
                    }else{
                        globalProperties.caCertPath = prefix + "/CAs/";
                        userCertificate clientCert = y.verifyCertificate(certBytes, globalProperties.caCertPath, globalProperties.keyPairAlgorithm, globalProperties.certSigAlgorithm);
                        if (clientCert != null){
                            if (clientCert.username.equals(username)){
                                certCorrupted = false;
                            }else{
                                certCorrupted = true;
                            }
                        }else{
                            certCorrupted = true;
                        }
                    }
                }
                File caKeyFile = new File(prefix + "/CAs/CA@" + CAServerName + ".pub");
                CAKeyFound = true;
                if (!caKeyFile.exists()){
                    CAKeyFound = false;
                }
            }else if (part == ZONES){
                file = new File(prefix + username + "/zones.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/zones.xml")) == null){
                        Thread.sleep(100);
                    }
                    zones.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList zl = docEle.getElementsByTagName("zone");
                        if(zl != null && zl.getLength() > 0) {
                            for (int i = 0; i < zl.getLength(); i++){
                                Element zel = (Element)zl.item(i);
                                zone z = new zone();
                                z.create(zel);
                                zones.add(z);
                            }
                        }
                    }
                }
            }else if (part == FRIENDS){
                file = new File(prefix + username + "/friends.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/friends.xml")) == null){
                        Thread.sleep(100);
                    }
                    friends.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList fl = docEle.getElementsByTagName("friend");
                        if(fl != null && fl.getLength() > 0) {
                            for (int i = 0; i < fl.getLength(); i++){
                                Element fel = (Element)fl.item(i);
                                friend f = new friend();
                                f.create(fel);
                                friends.add(f);
                            }
                        }
                    }
                }
            }else if (part == PENDINGFRIENDSHIPS){
                file = new File(prefix + username + "/pendingFriendships.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/pendingFriendships.xml")) == null){
                        Thread.sleep(100);
                    }
                    pendingFriendships.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList fl = docEle.getElementsByTagName("friend");
                        if(fl != null && fl.getLength() > 0) {
                            for (int i = 0; i < fl.getLength(); i++){
                                Element fel = (Element)fl.item(i);
                                friend f = new friend();
                                f.create(fel);
                                pendingFriendships.add(f);
                            }
                        }
                    }
                }
            }else if (part == AWAITINGFRIENDSHIPS){
                file = new File(prefix + username + "/awaitingFriendships.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/awaitingFriendships.xml")) == null){
                        Thread.sleep(100);
                    }
                    awaitingFriendships.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList fl = docEle.getElementsByTagName("friend");
                        if(fl != null && fl.getLength() > 0) {
                            for (int i = 0; i < fl.getLength(); i++){
                                Element fel = (Element)fl.item(i);
                                friend f = new friend();
                                f.create(fel);
                                awaitingFriendships.add(f);
                            }
                        }
                    }
                }
            }else if (part == SENTMIRRORINGREQUESTS){
                file = new File(prefix + username + "/sentMirroringRequests.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/sentMirroringRequests.xml")) == null){
                        Thread.sleep(100);
                    }
                    sentMirroringRequests.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList ml = docEle.getElementsByTagName("mirror");
                        if(ml != null && ml.getLength() > 0) {
                            for (int i = 0; i < ml.getLength(); i++){
                                Element mel = (Element)ml.item(i);
                                mirror m = new mirror();
                                m.create(mel);
                                sentMirroringRequests.add(m);
                            }
                        }
                    }
                }
            }else if (part == RECEIVEDMIRRORINGREQUESTS){
                file = new File(prefix + username + "/receivedMirroringRequests.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/receivedMirroringRequests.xml")) == null){
                        Thread.sleep(100);
                    }
                    receivedMirroringRequests.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList ml = docEle.getElementsByTagName("mirror");
                        if(ml != null && ml.getLength() > 0) {
                            for (int i = 0; i < ml.getLength(); i++){
                                Element mel = (Element)ml.item(i);
                                mirror m = new mirror();
                                m.create(mel);
                                receivedMirroringRequests.add(m);
                            }
                        }
                    }
                }
            }else if (part == PASSPHRASES){
                file = new File(prefix + username + "/passphrases.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/passphrases.xml")) == null){
                        Thread.sleep(100);
                    }
                    passphrases.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList pl = docEle.getElementsByTagName("passphraseEntry");
                        if(pl != null && pl.getLength() > 0) {
                            for (int i = 0; i < pl.getLength(); i++){
                                Element pel = (Element)pl.item(i);
                                passphraseEntry p = new passphraseEntry();
                                p.create(pel);
                                passphrases.add(p);
                            }
                        }
                    }
                }
            }else if (part == MIRRORS){
                file = new File(prefix + username + "/mirrors.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/mirrors.xml")) == null){
                        Thread.sleep(100);
                    }
                    mirrors.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList ml = docEle.getElementsByTagName("mirror");
                        if(ml != null && ml.getLength() > 0) {
                            for (int i = 0; i < ml.getLength(); i++){
                                Element mel = (Element)ml.item(i);
                                mirror m = new mirror();
                                m.create(mel);
                                mirrors.add(m);
                            }
                        }
                    }
                }
            }else if (part == ORIGINALS){
                file = new File(prefix + username + "/originals.xml");
                if (file.exists()){
                    while((readIn = readXML(prefix + username + "/originals.xml")) == null){
                        Thread.sleep(100);
                    }
                    originals.clear();
                    if (readIn.length > 0){
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        docEle = dom.getDocumentElement();
                        NodeList ol = docEle.getElementsByTagName("mirror");
                        if(ol != null && ol.getLength() > 0) {
                            for (int i = 0; i < ol.getLength(); i++){
                                Element oel = (Element)ol.item(i);
                                mirror o = new mirror();
                                o.create(oel);
                                originals.add(o);
                            }
                        }
                    }
                }
            }
            return true;
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
    }
    
    private boolean saveXML(String filename, Document dom){
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
    
    public boolean isFriend(String username){
        try{
            refresh(BASIC_INFO);
            refresh(FRIENDS);
            if (friends != null){
                for (int i = 0; i < friends.size(); i++){
                    friend f = new friend(friends.get(i));
                    if (username.equals(f.getUser().getUsername())){
                        return true;
                    }
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public long isMirroringFor(String username){
        try{
            refresh(BASIC_INFO);
            refresh(ORIGINALS);
            if (originals != null){
                for (int i = 0; i < originals.size(); i++){
                    mirror m = new mirror(originals.get(i));
                    if (username.equals(m.getUsername())){
                        return m.getLastSyncTime();
                    }
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return -1;
    }
    
    public boolean isMirrored(String username){
        try{
            refresh(BASIC_INFO);
            refresh(MIRRORS);
            if (mirrors != null){
                for (int i = 0; i < mirrors.size(); i++){
                    mirror m = new mirror(mirrors.get(i));
                    if (username.equals(m.getUsername())){
                        return true;
                    }
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public void save(int part){
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            Element rootEle;
            
            if (part == ALL){
                dom = db.newDocument();
                rootEle = dom.createElement("Settings");
                dom.appendChild(rootEle);
                rootEle.setAttribute("username", username);
                rootEle.setAttribute("maxNumberOfChanges", String.valueOf(maxNumberOfChanges));
                rootEle.setAttribute("lastSyncTime1", String.valueOf(lastSyncTime1));
                rootEle.setAttribute("lastSyncTime2", String.valueOf(lastSyncTime2));
                rootEle.setAttribute("syncPeriod", String.valueOf(syncPeriod));
                if (logUsage){
                    rootEle.setAttribute("logUsage", "true");
                }else{
                    rootEle.setAttribute("logUsage", "false");
                }
                rootEle.setAttribute("CAServerName", CAServerName);
                rootEle.setAttribute("rendezvousServerAddress", rendezvousServerAddress);
                rootEle.setAttribute("rendezvousServerPort", String.valueOf(rendezvousServerPort));
                rootEle.setAttribute("relayServerAddress", relayServerAddress);
                rootEle.setAttribute("relayServerPort", String.valueOf(relayServerPort));
                rootEle.setAttribute("STUNServerAddress", STUNServerAddress);
                rootEle.setAttribute("STUNServerPort", String.valueOf(STUNServerPort));
                rootEle.setAttribute("MyZonePort", String.valueOf(MyZonePort));
                rootEle.setAttribute("cacheSize", String.valueOf(cacheSize));
                rootEle.setAttribute("devPriority", String.valueOf(devPriority));
                while (!saveXML(prefix + "settings.xml", dom)){
                    Thread.sleep(100);
                    
                }
                dom = db.newDocument();
                rootEle = dom.createElement("zones");
                dom.appendChild(rootEle);
                for (int i = 0; i < zones.size(); i++){
                    zone z = new zone(zones.get(i));
                    rootEle.appendChild(z.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/zones.xml", dom)){
                    Thread.sleep(100);
                }
                dom = db.newDocument();
                rootEle = dom.createElement("friends");
                dom.appendChild(rootEle);
                for (int i = 0; i < friends.size(); i++){
                    friend f = new friend(friends.get(i));
                    rootEle.appendChild(f.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/friends.xml", dom)){
                    Thread.sleep(100);
                }
                dom = db.newDocument();
                rootEle = dom.createElement("pendingFriendships");
                dom.appendChild(rootEle);
                for (int i = 0; i < pendingFriendships.size(); i++){
                    friend f = new friend(pendingFriendships.get(i));
                    rootEle.appendChild(f.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/pendingFriendships.xml", dom)){
                    Thread.sleep(100);
                }
                dom = db.newDocument();
                rootEle = dom.createElement("awaitingFriendships");
                dom.appendChild(rootEle);
                for (int i = 0; i < awaitingFriendships.size(); i++){
                    friend f = new friend(awaitingFriendships.get(i));
                    rootEle.appendChild(f.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/awaitingFriendships.xml", dom)){
                    Thread.sleep(100);
                }
                dom = db.newDocument();
                rootEle = dom.createElement("mirrors");
                dom.appendChild(rootEle);
                for (int i = 0; i < mirrors.size(); i++){
                    mirror m = new mirror(mirrors.get(i));
                    if (isFriend(m.getUsername()))
                        rootEle.appendChild(m.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/mirrors.xml", dom)){
                    Thread.sleep(100);
                }
                dom = db.newDocument();
                rootEle = dom.createElement("mirrorings");
                dom.appendChild(rootEle);
                for (int i = 0; i < originals.size(); i++){
                    mirror m = new mirror(originals.get(i));
                    if (isFriend(m.getUsername()))
                        rootEle.appendChild(m.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/originals.xml", dom)){
                    Thread.sleep(100);
                }
                dom = db.newDocument();
                rootEle = dom.createElement("sentMirroringRequests");
                dom.appendChild(rootEle);
                for (int i = 0; i < sentMirroringRequests.size(); i++){
                    mirror m = new mirror(sentMirroringRequests.get(i));
                    if (isFriend(m.getUsername()))
                        rootEle.appendChild(m.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/sentMirroringRequests.xml", dom)){
                    Thread.sleep(100);
                }
                dom = db.newDocument();
                rootEle = dom.createElement("receivedMirroringRequests");
                dom.appendChild(rootEle);
                for (int i = 0; i < receivedMirroringRequests.size(); i++){
                    mirror m = new mirror(receivedMirroringRequests.get(i));
                    if (isFriend(m.getUsername()))
                        rootEle.appendChild(m.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/receivedMirroringRequests.xml", dom)){
                    Thread.sleep(100);
                }
                dom = db.newDocument();
                rootEle = dom.createElement("passphrases");
                dom.appendChild(rootEle);
                for (int i = 0; i < passphrases.size(); i++){
                    passphraseEntry p = new passphraseEntry(passphrases.get(i));
                    rootEle.appendChild(p.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/passphrases.xml", dom)){
                    Thread.sleep(100);
                }
            }else if (part == BASIC_INFO){
                dom = db.newDocument();
                rootEle = dom.createElement("Settings");
                dom.appendChild(rootEle);
                rootEle.setAttribute("username", username);
                rootEle.setAttribute("maxNumberOfChanges", String.valueOf(maxNumberOfChanges));
                rootEle.setAttribute("lastSyncTime1", String.valueOf(lastSyncTime1));
                rootEle.setAttribute("lastSyncTime2", String.valueOf(lastSyncTime2));
                rootEle.setAttribute("syncPeriod", String.valueOf(syncPeriod));
                if (logUsage){
                    rootEle.setAttribute("logUsage", "true");
                }else{
                    rootEle.setAttribute("logUsage", "false");
                }
                rootEle.setAttribute("CAServerName", CAServerName);
                rootEle.setAttribute("rendezvousServerAddress", rendezvousServerAddress);
                rootEle.setAttribute("rendezvousServerPort", String.valueOf(rendezvousServerPort));
                rootEle.setAttribute("relayServerAddress", relayServerAddress);
                rootEle.setAttribute("relayServerPort", String.valueOf(relayServerPort));
                rootEle.setAttribute("STUNServerAddress", STUNServerAddress);
                rootEle.setAttribute("STUNServerPort", String.valueOf(STUNServerPort));
                rootEle.setAttribute("MyZonePort", String.valueOf(MyZonePort));
                rootEle.setAttribute("cacheSize", String.valueOf(cacheSize));
                rootEle.setAttribute("devPriority", String.valueOf(devPriority));
                while (!saveXML(prefix + "settings.xml", dom)){
                    Thread.sleep(100);
                }
            }else if (part == ZONES){
                dom = db.newDocument();
                rootEle = dom.createElement("zones");
                dom.appendChild(rootEle);
                for (int i = 0; i < zones.size(); i++){
                    zone z = new zone(zones.get(i));
                    rootEle.appendChild(z.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/zones.xml", dom)){
                    Thread.sleep(100);
                }
            }else if (part == FRIENDS){
                dom = db.newDocument();
                rootEle = dom.createElement("friends");
                dom.appendChild(rootEle);
                for (int i = 0; i < friends.size(); i++){
                    friend f = new friend(friends.get(i));
                    rootEle.appendChild(f.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/friends.xml", dom)){
                    Thread.sleep(100);
                }
            }else if (part == PENDINGFRIENDSHIPS){
                dom = db.newDocument();
                rootEle = dom.createElement("pendingFriendships");
                dom.appendChild(rootEle);
                for (int i = 0; i < pendingFriendships.size(); i++){
                    friend f = new friend(pendingFriendships.get(i));
                    rootEle.appendChild(f.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/pendingFriendships.xml", dom)){
                    Thread.sleep(100);
                }
            }else if (part == AWAITINGFRIENDSHIPS){
                dom = db.newDocument();
                rootEle = dom.createElement("awaitingFriendships");
                dom.appendChild(rootEle);
                for (int i = 0; i < awaitingFriendships.size(); i++){
                    friend f = new friend(awaitingFriendships.get(i));
                    rootEle.appendChild(f.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/awaitingFriendships.xml", dom)){
                    Thread.sleep(100);
                }
            }else if (part == SENTMIRRORINGREQUESTS){
                dom = db.newDocument();
                rootEle = dom.createElement("sentMirroringRequests");
                dom.appendChild(rootEle);
                for (int i = 0; i < sentMirroringRequests.size(); i++){
                    mirror m = new mirror(sentMirroringRequests.get(i));
                    if (isFriend(m.getUsername()))
                        rootEle.appendChild(m.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/sentMirroringRequests.xml", dom)){
                    Thread.sleep(100);
                }
            }else if (part == RECEIVEDMIRRORINGREQUESTS){
                dom = db.newDocument();
                rootEle = dom.createElement("receivedMirroringRequests");
                dom.appendChild(rootEle);
                for (int i = 0; i < receivedMirroringRequests.size(); i++){
                    mirror m = new mirror(receivedMirroringRequests.get(i));
                    if (isFriend(m.getUsername()))
                        rootEle.appendChild(m.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/receivedMirroringRequests.xml", dom)){
                    Thread.sleep(100);
                }
            }else if (part == PASSPHRASES){
                dom = db.newDocument();
                rootEle = dom.createElement("passphrases");
                dom.appendChild(rootEle);
                for (int i = 0; i < passphrases.size(); i++){
                    passphraseEntry p = new passphraseEntry(passphrases.get(i));
                    rootEle.appendChild(p.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/passphrases.xml", dom)){
                    Thread.sleep(100);
                }
            }else if (part == MIRRORS){
                dom = db.newDocument();
                rootEle = dom.createElement("mirrors");
                dom.appendChild(rootEle);
                for (int i = 0; i < mirrors.size(); i++){
                    mirror m = new mirror(mirrors.get(i));
                    if (isFriend(m.getUsername()))
                        rootEle.appendChild(m.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/mirrors.xml", dom)){
                    Thread.sleep(100);
                }
            }else if (part == ORIGINALS){
                dom = db.newDocument();
                rootEle = dom.createElement("mirrorings");
                dom.appendChild(rootEle);
                for (int i = 0; i < originals.size(); i++){
                    mirror m = new mirror(originals.get(i));
                    if (isFriend(m.getUsername()))
                        rootEle.appendChild(m.createDOMElement(dom));
                }
                while (!saveXML(prefix + username + "/originals.xml", dom)){
                    Thread.sleep(100);
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    public void setLastSyncTime(int i, long value){
        if (i == 1){
            lastSyncTime1 = value;
        }else{
            lastSyncTime2 = value;
        }
        save(BASIC_INFO);
    }
    
    public long getLastSyncTime(int i){
        if (i == 1){
            return lastSyncTime1;
        }
        return lastSyncTime2;
    }
    
    private byte[] hashMessage(byte[] data, String mdAlgorithm){ 
        try{
            MessageDigest md;
            Security.addProvider(new BouncyCastleProvider());
            md = MessageDigest.getInstance(mdAlgorithm, "BC");
            byte[] hash = null;
            md.update(data, 0, data.length);
            hash = md.digest();
            return hash;
        }catch(NoSuchAlgorithmException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(java.security.NoSuchProviderException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
    }
    
    private byte[] encryptWithPriKey(byte[] plain, String keyAlgorithm, PrivateKey priKey){
        try{
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance(keyAlgorithm, "BC");
            cipher.init(Cipher.ENCRYPT_MODE, priKey);
            int msgLength = plain.length;
            int chunks = (int)(Math.ceil((double)msgLength/(double)globalProperties.plainBlockSize));
            byte[] encryptedMsg = new byte[(int)(globalProperties.certificateKeySize/8) * chunks ];
            byte[] encryptedChunk = new byte[globalProperties.plainBlockSize];
            byte[] plainChunk = new byte[globalProperties.plainBlockSize];
            int offset = 0;
            int i = 0;
            while(msgLength > plainChunk.length){
                System.arraycopy(plain, offset, plainChunk, 0, plainChunk.length);
                offset += plainChunk.length;
                msgLength -= plainChunk.length;
                encryptedChunk = cipher.doFinal(plainChunk);
                System.arraycopy(encryptedChunk, 0, encryptedMsg, i * encryptedChunk.length, encryptedChunk.length);
                i++;
            }
            if (msgLength > 0){
                plainChunk = new byte[msgLength];
                System.arraycopy(plain, offset, plainChunk, 0, msgLength);
                encryptedChunk = cipher.doFinal(plainChunk);
                System.arraycopy(encryptedChunk, 0, encryptedMsg, i * encryptedChunk.length, encryptedChunk.length);
            }
            return encryptedMsg;
        }catch(NoSuchAlgorithmException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(NoSuchPaddingException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(InvalidKeyException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(IllegalBlockSizeException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(BadPaddingException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(java.security.NoSuchProviderException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
    }
    
    public boolean sendFriendshipRequest(String username){
        refresh(BASIC_INFO);
        refresh(FRIENDS);
        refresh(PENDINGFRIENDSHIPS);
        refresh(AWAITINGFRIENDSHIPS);
        refresh(PASSPHRASES);
        try{
            boolean duplicate = false;
            for (int i = 0; i < friends.size(); i++){
                friend f = new friend(friends.get(i));
                if (f.getUser().getUsername().equals(username)){
                    if (pendingFriendships.size() > 0){
                        pendingFriendships.add(0, f);
                    }else{
                        pendingFriendships.add(f);
                    }
                    save(PENDINGFRIENDSHIPS);
                    save(PASSPHRASES);
                    return true;
                }
            }
            for (int i = 0; i < awaitingFriendships.size(); i++){
                friend f = new friend(awaitingFriendships.get(i));
                if (f.getUser().getUsername().equals(username)){
                    return false;
                }
            }
            for (int i = 0; i < pendingFriendships.size(); i++){
                friend f = new friend(pendingFriendships.get(i));
                if (f.getUser().getUsername().equals(username)){
                    return false;
                }
            }
            KeyPairUtil x = new KeyPairUtil();
            PrivateKey priKey;
            priKey = x.readPriKey(prefix + this.username + "/keys/", this.username, globalProperties.keyPairAlgorithm);
            String passphrase = String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis());
            byte[] passphraseBytes = encryptWithPriKey(passphrase.getBytes("UTF-8"), globalProperties.asymCipher, priKey);
            boolean passphraseExists = false;
            passphraseEntry pe = new passphraseEntry();
            for (int i = 0; i < passphrases.size(); i++){
                pe = new passphraseEntry(passphrases.get(i));
                if (pe.getUsername().equals(username)){
                    passphraseExists = true;
                    break;
                }
            }
            if (!passphraseExists){
                passphrase = new sun.misc.BASE64Encoder().encode(hashMessage(passphraseBytes, globalProperties.messageDigestAlgorithm));
                passphraseEntry p = new passphraseEntry(passphrase, username);
                passphrases.add(p);
            }else{
                passphrase = pe.getPassphrase();
            }
            user u = new user(username, null, null, 0);
            friend newFriend = new friend(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), passphrase, u, null, null, 0, null, 0, 0, 0);
            if (pendingFriendships.size() > 0){
                pendingFriendships.add(0, newFriend);
            }else{
                pendingFriendships.add(newFriend);
            }
            save(PENDINGFRIENDSHIPS);
            save(PASSPHRASES);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return true;
    }
    
    public boolean receiveFriendshipRequest(String username, String passphrase){
        refresh(BASIC_INFO);
        refresh(FRIENDS);
        refresh(AWAITINGFRIENDSHIPS);
        refresh(PENDINGFRIENDSHIPS);
        refresh(PASSPHRASES);
        try{
            user u = new user(username, null, null, 0);
            boolean duplicate = false;
            for (int i = 0; i < friends.size(); i++){
                friend f = new friend(friends.get(i));
                if (f.getUser().getUsername().equals(username)){
                    passphraseEntry pe = new passphraseEntry();
                    int j = 0;
                    for (j = 0; j < passphrases.size(); j++){
                        pe = new passphraseEntry(passphrases.get(j));
                        if (pe.getUsername().equals(username)){
                            break;
                        }
                    }
                    if (j < passphrases.size()){
                        pe.setPassphrase(passphrase);
                        passphrases.add(j, pe);
                        passphrases.remove(j + 1);
                        save(PASSPHRASES);
                    }else{
                        return false;
                    }
                    f.setPassphrase(passphrase);
                    friends.add(i, f);
                    friends.remove(i + 1);
                    save(FRIENDS);
                    return acceptFriendshipRequest(username, false);
                }
            }
            for (int i = 0; i < awaitingFriendships.size(); i++){
                friend f = new friend(awaitingFriendships.get(i));
                if (f.getUser().getUsername().equals(username)){
                    f = new friend(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), passphrase, u, null, null, 0, null, 0, 0, 0);
                    awaitingFriendships.add(i, f);
                    awaitingFriendships.remove(i + 1);
                    save(AWAITINGFRIENDSHIPS);
                    return false;
                }
            }
            friend f = new friend(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), passphrase, u, null, null, 0, null, 0, 0, 0);
            if (awaitingFriendships.size() > 0){
                awaitingFriendships.add(0, f); 
            }else{
                awaitingFriendships.add(f);
            }
            save(AWAITINGFRIENDSHIPS);
            for (int i = 0; i < pendingFriendships.size(); i++){
                f = new friend(pendingFriendships.get(i));
                if (f.getUser().getUsername().equals(username)){
                    deletePendingFriendshipRequest(username);
                    return acceptFriendshipRequest(username, true);
                }
            }
            return true;
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return true;
    }
    
    public boolean acceptFriendshipRequest(String username, boolean initiated){
        refresh(BASIC_INFO);
        refresh(FRIENDS);
        refresh(PASSPHRASES);
        refresh(AWAITINGFRIENDSHIPS);
        refresh(ZONES);
        try{
            user u = new user(username, null, null, 0);
            boolean duplicate = false;
            for (int i = 0; i < friends.size(); i++){
                friend f = new friend(friends.get(i));
                if (f.getUser().getUsername().equals(username)){
                    return sendFriendshipRequest(username);
                }
            }
            boolean found = false;
            friend f = new friend();
            for (int i = 0; i < awaitingFriendships.size(); i++){
                f = new friend(awaitingFriendships.get(i));
                if (f.getUser().getUsername().equals(username)){
                    awaitingFriendships.remove(i);
                    boolean passphraseExists = false;
                    passphraseEntry pe = new passphraseEntry();
                    for (i = 0; i < passphrases.size(); i++){
                        pe = new passphraseEntry(passphrases.get(i));
                        if (pe.getUsername().equals(username)){
                            passphraseExists = true;
                            break;
                        }
                    }
                    if (!passphraseExists){
                        passphrases.add(new passphraseEntry(f.getPassphrase(), username));
                    }
                    found = true;
                    break;
                }
            }
            if (!found){
                return false;
            }
            save(PASSPHRASES);
            save(AWAITINGFRIENDSHIPS);
            if (!initiated){
                sendFriendshipRequest(username);
            }
            f.setLatestVersion(0);
            friends.add(f);
            int i = 0;
            zone z = new zone();
            for (i = 0; i < zones.size(); i++){
                z = new zone(zones.get(i));
                if (z.getName().equals("All")){
                    break;
                }
            }
            if (i < zones.size()){
                z.addMember(f.getUser());
                zones.add(i, z);
                zones.remove(i + 1);
            }
            boolean passphraseExists = false;
            passphraseEntry pe = new passphraseEntry();
            for (i = 0; i < passphrases.size(); i++){
                pe = new passphraseEntry(passphrases.get(i));
                if (pe.getUsername().equals(username)){
                    passphraseExists = true;
                    break;
                }
            }
            if (!passphraseExists){
                passphrases.add(new passphraseEntry(f.getPassphrase(), username));
            }
            save(ZONES);
            save(FRIENDS);
            save(PASSPHRASES);
            if (!(new File(prefix + this.username + "/friends/" + username).exists())){
                boolean success = (new File(prefix + this.username + "/friends/" + username)).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + this.username + "/friends/" + username + "/audios").exists())){
                boolean success = (new File(prefix + this.username + "/friends/" + username + "/audios")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + this.username + "/friends/" + username + "/videos").exists())){
                boolean success = (new File(prefix + this.username + "/friends/" + username + "/videos")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + this.username + "/friends/" + username + "/photos").exists())){
                boolean success = (new File(prefix + this.username + "/friends/" + username + "/photos")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + this.username + "/friends/" + username + "/wall").exists())){
                boolean success = (new File(prefix + this.username + "/friends/" + username + "/wall")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + this.username + "/friends/" + username + "/cert").exists())){
                boolean success = (new File(prefix + this.username + "/friends/" + username + "/cert")).mkdirs();
                if (!success)
                    return false;
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return true;
    }
    
    public boolean deletePendingFriendshipRequest(String username){
        refresh(BASIC_INFO);
        refresh(PENDINGFRIENDSHIPS);
        refresh(PASSPHRASES);
        try{
            for (int i = 0; i < pendingFriendships.size(); i++){
                friend f = new friend(pendingFriendships.get(i));
                if (f.getUser().getUsername().equals(username)){
                    pendingFriendships.remove(i);
                }
            }
            for (int i = 0; i < passphrases.size(); i++){
                passphraseEntry pe = new passphraseEntry(passphrases.get(i));
                if (pe.getUsername().equals(username)){
                    passphrases.remove(i);
                }
            }
            save(PENDINGFRIENDSHIPS);
            save(PASSPHRASES);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return true;
    }
    
    public boolean deleteAwaitingFriendshipRequest(String username){
        refresh(BASIC_INFO);
        refresh(AWAITINGFRIENDSHIPS);
        refresh(PASSPHRASES);
        try{
            for (int i = 0; i < awaitingFriendships.size(); i++){
                friend f = new friend(awaitingFriendships.get(i));
                if (f.getUser().getUsername().equals(username)){
                    awaitingFriendships.remove(i);
                }
            }
            for (int i = 0; i < passphrases.size(); i++){
                passphraseEntry pe = new passphraseEntry(passphrases.get(i));
                if (pe.getUsername().equals(username)){
                    passphrases.remove(i);
                }
            }
            save(AWAITINGFRIENDSHIPS);
            save(PASSPHRASES);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return true;
    }
    
    public boolean sendMirrorRequest(String username){
        refresh(BASIC_INFO);
        refresh(SENTMIRRORINGREQUESTS);
        refresh(MIRRORS);
        refresh(FRIENDS);
        refresh(PASSPHRASES);
        try{
            for (int i = 0; i < sentMirroringRequests.size(); i++){
                mirror m = new mirror(sentMirroringRequests.get(i));
                if (m.getUsername().equals(username)){
                    return false;
                }
            }
            for (int i = 0; i < mirrors.size(); i++){
                mirror m = new mirror(mirrors.get(i));
                if (m.getUsername().equals(username)){
                    return false;
                }
            }
            String passphrase = "";
            for (int i = 0; i < friends.size(); i++){
                friend f = new friend(friends.get(i));
                if (f.getUser().getUsername().equals(username)){
                    passphrase = f.getPassphrase();
                    break;
                }
            }
            if (passphrase.equals(""))
                return false;
            for (int i = 0; i < passphrases.size(); i++){
                passphraseEntry pe = new passphraseEntry(passphrases.get(i));
                if (pe.getUsername().equals(username)){
                    passphrase = pe.getPassphrase();
                    break;
                }
            }
            if (passphrase.equals(""))
                return false;
            mirror newMirror = new mirror(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), username, passphrase, 0, 0, mirrors.size(), 0, 0);
            if (sentMirroringRequests.size() > 0){
                sentMirroringRequests.add(0, newMirror);
            }else{
                sentMirroringRequests.add(newMirror);
            }
            save(SENTMIRRORINGREQUESTS);
            profile userProfile = new profile(this.username, this.username, prefix);
            timelineEntry tle = new timelineEntry(newMirror.getId(), "mirror", newMirror);
            userProfile.updatePendingChanges("request", "mirror", tle);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return true;
    }
    
    public boolean acceptMirroringRequest(String username, long capacity){
        refresh(BASIC_INFO);
        refresh(RECEIVEDMIRRORINGREQUESTS);
        refresh(PASSPHRASES);
        refresh(ORIGINALS);
        try{
            boolean found = false;
            mirror m = new mirror();
            for (int i = 0; i < receivedMirroringRequests.size(); i++){
                m = new mirror(receivedMirroringRequests.get(i));
                if (m.getUsername().equals(username)){
                    receivedMirroringRequests.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found){
                return false;
            }
            String passphrase = "";
            for (int i = 0; i < passphrases.size(); i++){
                passphraseEntry pe = new passphraseEntry(passphrases.get(i));
                if (pe.getUsername().equals(username)){
                    passphrase = pe.getPassphrase();
                    break;
                }
            }
            if (passphrase.equals(""))
                return false;
            m = new mirror(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), username, passphrase, capacity, 0, 0, 0, 0);
            originals.add(m);
            if (!(new File(prefix + "mirroring/" + username).exists())){
                boolean success = (new File(prefix + "mirroring/" + username)).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + "mirroring/" + username + "/audios").exists())){
                boolean success = (new File(prefix + "mirroring/" + username + "/audios")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + "mirroring/" + username + "/videos").exists())){
                boolean success = (new File(prefix + "mirroring/" + username + "/videos")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + "mirroring/" + username + "/photos").exists())){
                boolean success = (new File(prefix + "mirroring/" + username + "/photos")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + "mirroring/" + username + "/zones").exists())){
                boolean success = (new File(prefix + "mirroring/" + username + "/zones")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + "mirroring/" + username + "/zones/All").exists())){
                boolean success = (new File(prefix + "mirroring/" + username + "/zones/All")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + "mirroring/" + username + "/zones/All/wall").exists())){
                boolean success = (new File(prefix + "mirroring/" + username + "/zones/All/wall")).mkdirs();
                if (!success)
                    return false;
            }
            if (!(new File(prefix + "mirroring/" + username + "/thumbnails").exists())){
                boolean success = (new File(prefix + "mirroring/" + username + "/thumbnails")).mkdirs();
                if (!success)
                    return false;
            }
            save(RECEIVEDMIRRORINGREQUESTS);
            save(ORIGINALS);
            profile userProfile = new profile(this.username, this.username, prefix);
            timelineEntry tle = new timelineEntry(m.getId(), "mirror", m);
            userProfile.updatePendingChanges("add", "mirror", tle);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return true;
    }
    
    public boolean deleteSentMirroringRequest(String username){
        refresh(BASIC_INFO);
        refresh(SENTMIRRORINGREQUESTS);
        try{
            for (int i = 0; i < sentMirroringRequests.size(); i++){
                mirror m = new mirror(sentMirroringRequests.get(i));
                if (m.getUsername().equals(username))
                {
                    sentMirroringRequests.remove(i);
                }
            }
            save(SENTMIRRORINGREQUESTS);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return true;
    }
    
    public boolean deleteReceivedMirroringRequest(String username){
        refresh(BASIC_INFO);
        refresh(RECEIVEDMIRRORINGREQUESTS);
        try{
            for (int i = 0; i < receivedMirroringRequests.size(); i++){
                mirror m = new mirror(receivedMirroringRequests.get(i));
                if (m.getUsername().equals(username))
                {
                    receivedMirroringRequests.remove(i);
                }
            }
            save(RECEIVEDMIRRORINGREQUESTS);
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return true;
    }
    
    public boolean moveUpMirror(String username){
        refresh(BASIC_INFO);
        refresh(MIRRORS);
        try{
            for (int i = 0; i < mirrors.size(); i++){
                mirror m = new mirror(mirrors.get(i));
                if (m.getUsername().equals(username)){
                    if (i > 0){
                        mirror prev = new mirror(mirrors.get(i - 1));
                        prev.setPriority(i);
                        m.setPriority(i - 1);
                        mirrors.remove(i - 1);
                        mirrors.remove(i - 1);
                        mirrors.add(i - 1, prev);
                        mirrors.add(i - 1, m);
                        save(MIRRORS);
                        return true;
                    }
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public boolean moveDownMirror(String username){
        refresh(BASIC_INFO);
        refresh(MIRRORS);
        try{
            for (int i = 0; i < mirrors.size(); i++){
                mirror m = new mirror(mirrors.get(i));
                if (m.getUsername().equals(username)){
                    if (i < mirrors.size() - 1){
                        mirror next = new mirror(mirrors.get(i + 1));
                        next.setPriority(i);
                        m.setPriority(i + 1);
                        mirrors.remove(i);
                        mirrors.remove(i);
                        mirrors.add(i, m);
                        mirrors.add(i, next);
                        save(MIRRORS);
                        return true;
                    }
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public boolean deleteMirror(String username){
        refresh(BASIC_INFO);
        refresh(MIRRORS);
        try{
            for (int i = 0; i < mirrors.size(); i++){
                mirror m = new mirror(mirrors.get(i));
                if (m.getUsername().equals(username)){
                    mirrors.remove(i);
                    for (int j = i; j < mirrors.size(); j++){
                        mirror n = new mirror(mirrors.get(j));
                        n.setPriority(j);
                        mirrors.add(j, n);
                        mirrors.remove(j + 1);
                    }
                    save(MIRRORS);
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
    
    public boolean deleteOriginal(String username){
        refresh(BASIC_INFO);
        refresh(ORIGINALS);
        try{
            for (int i = 0; i < originals.size(); i++){
                mirror o = new mirror(originals.get(i));
                if (o.getUsername().equals(username)){
                    originals.remove(i);
                    save(ORIGINALS);
                    File dir = new File(prefix + "mirroring/" + username);
                    return deleteDir(dir);
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public boolean updateOriginal(String username, long capacity){
        refresh(BASIC_INFO);
        refresh(ORIGINALS);
        try{
            for (int i = 0; i < originals.size(); i++){
                mirror o = new mirror(originals.get(i));
                if (o.getUsername().equals(username)){
                    o.setCapacity(capacity);
                    originals.add(i, o);
                    originals.remove(i + 1);
                    save(ORIGINALS);
                    profile userProfile = new profile(this.username, this.username, prefix);
                    timelineEntry tle = new timelineEntry(o.getId(), "mirror", o);
                    userProfile.updatePendingChanges("update", "mirror", tle);
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
    
    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        
        // The directory is now empty so delete it
        return dir.delete();
    }
    
    public boolean deleteZone(long zoneId, String zoneName) {
        refresh(BASIC_INFO);
        refresh(ZONES);
        boolean found = false;
        for (int i = 0; i < zones.size(); i++){
            zone z = new zone(zones.get(i));
            if (z.getId() == zoneId && zoneName.equals(z.getName())){
                zones.remove(i);
                save(ZONES);
                found = true;
                break;
            }
        }
        if (!found)
            return false;
        File dir = new File(prefix + username + "/zones/" + zoneName);
        return deleteDir(dir);
    }
    
    public boolean deleteFriend(String username){
        refresh(BASIC_INFO);
        refresh(ZONES);
        refresh(FRIENDS);
        try{
            zone z = new zone();
            for (int i = 0; i < zones.size(); i++){
                z = new zone(zones.get(i));
                z.removeMember(username);
                zones.add(i, z);
                zones.remove(i + 1);
            }
            save(ZONES);
            profile userProfile = new profile(this.username, this.username, prefix);
            userProfile.fileList.clear();
            userProfile.parseDocument(prefix + this.username + "/correctImage.xml", "fileListEntry");
            for (int i = userProfile.fileList.size() - 1; i >= 0; i--){
                fileListEntry fle = new fileListEntry(userProfile.fileList.get(i));
                if (fle.getOwner().equals(username)){
                    userProfile.fileList.remove(i);
                }
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db;
            try{
                db = dbf.newDocumentBuilder();
            }catch(javax.xml.parsers.ParserConfigurationException e){
                if (DEBUG){
                    e.printStackTrace();
                }
                return false;
            }
            Document fileListDOM = db.newDocument();
            Element rootEle = fileListDOM.createElement("correctImage");
            fileListDOM.appendChild(rootEle);
            for (int i = 0; i < userProfile.fileList.size(); i++){
                fileListEntry f = new fileListEntry(userProfile.fileList.get(i));
                rootEle.appendChild(f.createDOMElement(fileListDOM));
            }
            while (!userProfile.saveXML(prefix + this.username + "/correctImage.xml", fileListDOM)){
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            userProfile.fileList.clear();
            userProfile.parseDocument(prefix + this.username + "/existingImage.xml", "fileListEntry");
            for (int i = userProfile.fileList.size() - 1; i >= 0; i--){
                fileListEntry fle = new fileListEntry(userProfile.fileList.get(i));
                if (fle.getOwner().equals(username)){
                    userProfile.fileList.remove(i);
                }
            }
            dbf = DocumentBuilderFactory.newInstance();
            try{
                db = dbf.newDocumentBuilder();
            }catch(javax.xml.parsers.ParserConfigurationException e){
                if (DEBUG){
                    e.printStackTrace();
                }
                return false;
            }
            fileListDOM = db.newDocument();
            rootEle = fileListDOM.createElement("existingImage");
            fileListDOM.appendChild(rootEle);
            for (int i = 0; i < userProfile.fileList.size(); i++){
                fileListEntry f = new fileListEntry(userProfile.fileList.get(i));
                rootEle.appendChild(f.createDOMElement(fileListDOM));
            }
            while (!userProfile.saveXML(prefix + this.username + "/existingImage.xml", fileListDOM)){
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            for (int i = 0; i < friends.size(); i++){
                friend f = new friend(friends.get(i));
                user u = new user(f.getUser());
                if (u.getUsername().equals(username)){
                    friends.remove(i);
                    deleteOriginal(username);
                    deleteMirror(username);
                    File dir = new File(prefix + this.username + "/friends/" + username);
                    deleteDir(dir);
                    break;
                }
            }
            save(FRIENDS);
            for (int i = 0; i < passphrases.size(); i++){
                passphraseEntry p = new passphraseEntry(passphrases.get(i));
                if (p.getUsername().equals(username)){
                    passphrases.remove(i);
                    break;
                }
            }
            save(PASSPHRASES);
            return true;
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public boolean addMirror(String clientName, mirror m){
        try{
            refresh(BASIC_INFO);
            refresh(SENTMIRRORINGREQUESTS);
            refresh(MIRRORS);
            refresh(ORIGINALS);
            boolean found = false;
            if (m.getUsername().equals(username)){
                for (int i = 0; i < sentMirroringRequests.size(); i++){
                    mirror tmp = new mirror(sentMirroringRequests.get(i));
                    if (tmp.getUsername().equals(clientName)){
                        sentMirroringRequests.remove(i);
                        found = true;
                        break;
                    }
                }
                save(SENTMIRRORINGREQUESTS);
                if (found){
                    m.setUsername(clientName);
                    m.setPriority(mirrors.size());
                    mirrors.add(m);
                    save(MIRRORS);
                    return true;
                }else{
                    return false;
                }
                
            }
            int i = 0;
            mirror tmp = new mirror();
            for (i = 0; i < originals.size(); i++){
                tmp = new mirror(originals.get(i));
                if (tmp.getUsername().equals(m.getUsername())){
                    break;
                }
            }
            if (i == originals.size())
                return false;
            File file = new File(prefix + "mirroring/" + tmp.getUsername() + "/sentMirroringRequests.xml");
            if (!file.exists()){
                return false;
            }
            
            List<mirror> originalSentMirroringRequests = new ArrayList();
            byte[] readIn = null;
            while((readIn = readXML(prefix + "mirroring/" + tmp.getUsername() + "/sentMirroringRequests.xml")) == null){
                Thread.sleep(100);
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            if (readIn.length > 0){
                dom = db.parse(new ByteArrayInputStream(readIn));
                Element docEle = dom.getDocumentElement();
                NodeList ml = docEle.getElementsByTagName("mirror");
                found = false;
                if(ml != null && ml.getLength() > 0) {
                    for (i = 0; i < ml.getLength(); i++){
                        Element mel = (Element)ml.item(i);
                        mirror mi = new mirror();
                        mi.create(mel);
                        if (!mi.getUsername().equals(clientName)){
                            originalSentMirroringRequests.add(mi);
                        }else{
                            found = true;
                        }
                    }
                }
                if (!found)
                    return false;
                dom = db.newDocument();
                Element rootEle = dom.createElement("sentMirroringRequests");
                dom.appendChild(rootEle);
                for (i = 0; i < originalSentMirroringRequests.size(); i++){
                    mirror mi = new mirror(originalSentMirroringRequests.get(i));
                    rootEle.appendChild(mi.createDOMElement(dom));
                }
                while (!saveXML(prefix + "mirroring/" + tmp.getUsername() + "/sentMirroringRequests.xml", dom)){
                    Thread.sleep(100);
                }
            }
            file = new File(prefix + "mirroring/" + tmp.getUsername() + "/mirrors.xml");
            List<mirror> originalMirrors = new ArrayList();
            while((readIn = readXML(prefix + "mirroring/" + tmp.getUsername() + "/mirrors.xml")) == null){
                Thread.sleep(100);
            }
            if (readIn.length > 0){
                dom = db.parse(new ByteArrayInputStream(readIn));
                Element docEle = dom.getDocumentElement();
                NodeList ml = docEle.getElementsByTagName("mirror");
                if(ml != null && ml.getLength() > 0) {
                    for (i = 0; i < ml.getLength(); i++){
                        Element mel = (Element)ml.item(i);
                        mirror mi = new mirror();
                        mi.create(mel);
                        originalMirrors.add(mi);
                    }
                }
                m.setUsername(clientName);
                m.setPriority(originalMirrors.size());
                originalMirrors.add(m);
                
                dom = db.newDocument();
                Element rootEle = dom.createElement("mirrors");
                dom.appendChild(rootEle);
                for (i = 0; i < originalMirrors.size(); i++){
                    mirror mi = new mirror(originalMirrors.get(i));
                    rootEle.appendChild(mi.createDOMElement(dom));
                }
                while (!saveXML(prefix + "mirroring/" + tmp.getUsername() + "/mirrors.xml", dom)){
                    Thread.sleep(100);
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return true;
        
    }
    
    public boolean updateMirror(String clientName, mirror m){
        try{
            refresh(BASIC_INFO);
            refresh(MIRRORS);
            refresh(ORIGINALS);
            boolean found = false;
            if (m.getUsername().equals(username)){
                for (int i = 0; i < mirrors.size(); i++){
                    mirror tmp = new mirror(mirrors.get(i));
                    if (tmp.getUsername().equals(clientName)){
                        m.setUsername(clientName);
                        m.setPriority(tmp.getPriority());
                        mirrors.add(i, m);
                        mirrors.remove(i + 1);
                        save(MIRRORS);
                        return true;
                    }
                }
            }
            int i = 0;
            mirror tmp = new mirror();
            for (i = 0; i < originals.size(); i++){
                tmp = new mirror(originals.get(i));
                if (tmp.getUsername().equals(m.getUsername())){
                    break;
                }
            }
            if (i == originals.size())
                return false;
            
            File file = new File(prefix + "mirroring/" + tmp.getUsername() + "/mirrors.xml");
            List<mirror> originalMirrors = new ArrayList();
            byte[] readIn = null;
            while((readIn = readXML(prefix + "mirroring/" + tmp.getUsername() + "/mirrors.xml")) == null){
                Thread.sleep(100);
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;
            if (readIn.length > 0){
                dom = db.parse(new ByteArrayInputStream(readIn));
                Element docEle = dom.getDocumentElement();
                NodeList ml = docEle.getElementsByTagName("mirror");
                if(ml != null && ml.getLength() > 0) {
                    for (i = 0; i < ml.getLength(); i++){
                        Element mel = (Element)ml.item(i);
                        mirror mi = new mirror();
                        mi.create(mel);
                        if (mi.getUsername().equals(clientName)){
                            m.setUsername(clientName);
                            m.setPriority(mi.getPriority());
                            originalMirrors.add(m);
                        }else{
                            originalMirrors.add(mi);
                        }
                    }
                }
                
                dom = db.newDocument();
                Element rootEle = dom.createElement("mirrors");
                dom.appendChild(rootEle);
                for (i = 0; i < originalMirrors.size(); i++){
                    mirror mi = new mirror(originalMirrors.get(i));
                    rootEle.appendChild(mi.createDOMElement(dom));
                }
                while (!saveXML(prefix + "mirroring/" + tmp.getUsername() + "/mirrors.xml", dom)){
                    Thread.sleep(100);
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return true;
        
    }
    
    public String toString(){
        String result = "Settings Info:\n";
        result += "username: " + username;
        result += "\n";
        result += "maxNumberOfChanges: " + maxNumberOfChanges;
        result += "\n";
        result += "CAServerName: " + CAServerName;
        result += "\n";
        result += "rendezvousServerAddress: " + rendezvousServerAddress;
        result += "\n";
        result += "rendezvousServerPort: " + rendezvousServerPort;
        result += "\n";
        result += "relayServerAddress: " + relayServerAddress;
        result += "\n";
        result += "relayServerPort: " + relayServerPort;
        result += "\n";
        result += "STUNServerAddress: " + STUNServerAddress;
        result += "\n";
        result += "STUNServerPort: " + STUNServerPort;
        result += "\n";
        result += "cacheSize: " + cacheSize;
        result += "\n";
        result += "MyZonePort: " + MyZonePort;
        result += "\n";
        result += "Device Priority: " + devPriority;
        result += "\n";
        result += "lastSyncTime1: " + lastSyncTime1;
        result += "\n";
        result += "lastSyncTime2: " + lastSyncTime2;
        result += "\n";
        result += "logUsage: " + logUsage;
        result += "\n";
        result += "syncPeriod: " + syncPeriod;
        result += "\n";
        for (int i = 0; i < zones.size(); i++){
            zone z = new zone(zones.get(i));
            result += z.toString();
        }
        for (int i = 0; i < friends.size(); i++){
            friend f = new friend(friends.get(i));
            result += f.toString();
        }
        for (int i = 0; i < mirrors.size(); i++){
            mirror m = new mirror(mirrors.get(i));
            result += m.toString();
        }
        return result;
    }
}
