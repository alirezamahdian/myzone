/* 
 =============================================================================  
 |Copyright (C) 2012  Alireza Mahdian email:alireza.mahdian@colorado.edu     |
 |This program is free software: you can redistribute but NOT modify 	     |
 |it under the terms of the GNU General Public License as published by       |
 |the Free Software Foundation, either version 3 of the License, or          |
 |(at your option) any later version. Alireza Mahdian reservers all the      |
 |commit rights of this code.                                                |
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
import MyZone.*;

/* this file contains the implementation of the cleaner class.
 cleaner class is responsible for removal of files that are either 
 removed from the host's profile or files that are old and are taking
 the capacity of cache as defined in the settings. 
 the old files are only removed if the current cache space has reached
 its maximum capacity. same principle applies for mirrored profiles.
 */

public class cleaner {
    
    private final static boolean DEBUG = false;
    private String accountHolder;
    private String prefix;
    public List<fileListEntry> existingList;
    public List<fileListEntry> correctList;
    Settings mainSettings;
    
    public cleaner(String prefix, String accountHolder){
        this.prefix = prefix;
        this.accountHolder = accountHolder;
        existingList = new ArrayList();
        correctList = new ArrayList();
        mainSettings = new Settings(prefix);
        mainSettings.refresh(mainSettings.BASIC_INFO);
        if (!mainSettings.username.equals(accountHolder)){
            this.prefix = prefix + "mirroring/";
        }
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
                    Thread.sleep(100);
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
    
    public boolean clean(){
        try {
            mainSettings.refresh(mainSettings.ORIGINALS);
            long cacheSize = 500 * 1024 * 1024;
            long used = 0;
            mirror m = new mirror();
            List<zone> zones = new ArrayList<zone>();
            
            int mirrorIndex = 0;
            if (accountHolder.equals(mainSettings.username)){
                cacheSize = mainSettings.cacheSize * 1024 * 1024;
            }else{
                for (mirrorIndex = 0; mirrorIndex < mainSettings.originals.size(); mirrorIndex++){
                    m = new mirror(mainSettings.originals.get(mirrorIndex));
                    if (m.getUsername().equals(accountHolder)){
                        cacheSize = m.getCapacity();
                        break;
                    }
                }
                if (mirrorIndex == mainSettings.originals.size())
                    return false;
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document existingDom, correctDom, zonesDom;
            FileInputStream fis = null;
            FileInputStream efis = null;
            FileInputStream cfis = null;
            ByteArrayOutputStream baos = null;
            byte[] b = new byte[1024];
            int count = 0;
            File correctImageFile = new File(prefix + accountHolder + "/correctImage.xml");
            if (!correctImageFile.exists()){
                return false;
            }
            File existingImageFile = new File(prefix + accountHolder + "/existingImage.xml");
            if (!existingImageFile.exists()){
                return false;
            }
            File zonesFile = new File(prefix + accountHolder + "/zones.xml");
            if (!zonesFile.exists()){
                return false;
            }
            fis = new FileInputStream(zonesFile);
            FileChannel zonesFileChannel = fis.getChannel();
            FileLock zonesFileLock = null;
            while ((zonesFileLock = zonesFileChannel.tryLock(0L, Long.MAX_VALUE, true)) == null){
                if (DEBUG){
                    Thread.sleep(50);
                }
            }
            ByteBuffer buf = ByteBuffer.wrap(b);
            baos = new ByteArrayOutputStream();
            count = 0;
            while((count = zonesFileChannel.read(buf)) > 0){
                baos.write(b, 0, count);
                buf.rewind();
            }
            byte[] readIn = baos.toByteArray();
            zonesFileLock.release();
            zonesFileChannel.close();
            fis.close();
            baos.close();
            zonesDom = db.parse(new ByteArrayInputStream(readIn));
            Element docEle = zonesDom.getDocumentElement();
            NodeList zl = docEle.getElementsByTagName("zone");
            if(zl != null && zl.getLength() > 0) {
                for (int i = 0; i < zl.getLength(); i++){
                    Element zel = (Element)zl.item(i);
                    zone z = new zone();
                    z.create(zel);
                    zones.add(z);
                }
            }
            cfis = new FileInputStream(correctImageFile);
            FileChannel channelCorrectImageFile = cfis.getChannel();
            FileLock correctLock = channelCorrectImageFile.lock();
            while (correctLock == null){
                if (DEBUG){
                    Thread.sleep(50);
                }
                correctLock = channelCorrectImageFile.tryLock();
            }
            baos = new ByteArrayOutputStream();
            count = 0;
            while((count = channelCorrectImageFile.read(buf)) > 0){
                baos.write(b, 0, count);
                buf.rewind();
            }
            readIn = baos.toByteArray();
            baos.close();
            correctDom = db.parse(new ByteArrayInputStream(readIn));
            
            efis = new FileInputStream(existingImageFile);
            FileChannel channelExistingImageFile = efis.getChannel();
            FileLock existingLock = channelExistingImageFile.lock();
            while (existingLock == null){
                if (DEBUG){
                    Thread.sleep(50);
                }
                existingLock = channelExistingImageFile.tryLock();
            }
            baos = new ByteArrayOutputStream();
            count = 0;
            while((count = channelExistingImageFile.read(buf)) > 0){
                baos.write(b, 0, count);
                buf.rewind();
            }
            readIn = baos.toByteArray();
            baos.close();
            existingDom = db.parse(new ByteArrayInputStream(readIn));
            docEle = correctDom.getDocumentElement();
            
            NodeList nl = docEle.getElementsByTagName("fileListEntry");
            if(nl != null && nl.getLength() > 0) {
                for(int i = 0 ; i < nl.getLength(); i++) {
                    Element el = (Element)nl.item(i);
                    fileListEntry fle = new fileListEntry();
                    fle.create(el);
                    if (accountHolder.equals(mainSettings.username)){
                        if (mainSettings.isFriend(fle.getOwner()) || fle.getOwner().equals(accountHolder)){
                            if (fle.getOwner().equals(accountHolder)){
                                for (int j = 0; j < zones.size(); j++){
                                    zone z = new zone(zones.get(j));
                                    if (z.getName().equals(fle.getZone())){
                                        correctList.add(fle);
                                        used += fle.getFilesize();
                                        break;
                                    }
                                }
                            }else{
                                correctList.add(fle);
                                used += fle.getFilesize();
                            }
                        }
                    }else{
                        if (fle.getOwner().equals(accountHolder)){
                            for (int j = 0; j < zones.size(); j++){
                                zone z = new zone(zones.get(j));
                                if (z.getName().equals(fle.getZone())){
                                    correctList.add(fle);
                                    used += fle.getFilesize();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (used > cacheSize){
                for (int i = correctList.size() - 1; i >= 0; i--){
                    fileListEntry fle = new fileListEntry(correctList.get(i));
                    if (!fle.getOwner().equals(accountHolder) && used > cacheSize){
                        used -= fle.getFilesize();
                        correctList.remove(i);
                    }
                }
            }
            if (used > cacheSize){
                for (int i = correctList.size() - 1; i >= 0; i--){
                    fileListEntry fle = new fileListEntry(correctList.get(i));
                    if (used > cacheSize){
                        used -= fle.getFilesize();
                        correctList.remove(i);
                    }else{
                        break;
                    }
                }
            }
            if (!accountHolder.equals(mainSettings.username)){
                m.setUsed(used);
                mainSettings.updateOriginal(m);
            }
            docEle = existingDom.getDocumentElement();
            
            nl = docEle.getElementsByTagName("fileListEntry");
            if(nl != null && nl.getLength() > 0) {
                for(int i = 0 ; i < nl.getLength(); i++) {
                    Element el = (Element)nl.item(i);
                    fileListEntry fle = new fileListEntry();
                    fle.create(el);
                    existingList.add(fle);
                    
                }
            }
            boolean success;
            boolean delete;
            for (int i = 0; i < existingList.size(); i++){
                fileListEntry fle = new fileListEntry(existingList.get(i));
                delete = true;
                success = false;
                for (int j = 0; j < correctList.size(); j++){
                    fileListEntry ele = new fileListEntry(correctList.get(j));
                    if ( new String(ele.getPath() + ele.getFilename()).equals(new String(fle.getPath() + fle.getFilename())) ){
                        delete = false;
                        break;
                    }
                }
                if (delete){
                    try{
                        if (!(new File(fle.getPath() + fle.getFilename())).exists())
                            success = true;
                        else
                            success = (new File(fle.getPath() + fle.getFilename())).delete();
                    }catch(Exception e){
                        if (DEBUG){
                            e.printStackTrace();
                        }
                    }
                    if (success) {
                        existingList.remove(fle);
                    }
                }
            }
            existingLock.release();
            channelExistingImageFile.close();
            efis.close();
            if (!accountHolder.equals(mainSettings.username)){
                File directory = new File(prefix + accountHolder + "/photos");
                File[] fileList = null;
                if (directory.exists()){
                    fileList = directory.listFiles();
                    for (int i = 0; i < fileList.length; i++){
                        int j = 0;
                        for (j = 0; j < correctList.size(); j++){
                            fileListEntry fle = new fileListEntry(correctList.get(j));
                            if (fle.getFilename().contains(fileList[i].getName())){
                                break;
                            }
                        }
                        if (j == correctList.size()){
                            fileList[i].delete();
                        }
                    }
                }
                directory = new File(prefix + accountHolder + "/videos");
                fileList = null;
                if (directory.exists()){
                    fileList = directory.listFiles();
                    for (int i = 0; i < fileList.length; i++){
                        int j = 0;
                        for (j = 0; j < correctList.size(); j++){
                            fileListEntry fle = new fileListEntry(correctList.get(j));
                            if (fle.getFilename().contains(fileList[i].getName())){
                                break;
                            }
                        }
                        if (j == correctList.size()){
                            fileList[i].delete();
                        }
                    }
                }
                directory = new File(prefix + accountHolder + "/audios");
                fileList = null;
                if (directory.exists()){
                    fileList = directory.listFiles();
                    for (int i = 0; i < fileList.length; i++){
                        int j = 0;
                        for (j = 0; j < correctList.size(); j++){
                            fileListEntry fle = new fileListEntry(correctList.get(j));
                            if (fle.getFilename().contains(fileList[i].getName())){
                                break;
                            }
                        }
                        if (j == correctList.size()){
                            fileList[i].delete();
                        }
                    }
                }
            }
            correctLock.release();
            channelCorrectImageFile.close();
            cfis.close();
            correctDom = db.newDocument();
            Element rootEle = correctDom.createElement("correctImage");
            correctDom.appendChild(rootEle);
            for (int i = 0; i < correctList.size(); i++){
                fileListEntry f = new fileListEntry(correctList.get(i));
                rootEle.appendChild(f.createDOMElement(correctDom));
            }
            saveXML(prefix + accountHolder + "/correctImage.xml", correctDom);
            existingDom = db.newDocument();
            rootEle = existingDom.createElement("existingImage");
            existingDom.appendChild(rootEle);
            for (int i = 0; i < existingList.size(); i++){
                fileListEntry f = new fileListEntry(existingList.get(i));
                rootEle.appendChild(f.createDOMElement(existingDom));
            }
            saveXML(prefix + accountHolder + "/existingImage.xml", existingDom);
        }catch (Exception e) {
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
}
