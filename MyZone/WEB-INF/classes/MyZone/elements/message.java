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

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import utils.attributes.*;
import utils.security.KeyPairUtil.*;
import utils.security.CertUtil.*;
import utils.security.CertUtil.userCertificate;

import MyZone.*;
import MyZone.elements.user;

/*
 This file contains the implementation of the message element.
 Please refer to messages.xsd for detailed description of its elements and
 attributes.
 */

public class message {
    
    private final static boolean DEBUG = false;
    private globalAttributes globalProperties = new globalAttributes();
    private PublicKey pubKey = null;
    private PrivateKey priKey;
    private long id;
    private String encrypted;
    private String type;
    private user sender;
    private user receiver;
    private String subject;
    private String body;
    private byte cert[];
    private Settings settings;
    private String prefix;
    
    protected static final byte[] intToByteArray(int value) {
        return new byte[] {
			(byte)(value >>> 24),
			(byte)(value >>> 16),
			(byte)(value >>> 8),
			(byte)value};
	}
    
    private static final int byteArrayToInt(byte [] b) {
        return (b[0] << 24)
		+ ((b[1] & 0xFF) << 16)
		+ ((b[2] & 0xFF) << 8)
		+ (b[3] & 0xFF);
	}
    
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
    
    public message(long id, String encrypted, String type, user sender, user receiver, String subject, String body){
        settings = new Settings("./MyZone/");
        settings.refresh(settings.BASIC_INFO);
        if (settings.username == null){
            settings = new Settings("../../");
            settings.refresh(settings.BASIC_INFO);
            prefix = "../../";
        }else{
            prefix = "./MyZone/";
        }
        globalProperties.init(prefix);
        this.id = id;
        this.encrypted = encrypted;
        this.type = type;
        this.sender = new user();
        this.receiver = new user();
        if (sender != null){
            this.sender.setUsername(sender.getUsername());
            this.sender.setFirstName(sender.getFirstName());
            this.sender.setLastName(sender.getLastName());
            if (!sender.getUsername().equals(settings.username)){
                userCertificate uc = readCert(sender.getUsername());
                if (uc != null)
                    pubKey = uc.publicKey;
            }
        }
        if (receiver != null){
            this.receiver.setUsername(receiver.getUsername());
            this.receiver.setFirstName(receiver.getFirstName());
            this.receiver.setLastName(receiver.getLastName());
            if (!receiver.getUsername().equals(settings.username)){
                userCertificate uc = readCert(receiver.getUsername());
                if (uc != null)
                    pubKey = uc.publicKey;
            }
        }
        KeyPairUtil x = new KeyPairUtil();
        priKey = x.readPriKey(prefix + settings.username + "/keys/", settings.username, globalProperties.keyPairAlgorithm);
        this.subject = subject;
        this.body = body;
    }
	
    public message(){
        settings = new Settings("./MyZone/");
        settings.refresh(settings.BASIC_INFO);
        if (settings.username == null){
            settings = new Settings("../../");
            settings.refresh(settings.BASIC_INFO);
            prefix = "../../";
        }else{
            prefix = "./MyZone/";
        }
        if (settings.username == null){
            settings = new Settings("../../");
            settings.refresh(settings.BASIC_INFO);
        }
        globalProperties.init(prefix);
        receiver = new user();
        sender = new user();
    }
    
    public message(message m){
        settings = new Settings("./MyZone/");
        settings.refresh(settings.BASIC_INFO);
        if (settings.username == null){
            settings = new Settings("../../");
            settings.refresh(settings.BASIC_INFO);
            prefix = "../../";
        }else{
            prefix = "./MyZone/";
        }
        globalProperties.init(prefix);
        this.id = m.getId();
        this.encrypted = m.getEncrypted();
        this.type = m.getType();
        this.sender = new user();
        if (m.getSender() != null){
            this.sender.setUsername(m.getSender().getUsername());
            this.sender.setFirstName(m.getSender().getFirstName());
            this.sender.setLastName(m.getSender().getLastName());
        }
        this.receiver = new user();
        if (m.getReceiver() != null){
            this.receiver.setUsername(m.getReceiver().getUsername());
            this.receiver.setFirstName(m.getReceiver().getFirstName());
            this.receiver.setLastName(m.getReceiver().getLastName());
        }
        this.subject = m.getSubject();
        this.body = m.getBody();
        this.pubKey = m.getPubKey();
        this.priKey = m.getPriKey();
    }
    
    public PublicKey getPubKey(){
        return pubKey;
    }
    
    public PrivateKey getPriKey(){
        return priKey;
    }
    
    public long getId(){
        return id; 
    }
    
    public void setId(long id){
        this.id = id;
    }
    
    public String getEncrypted(){
        return encrypted;
    }
    
    public void setEncrypted(String encrypted){
        this.encrypted = encrypted;
    }
    
    public String getType(){
        return type;
    }
    
    public void setType(String type){
        this.type = type;
    }
    
    public String getSubject(){
        return subject;
    }
    
    public void setSubject(String subject){
        this.subject = subject;
    }
    
    public user getSender(){
        return sender;
    }
    
    public void setSender(user u){
        sender = new user(u);
    }
    
    public user getReceiver(){
        return receiver;
    }
    
    public void setReceiver(user u){
        receiver = new user(u);
    }
    
    public String getBody(){
        return body;
    }
    
    public void setBody(String body){
        this.body = body;
    }
    
	public void create(Element el) {
		
		id = Long.parseLong(el.getAttribute("id"));
        encrypted = el.getAttribute("encrypted");
        type = getTextValue(el, "type");
        NodeList usrList = el.getElementsByTagName("receiver");
		if(usrList != null && usrList.getLength() > 0){
            Element usrEl = (Element)usrList.item(0);
            receiver.create(usrEl);
        }
        if (!receiver.getUsername().equals(settings.username)){
            if (!settings.isFriend(receiver.getUsername())){
                subject = null;
                body = null;
                return;
            }
            userCertificate uc = readCert(receiver.getUsername());
            if (uc != null)
                pubKey = uc.publicKey;
        }
        usrList = el.getElementsByTagName("sender");
		if(usrList != null && usrList.getLength() > 0){
            Element usrEl = (Element)usrList.item(0);
            sender.create(usrEl);
        }
        if (!sender.getUsername().equals(settings.username)){
            if (!settings.isFriend(sender.getUsername())){
                subject = null;
                body = null;
                return;
            }
            userCertificate uc = readCert(sender.getUsername());
            if (uc != null)
                pubKey = uc.publicKey;
        }
        KeyPairUtil x = new KeyPairUtil();
        priKey = x.readPriKey(prefix + settings.username + "/keys/", settings.username, globalProperties.keyPairAlgorithm);
        if (type.equals("sent")){
            subject = getTextValue(el, "subject");
            body = getTextValue(el, "body");
        }else{
            try{
                String encryptedMsg = getTextValue(el, "subject");
                byte[] encryptedMsgBytes;
                byte[] decryptedMsgBytes;
                if (encryptedMsg != null){
                    if (receiver.getUsername().equals(settings.username) && encrypted.equals("true")){
                        encryptedMsgBytes = new sun.misc.BASE64Decoder().decodeBuffer(encryptedMsg);
                        decryptedMsgBytes = decryptWithPriKey(encryptedMsgBytes, globalProperties.asymCipher);
                        decryptedMsgBytes = decryptWithPubKey(decryptedMsgBytes, globalProperties.asymCipher);
                        subject = new String(decryptedMsgBytes);
                    }else{
                        subject = encryptedMsg;
                    }
                }
                encryptedMsg = getTextValue(el, "body");
                if (receiver.getUsername().equals(settings.username) && encrypted.equals("true")){
                    encryptedMsgBytes = new sun.misc.BASE64Decoder().decodeBuffer(encryptedMsg);
                    decryptedMsgBytes = decryptWithPriKey(encryptedMsgBytes, globalProperties.asymCipher);
                    decryptedMsgBytes = decryptWithPubKey(decryptedMsgBytes, globalProperties.asymCipher);
                    body = new String(decryptedMsgBytes);
                    encrypted = "false";
                }else{
                    body = encryptedMsg;
                }
            }catch(Exception e){
                if (DEBUG){
                    e.printStackTrace();
                }
            }
        }
	}
    
    public Element createDOMElement(Document dom){
        Element el = dom.createElement("message");
		if (body.equals("")){
            body = " ";
        }
        if (subject.equals("")){
            subject = "No Subject";
        }
        el.setAttribute("id", String.valueOf(id));
        if (type != null){
            Element typeEl = dom.createElement("type");
            Text typeText = dom.createTextNode(type);
            typeEl.appendChild(typeText);
            el.appendChild(typeEl);
        }
        if (receiver != null){
            Element receiverEl = dom.createElement("receiver");
            receiverEl.appendChild(receiver.createDOMElement(dom));
            el.appendChild(receiverEl);
        }
        if (sender != null){
            Element senderEl = dom.createElement("sender");
            senderEl.appendChild(sender.createDOMElement(dom));
            el.appendChild(senderEl);
        }
        if (type.equals("sent")){
            if (subject != null){
                Element subjectEl = dom.createElement("subject");
                Text subjectText = dom.createTextNode(subject);
                subjectEl.appendChild(subjectText);
                el.appendChild(subjectEl);
            }
            if (body != null){
                Element bodyEl = dom.createElement("body");
                Text bodyText = dom.createTextNode(body);
                bodyEl.appendChild(bodyText);
                el.appendChild(bodyEl);
            }
        }else{
            try{
                byte[] encryptedMsgBytes;
                String encryptedMsg;
                Text subjectText;
                Text bodyText;
                if (subject != null){
                    Element subjectEl = dom.createElement("subject");
                    if (sender.getUsername().equals(settings.username) && !encrypted.equals("true")){
                        byte[] subjectLenBytes = intToByteArray(subject.getBytes("UTF-8").length);
                        encryptedMsgBytes = encryptWithPriKey(subject.getBytes("UTF-8"), globalProperties.asymCipher);
                        byte[] encryptedMsgBytesWithRawLen = new byte[encryptedMsgBytes.length + subjectLenBytes.length];
                        System.arraycopy(subjectLenBytes, 0, encryptedMsgBytesWithRawLen, 0, subjectLenBytes.length);
                        System.arraycopy(encryptedMsgBytes, 0, encryptedMsgBytesWithRawLen, subjectLenBytes.length, encryptedMsgBytes.length);
                        byte[] encryptedMsgBytesLen = intToByteArray(encryptedMsgBytesWithRawLen.length);
                        encryptedMsgBytes = encryptWithPubKey(encryptedMsgBytesWithRawLen, globalProperties.asymCipher);
                        byte[] wrappedEncryptedMsgBytes = new byte[encryptedMsgBytes.length + encryptedMsgBytesLen.length];
                        System.arraycopy(encryptedMsgBytesLen, 0, wrappedEncryptedMsgBytes, 0, encryptedMsgBytesLen.length);
                        System.arraycopy(encryptedMsgBytes, 0, wrappedEncryptedMsgBytes, encryptedMsgBytesLen.length, encryptedMsgBytes.length);
                        encryptedMsg = new sun.misc.BASE64Encoder().encode(wrappedEncryptedMsgBytes);
                        subjectText = dom.createTextNode(encryptedMsg);
                    }else{
                        subjectText = dom.createTextNode(subject);
                    }
                    subjectEl.appendChild(subjectText);
                    el.appendChild(subjectEl);
                }
                Element bodyEl = dom.createElement("body");
                if (sender.getUsername().equals(settings.username) && !encrypted.equals("true")){
                    byte[] bodyLenBytes = intToByteArray(body.getBytes("UTF-8").length);
                    encryptedMsgBytes = encryptWithPriKey(body.getBytes("UTF-8"), globalProperties.asymCipher);
                    byte[] encryptedMsgBytesWithRawLen = new byte[encryptedMsgBytes.length + bodyLenBytes.length];
                    System.arraycopy(bodyLenBytes, 0, encryptedMsgBytesWithRawLen, 0, bodyLenBytes.length);
                    System.arraycopy(encryptedMsgBytes, 0, encryptedMsgBytesWithRawLen, bodyLenBytes.length, encryptedMsgBytes.length);
                    byte[] encryptedMsgBytesLen = intToByteArray(encryptedMsgBytesWithRawLen.length);
                    encryptedMsgBytes = encryptWithPubKey(encryptedMsgBytesWithRawLen, globalProperties.asymCipher);
                    byte[] wrappedEncryptedMsgBytes = new byte[encryptedMsgBytes.length + encryptedMsgBytesLen.length];
                    System.arraycopy(encryptedMsgBytesLen, 0, wrappedEncryptedMsgBytes, 0, encryptedMsgBytesLen.length);
                    System.arraycopy(encryptedMsgBytes, 0, wrappedEncryptedMsgBytes, encryptedMsgBytesLen.length, encryptedMsgBytes.length);
                    encryptedMsg = new sun.misc.BASE64Encoder().encode(wrappedEncryptedMsgBytes);
                    bodyText = dom.createTextNode(encryptedMsg);
                    encrypted = "true";
                }else{
                    bodyText = dom.createTextNode(body);
                }
                bodyEl.appendChild(bodyText);
                el.appendChild(bodyEl);
            }catch(Exception e){
                if (DEBUG){
                    e.printStackTrace();
                }
            }
        }
        el.setAttribute("encrypted", encrypted);
		return el;
    }
    
    public String generateHTML(String type){
        String res = "";
        user usr = new user();
        if (this.type.equals("sent")){
            usr = receiver;
        }else{
            usr = sender;
        }
        if (subject == null)
            subject = "";
        if (type.equals("sidebarAbstract")){
            res += "<a href=\"messages.jsp?show=message&id=" + id + "\" >\n";
            res += "<div class=\"right-sidebar-entry-row\">\n";
            res += "<div class=\"sidebar-thumbnail\">\n";
            res += "<img src=\"" + usr.getThumbnail() + "\" alt=\"" + usr.getFirstName() + " " + usr.getLastName() + "\" width=\"50\" height=\"50\">\n";
            res += "</img>\n";
            res += "</div>\n";
            int endIndex = 30;
            String dots = "...";
            if (body.length() < 30){
                endIndex = body.length();
                dots = "";
            }
            res += "<div class=\"sidebar-entry-textbox3\"><p><h3>" + usr.getFirstName() + " " + usr.getLastName() + "</h3>: " + body.substring(0, endIndex) + dots  + "</p>\n";
            res += "</div>\n";
            res += "</div>\n";
            res += "</a>\n";
        }else if (type.equals("abstract")){
            res += "<div class=\"message-body\">\n";
            res += "<div class=\"content-thumbnail\">\n";
            res += "<img src=\"" + usr.getThumbnail() + "\" alt=\"" + usr.getFirstName() + " " + usr.getLastName() + "\" width=\"50\" height=\"50\">\n";
            res += "</img>\n";
            res += "</div>\n";
            res += "<div class=\"message-user\">\n";
            res += "<a>" + usr.getFirstName() + " " + usr.getLastName() + "</a>\n";
            res += "</div>\n";
            Calendar now = Calendar.getInstance();
            TimeZone local = now.getTimeZone();
            now.setTimeZone(TimeZone.getTimeZone("UTC"));
            now.setTimeInMillis(id);
            now.setTimeZone(local);
            res += "<div class=\"message-date\">\n";
            res += "<h3>" + now.getTime() + "</h3>\n";
            res += "</div>\n";
            res += "<div class=\"message-abstract\">\n";
            res += "<a href=\"messages.jsp?show=message&id=" + id + "\" ><p>Subject:&nbsp;</p>" + subject + "</a>\n";
            res += "<br />\n";
            int endIndex = 75;
            String dots = "...";
            if (body.length() < 75){
                endIndex = body.length();
                dots = "";
            }
            res += "<a href=\"messages.jsp?show=message&id=" + id + "\" >" + body.substring(0, endIndex) + dots + "</a>\n";
            res += "</div>\n";
            res += "</div>\n";
        }else if (type.equals("complete")){
            res += "<div class=\"message-body\">\n";
            res += "<div class=\"content-thumbnail\">\n";
            res += "<img src=\"" + usr.getThumbnail() + "\" alt=\"" + usr.getFirstName() + " " + usr.getLastName() + "\" width=\"50\" height=\"50\">\n";
            res += "</img>\n";
            res += "</div>\n";
            res += "<div class=\"message-user\">\n";
            res += "<a href=\"profile.jsp?profileOwner=" + usr.getUsername() + "\">" + usr.getFirstName() + " " + usr.getLastName() + "</a>\n";
            res += "</div>\n";
            Calendar now = Calendar.getInstance();
            TimeZone local = now.getTimeZone();
            now.setTimeZone(TimeZone.getTimeZone("UTC"));
            now.setTimeInMillis(id);
            now.setTimeZone(local);
            res += "<div class=\"message-date\">\n";
            res += "<h3>" + now.getTime() + "</h3>\n";
            res += "</div>\n";
            res += "<div class=\"message-abstract\">\n";
            res += "<h3>Subject:&nbsp;</h3>" + subject + "<br />&nbsp;<br />\n";
            res += "<p>" + body.replaceAll("\n", "<br />") + "</p>\n";
            res += "</div>\n";
            res += "</div>\n"; 
        }
        return res;
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Message info \n");
		sb.append("id:" + id);
		sb.append("\n");
        sb.append("type:" + type);
		sb.append("\n");
        sb.append(sender.toString());
        sb.append(receiver.toString());
		sb.append("subject:" + subject);
		sb.append("\n");
        sb.append("body:" + body);
		sb.append("\n");
		return sb.toString();
	}
    
    private byte[] encryptWithPriKey(byte[] plain, String keyAlgorithm){
        try{
            Cipher cipher = Cipher.getInstance(keyAlgorithm);
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
        }
        return null;
    }
    
    private byte[] encryptWithPubKey(byte[] plain, String keyAlgorithm){
        try{
            Cipher cipher = Cipher.getInstance(keyAlgorithm);
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
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
        }
        return null;
    }
    
    private byte[] decryptWithPubKey(byte[] encryptedMsgWithLength, String keyAlgorithm){
		try{
            byte[] plainMsgLenBytes = new byte[4];
            System.arraycopy(encryptedMsgWithLength, 0, plainMsgLenBytes, 0, 4);
            int msgLength = byteArrayToInt(plainMsgLenBytes);
            byte[] encryptedMsg = new byte[encryptedMsgWithLength.length - 4];
            System.arraycopy(encryptedMsgWithLength, 4, encryptedMsg, 0, encryptedMsg.length);
            int chunks = (int)(encryptedMsg.length / (int)(globalProperties.certificateKeySize/8));
            byte[] decryptedMsg = new byte[chunks * (int)(globalProperties.certificateKeySize/8)];
            byte[] encryptedChunk = new byte[(int)(globalProperties.certificateKeySize/8)];
            byte[] decryptedChunk = null;
            Cipher cipher = Cipher.getInstance(keyAlgorithm); 
            cipher.init(Cipher.DECRYPT_MODE, pubKey);
            int length = 0;
            for(int i = 0; i < chunks; i++)
            {
                System.arraycopy(encryptedMsg, i * (int)(globalProperties.certificateKeySize/8), encryptedChunk, 0, encryptedChunk.length);
                decryptedChunk = cipher.doFinal(encryptedChunk);
                System.arraycopy(decryptedChunk, 0, decryptedMsg, length, decryptedChunk.length);
                length += decryptedChunk.length;
            }
            byte[] plainMsg = new byte[msgLength];
            System.arraycopy(decryptedMsg, 0, plainMsg, 0, plainMsg.length);
            return plainMsg;
        }catch(NoSuchAlgorithmException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(NoSuchPaddingException e){
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
        }catch(InvalidKeyException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
	}
    
    private byte[] decryptWithPriKey(byte[] encryptedMsgWithLength, String keyAlgorithm){
		try{
            byte[] plainMsgLenBytes = new byte[4];
            System.arraycopy(encryptedMsgWithLength, 0, plainMsgLenBytes, 0, 4);
            int msgLength = byteArrayToInt(plainMsgLenBytes);
            byte[] encryptedMsg = new byte[encryptedMsgWithLength.length - 4];
            System.arraycopy(encryptedMsgWithLength, 4, encryptedMsg, 0, encryptedMsg.length);
            int chunks = (int)(encryptedMsg.length / (int)(globalProperties.certificateKeySize/8));
            byte[] decryptedMsg = new byte[chunks * (int)(globalProperties.certificateKeySize/8)];
            byte[] encryptedChunk = new byte[(int)(globalProperties.certificateKeySize/8)];
            byte[] decryptedChunk = null;
            Cipher cipher = Cipher.getInstance(keyAlgorithm); 
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            int length = 0;
            for(int i = 0; i < chunks; i++)
            {
                System.arraycopy(encryptedMsg, i * (int)(globalProperties.certificateKeySize/8), encryptedChunk, 0, encryptedChunk.length);
                decryptedChunk = cipher.doFinal(encryptedChunk);
                System.arraycopy(decryptedChunk, 0, decryptedMsg, length, decryptedChunk.length);
                length += decryptedChunk.length;
            }
            byte[] plainMsg = new byte[msgLength];
            System.arraycopy(decryptedMsg, 0, plainMsg, 0, plainMsg.length);
            return plainMsg;
        }catch(NoSuchAlgorithmException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(NoSuchPaddingException e){
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
        }catch(InvalidKeyException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
	}
    
    private userCertificate readCert(String username){
        CertVerifier y = new CertVerifier();
        byte[] certBytes = y.readRawFile(prefix + settings.username + "/friends/" + username + "/cert/", username + ".cert");
        if (certBytes == null){
            return null;
        }
        userCertificate clientCert = y.verifyCertificate(certBytes, globalProperties.caCertPath, globalProperties.keyPairAlgorithm, globalProperties.certSigAlgorithm);
        if (clientCert != null){
            return clientCert;
        }
        return null;
    }
}
