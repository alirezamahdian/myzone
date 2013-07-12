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
import java.net.*;
import java.net.BindException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

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
import java.net.*;

import utils.attributes.*;
import utils.peer.*;
import utils.net.SecureSocket.*;
import utils.net.SecureSocket.RelaySocket.*;
import utils.security.KeyPairUtil.*;
import utils.security.CertUtil.*;
import utils.security.CertUtil.userCertificate;

import MyZone.*;
import MyZone.elements.*;

/*
 This file contains the implementation of the servingThread and all of its components.
 servingThread is instantiated from MyZoneEngine class and is responsible for all the 
 functionalities of a user in the role of a server. For each received connection, 
 servingThread starts a server thread which serves a single request from the client.
 The requests are either syncing with the original copy holder, receiving posts from other 
 friends, or sending updates to friends. The syncing process is done using the sync 
 function of the server thread. Receiving posts are done in receiveUpdates function of 
 the server thread. Sending updates are done in sendUpdates function of the server thread.
 */

public class servingThread extends Thread{
    
    private final static boolean DEBUG = false;
    private String prefix;
    private discoveryInfo di;
    private globalAttributes globalProperties = new globalAttributes();
    private Settings mainSettings;
    private relayServer relay;
    private Object postLock = new Object();
    servingThread(String prefix, discoveryInfo di, relayServer relay){
        setDaemon(true);
        this.prefix = prefix;
        this.di = di;
        this.relay = relay;
        globalProperties.init(prefix);
        mainSettings = new Settings(prefix);
        mainSettings.refresh(mainSettings.ALL);
    }
    
    public void run(){
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try{
            db = dbf.newDocumentBuilder();
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return;
        }
        Document dom;
        Element docEle;
        byte[] readIn = null;
        if(di.isOpenAccess()){
            // have public ip address
            TLSServerSocket srv = new TLSServerSocket(globalProperties.TCP, mainSettings.MyZonePort, mainSettings.username, globalProperties.myCertPath, globalProperties.myKeyPath, globalProperties.sessionCipher);
            while(true)
            {
                synchronized(mainSettings){
                    mainSettings.refresh(mainSettings.ALL);
                }
                List<friend> friendsOfOriginals = new ArrayList();
                srv.clearAccessList();
                synchronized(mainSettings.originals){
                    for (int i = 0; i < mainSettings.originals.size(); i++){
                        mirror origUser = new mirror(mainSettings.originals.get(i));
                        File file = new File(prefix + "mirroring/" + origUser.getUsername() + "/friends.xml");
                        if (file.exists()){
                            while((readIn = mainSettings.readXML(prefix + "mirroring/" + origUser.getUsername() + "/friends.xml")) == null){
                                try{
                                    Thread.sleep(100);
                                }catch(Exception e){
                                    if (DEBUG){
                                        e.printStackTrace();
                                    }
                                }
                            }
                            if (readIn.length > 0){
                                try{
                                    dom = db.parse(new ByteArrayInputStream(readIn));
                                }catch(Exception e){
                                    if (DEBUG){
                                        e.printStackTrace();
                                    }
                                    continue;
                                }
                                docEle = dom.getDocumentElement();
                                NodeList fl = docEle.getElementsByTagName("friend");
                                if(fl != null && fl.getLength() > 0) {
                                    for (int j = 0; j < fl.getLength(); j++){
                                        Element fel = (Element)fl.item(j);
                                        friend f = new friend();
                                        f.create(fel);
                                        friendsOfOriginals.add(f);
                                    }
                                }
                            }
                        }
                    }
                }
                synchronized(mainSettings.friends){
                    for (int i = 0; i < mainSettings.friends.size(); i++){
                        friend f = new friend(mainSettings.friends.get(i));
                        srv.grantAccess(f.getUser().getUsername());
                    }
                }
                for (int i = 0; i < friendsOfOriginals.size(); i++){
                    friend f = new friend(friendsOfOriginals.get(i));
                    srv.grantAccess(f.getUser().getUsername());
                }
                synchronized(mainSettings.pendingFriendships){
                    for (int i = 0; i < mainSettings.pendingFriendships.size(); i++){
                        friend f = new friend(mainSettings.pendingFriendships.get(i));
                        srv.grantAccess(f.getUser().getUsername());
                    }
                }
                srv.grantAccess(mainSettings.username);
                SecureSocket conn = null;
                try{
                    conn = srv.accept();
                }catch(SocketException e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                    return;
                }
                if (conn != null){
                    new server(conn, srv.clientUsername).start();
                    try{
                        Thread.sleep(100);
                    }catch(Exception e){
                        if (DEBUG){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        if(di.isFullCone()){
            // behind full cone NAT (UDP hole punching possible)
            TLSServerSocket srv = new TLSServerSocket(globalProperties.UDP, mainSettings.MyZonePort, mainSettings.username, globalProperties.myCertPath, globalProperties.myKeyPath, globalProperties.sessionCipher);
            if (DEBUG){
                System.out.println("DEBUG: line 203 of servingThread.java. public port = " + di.getPublicPort());
            }
            while(true)
            {
                synchronized(mainSettings){
                    mainSettings.refresh(mainSettings.ALL);
                }
                List<friend> friendsOfOriginals = new ArrayList();
                srv.clearAccessList();
                synchronized(mainSettings.originals){
                    for (int i = 0; i < mainSettings.originals.size(); i++){
                        mirror origUser = new mirror(mainSettings.originals.get(i));
                        File file = new File(prefix + "mirroring/" + origUser.getUsername() + "/friends.xml");
                        if (file.exists()){
                            while((readIn = mainSettings.readXML(prefix + "mirroring/" + origUser.getUsername() + "/friends.xml")) == null){
                                try{
                                    Thread.sleep(100);
                                }catch(Exception e){
                                    if (DEBUG){
                                        e.printStackTrace();
                                    }
                                }
                            }
                            if (readIn.length > 0){
                                try{
                                    dom = db.parse(new ByteArrayInputStream(readIn));
                                }catch(Exception e){
                                    if (DEBUG){
                                        e.printStackTrace();
                                    }
                                    continue;
                                }
                                docEle = dom.getDocumentElement();
                                NodeList fl = docEle.getElementsByTagName("friend");
                                if(fl != null && fl.getLength() > 0) {
                                    for (int j = 0; j < fl.getLength(); j++){
                                        Element fel = (Element)fl.item(j);
                                        friend f = new friend();
                                        f.create(fel);
                                        friendsOfOriginals.add(f);
                                    }
                                }
                            }
                        }
                    }
                }
                synchronized(mainSettings.friends){
                    for (int i = 0; i < mainSettings.friends.size(); i++){
                        friend f = new friend(mainSettings.friends.get(i));
                        srv.grantAccess(f.getUser().getUsername());
                    }
                }
                for (int i = 0; i < friendsOfOriginals.size(); i++){
                    friend f = new friend(friendsOfOriginals.get(i));
                    srv.grantAccess(f.getUser().getUsername());
                }
                synchronized(mainSettings.pendingFriendships){
                    for (int i = 0; i < mainSettings.pendingFriendships.size(); i++){
                        friend f = new friend(mainSettings.pendingFriendships.get(i));
                        srv.grantAccess(f.getUser().getUsername());
                    }
                }
                srv.grantAccess(mainSettings.username);
                SecureSocket conn = null;
                try{
                    conn = srv.accept();
                }catch(SocketException e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                    return;
                }
                if (conn != null){
                    new server(conn, srv.clientUsername).start();
                    try{
                        Thread.sleep(100);
                    }catch(Exception e){
                        if (DEBUG){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }else{
            // behind symmetric NAT (can only use an outside relay)
            RelayTLSServerSocket relaySrv = null;
            try{
                relaySrv = new RelayTLSServerSocket(relay.relayAddress, relay.port, mainSettings.username, globalProperties.myCertPath, globalProperties.myKeyPath, globalProperties.sessionCipher);
            }catch(SocketException e){
                if (DEBUG){
                    e.printStackTrace();
                }
                return;
            }
            while(true)
            {
                synchronized(mainSettings){
                    mainSettings.refresh(mainSettings.ALL);
                }
                List<friend> friendsOfOriginals = new ArrayList();
                relaySrv.clearAccessList();
                synchronized(mainSettings.originals){
                    for (int i = 0; i < mainSettings.originals.size(); i++){
                        mirror origUser = new mirror(mainSettings.originals.get(i));
                        File file = new File(prefix + "mirroring/" + origUser.getUsername() + "/friends.xml");
                        if (file.exists()){
                            while((readIn = mainSettings.readXML(prefix + "mirroring/" + origUser.getUsername() + "/friends.xml")) == null){
                                try{
                                    Thread.sleep(100);
                                }catch(Exception e){
                                    if (DEBUG){
                                        e.printStackTrace();
                                    }
                                }
                            }
                            if (readIn.length > 0){
                                try{
                                    dom = db.parse(new ByteArrayInputStream(readIn));
                                }catch(Exception e){
                                    if (DEBUG){
                                        e.printStackTrace();
                                    }
                                    continue;
                                }
                                docEle = dom.getDocumentElement();
                                NodeList fl = docEle.getElementsByTagName("friend");
                                if(fl != null && fl.getLength() > 0) {
                                    for (int j = 0; j < fl.getLength(); j++){
                                        Element fel = (Element)fl.item(j);
                                        friend f = new friend();
                                        f.create(fel);
                                        friendsOfOriginals.add(f);
                                    }
                                }
                            }
                        }
                    }
                }
                synchronized(mainSettings.friends){
                    for (int i = 0; i < mainSettings.friends.size(); i++){
                        friend f = new friend(mainSettings.friends.get(i));
                        relaySrv.grantAccess(f.getUser().getUsername());
                    }
                }
                for (int i = 0; i < friendsOfOriginals.size(); i++){
                    friend f = new friend(friendsOfOriginals.get(i));
                    relaySrv.grantAccess(f.getUser().getUsername());
                }
                synchronized(mainSettings.pendingFriendships){
                    for (int i = 0; i < mainSettings.pendingFriendships.size(); i++){
                        friend f = new friend(mainSettings.pendingFriendships.get(i));
                        relaySrv.grantAccess(f.getUser().getUsername());
                    }
                }
                relaySrv.grantAccess(mainSettings.username);
                SecureSocket conn = null;
                try{
                    conn = relaySrv.accept();
                }catch(SocketException e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                    return;
                }
                if (conn != null){
                    new server(conn, relaySrv.clientUsername).start();
                    try{
                        Thread.sleep(100);
                    }catch(Exception e){
                        if (DEBUG){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    
    class server extends Thread{
        private String clientName;
        private SecureSocket conn;
        private cleaner sweeper;
        
        server(SecureSocket conn, String clientName){
            setDaemon(true);
            this.clientName = clientName;
            this.conn = conn;
        }
        
        private List<String> getModifiedFiles(String username, String clientName, long lastVersion){
            File file = null;
            FileChannel channel = null;
            FileLock lock = null;
            FileInputStream fis = null;
            ByteArrayOutputStream baos = null;
            byte[] readIn = null;
            try{
                String path = "";
                if (username.equals(mainSettings.username)){
                    if (username.equals(clientName)){ // syncing with higher priority
                        path = prefix + username;
                    }else{ // getting updates from original user
                        path = prefix + username + "/zones/";
                    }
                }else{
                    if (username.equals(clientName)){ // syncing as a mirror
                        path = prefix + "mirroring/" + username;
                    }else{ // getting updates from mirror
                        path = prefix + "mirroring/" + username + "/zones/";
                    }
                }
                List<String> filenames = new ArrayList();
                if (clientName.equals(username)){
                    // strictly used for syncing
                    List<zone> zones = new ArrayList();
                    if (mainSettings.username.equals(clientName)){ // syncing with a higher priority user
                        if (new File(path + "/outbox.xml").exists())
                            filenames.add(new String(path + "/outbox.xml"));
                        if (new File(path + "/passphrases.xml").exists())
                            filenames.add(new String(path + "/passphrases.xml"));
                        if (new File(path + "/pendingFriendships.xml").exists())
                            filenames.add(new String(path + "/pendingFriendships.xml"));
                        if (new File(path + "/friends.xml").exists())
                            filenames.add(new String(path + "/friends.xml"));
                        if (new File(path + "/zones.xml").exists())
                            filenames.add(new String(path + "/zones.xml"));
                        synchronized(mainSettings.zones){
                            mainSettings.refresh(mainSettings.ZONES);
                            for (int i = 0; i < mainSettings.zones.size(); i++){
                                zone z = new zone(mainSettings.zones.get(i));
                                zones.add(z);
                            }
                        }
                    }else{
                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                        DocumentBuilder db = dbf.newDocumentBuilder();
                        Document dom;
                        file = new File(path + "/zones.xml");
                        if (!file.exists()){
                            return filenames;
                        }
                        fis = new FileInputStream(file);
                        channel = fis.getChannel();
                        lock = null;
                        while (lock == null){
                            try{
                                lock = channel.tryLock(0L, Long.MAX_VALUE, true);
                            }catch(Exception e){
                                if (DEBUG){
                                    e.printStackTrace();
                                }
                            }
                            Thread.yield();
                        }
                        baos = new ByteArrayOutputStream();
                        byte[] b = new byte[100000];
                        ByteBuffer buf = ByteBuffer.wrap(b);
                        int count = 0;
                        while((count = channel.read(buf)) > 0){
                            baos.write(b, 0, count);
                            buf.rewind();
                        }
                        readIn = baos.toByteArray();
                        baos.close();
                        lock.release();
                        channel.close();
                        fis.close();
                        zones.clear();
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
                    }
                    if (new File(path + "/awaitingFriendships.xml").exists())
                        filenames.add(new String(path + "/awaitingFriendships.xml"));
                    if (new File(path + "/inbox.xml").exists())
                        filenames.add(new String(path + "/inbox.xml"));
                    if (new File(path + "/events.xml").exists())
                        filenames.add(new String(path + "/events.xml"));
                    if (new File(path + "/notifications.xml").exists())
                        filenames.add(new String(path + "/notifications.xml"));
                    if (new File(path + "/receivedMirroringRequests.xml").exists())
                        filenames.add(new String(path + "/receivedMirroringRequests.xml"));
                    for (int i = 0; i < zones.size(); i++){
                        zone z = new zone(zones.get(i));
                        file = new File(path + "/zones/" + z.getName() + "/modifiedFiles.xml");
                        boolean exists = file.exists();
                        if (!exists){
                            continue;
                        }
                        fis = new FileInputStream(file);
                        channel = fis.getChannel();
                        lock = null;
                        while (lock == null){
                            try{
                                lock = channel.tryLock(0L, Long.MAX_VALUE, true);
                            }catch(Exception e){
                                if (DEBUG){
                                    e.printStackTrace();
                                }
                            }
                            Thread.yield();
                        }
                        baos = new ByteArrayOutputStream();
                        byte[] b = new byte[100000];
                        ByteBuffer buf = ByteBuffer.wrap(b);
                        int count = 0;
                        while((count = channel.read(buf)) > 0){
                            baos.write(b, 0, count);
                            buf.rewind();
                        }
                        readIn = baos.toByteArray();
                        baos.close();
                        lock.release();
                        channel.close();
                        fis.close();
                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                        DocumentBuilder db = dbf.newDocumentBuilder();
                        Document dom;
                        
                        dom = db.parse(new ByteArrayInputStream(readIn));
                        Element docEle = dom.getDocumentElement();
                        
                        // load all log entries
                        NodeList nl = docEle.getElementsByTagName("changeEntry");
                        if(nl != null && nl.getLength() > 0) {
                            for(int j = nl.getLength() - 1 ; j >= 0; j--) {
                                Element el = (Element)nl.item(j);
                                changeEntry ce = new changeEntry();
                                ce.create(el);
                                if (ce.getId() > lastVersion){
                                    boolean found = false;
                                    String filename = "";
                                    if (username.equals(mainSettings.username)){
                                        filename = ce.getFilename().substring(ce.getFilename().indexOf(username), ce.getFilename().length());
                                        filename = prefix + filename;
                                    }else{
                                        filename = ce.getFilename().substring(ce.getFilename().indexOf(username), ce.getFilename().length());
                                        filename = prefix + "mirroring/" + filename;
                                    }
                                    for (int k = 0; k < filenames.size(); k++){
                                        String name = (String)filenames.get(k);
                                        if (name.equals(filename)){
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found && !filename.contains(clientName + ".jpg")){
                                        filenames.add(filename);
                                    }
                                }
                            }
                        }
                    }
                }else{
                    profile wallMergerProfile = new profile(username, username, prefix);
                    List<zone> zones = new ArrayList<zone>();
                    if (username.equals(mainSettings.username)){
                        synchronized(mainSettings.zones){
                            mainSettings.refresh(mainSettings.ZONES);
                            for (int i= 0; i < mainSettings.zones.size(); i++){
                                zone z = new zone(mainSettings.zones.get(i));
                                zones.add(z);
                            }
                        }
                    }else{
                        file = new File(prefix + "mirroring/" + username + "/zones.xml");
                        if (file.exists()){
                            while((readIn = mainSettings.readXML(prefix + "mirroring/" + username + "/zones.xml")) == null){
                                Thread.sleep(100);
                            }
                            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                            DocumentBuilder db = dbf.newDocumentBuilder();
                            Document dom;
                            if (readIn.length > 0){
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
                            }
                        }
                    }
                    for (int i = 0; i < zones.size(); i++){
                        zone z = new zone(zones.get(i));
                        if (z.isMember(clientName) != null){
                            file = new File(path + z.getName() + "/modifiedFiles.xml");
                            boolean exists = file.exists();
                            if (!exists){
                                continue;
                            }
                            fis = new FileInputStream(file);
                            channel = fis.getChannel();
                            lock = null;
                            while (lock == null){
                                try{
                                    lock = channel.tryLock(0L, Long.MAX_VALUE, true);
                                }catch(Exception e){
                                    if (DEBUG){
                                        e.printStackTrace();
                                    }
                                }
                                Thread.yield();
                            }
                            baos = new ByteArrayOutputStream();
                            byte[] b = new byte[100000];
                            ByteBuffer buf = ByteBuffer.wrap(b);
                            int count = 0;
                            while((count = channel.read(buf)) > 0){
                                baos.write(b, 0, count);
                                buf.rewind();
                            }
                            readIn = baos.toByteArray();
                            baos.close();
                            lock.release();
                            channel.close();
                            fis.close();
                            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                            DocumentBuilder db = dbf.newDocumentBuilder();
                            Document dom;
                            dom = db.parse(new ByteArrayInputStream(readIn));
                            Element docEle = dom.getDocumentElement();
                            
                            // load all log entries
                            NodeList nl = docEle.getElementsByTagName("changeEntry");
                            if(nl != null && nl.getLength() > 0) {
                                for(int j = nl.getLength() - 1 ; j >= 0 ; j--) {
                                    Element el = (Element)nl.item(j);
                                    changeEntry ce = new changeEntry();
                                    ce.create(el);
                                    long lastUpdated = 0;
                                    if (lastVersion >= z.isMember(clientName).getMemberSince()){
                                        lastUpdated = lastVersion;
                                    }
                                    if (ce.getId() > lastUpdated){
                                        boolean found = false;
                                        String filename = "";
                                        if (username.equals(mainSettings.username)){
                                            filename = ce.getFilename().substring(ce.getFilename().indexOf(username), ce.getFilename().length());
                                            filename = prefix + filename;
                                        }else{
                                            filename = ce.getFilename().substring(ce.getFilename().indexOf(username), ce.getFilename().length());
                                            filename = prefix + "mirroring/" + filename;
                                        }
                                        for (int k = 0; k < filenames.size(); k++){
                                            String name = (String)filenames.get(k);
                                            if (name.equals(filename)){
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found){
                                            if (filename.contains("wall")){
                                                wallMergerProfile.loadWall(filename);
                                                String dirName = "";
                                                if (filename.contains("mirroring")){
                                                    dirName = prefix + "mirroring/" + username + "/tmp/"; 
                                                }else{
                                                    dirName = prefix + mainSettings.username + "/tmp/"; 
                                                }
                                                wallMergerProfile.trimWall(lastVersion);
                                                wallMergerProfile.mergeWall(dirName + clientName + "wall.xml");
                                                int k = 0;
                                                for (k = 0; k < filenames.size(); k++){
                                                    String name = new String(filenames.get(k));
                                                    if (name.equals(dirName + clientName + "wall.xml")){
                                                        break;
                                                    }
                                                }
                                                if (k == filenames.size()){
                                                    filenames.add(dirName + clientName + "wall.xml");
                                                }
                                            }else{
                                                if (!filename.contains(clientName + ".jpg")){
                                                    filenames.add(filename);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return filenames;
            }catch(Exception e){
                if (DEBUG){
                    e.printStackTrace();
                }
            }finally{
                try{
                    if (baos != null)
                        baos.close();
                    if (lock != null){
                        if (channel.isOpen()){
                            lock.release();
                            channel.close();
                        }
                    }
                    if (channel != null){
                        channel.close();
                    }
                    if (fis != null)
                        fis.close();
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
        // sync is always initiated by the original holder.
        private boolean sync(String username, long lastSyncedOn){
            FileChannel channel = null;
            FileLock lock = null;
            FileInputStream fis = null;
            FileOutputStream fos = null;
            mirror m = new mirror();
            long traffic = 0;
            try{
                if (!username.equals(clientName)){
                    System.out.println("DEBUG: line 738 of servingThread.java. returning false");
                    return false;
                }
                int i = 0;
                if (!mainSettings.username.equals(username)){
                    synchronized(mainSettings.originals){
                        mainSettings.refresh(mainSettings.ORIGINALS);
                        for (i = 0; i < mainSettings.originals.size(); i++){
                            m = mainSettings.originals.get(i);
                            if (m.getUsername().equals(username)){
                                break;
                            }
                        }
                        if (i == mainSettings.originals.size()){
                            String s = "DELETED";
                            byte[] response = s.getBytes("UTF-8");
                            conn.send(response);
                            if (DEBUG){
                                System.out.println("DEBUG: line 756 of servingThread.java. returning DELETED");
                            }
                            return false;
                        }
                    }
                }else{
                    m.setUsername(username);
                }
                if (m.getStatus().equals("syncing")){
                    if (DEBUG){
                        System.out.println("DEBUG: line 766 of servingThread.java. STILL SYNCING return false");
                    }
                    return false;
                }else{
                    m.setStatus("syncing");
                    mainSettings.updateOriginal(m);
                }
                System.out.println("Syncing with an original copy ... please do not disconnect!!!");
                String s = "GET_READY_FOR_UPDATES";
                byte[] response = s.getBytes("UTF-8");
                conn.send(response);
                if (DEBUG){
                    System.out.println("DEBUG: line 778 of servingThread.java. sent " + s);
                }
                response = conn.receive();
                s = new String(response, "UTF-8");
                if (s.indexOf("READY") != 0){
                    if (DEBUG){
                        System.out.println("DEBUG: line 784 of servingThread.java. returning false");
                        return false;
                    }
                }
                List<String> filenames = getModifiedFiles(username, clientName, lastSyncedOn);
                for (i = 0; i < filenames.size(); i++){
                    String absoluteFilename = (String)filenames.get(i);
                    String filename = "";
                    filename = absoluteFilename.substring(absoluteFilename.indexOf(username),absoluteFilename.length());
                    File toBeSend = new File(absoluteFilename);
                    if (!toBeSend.exists())
                        continue;
                    fis = new FileInputStream(toBeSend);
                    channel = fis.getChannel();
                    lock = null;
                    while (lock == null){
                        try{
                            lock = channel.tryLock(0L, Long.MAX_VALUE, true);
                        }catch(Exception e){
                            if (DEBUG){
                                e.printStackTrace();
                            }
                        }
                        Thread.yield();
                    }
                    
                    byte[] b = new byte[100000];
                    ByteBuffer buf = ByteBuffer.wrap(b);
                    int bytesRead = 0;
                    byte[] tosend;
                    // SENDING_FILE|FILENAME|FILE_LENGTH|
                    s = "SENDING_FILE|" + filename + "|" + toBeSend.length() + "|";
                    response = s.getBytes("UTF-8");
                    conn.send(response);
                    byte[] ackBytes = conn.receive();
                    String ack = new String(ackBytes, "UTF-8");
                    if (ack.indexOf("READY") != 0)
                    {
                        lock.release();
                        channel.close();
                        fis.close();
                        continue;
                    }
                    int retries = 0;
                    long filelength = toBeSend.length();
                    while (-1 != (bytesRead = channel.read(buf)) && retries < 20){
                        tosend = new byte[bytesRead];
                        System.arraycopy(b, 0, tosend, 0, bytesRead);
                        conn.send(tosend);
                        ackBytes = conn.receive();
                        ack = new String(ackBytes, "UTF-8");
                        retries = 0;
                        while (!ack.equals("ACK") && retries < 20){
                            conn.send(tosend);
                            ackBytes = conn.receive();
                            ack = new String(ackBytes, "UTF-8");
                            retries++;
                        }
                        buf.rewind();
                    }
                    lock.release();
                    channel.close();
                    fis.close();
                    if (filename.contains("awaitingFriendships.xml") || filename.contains("receivedMirroringRequests.xml")){
                        toBeSend.delete();
                    }
                }
                s = "DONE";
                response = s.getBytes("UTF-8");
                conn.send(response);
                if (!mainSettings.username.equals(username)){
                    synchronized(mainSettings.originals){
                        mainSettings.refresh(mainSettings.ORIGINALS);
                        for (i = 0; i < mainSettings.originals.size(); i++){
                            mirror mi = new mirror(mainSettings.originals.get(i));
                            if (mi.getUsername().equals(m.getUsername())){
                                break;
                            }
                        }
                        if (i < mainSettings.originals.size()){
                            m.setLastSyncTime(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis());
                            mainSettings.updateOriginal(m);
                        }
                    }
                }
                Thread.sleep(120000); // wait 2 minutes for the client to merge the updates.
                response = conn.receive();
                s = new String(response, "UTF-8");
                if (s.indexOf("GET_READY_FOR_UPDATES") == 0){
                    s = "READY";
                    response = s.getBytes("UTF-8");
                    conn.send(response);
                    response = conn.receive();
                    s = new String(response, "UTF-8");
                    while(s.indexOf("DONE") != 0){
                        int retries = 0;
                        while(!s.contains("SENDING_FILE") && s.indexOf("|") < s.indexOf("|", s.indexOf("|") + 1) && s.indexOf("|", s.indexOf("|") + 1) < s.lastIndexOf("|")){
                            if (retries == 20)
                                return false;
                            s = "NACK";
                            response = s.getBytes("UTF-8");
                            conn.send(response);
                            response = conn.receive();
                            s = new String(response, "UTF-8");
                            retries++;
                        }
                        String filename = s.substring(s.indexOf("|") + 1, s.indexOf("|", s.indexOf("|") + 1));
                        if (DEBUG){
                            System.out.println("DEBUG: line 892 of servingThread.java. recevdFilename = " + filename);
                            System.out.println(s.indexOf("|", s.indexOf("|") + 1) + 1);
                        }
                        long filelength = Long.parseLong(s.substring(s.indexOf("|", s.indexOf("|") + 1) + 1, s.lastIndexOf("|")));
                        if (DEBUG){
                            System.out.println("DEBUG: line 897 of servingThread.java. recevdFileLength = " + filelength);
                        }
                        File toBeReceived = null;
                        if (username.equals(mainSettings.username)){
                            String dirName = filename.substring(0, filename.lastIndexOf("/"));
                            if (!(new File(prefix + dirName)).exists()){
                                boolean success = (new File(prefix + dirName)).mkdirs();
                                if (success)
                                    toBeReceived = new File(prefix + filename);
                                else{
                                    return false;
                                }
                            }else{
                                toBeReceived = new File(prefix + filename);
                            }
                        }else{
                            String dirName = filename.substring(0, filename.lastIndexOf("/"));
                            if (!(new File(prefix + "mirroring/" + dirName)).exists()){ 
                                boolean success = (new File(prefix + "mirroring/" + dirName)).mkdirs();
                                if (success)
                                    toBeReceived = new File(prefix + "mirroring/" + filename);
                                else{
                                    return false;
                                }
                            }else{
                                toBeReceived = new File(prefix + "mirroring/" + filename);
                            }
                        }
                        if (toBeReceived.exists()){
                            if (toBeReceived.length() == filelength){
                                response = new String("SKIP").getBytes("UTF-8");
                                conn.send(response);
                                response = conn.receive();
                                s = new String(response, "UTF-8");
                                continue;
                            }else{
                                response = new String("ACK").getBytes("UTF-8");
                                conn.send(response);
                            }
                        }else{
                            response = new String("ACK").getBytes("UTF-8");
                            conn.send(response);
                        }
                        long fileSize = filelength;
                        if (toBeReceived.exists()){
                            if ( (toBeReceived.getName().contains("friends.xml") || toBeReceived.getName().contains("mirrors.xml")) && username.equals(mainSettings.username)){
                                File tmp = new File(prefix + filename + ".rcv");
                                fos = new FileOutputStream(tmp);
                                channel = fos.getChannel();
                                while (filelength > 0){
                                    byte[] b = conn.receive();
                                    ByteBuffer buf = ByteBuffer.wrap(b);
                                    channel.write(buf);
                                    filelength -= b.length;
                                    retries = 0;
                                    if (b.length > 0){
                                        response = new String("ACK").getBytes("UTF-8");
                                        conn.send(response);
                                    }else{
                                        if (retries == 20){
                                            return false;
                                        }
                                        response = new String("NACK").getBytes("UTF-8");
                                        conn.send(response);
                                        retries++;
                                    }
                                }
                                channel.close();
                                fos.flush();
                                fos.close();
                                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                                DocumentBuilder db = dbf.newDocumentBuilder();
                                Document dom = db.parse(tmp);
                                Element docEle = dom.getDocumentElement();
                                if (toBeReceived.getName().contains("friends.xml")){
                                    NodeList fl = docEle.getElementsByTagName("friend");
                                    List<friend> friends = new ArrayList<friend>();
                                    if(fl != null && fl.getLength() > 0) {
                                        for (int j = 0; j < fl.getLength(); j++){
                                            Element fel = (Element)fl.item(j);
                                            friend f = new friend();
                                            f.create(fel);
                                            friends.add(f);
                                        }
                                    }
                                    int k = 0;
                                    mainSettings.refresh(mainSettings.BASIC_INFO);
                                    mainSettings.refresh(mainSettings.FRIENDS);
                                    for (k = 0; k < mainSettings.friends.size(); k++){
                                        friend ef = new friend(mainSettings.friends.get(k));
                                        int l = 0;
                                        for (l = 0; l < friends.size(); l++){
                                            friend f = new friend(friends.get(l));
                                            if (ef.getUser().getUsername().equals(f.getUser().getUsername())){
                                                break;
                                            }
                                        }
                                        if (l == friends.size()){
                                            mainSettings.friends.remove(k);
                                        }
                                    }
                                    k = 0;
                                    mainSettings.save(mainSettings.FRIENDS);
                                    for (k = 0; k < friends.size(); k++){
                                        friend f = new friend(friends.get(k));
                                        int l = 0;
                                        for (l = 0; l < mainSettings.friends.size(); l++){
                                            friend ef = new friend(mainSettings.friends.get(l));
                                            if (f.getUser().getUsername().equals(ef.getUser().getUsername())){
                                                if (f.getLastUpdateTime() > ef.getLastUpdateTime()){
                                                    f.setLatestVersion(ef.getLatestVersion());
                                                    mainSettings.friends.add(l, f); // existing friend was modified
                                                    mainSettings.friends.remove(l + 1);
                                                }
                                                break;
                                            }
                                        }
                                        if (l == mainSettings.friends.size()){
                                            f.setLatestVersion(0);
                                            mainSettings.friends.add(f); // add as a new friend
                                            if (!(new File(prefix + mainSettings.username + "/friends/" + f.getUser().getUsername()).exists())){
                                                boolean success = (new File(prefix + mainSettings.username + "/friends/" + f.getUser().getUsername())).mkdirs();
                                            }
                                            if (!(new File(prefix + mainSettings.username + "/friends/" + f.getUser().getUsername() + "/audios").exists())){
                                                boolean success = (new File(prefix + mainSettings.username + "/friends/" + f.getUser().getUsername() + "/audios")).mkdirs();
                                            }
                                            if (!(new File(prefix + mainSettings.username + "/friends/" + f.getUser().getUsername() + "/videos").exists())){
                                                boolean success = (new File(prefix + mainSettings.username + "/friends/" + f.getUser().getUsername() + "/videos")).mkdirs();
                                            }
                                            if (!(new File(prefix + mainSettings.username + "/friends/" + f.getUser().getUsername() + "/photos").exists())){
                                                boolean success = (new File(prefix + mainSettings.username + "/friends/" + f.getUser().getUsername() + "/photos")).mkdirs();
                                            }
                                            if (!(new File(prefix + mainSettings.username + "/friends/" + f.getUser().getUsername() + "/wall").exists())){
                                                boolean success = (new File(prefix + mainSettings.username + "/friends/" + f.getUser().getUsername() + "/wall")).mkdirs();
                                            }
                                            if (!(new File(prefix + mainSettings.username + "/friends/" + f.getUser().getUsername() + "/cert").exists())){
                                                boolean success = (new File(prefix + mainSettings.username + "/friends/" + f.getUser().getUsername() + "/cert")).mkdirs();
                                            }
                                        }
                                    }
                                    mainSettings.save(mainSettings.FRIENDS);
                                }
                                if (toBeReceived.getName().contains("mirrors.xml")){
                                    NodeList ml = docEle.getElementsByTagName("mirror");
                                    List<mirror> mirrors = new ArrayList<mirror>();
                                    boolean updated = false;
                                    if(ml != null && ml.getLength() > 0) {
                                        for (int j = 0; j < ml.getLength(); j++){
                                            Element mel = (Element)ml.item(j);
                                            mirror mi = new mirror();
                                            mi.create(mel);
                                            mirrors.add(mi);
                                        }
                                    }
                                    int k = 0;
                                    mainSettings.refresh(mainSettings.BASIC_INFO);
                                    mainSettings.refresh(mainSettings.MIRRORS);
                                    for (k = 0; k < mainSettings.mirrors.size(); k++){
                                        mirror em = new mirror(mainSettings.mirrors.get(k));
                                        int l = 0;
                                        for (l = 0; l < mirrors.size(); l++){
                                            mirror mi = new mirror(mirrors.get(l));
                                            if (em.getUsername().equals(mi.getUsername())){
                                                break;
                                            }
                                        }
                                        if (l == mirrors.size()){
                                            mainSettings.mirrors.remove(k);
                                        }
                                    }
                                    k = 0;
                                    for (k = 0; k < mirrors.size(); k++){
                                        mirror mi = new mirror(mirrors.get(k));
                                        int l = 0;
                                        for (l = 0; l < mainSettings.mirrors.size(); l++){
                                            mirror em = new mirror(mainSettings.mirrors.get(l));
                                            if (em.getUsername().equals(mi.getUsername())){
                                                if (mi.getLastUpdateTime() > em.getLastUpdateTime()){
                                                    mi.setLastSyncTime(em.getLastSyncTime());
                                                    mi.setUsed(em.getUsed());
                                                    mainSettings.mirrors.remove(l);
                                                    mainSettings.mirrors.add(mi); // append to the end for now
                                                    updated = true;
                                                }
                                                break;
                                            }
                                            if (l == mainSettings.mirrors.size()){
                                                mi.setLastSyncTime(0);
                                                mi.setUsed(0);
                                                mainSettings.mirrors.add(mi); // add as a new mirror
                                                updated = true;
                                            }
                                        }
                                    }
                                    if (updated){
                                        for (int j = 0; j < mainSettings.mirrors.size(); j++){
                                            mirror em = new mirror(mainSettings.mirrors.get(j));
                                            if (em.getPriority() != j){
                                                if (em.getPriority() > j){
                                                    mainSettings.mirrors.remove(j);
                                                    mainSettings.mirrors.add(em); // append it to the end for now
                                                }else{
                                                    mainSettings.mirrors.remove(j);
                                                    mainSettings.mirrors.add(em.getPriority(), em); // the exisitng one at this position must have a priority >= em.getPriority()
                                                }
                                            }
                                        }
                                        for (int j = 0; j < mainSettings.mirrors.size(); j++){
                                            mirror em = new mirror(mainSettings.mirrors.get(j));
                                            if (em.getPriority() != j){
                                                em.setPriority(j);
                                                mainSettings.mirrors.add(j, em);
                                                mainSettings.mirrors.remove(j + 1);
                                            }
                                        }
                                        mainSettings.save(mainSettings.MIRRORS);
                                    }
                                }
                                tmp.delete();
                            }else{
                                fos = new FileOutputStream(toBeReceived);
                                channel = fos.getChannel();
                                lock = null;
                                while (lock == null){
                                    try{
                                        lock = channel.tryLock();
                                    }catch(Exception e){
                                        if (DEBUG){
                                            e.printStackTrace();
                                        }
                                    }
                                    Thread.yield();
                                }
                                
                                while (filelength > 0){
                                    byte[] b = conn.receive();
                                    ByteBuffer buf = ByteBuffer.wrap(b);
                                    channel.write(buf);
                                    filelength -= b.length;
                                    retries = 0;
                                    if (b.length > 0){
                                        response = new String("ACK").getBytes("UTF-8");
                                        conn.send(response);
                                    }else{
                                        if (retries == 20){
                                            return false;
                                        }
                                        response = new String("NACK").getBytes("UTF-8");
                                        conn.send(response);
                                        retries++;
                                    }
                                }
                                lock.release();
                                channel.close();
                                fos.flush();
                                fos.close();
                            }
                        }else{
                            fos = new FileOutputStream(toBeReceived);
                            while (filelength > 0){
                                byte[] buf = conn.receive();
                                fos.write(buf, 0, buf.length);
                                filelength -= buf.length;
                                if (buf.length > 0){
                                    response = new String("ACK").getBytes("UTF-8");
                                    conn.send(response);
                                }else{
                                    response = new String("NACK").getBytes("UTF-8");
                                    conn.send(response);
                                }
                            }
                            fos.flush();
                            fos.close();
                        }
                        if (!toBeReceived.getName().contains(".xml")){
                            traffic += fileSize;
                        }
                        response = conn.receive();
                        s = new String(response, "UTF-8");
                        if (DEBUG){
                            System.out.println("DEBUG: line 1177 of servingThread.java. response at the end of while loop = " + s);
                        }
                    }
                    s = "0";
                    if (!mainSettings.username.equals(username)){
                        synchronized(mainSettings.originals){
                            mainSettings.refresh(mainSettings.ORIGINALS);
                            for (i = 0; i < mainSettings.originals.size(); i++){
                                mirror mi = new mirror(mainSettings.originals.get(i));
                                if (mi.getUsername().equals(m.getUsername())){
                                    break;
                                }
                            }
                            if (i < mainSettings.originals.size()){
                                s = String.valueOf(m.getUsed() + traffic);
                                m.setUsed(m.getUsed() + traffic);
                                mainSettings.updateOriginal(m);
                            }
                        }
                    }
                    conn.send(s.getBytes("UTF-8"));
                }
                m.setStatus("idle");
                mainSettings.updateOriginal(m);
                System.out.println("Successfully synced with the original copy of " + m.getUsername() + " \nyou can safely disconnect now ...");
                return true;
            }catch(Exception e){
                if (DEBUG){
                    e.printStackTrace();
                }
            }finally{
                try{
                    if (m.getStatus().equals("syncing")){
                        m.setStatus("idle");
                        m.setUsed(m.getUsed() + traffic);
                        mainSettings.updateOriginal(m);
                    }
                    if (lock != null){
                        if (channel.isOpen()){
                            lock.release();
                            channel.close();
                        }
                    }
                    if (channel != null){
                        channel.close();
                    }
                    if (fos != null)
                        fos.close();
                    if (fis != null)
                        fis.close();
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
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
                }
            }
            return false;
        }
        
        private boolean receiveUpdates(String username, long latestVersion){
            FileChannel channel = null;
            FileLock lock = null;
            FileInputStream fis = null;
            ByteArrayOutputStream baos = null;
            FileOutputStream fos = null;
            try{
                synchronized(postLock){
                    if (!mainSettings.username.equals(username)){
                        int i = 0;
                        synchronized(mainSettings.originals){
                            mainSettings.refresh(mainSettings.ORIGINALS);
                            for (i = 0; i < mainSettings.originals.size(); i++){
                                mirror m = new mirror(mainSettings.originals.get(i));
                                if (m.getUsername().equals(username)){
                                    break;
                                }
                            }
                            if (i == mainSettings.originals.size()){
                                return false;
                            }
                        }
                    }
                    profile userProfile = new profile(username, username, prefix);
                    byte[] response = new String("SEND_XML").getBytes("UTF-8");
                    conn.send(response);
                    byte[] xmlUpdates = conn.receive();
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document dom;
                    dom = db.parse(new ByteArrayInputStream(xmlUpdates));
                    
                    Element docEle = dom.getDocumentElement();
                    List<String> files = new ArrayList<String>();
                    List<String> filesAssociations = new ArrayList<String>();
                    NodeList nl = docEle.getElementsByTagName("pendingChange");
                    
                    if(nl != null && nl.getLength() > 0) {
                        for(int i = 0 ; i < nl.getLength(); i++) {
                            Element el = (Element)nl.item(i);
                            pendingChange pc = new pendingChange();
                            pc.create(el);
                            if (pc.getAction().equals("add")){
                                if (pc.getShareWith().equals("none") || userProfile.hasAccess(clientName, pc.getShareWith())){
                                    if (pc.getType().equals("mirror")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        synchronized(mainSettings.mirrors){
                                            mainSettings.addMirror(clientName, entry.mi);
                                        }
                                    }else if (pc.getType().equals("link")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (entry.l.getPostedBy().getUsername().equals(clientName)){
                                            userProfile.addLink(entry.l, true);
                                        }
                                    }else if (pc.getType().equals("wallPost")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (entry.s.getPostedBy().getUsername().equals(clientName)){
                                            userProfile.addWallPost(entry.s, true);
                                        }
                                    }else if (pc.getType().equals("photo")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (entry.p.getPostedBy().getUsername().equals(clientName)){
                                            userProfile.addPhoto(entry.p, true);
                                            files.add(new String(clientName + "/friends/" + username + "/photos/" + entry.p.getFilename()));
                                            filesAssociations.add(pc.getShareWith());
                                        }
                                    }else if (pc.getType().equals("photoAlbum")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (entry.pa.getPostedBy().getUsername().equals(clientName) || entry.pa.getTitle().equals("Wall")){
                                            userProfile.addPhotoAlbum(entry.pa, true);
                                        }
                                    }else if (pc.getType().equals("video")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (entry.v.getPostedBy().getUsername().equals(clientName)){
                                            userProfile.addVideo(entry.v, true);
                                            files.add(new String(clientName + "/friends/" + username + "/videos/" + entry.v.getFilename()));
                                            filesAssociations.add(pc.getShareWith());
                                        }
                                    }else if (pc.getType().equals("videoAlbum")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (entry.va.getPostedBy().getUsername().equals(clientName) || entry.va.getTitle().equals("Wall")){
                                            userProfile.addVideoAlbum(entry.va, true);
                                        }
                                    }else if (pc.getType().equals("audio")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (entry.a.getPostedBy().getUsername().equals(clientName)){
                                            userProfile.addAudio(entry.a, true);
                                            files.add(new String(clientName + "/friends/" + username + "/audios/" + entry.a.getFilename()));
                                            filesAssociations.add(pc.getShareWith());
                                        }
                                    }else if (pc.getType().equals("audioAlbum")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (entry.aa.getPostedBy().getUsername().equals(clientName) || entry.aa.getTitle().equals("Wall")){
                                            userProfile.addAudioAlbum(entry.aa, true);
                                        }
                                    }else if (pc.getType().equals("linkComment")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<comment> comments = entry.l.getComments();
                                        if (comments != null){
                                            for (int j = 0; j < comments.size(); j++){
                                                comment c = comments.get(j);
                                                if (c.getUser().getUsername().equals(clientName)){
                                                    userProfile.addComment(c, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("wallPostComment")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<comment> comments = entry.s.getComments();
                                        if (comments != null){
                                            for (int j = 0; j < comments.size(); j++){
                                                comment c = comments.get(j);
                                                if (c.getUser().getUsername().equals(clientName)){
                                                    userProfile.addComment(c, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("audioAlbumComment")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<comment> comments = entry.aa.getComments();
                                        if (comments != null){
                                            for (int j = 0; j < comments.size(); j++){
                                                comment c = new comment(comments.get(j));
                                                if (c.getUser().getUsername().equals(clientName)){
                                                    userProfile.addComment(c, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("videoAlbumComment")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<comment> comments = entry.va.getComments();
                                        if (comments != null){
                                            for (int j = 0; j < comments.size(); j++){
                                                comment c = new comment(comments.get(j));
                                                if (c.getUser().getUsername().equals(clientName)){
                                                    userProfile.addComment(c, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("photoAlbumComment")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<comment> comments = entry.pa.getComments();
                                        if (comments != null){
                                            for (int j = 0; j < comments.size(); j++){
                                                comment c = new comment(comments.get(j));
                                                if (c.getUser().getUsername().equals(clientName)){
                                                    userProfile.addComment(c, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("audioComment")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<comment> comments = entry.a.getComments();
                                        if (comments != null){
                                            for (int j = 0; j < comments.size(); j++){
                                                comment c = new comment(comments.get(j));
                                                if (c.getUser().getUsername().equals(clientName)){
                                                    userProfile.addComment(c, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("videoComment")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<comment> comments = entry.v.getComments();
                                        if (comments != null){
                                            for (int j = 0; j < comments.size(); j++){
                                                comment c = new comment(comments.get(j));
                                                if (c.getUser().getUsername().equals(clientName)){
                                                    userProfile.addComment(c, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("photoComment")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<comment> comments = entry.p.getComments();
                                        if (comments != null){
                                            for (int j = 0; j < comments.size(); j++){
                                                comment c = new comment(comments.get(j));
                                                if (c.getUser().getUsername().equals(clientName)){
                                                    userProfile.addComment(c, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("linkLike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<like> likes = entry.l.getLikes();
                                        if (likes != null){
                                            for (int j = 0; j < likes.size(); j++){
                                                like li = new like(likes.get(j));
                                                if (li.getUser().getUsername().equals(clientName)){
                                                    userProfile.addlike(li, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("wallPostLike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<like> likes = entry.s.getLikes();
                                        if (likes != null){
                                            for (int j = 0; j < likes.size(); j++){
                                                like li = new like(likes.get(j));
                                                if (li.getUser().getUsername().equals(clientName)){
                                                    userProfile.addlike(li, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("audioAlbumLike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<like> likes = entry.aa.getLikes();
                                        if (likes != null){
                                            for (int j = 0; j < likes.size(); j++){
                                                like li = new like(likes.get(j));
                                                if (li.getUser().getUsername().equals(clientName)){
                                                    userProfile.addlike(li, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("videoAlbumLike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<like> likes = entry.va.getLikes();
                                        if (likes != null){
                                            for (int j = 0; j < likes.size(); j++){
                                                like li = new like(likes.get(j));
                                                if (li.getUser().getUsername().equals(clientName)){
                                                    userProfile.addlike(li, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("photoAlbumLike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<like> likes = entry.pa.getLikes();
                                        if (likes != null){
                                            for (int j = 0; j < likes.size(); j++){
                                                like li = new like(likes.get(j));
                                                if (li.getUser().getUsername().equals(clientName)){
                                                    userProfile.addlike(li, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("audioLike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<like> likes = entry.a.getLikes();
                                        if (likes != null){
                                            for (int j = 0; j < likes.size(); j++){
                                                like li = new like(likes.get(j));
                                                if (li.getUser().getUsername().equals(clientName)){
                                                    userProfile.addlike(li, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("videoLike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<like> likes = entry.v.getLikes();
                                        if (likes != null){
                                            for (int j = 0; j < likes.size(); j++){
                                                like li = new like(likes.get(j));
                                                if (li.getUser().getUsername().equals(clientName)){
                                                    userProfile.addlike(li, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("photoLike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<like> likes = entry.p.getLikes();
                                        if (likes != null){
                                            for (int j = 0; j < likes.size(); j++){
                                                like li = new like(likes.get(j));
                                                if (li.getUser().getUsername().equals(clientName)){
                                                    userProfile.addlike(li, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("linkDislike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<dislike> dislikes = entry.l.getDislikes();
                                        if (dislikes != null){
                                            for (int j = 0; j < dislikes.size(); j++){
                                                dislike dli = new dislike(dislikes.get(j));
                                                if (dli.getUser().getUsername().equals(clientName)){
                                                    userProfile.addDislike(dli, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("wallPostDislike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<dislike> dislikes = entry.s.getDislikes();
                                        if (dislikes != null){
                                            for (int j = 0; j < dislikes.size(); j++){
                                                dislike dli = new dislike(dislikes.get(j));
                                                if (dli.getUser().getUsername().equals(clientName)){
                                                    userProfile.addDislike(dli, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("audioAlbumDislike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<dislike> dislikes = entry.aa.getDislikes();
                                        if (dislikes != null){
                                            for (int j = 0; j < dislikes.size(); j++){
                                                dislike dli = new dislike(dislikes.get(j));
                                                if (dli.getUser().getUsername().equals(clientName)){
                                                    userProfile.addDislike(dli, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("videoAlbumDislike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<dislike> dislikes = entry.va.getDislikes();
                                        if (dislikes != null){
                                            for (int j = 0; j < dislikes.size(); j++){
                                                dislike dli = new dislike(dislikes.get(j));
                                                if (dli.getUser().getUsername().equals(clientName)){
                                                    userProfile.addDislike(dli, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("photoAlbumDislike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<dislike> dislikes = entry.pa.getDislikes();
                                        if (dislikes != null){
                                            for (int j = 0; j < dislikes.size(); j++){
                                                dislike dli = new dislike(dislikes.get(j));
                                                if (dli.getUser().getUsername().equals(clientName)){
                                                    userProfile.addDislike(dli, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("audioDislike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<dislike> dislikes = entry.a.getDislikes();
                                        if (dislikes != null){
                                            for (int j = 0; j < dislikes.size(); j++){
                                                dislike dli = new dislike(dislikes.get(j));
                                                if (dli.getUser().getUsername().equals(clientName)){
                                                    userProfile.addDislike(dli, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("videoDislike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<dislike> dislikes = entry.v.getDislikes();
                                        if (dislikes != null){
                                            for (int j = 0; j < dislikes.size(); j++){
                                                dislike dli = new dislike(dislikes.get(j));
                                                if (dli.getUser().getUsername().equals(clientName)){
                                                    userProfile.addDislike(dli, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("photoDislike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<dislike> dislikes = entry.p.getDislikes();
                                        if (dislikes != null){
                                            for (int j = 0; j < dislikes.size(); j++){
                                                dislike dli = new dislike(dislikes.get(j));
                                                if (dli.getUser().getUsername().equals(clientName)){
                                                    userProfile.addDislike(dli, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("event")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        userProfile.addEvent(entry.e, false);
                                    }else if (pc.getType().equals("message")){
                                        userProfile.loadInbox();
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        int x = 0;
                                        message me = null;
                                        for (x = 0; x < userProfile.receivedMsgs.size(); x++){
                                            me = new message(userProfile.receivedMsgs.get(x));
                                            if (me.getId() <= entry.m.getId()){
                                                break;
                                            }
                                        }
                                        if (x == userProfile.receivedMsgs.size()){
                                            userProfile.receivedMsgs.add(entry.m);
                                        }else{
                                            if (me.getId() == entry.m.getId()){
                                                
                                            }else{
                                                userProfile.receivedMsgs.add(x, entry.m);
                                            }
                                        }
                                        userProfile.saveInbox();
                                    }
                                }
                            }else if (pc.getAction().equals("remove")){
                                if (pc.getShareWith().equals("none") || userProfile.hasAccess(clientName, pc.getShareWith())){
                                    if (pc.getType().equals("link")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (entry.l.getPostedBy().getUsername().equals(clientName))
                                        {
                                            userProfile.removeLink(entry.id, entry.l.getShareWith(), clientName, true);
                                        }
                                    }else if (pc.getType().equals("wallPost")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (entry.s.getPostedBy().getUsername().equals(clientName))
                                        {
                                            userProfile.removewallPost(entry.id, entry.s.getShareWith(), clientName, true);
                                        }
                                    }else if (pc.getType().equals("photo")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (entry.p.getPostedBy().getUsername().equals(clientName))
                                        {
                                            userProfile.removePhoto(entry.p.getParent(), entry.id, entry.p.getShareWith(), clientName, true);
                                        }
                                    }else if (pc.getType().equals("photoAlbum")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (entry.pa.getPostedBy().getUsername().equals(clientName))
                                        {
                                            userProfile.removePhotoAlbum(entry.id, entry.pa.getShareWith(), clientName, true);
                                        }
                                    }else if (pc.getType().equals("video")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (entry.v.getPostedBy().getUsername().equals(clientName))
                                        {
                                            userProfile.removeVideo(entry.v.getParent(), entry.id, entry.v.getShareWith(), clientName, true);
                                        }
                                    }else if (pc.getType().equals("videoAlbum")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (entry.va.getPostedBy().getUsername().equals(clientName))
                                        {
                                            userProfile.removeVideoAlbum(entry.id, entry.va.getShareWith(), clientName, true);
                                        }
                                    }else if (pc.getType().equals("audio")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (entry.a.getPostedBy().getUsername().equals(clientName))
                                        {
                                            userProfile.removeAudio(entry.a.getParent(), entry.id, entry.a.getShareWith(), clientName, true);
                                        }
                                    }else if (pc.getType().equals("audioAlbum")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (entry.aa.getPostedBy().getUsername().equals(clientName))
                                        {
                                            userProfile.removeAudioAlbum(entry.id, entry.aa.getShareWith(), clientName, true);
                                        }
                                    }else if (pc.getType().equals("linkComment")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<comment> comments = entry.l.getComments();
                                        if (comments != null){
                                            for (int j = 0; j < comments.size(); j++){
                                                comment c = new comment(comments.get(j));
                                                if (c.getUser().getUsername().equals(clientName)){
                                                    userProfile.removeComment(c.getType(), c.getGrandParent(), c.getParent(), c.getId(), c.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("wallPostComment")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<comment> comments = entry.s.getComments();
                                        if (comments != null){
                                            for (int j = 0; j < comments.size(); j++){
                                                comment c = new comment(comments.get(j));
                                                if (c.getUser().getUsername().equals(clientName)){
                                                    userProfile.removeComment(c.getType(), c.getGrandParent(), c.getParent(), c.getId(), c.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("audioAlbumComment")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<comment> comments = entry.aa.getComments();
                                        if (comments != null){
                                            for (int j = 0; j < comments.size(); j++){
                                                comment c = new comment(comments.get(j));
                                                if (c.getUser().getUsername().equals(clientName)){
                                                    userProfile.removeComment(c.getType(), c.getGrandParent(), c.getParent(), c.getId(), c.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("videoAlbumComment")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<comment> comments = entry.va.getComments();
                                        if (comments != null){
                                            for (int j = 0; j < comments.size(); j++){
                                                comment c = new comment(comments.get(j));
                                                if (c.getUser().getUsername().equals(clientName)){
                                                    userProfile.removeComment(c.getType(), c.getGrandParent(), c.getParent(), c.getId(), c.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("photoAlbumComment")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<comment> comments = entry.pa.getComments();
                                        if (comments != null){
                                            for (int j = 0; j < comments.size(); j++){
                                                comment c = new comment(comments.get(j));
                                                if (c.getUser().getUsername().equals(clientName)){
                                                    userProfile.removeComment(c.getType(), c.getGrandParent(), c.getParent(), c.getId(), c.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("audioComment")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<comment> comments = entry.a.getComments();
                                        if (comments != null){
                                            for (int j = 0; j < comments.size(); j++){
                                                comment c = new comment(comments.get(j));
                                                if (c.getUser().getUsername().equals(clientName)){
                                                    userProfile.removeComment(c.getType(), c.getGrandParent(), c.getParent(), c.getId(), c.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("videoComment")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<comment> comments = entry.v.getComments();
                                        if (comments != null){
                                            for (int j = 0; j < comments.size(); j++){
                                                comment c = new comment(comments.get(j));
                                                if (c.getUser().getUsername().equals(clientName)){
                                                    userProfile.removeComment(c.getType(), c.getGrandParent(), c.getParent(), c.getId(), c.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("photoComment")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<comment> comments = entry.p.getComments();
                                        if (comments != null){
                                            for (int j = 0; j < comments.size(); j++){
                                                comment c = new comment(comments.get(j));
                                                if (c.getUser().getUsername().equals(clientName)){
                                                    userProfile.removeComment(c.getType(), c.getGrandParent(), c.getParent(), c.getId(), c.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("linkLike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<like> likes = entry.l.getLikes();
                                        if (likes != null){
                                            for (int j = 0; j < likes.size(); j++){
                                                like li = new like(likes.get(j));
                                                if (li.getUser().getUsername().equals(clientName)){
                                                    userProfile.removelike(li.getType(), li.getGrandParent(), li.getParent(), li.getId(), li.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("wallPostLike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<like> likes = entry.s.getLikes();
                                        if (likes != null){
                                            for (int j = 0; j < likes.size(); j++){
                                                like li = new like(likes.get(j));
                                                if (li.getUser().getUsername().equals(clientName)){
                                                    userProfile.removelike(li.getType(), li.getGrandParent(), li.getParent(), li.getId(), li.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("audioAlbumLike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<like> likes = entry.aa.getLikes();
                                        if (likes != null){
                                            for (int j = 0; j < likes.size(); j++){
                                                like li = new like(likes.get(j));
                                                if (li.getUser().getUsername().equals(clientName)){
                                                    userProfile.removelike(li.getType(), li.getGrandParent(), li.getParent(), li.getId(), li.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("videoAlbumLike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<like> likes = entry.va.getLikes();
                                        if (likes != null){
                                            for (int j = 0; j < likes.size(); j++){
                                                like li = new like(likes.get(j));
                                                if (li.getUser().getUsername().equals(clientName)){
                                                    userProfile.removelike(li.getType(), li.getGrandParent(), li.getParent(), li.getId(), li.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("photoAlbumLike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<like> likes = entry.pa.getLikes();
                                        if (likes != null){
                                            for (int j = 0; j < likes.size(); j++){
                                                like li = new like(likes.get(j));
                                                if (li.getUser().getUsername().equals(clientName)){
                                                    userProfile.removelike(li.getType(), li.getGrandParent(), li.getParent(), li.getId(), li.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("audioLike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<like> likes = entry.a.getLikes();
                                        if (likes != null){
                                            for (int j = 0; j < likes.size(); j++){
                                                like li = new like(likes.get(j));
                                                if (li.getUser().getUsername().equals(clientName)){
                                                    userProfile.removelike(li.getType(), li.getGrandParent(), li.getParent(), li.getId(), li.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("videoLike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<like> likes = entry.v.getLikes();
                                        if (likes != null){
                                            for (int j = 0; j < likes.size(); j++){
                                                like li = new like(likes.get(j));
                                                if (li.getUser().getUsername().equals(clientName)){
                                                    userProfile.removelike(li.getType(), li.getGrandParent(), li.getParent(), li.getId(), li.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("photoLike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<like> likes = entry.p.getLikes();
                                        if (likes != null){
                                            for (int j = 0; j < likes.size(); j++){
                                                like li = new like(likes.get(j));
                                                if (li.getUser().getUsername().equals(clientName)){
                                                    userProfile.removelike(li.getType(), li.getGrandParent(), li.getParent(), li.getId(), li.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("linkDislike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<dislike> dislikes = entry.l.getDislikes();
                                        if (dislikes != null){
                                            for (int j = 0; j < dislikes.size(); j++){
                                                dislike dli = new dislike(dislikes.get(j));
                                                if (dli.getUser().getUsername().equals(clientName)){
                                                    userProfile.removeDislike(dli.getType(), dli.getGrandParent(), dli.getParent(), dli.getId(), dli.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("wallPostDislike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<dislike> dislikes = entry.s.getDislikes();
                                        if (dislikes != null){
                                            for (int j = 0; j < dislikes.size(); j++){
                                                dislike dli = new dislike(dislikes.get(j));
                                                if (dli.getUser().getUsername().equals(clientName)){
                                                    userProfile.removeDislike(dli.getType(), dli.getGrandParent(), dli.getParent(), dli.getId(), dli.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("audioAlbumDislike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<dislike> dislikes = entry.aa.getDislikes();
                                        if (dislikes != null){
                                            for (int j = 0; j < dislikes.size(); j++){
                                                dislike dli = new dislike(dislikes.get(j));
                                                if (dli.getUser().getUsername().equals(clientName)){
                                                    userProfile.removeDislike(dli.getType(), dli.getGrandParent(), dli.getParent(), dli.getId(), dli.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("videoAlbumDislike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<dislike> dislikes = entry.va.getDislikes();
                                        if (dislikes != null){
                                            for (int j = 0; j < dislikes.size(); j++){
                                                dislike dli = new dislike(dislikes.get(j));
                                                if (dli.getUser().getUsername().equals(clientName)){
                                                    userProfile.removeDislike(dli.getType(), dli.getGrandParent(), dli.getParent(), dli.getId(), dli.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("photoAlbumDislike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<dislike> dislikes = entry.pa.getDislikes();
                                        if (dislikes != null){
                                            for (int j = 0; j < dislikes.size(); j++){
                                                dislike dli = new dislike(dislikes.get(j));
                                                if (dli.getUser().getUsername().equals(clientName)){
                                                    userProfile.removeDislike(dli.getType(), dli.getGrandParent(), dli.getParent(), dli.getId(), dli.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("audioDislike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<dislike> dislikes = entry.a.getDislikes();
                                        if (dislikes != null){
                                            for (int j = 0; j < dislikes.size(); j++){
                                                dislike dli = new dislike(dislikes.get(j));
                                                if (dli.getUser().getUsername().equals(clientName)){
                                                    userProfile.removeDislike(dli.getType(), dli.getGrandParent(), dli.getParent(), dli.getId(), dli.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("videoDislike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<dislike> dislikes = entry.v.getDislikes();
                                        if (dislikes != null){
                                            for (int j = 0; j < dislikes.size(); j++){
                                                dislike dli = new dislike(dislikes.get(j));
                                                if (dli.getUser().getUsername().equals(clientName)){
                                                    userProfile.removeDislike(dli.getType(), dli.getGrandParent(), dli.getParent(), dli.getId(), dli.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("photoDislike")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        List<dislike> dislikes = entry.p.getDislikes();
                                        if (dislikes != null){
                                            for (int j = 0; j < dislikes.size(); j++){
                                                dislike dli = new dislike(dislikes.get(j));
                                                if (dli.getUser().getUsername().equals(clientName)){
                                                    userProfile.removeDislike(dli.getType(), dli.getGrandParent(), dli.getParent(), dli.getId(), dli.getShareWith(), clientName, true);
                                                }
                                            }
                                        }
                                    }else if (pc.getType().equals("event")){
                                        timelineEntry entry = new timelineEntry(pc.getEntry());
                                        if (clientName.equals(entry.e.getCreator().getUsername())){
                                            userProfile.removeEvent(entry.e.getId());
                                        }
                                    }
                                }
                            }else if (pc.getAction().equals("update")){
                                if (pc.getType().equals("event")){
                                    timelineEntry entry = new timelineEntry(pc.getEntry());
                                    if (clientName.equals(entry.e.getCreator().getUsername())){
                                        userProfile.addEvent(entry.e, true);
                                    }
                                }else if (pc.getType().equals("mirror")){
                                    timelineEntry entry = new timelineEntry(pc.getEntry());
                                    synchronized(mainSettings.mirrors){
                                        mainSettings.updateMirror(clientName, entry.mi);
                                    }
                                }
                            }else if (pc.getAction().equals("request")){
                                if (pc.getType().equals("mirror")){
                                    timelineEntry entry = new timelineEntry(pc.getEntry());
                                    entry.mi.setUsername(clientName);
                                    if (mainSettings.username.equals(username)){
                                        synchronized(mainSettings.receivedMirroringRequests){
                                            mainSettings.refresh(mainSettings.RECEIVEDMIRRORINGREQUESTS);
                                            if (mainSettings.receivedMirroringRequests.size() > 0){
                                                mainSettings.receivedMirroringRequests.add(0, entry.mi);
                                            }else{
                                                mainSettings.receivedMirroringRequests.add(entry.mi);
                                            }
                                            mainSettings.save(mainSettings.RECEIVEDMIRRORINGREQUESTS);
                                        }
                                    }else{
                                        List<mirror> receivedMirroringRequests = new ArrayList();
                                        File file = new File(prefix + "mirroring/" + username + "/receivedMirroringRequests.xml");
                                        if (file.exists()){
                                            fis = new FileInputStream(file);
                                            channel = fis.getChannel();
                                            lock = null;
                                            while (lock == null){
                                                try{
                                                    lock = channel.tryLock(0L, Long.MAX_VALUE, true);
                                                }catch(Exception e){
                                                    if (DEBUG){
                                                        e.printStackTrace();
                                                    }
                                                    lock = null;
                                                }
                                                Thread.yield();
                                            }
                                            baos = new ByteArrayOutputStream();
                                            byte[] b = new byte[100000];
                                            ByteBuffer buf = ByteBuffer.wrap(b);
                                            int count = 0;
                                            while((count = channel.read(buf)) > 0){
                                                baos.write(b, 0, count);
                                                buf.rewind();
                                            }
                                            byte[] readIn = baos.toByteArray();
                                            baos.close();
                                            lock.release();
                                            channel.close();
                                            fis.close();
                                            receivedMirroringRequests.clear();
                                            dom = db.parse(new ByteArrayInputStream(readIn));
                                            docEle = dom.getDocumentElement();
                                            NodeList ml = docEle.getElementsByTagName("mirror");
                                            if(ml != null && ml.getLength() > 0) {
                                                for (int j = 0; j < ml.getLength(); j++){
                                                    Element mel = (Element)ml.item(j);
                                                    mirror m = new mirror();
                                                    m.create(mel);
                                                    receivedMirroringRequests.add(m);
                                                }
                                            }
                                        }
                                        if (receivedMirroringRequests.size() > 0){
                                            receivedMirroringRequests.add(0, entry.mi);
                                        }else{
                                            receivedMirroringRequests.add(entry.mi);
                                        }
                                        dom = db.newDocument();
                                        Element rootEle = dom.createElement("receivedMirroringRequests");
                                        dom.appendChild(rootEle);
                                        for (int j = 0; j < receivedMirroringRequests.size(); j++){
                                            mirror m = new mirror(receivedMirroringRequests.get(j));
                                            rootEle.appendChild(m.createDOMElement(dom));
                                        }
                                        saveXML(prefix + "mirroring/" + username + "/receivedMirroringRequests.xml", dom);
                                    }
                                }
                            }else if (pc.getAction().equals("respond")){
                                if (pc.getType().equals("event")){
                                    userProfile.loadEvents();
                                    timelineEntry entry = new timelineEntry(pc.getEntry());
                                    for (int j = 0; j < userProfile.events.size(); j++){
                                        event e = new event(userProfile.events.get(j));
                                        if (e.getId() == entry.e.getId()){
                                            List<user> invitees = e.getInvitees();
                                            user u = new user();
                                            int k = 0;
                                            for (k = 0; k < invitees.size(); k++){
                                                u = new user(invitees.get(k));
                                                if (u.getUsername().equals(clientName)){
                                                    break;
                                                }
                                            }
                                            if (k < invitees.size()){
                                                if (entry.e.getDecision().equals("Not Attending")){
                                                    e.removeAccepted(clientName);
                                                    e.addDeclined(u);
                                                    userProfile.events.add(j, e);
                                                    userProfile.events.remove(j + 1);
                                                    userProfile.saveEvents();
                                                    userProfile.updatePendingChanges("update", "event", new timelineEntry(e.getId(), "event", e));
                                                }else if (entry.e.getDecision().equals("Attending")){
                                                    e.removeDeclined(clientName);
                                                    e.addAccepted(u);
                                                    userProfile.events.add(j, e);
                                                    userProfile.events.remove(j + 1);
                                                    userProfile.saveEvents();
                                                    userProfile.updatePendingChanges("update", "event", new timelineEntry(e.getId(), "event", e));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (files.size() > 0){
                        long traffic = 0;
                        for (int i = 0; i < files.size(); i++){
                            String s = "SEND_FILE|";
                            String filename = (String)files.get(i);
                            s += filename + "|";
                            if (DEBUG){
                                System.out.println("DEBUG: line 2129 of servingThread.java. " + s);
                            }
                            if (filename.contains("/audios/")){
                                filename = filename.substring(filename.lastIndexOf("/audios/"), filename.length());
                            }else if (filename.contains("/videos/")){
                                filename = filename.substring(filename.lastIndexOf("/videos/"), filename.length());
                            }else if (filename.contains("/photos/")){
                                filename = filename.substring(filename.lastIndexOf("/photos/"), filename.length());
                            }
                            if (username.equals(mainSettings.username)){
                                filename = prefix + username + filename;
                            }else{
                                filename = prefix + "mirroring/" + username + filename;
                            }
                            File file = new File(filename);
                            response = s.getBytes("UTF-8");
                            conn.send(response);
                            response = conn.receive();
                            long filelength = 0;
                            long fileSize = 0;
                            s = new String(response, "UTF-8");
                            if (s.lastIndexOf("NOT_FOUND") == 0){
                                continue;
                            }else{
                                filelength = Long.parseLong(s.substring(0, s.indexOf("|")));
                                fileSize = filelength;
                            }
                            if (!filename.contains(".xml") && file.exists() && file.length() == filelength){
                                s = "SKIP";
                                response = s.getBytes("UTF-8");
                                conn.send(response);
                                continue;
                            }
                            if (file.exists()){
                                fos = new FileOutputStream(file);
                                channel = fos.getChannel();
                                lock = null;
                                while (lock == null){
                                    try{
                                        lock = channel.tryLock();
                                    }catch(Exception e){
                                        if (DEBUG){
                                            e.printStackTrace();
                                        }
                                        lock = null;
                                    }
                                    Thread.yield();
                                }
                                int retries = 0;
                                s = "READY";
                                response = s.getBytes("UTF-8");
                                conn.send(response);
                                while (filelength > 0){
                                    byte[] b = conn.receive();
                                    ByteBuffer buf = ByteBuffer.wrap(b);
                                    channel.write(buf);
                                    filelength -= b.length;
                                    if (b.length > 0){
                                        retries = 0;
                                        response = new String("ACK").getBytes("UTF-8");
                                        conn.send(response);
                                    }else{
                                        if (retries == 20){
                                            lock.release();
                                            channel.close();
                                            fos.flush();
                                            fos.close();
                                            break;
                                        }
                                        response = new String("NACK").getBytes("UTF-8");
                                        conn.send(response);
                                        retries++;
                                    }
                                }
                                lock.release();
                                channel.close();
                                fos.flush();
                                fos.close();
                                if (!filename.contains(".xml")){
                                    traffic += fileSize;
                                }
                                String dirPath = filename.replaceFirst(prefix, "./MyZone/");
                                dirPath = dirPath.substring(0, dirPath.lastIndexOf("/") + 1);
                                userProfile.updateModifiedFiles("add", Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), dirPath + filename.substring(filename.lastIndexOf("/") + 1, filename.length()), filesAssociations.get(i));
                            }else{
                                fos = new FileOutputStream(file);
                                s = "READY";
                                response = s.getBytes("UTF-8");
                                conn.send(response);
                                int retries = 0;
                                String dirPath = filename.replaceFirst(prefix, "./MyZone/");
                                dirPath = dirPath.substring(0, dirPath.lastIndexOf("/") + 1);
                                fileListEntry fle = new fileListEntry(username, filesAssociations.get(i), dirPath, filename.substring(filename.lastIndexOf("/") + 1, filename.length()), filelength);
                                while (filelength > 0){
                                    byte[] buf = conn.receive();
                                    fos.write(buf, 0, buf.length);
                                    filelength -= buf.length;
                                    if (buf.length > 0){
                                        retries = 0;
                                        response = new String("ACK").getBytes("UTF-8");
                                        conn.send(response);
                                    }else{
                                        if (retries == 20){
                                            break;
                                        }
                                        response = new String("NACK").getBytes("UTF-8");
                                        conn.send(response);
                                        retries++;
                                    }
                                }
                                fos.flush();
                                fos.close();
                                if (!filename.contains(".xml")){
                                    traffic += fileSize;
                                }
                                if (!filename.contains(".xml")){
                                    userProfile.updateImage("correctImage", "add", fle);
                                    userProfile.updateImage("existingImage", "add", fle);
                                }
                                userProfile.updateModifiedFiles("add", Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), dirPath + filename.substring(filename.lastIndexOf("/") + 1, filename.length()), filesAssociations.get(i));
                            }
                        }
                        if (!mainSettings.username.equals(username)){
                            int i = 0;
                            synchronized(mainSettings.originals){
                                mainSettings.refresh(mainSettings.ORIGINALS);
                                mirror m = new mirror();
                                for (i = 0; i < mainSettings.originals.size(); i++){
                                    m = new mirror(mainSettings.originals.get(i));
                                    if (m.getUsername().equals(username)){
                                        break;
                                    }
                                }
                                if (i < mainSettings.originals.size()){
                                    m.setUsed(m.getUsed() + traffic);
                                    mainSettings.updateOriginal(m);
                                }
                            }
                        }            
                    }
                    String s = "DONE";
                    response = s.getBytes("UTF-8");
                    conn.send(response);
                    response = conn.receive();
                    s = new String(response, "UTF-8");
                    if (s.indexOf("SEND_UPDATES") != 0){
                        return false;
                    }
                    sendUpdates(username, latestVersion);
                }
                return true;
            }catch(Exception e){
                if (DEBUG){
                    e.printStackTrace();
                }
                return false;
            }finally{
                try{
                    if (baos != null)
                        baos.close();
                    if (lock != null){
                        if (channel.isOpen()){
                            lock.release();
                            channel.close();
                        }
                    }
                    if (channel != null){
                        channel.close();
                    }
                    if (fis != null)
                        fis.close();
                    if (fos != null)
                        fos.close();
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
        }
        
        private boolean sendUpdates(String username, long latestVersion){
            if (!mainSettings.username.equals(username)){
                int i = 0;
                synchronized(mainSettings.originals){
                    mainSettings.refresh(mainSettings.ORIGINALS);
                    for (i = 0; i < mainSettings.originals.size(); i++){
                        mirror m = new mirror(mainSettings.originals.get(i));
                        if (m.getUsername().equals(username)){
                            break;
                        }
                    }
                    if (i == mainSettings.originals.size()){
                        return false;
                    }
                }
            }
            FileChannel channel = null;
            FileLock lock = null;
            FileInputStream fis = null;
            try{
                String s = "GET_READY_FOR_UPDATES";
                byte[] response = s.getBytes("UTF-8");
                conn.send(response);
                response = conn.receive();
                s = new String(response, "UTF-8");
                if (s.equals("READY")){
                    List<String> files = getModifiedFiles(username, clientName, latestVersion);
                    if (DEBUG){
                        System.out.println("DEBUG: line 2338 of servingThread.java. Number of modified files = " + files.size());
                    }
                    for (int i = 0; i < files.size(); i++){
                        String absoluteFilename = new String(files.get(i));
                        String filename = "";
                        if (DEBUG){
                            System.out.println("DEBUG: line 2344 of servingThread.java. " + absoluteFilename);
                        }
                        filename = absoluteFilename.substring(absoluteFilename.indexOf(username), absoluteFilename.length());
                        File toBeSend = new File(absoluteFilename);
                        if (!toBeSend.exists())
                            continue;
                        if (absoluteFilename.contains(".xml")){
                            fis = new FileInputStream(toBeSend);
                            channel = fis.getChannel();
                            lock = null;
                            while (lock == null){
                                try{
                                    lock = channel.tryLock(0L, Long.MAX_VALUE, true);
                                }catch(Exception e){
                                    if (DEBUG){
                                        e.printStackTrace();
                                    }
                                    lock = null;
                                }
                                Thread.yield();
                            }
                            byte[] b = new byte[100000];
                            ByteBuffer buf = ByteBuffer.wrap(b);
                            int bytesRead = 0;
                            byte[] tosend;
                            // SENDING_FILE|FILENAME|FILE_LENGTH|
                            s = "SENDING_FILE|" + filename + "|" + toBeSend.length() + "|";
                            
                            if (filename.contains("history.xml")){
                                lock.release();
                                channel.close();
                                fis.close();
                                continue;
                            }
                            response = s.getBytes("UTF-8");
                            conn.send(response);
                            byte[] ackBytes = conn.receive();
                            String ack = new String(ackBytes, "UTF-8");
                            if (ack.indexOf("READY") != 0){
                                lock.release();
                                channel.close();
                                fis.close();
                                continue; // skip to the next file
                            }
                            int retries = 0;
                            long filelength = toBeSend.length();
                            while (-1 != (bytesRead = channel.read(buf)) && retries < 20){
                                tosend = new byte[bytesRead];
                                filelength -= bytesRead;
                                System.arraycopy(b, 0, tosend, 0, bytesRead);
                                conn.send(tosend);
                                ackBytes = conn.receive();
                                ack = new String(ackBytes, "UTF-8");
                                retries = 0;
                                while (!ack.equals("ACK") && retries < 20){
                                    conn.send(tosend);
                                    ackBytes = conn.receive();
                                    ack = new String(ackBytes, "UTF-8");
                                    retries++;
                                }
                                buf.rewind();
                            }
                            lock.release();
                            channel.close();
                            fis.close();
                        }else{
                            fis = new FileInputStream(toBeSend);
                            byte[] buf = new byte[100000];
                            int bytesRead = 0;
                            byte[] tosend;
                            // SENDING_FILE|FILENAME|FILE_LENGTH|
                            s = "SENDING_FILE|" + filename + "|" + toBeSend.length() + "|";
                            
                            response = s.getBytes("UTF-8");
                            conn.send(response);
                            byte[] ackBytes = conn.receive();
                            String ack = new String(ackBytes, "UTF-8");
                            if (ack.indexOf("READY") != 0){
                                fis.close();
                                continue; // skip to the next file
                            }
                            int retries = 0;
                            long filelength = toBeSend.length();
                            while (-1 != (bytesRead = fis.read(buf, 0, buf.length)) && retries < 20){
                                tosend = new byte[bytesRead];
                                filelength -= bytesRead;
                                System.arraycopy(buf, 0, tosend, 0, bytesRead);
                                conn.send(tosend);
                                ackBytes = conn.receive();
                                ack = new String(ackBytes, "UTF-8");
                                retries = 0;
                                while (!ack.equals("ACK") && retries < 20){
                                    conn.send(tosend);
                                    ackBytes = conn.receive();
                                    ack = new String(ackBytes, "UTF-8");
                                    retries++;
                                }
                            }
                            fis.close();
                        }
                        if (absoluteFilename.contains("/tmp")){
                            toBeSend.delete();
                        }
                    }
                    s = "DONE";
                    response = s.getBytes("UTF-8");
                    conn.send(response);
                }
                return true;
            }catch(Exception e){
                if (DEBUG){
                    e.printStackTrace();
                }
                return false;
            }finally{
                try{
                    if (lock != null){
                        if (channel.isOpen()){
                            lock.release();
                            channel.close();
                        }
                    }
                    if (channel != null){
                        channel.close();
                    }
                    if (fis != null)
                        fis.close();
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
        }
        
        public void run(){
            try{
                if (DEBUG){
                    System.out.println("DEBUG: line 2482 of servingThread.java. clientName = " + clientName);
                }
                // type of request:
                // GET|USERNAME|LATESTVERSION|
                // POST|USERNAME|LATESTVERSION|
                // SYNC|USERNAME|LASTSYNCTIME|
                byte[] recvMsg = conn.receive();
                String s = new String(recvMsg, "UTF-8");
                if (s.indexOf("GET") == 0){
                    if (s.indexOf("|") == 3){
                        String username = s.substring(4, s.indexOf("|", 4));
                        if (DEBUG){
                            System.out.println("DEBUG: line 2494 of servingThread.java. username = " + username);
                        }
                        long latestVersion = Long.parseLong(s.substring(s.indexOf("|", 4) + 1, s.indexOf("|", s.indexOf("|", 4) + 1)));
                        sendUpdates(username, latestVersion);
                        conn.close();
                        sweeper = new cleaner(prefix, username);
                        sweeper.clean();
                    }
                }else if (s.indexOf("POST") == 0){
                    if (s.indexOf("|") == 4){
                        String username = s.substring(5, s.indexOf("|", 5));
                        if (DEBUG){
                            System.out.println("DEBUG: line 2506 of servingThread.java. ********* RECEIVED A POST *********");
                            System.out.println("DEBUG: line 2507 of servingThread.java. username = " + username);
                        }
                        long latestVersion = Long.parseLong(s.substring(s.indexOf("|", 5) + 1, s.indexOf("|", s.indexOf("|", 5) + 1)));
                        if (DEBUG){
                            System.out.println("DEBUG: line 2510 of servingThread.java. latestVersion = " + latestVersion);
                        }
                        receiveUpdates(username, latestVersion);
                        conn.close();
                        sweeper = new cleaner(prefix, username);
                        sweeper.clean();
                    }
                }else if (s.indexOf("SYNC") == 0){
                    if (DEBUG){
                        System.out.println("DEBUG: line 2520 of servingThread.java. ******** going to sync ********");
                    }
                    if (s.indexOf("|") == 4){
                        String username = s.substring(5, s.indexOf("|", 5));
                        if (DEBUG){
                            System.out.println("DEBUG: line 2525 of servingThread.java. syncing with username = " + username);
                        }
                        long latestVersion = Long.parseLong(s.substring(s.indexOf("|", 5) + 1, s.indexOf("|", s.indexOf("|", 5) + 1)));
                        if (DEBUG){
                            System.out.println("DEBUG: line 2529 of servingThread.java. last sync time = " + latestVersion);
                        }
                        sync(username, latestVersion);
                        conn.close();
                        sweeper = new cleaner(prefix, username);
                        sweeper.clean();
                    }
                }else{
                    conn.close();
                }
                if (DEBUG){
                    System.out.println("DEBUG: line 2540 of servingThread.java. SERVED");
                }
            }catch(IOException e){
                if (DEBUG){
                    e.printStackTrace();
                }
                if (conn != null){
                    if (!conn.isClosed()){
                        conn.close();
                    }
                }
            }
        }
    }
}



