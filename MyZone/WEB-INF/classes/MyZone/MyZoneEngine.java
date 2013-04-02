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

import java.io.*;
import java.net.*;
import java.net.BindException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.TimeZone;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

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
 This file contains the implementation of the MyZoneEngine class.
 MyZoneEngine is responsible for:
 1. determinining the kind of NAT that the user is behind.
 2. requesting relay server from the rendezvous server if the client
 is behind a non traversable NAT.
 2. registering with the rendezvousServer.
 3. receiving friendship requests from the rendezvous server.
 4. sending out pending friendship requests to the rendezvous server.
 5. starting the clientThread.
 6. starting the servingThread.
 */

public class MyZoneEngine{
    
    private final static boolean DEBUG = false;
    private header hdr = new header();
    private globalAttributes globalProperties = new globalAttributes();
    private PrivateKey priKey;
    private Key sessionKey;
    private List<friendshipRequest> friendRequests;
    private final static int IDLE_LIMIT = 5000;
    private discoveryInfo di;
    private relayServer relay = new relayServer();
    private servingThread server;
    private clientThread client;
    private Settings mainSettings;
    private String myIPAddress = "";
    private String prefix = "./";
    
    public MyZoneEngine(String prefix){
        this.prefix = prefix;
        mainSettings = new Settings(prefix);
        mainSettings.refresh(mainSettings.ALL);
        globalProperties.init(prefix);
        friendRequests = new ArrayList();
        KeyPairUtil x = new KeyPairUtil();
        priKey = x.readPriKey(prefix + mainSettings.username + "/keys/", mainSettings.username, globalProperties.keyPairAlgorithm);
    }
    
    private static final byte[] intToByteArray(int value) {
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
    
    private boolean selectInterface(){
        try {
            if (mainSettings.MyZonePort < 0 || mainSettings.MyZonePort > 65535){
                if (DEBUG){
                    System.out.println("DEBUG: line 112 of MyZoneEngine.java. Invalid MyZone Port Number");
                }
                return false;
            }
            if (mainSettings.STUNServerAddress == null || mainSettings.STUNServerAddress.equals("")){
                if (DEBUG){
                    System.out.println("DEBUG: line 118 of MyZoneEngine.java. Could not find the address of the STUN Server");
                }
                return false;
            }
            if (mainSettings.STUNServerPort < 0 || mainSettings.STUNServerPort > 65535){
                if (DEBUG){
                    System.out.println("DEBUG: line 124 of MyZoneEngine.java. Invalid STUN Server Port Number");
                }
                return false;
            }
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            BufferedReader input;
            List<discoveryInfo> iAddressList = new ArrayList();
            while (ifaces.hasMoreElements()) {
                int k = 0;
                k++;
                NetworkInterface iface = ifaces.nextElement();
                Enumeration<InetAddress> iaddresses = iface.getInetAddresses();
                while (iaddresses.hasMoreElements()) {
                    InetAddress iaddress = iaddresses.nextElement();
                    int j = 0;
                    j++;
                    if (Class.forName("java.net.Inet4Address").isInstance(iaddress)) {
                        if ((!iaddress.isLoopbackAddress()) && (!iaddress.isLinkLocalAddress())) {
                            discoverNAT res = new discoverNAT(iaddress, mainSettings.MyZonePort, mainSettings.STUNServerAddress, mainSettings.STUNServerPort);
                            discoveryInfo d = res.test();
                            if (d.isOpenAccess()){
                                if (iAddressList.size() == 0){
                                    iAddressList.add(d);
                                }else{
                                    iAddressList.add(0, d);
                                }
                            }else if (d.isFullCone()){
                                int i = 0;
                                for (i = 0; i < iAddressList.size(); i++){
                                    discoveryInfo dd = (discoveryInfo)iAddressList.get(i);
                                    if (!dd.isOpenAccess()){
                                        iAddressList.add(i, d);
                                        break;
                                    }
                                }
                                if (iAddressList.size() == 0){
                                    iAddressList.add(d);
                                }
                            }else{
                                iAddressList.add(d);
                            }
                        }
                    }
                }
            }
            if (iAddressList.size() == 0){
                if (DEBUG){
                    System.out.println("DEBUG: line 170 of MyZoneEngine.java. Could not find any Network Interfaces");
                }
                return false;
            }
            di = iAddressList.get(0);
            String [] temp;
            temp = di.getPublicIP().getHostAddress().split("/");
            myIPAddress = temp[0];
        } catch (Exception e) {
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return true;
    }
    
    private byte[] encryptWithPubKey(byte[] plain, String keyAlgorithm, Key key){
        try{
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance(keyAlgorithm, "BC");
            cipher.init(Cipher.ENCRYPT_MODE, key);
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
    
    private byte[] encryptWithPriKey(byte[] plain, String keyAlgorithm){
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
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance(keyAlgorithm, "BC");
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
        }catch(java.security.NoSuchProviderException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
	}
    
    private byte[] hashMessage(byte[] data, String mdAlgorithm){
        try{
            Security.addProvider(new BouncyCastleProvider());
            MessageDigest md;
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
    
    private byte[] encryptSession(byte[] data){
        try{
            // Get a cipher object.
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance(globalProperties.sessionCipher, "BC");
            cipher.init(Cipher.ENCRYPT_MODE, sessionKey);
            
            // encrypt using the cypher
            byte[] raw = cipher.doFinal(data);
            return raw;
        }catch(IllegalBlockSizeException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(BadPaddingException e){
            if (DEBUG){
                e.printStackTrace();
            }
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
        }catch(java.security.NoSuchProviderException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
    }
    
    private byte[] decryptSession(byte[] encrypted){
        
        try{
            // Get a cipher object.
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance(globalProperties.sessionCipher, "BC");
            cipher.init(Cipher.DECRYPT_MODE, sessionKey);
            
            //decode the message
            byte[] data = cipher.doFinal(encrypted);
            
            //converts the decoded message to a String
            return data;
        }catch(InvalidKeyException e){
            if (DEBUG){
                e.printStackTrace();
            }
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
        }catch(java.security.NoSuchProviderException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
    }
    
    public boolean requestRelay(){
        try{
            Socket toRendServer = new Socket(mainSettings.rendezvousServerAddress, mainSettings.rendezvousServerPort);
            toRendServer.setSoTimeout(IDLE_LIMIT);
            DataOutputStream outToRendServer = new DataOutputStream(toRendServer.getOutputStream());
            // client -> server: MSG_LENGTH|REQUEST_RELAY_SERVER
            byte[] buffer = new byte[8];
            System.arraycopy(intToByteArray(4), 0, buffer, 0, 4);
            System.arraycopy(intToByteArray(hdr.REQUEST_RELAY_SERVER), 0, buffer, 4, 4);
            outToRendServer.write(buffer, 0, buffer.length);
            outToRendServer.flush();
            /* server -> client: MSG_LENGTH|RELAY_SERVER_SENT|RELAY_ADDRESS.length|RELAY_ADDRESS|RELAY_PORT
             OR
             server -> client: MSG_LENGTH|NO_RELAY_AVAILABLE
             */
            byte[] lenBytes = new byte[4];
            int i = 0;
            while(i < 4){
                i += toRendServer.getInputStream().read(lenBytes, i, 4 - i);
                if (i < 0){
                    return false;
                }
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            int len = byteArrayToInt(lenBytes);
            i = 0;
            byte[] msg = new byte[len];
            while(i < len){
                i += toRendServer.getInputStream().read(msg, i, len - i);
                if (i < 0){
                    return false;
                }
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            byte[] replyType = new byte[4];
            System.arraycopy(msg, 0, replyType, 0, 4);
            if (byteArrayToInt(replyType) != hdr.RELAY_SERVER_SENT){
                return false;
            }
            lenBytes = new byte[4];
            System.arraycopy(msg, 4, lenBytes, 0, 4);
            len = byteArrayToInt(lenBytes);
            byte[] relayAddressBytes = new byte[len];
            System.arraycopy(msg, 8, relayAddressBytes, 0, len);
            byte[] relayPortBytes = new byte[4];
            System.arraycopy(msg, 8 + len, relayPortBytes, 0, 4);
            relay.relayAddress = new String(relayAddressBytes, "UTF-8");
            relay.port = byteArrayToInt(relayPortBytes);
            if (DEBUG){
                System.out.println("DEBUG: line 479 of MyZoneEngine.java. Received relay server info: " + relay);
            }
            return true;
        }catch(UnknownHostException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(IOException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
    }
    
    private byte[] readMyCert(){
        CertVerifier y = new CertVerifier();
        String myCertPath = prefix + mainSettings.username + "/cert/";
        byte[] certBytes = y.readRawFile(myCertPath, mainSettings.username + ".cert");
        return certBytes;
    }
    
    private byte[] signHash(byte[] rawBytes){
        byte[] hash = hashMessage(rawBytes, globalProperties.messageDigestAlgorithm);
        byte[] signedHash;
        signedHash = encryptWithPriKey(hash, globalProperties.asymCipher);
        return signedHash;
    }
    
    public boolean registerPeer(){
        /* client -> server: MSG_LENGTH|REGISTER_PEER|CERTIFICATE.length|CERTIFICATE
         server -> client: MSG_LENGTH|SESSIONKEY.length|E(P_peer)[SESSIONKEY]|hash
         client -> server: MSG_LENGTH|SESSIONKEY_RETRIEVED|E(SESSIONKEY)[PRIORITY|PORT|TYPE_OF_NAT|TYPE_OF_PROTOCOL|
         ->IP_ADDRESS.length|IP_ADDRESS|RELAY_ADDRESS.length|RELAY_ADDRESS|RELAY_PORT|PASSPHRASES.length|
         ->[PASSPHRASE.length|PASSPHRASE]*MIRRORS.length|[MIRROR_USERNAME.length|MIRROR_USERNAME]*]|E(Q_peer)[MD]|E(Q_peer)[MIRRORS_HASH]]|hash
         MD = hash(PRIORITY+IP_ADDRESS+PORT+TYPE_OF_PROTOCOL+RELAY_ADDRESS+RELAY_PORT)
         MIRRORS_HASH = hash(MIRROR_USERNAME*)
         OR
         server -> client: MSG_LENGTH|PEER_NOT_REGISTERD
         */
        Socket toRendServer = null;
        DataOutputStream outToRendServer = null;
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        OutputStream os = null;
        try{
            toRendServer = new Socket(mainSettings.rendezvousServerAddress, mainSettings.rendezvousServerPort);
            toRendServer.setSoTimeout(IDLE_LIMIT);
            outToRendServer = new DataOutputStream(toRendServer.getOutputStream());
            // client -> server: MSG_LENGTH|REGISTER_PEER|CERTIFICATE.length|CERTIFICATE
            byte[] certBytes = readMyCert();
            if (certBytes == null){
                return false;
            }
            byte[] buffer = new byte[4 + 4 + 4 + certBytes.length];
            int len = 0;
            System.arraycopy(intToByteArray(4 + 4 + certBytes.length), 0, buffer, len, 4);
            len += 4;
            System.arraycopy(intToByteArray(hdr.REGISTER_PEER), 0, buffer, len, 4);
            len += 4;
            System.arraycopy(intToByteArray(certBytes.length), 0, buffer, len, 4);
            len += 4;
            System.arraycopy(certBytes, 0, buffer, len, certBytes.length);
            outToRendServer.write(buffer, 0, buffer.length);
            outToRendServer.flush();
            // server -> client: MSG_LENGTH|SESSIONKEY.length|E(P_peer)[SESSIONKEY]|hash
            byte[] lenBytes = new byte[4];
            int i = 0;
            while(i < 4){
                i += toRendServer.getInputStream().read(lenBytes, i, 4 - i);
                if (i < 0){
                    return false;
                }
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            len = byteArrayToInt(lenBytes);
            byte[] msg = new byte[len];
            i = 0;
            while(i < len){
                i += toRendServer.getInputStream().read(msg, i, len - i);
                if (i < 0){
                    return false;
                }
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            byte[] encryptedSessionKeyWithLength = new byte[msg.length - globalProperties.messageDigestSize];
            System.arraycopy(msg, 0, encryptedSessionKeyWithLength, 0, encryptedSessionKeyWithLength.length);
            byte[] sessionKeyBytes = decryptWithPriKey(encryptedSessionKeyWithLength, globalProperties.asymCipher);
            if (sessionKeyBytes == null){
                return false;
            }
            sessionKey = new SecretKeySpec(sessionKeyBytes, globalProperties.sessionKeyType);
            byte[] hash = hashMessage(sessionKeyBytes, globalProperties.messageDigestAlgorithm);
            byte[] recvdHash = new byte[globalProperties.messageDigestSize];
            System.arraycopy(msg, encryptedSessionKeyWithLength.length, recvdHash, 0, recvdHash.length);
            String hashStr = new sun.misc.BASE64Encoder().encode(hash);
            String recvdHashStr = new sun.misc.BASE64Encoder().encode(recvdHash);
            if (!hashStr.equals(recvdHashStr))
            {
                return false;
            }
            int typeOfNat;
            int typeOfProtocol;
            String relayAddress;
            int relayPort;
            if (di.isOpenAccess()){
                if (myIPAddress.indexOf("10") == 0 || myIPAddress.indexOf("172") == 0 || myIPAddress.indexOf("192") == 0){
                    // because of CU's switch configuration campus IPs are
                    // falsely recognized as public IPs. this condition would
                    // prevent that.
                    di.resetOpenAccess();
                    di.resetFullCone();
                    typeOfNat = globalProperties.SYMMETRIC_NAT;
                    typeOfProtocol = globalProperties.TCP;
                    if (server != null && server.isAlive()){
                        relayAddress = mainSettings.relayServerAddress;
                        relayPort = mainSettings.relayServerPort;
                    }else{
                        if (!requestRelay()){
                            return false;
                        }
                        if (relay.relayAddress.equals("0.0.0.0") || relay.relayAddress.equals("127.0.0.1") || relay.relayAddress.equals("localhost")){
                            relay.relayAddress = mainSettings.rendezvousServerAddress;
                        }
                        relayAddress = relay.relayAddress;
                        relayPort = relay.port;
                        mainSettings.relayServerAddress = relay.relayAddress;
                        mainSettings.relayServerPort = relay.port;
                    }
                }else{
                    typeOfNat = globalProperties.PUBLIC_IP;
                    typeOfProtocol = globalProperties.TCP;
                    relayAddress = "0.0.0.0";
                    relayPort = 0;
                    mainSettings.relayServerAddress = relay.relayAddress;
                    mainSettings.relayServerPort = relay.port;
                }
            }else if (di.isFullCone()){
                typeOfNat = globalProperties.FULL_CONE_NAT;
                typeOfProtocol = globalProperties.UDP;
                relayAddress = "0.0.0.0";
                relayPort = 0;
                mainSettings.relayServerAddress = relay.relayAddress;
                mainSettings.relayServerPort = relay.port;
            }else{
                typeOfNat = globalProperties.SYMMETRIC_NAT;
                typeOfProtocol = globalProperties.TCP;
                if (server != null && server.isAlive()){
                    relayAddress = mainSettings.relayServerAddress;
                    relayPort = mainSettings.relayServerPort;
                }else{
                    if (!requestRelay()){
                        return false;
                    }
                    if (relay.relayAddress.equals("0.0.0.0") || relay.relayAddress.equals("127.0.0.1") || relay.relayAddress.equals("localhost")){
                        relay.relayAddress = mainSettings.rendezvousServerAddress;
                    }
                    relayAddress = relay.relayAddress;
                    relayPort = relay.port;
                    mainSettings.relayServerAddress = relay.relayAddress;
                    mainSettings.relayServerPort = relay.port;
                }
            }
            mainSettings.save(mainSettings.BASIC_INFO);
            /* client -> server: MSG_LENGTH|SESSIONKEY_RETRIEVED|E(SESSIONKEY)[PRIORITY|PORT|TYPE_OF_NAT|TYPE_OF_PROTOCOL|
             ->IP_ADDRESS.length|IP_ADDRESS|RELAY_ADDRESS.length|RELAY_ADDRESS|RELAY_PORT|PASSPHRASES.LENGTH|[PASSPHRASE.length|
             ->PASSPHRASE]*|MIRRORS.LENGTH|[MIRROR_USERNAME.length|MIRROR_USERNAME]*|E(Q_peer)[MD]]|hash
             MD = hash(PRIORITY|IP_ADDRESS+PORT+TYPE_OF_PROTOCOL+RELAY_ADDRESS+RELAY_PORT+[MIRROR_USERNAME]*
             */
            String myPassphrases = "";
            for (i = 0; i < mainSettings.passphrases.size(); i++){
                passphraseEntry p = new passphraseEntry(mainSettings.passphrases.get(i));
                myPassphrases += p.getPassphrase();
            }
            String myMirrors = "";
            for (i = 0; i < mainSettings.mirrors.size(); i++){
                mirror m = new mirror(mainSettings.mirrors.get(i));
                myMirrors += m.getUsername();
            }
            byte[] rawBytes = new byte[4 + myIPAddress.getBytes("UTF-8").length + 4 + 4 + relayAddress.getBytes("UTF-8").length + 4];
            len = 0;
            System.arraycopy(intToByteArray(mainSettings.devPriority), 0, rawBytes, len, 4);
            len += 4;
            System.arraycopy(myIPAddress.getBytes("UTF-8"), 0, rawBytes, len, myIPAddress.getBytes("UTF-8").length);
            len += myIPAddress.getBytes("UTF-8").length;
            System.arraycopy(intToByteArray(di.getPublicPort()), 0, rawBytes, len, 4);
            len += 4;
            System.arraycopy(intToByteArray(typeOfProtocol), 0, rawBytes, len, 4);
            len += 4;
            System.arraycopy(relayAddress.getBytes("UTF-8"), 0, rawBytes, len, relayAddress.getBytes("UTF-8").length);
            len += relayAddress.getBytes("UTF-8").length;
            System.arraycopy(intToByteArray(relayPort), 0, rawBytes, len, 4);
            len += 4;
            byte[] signedHash = signHash(rawBytes);
            byte[] mirrorHash = signHash(myMirrors.getBytes("UTF-8"));
            byte[] rawReply = new byte[36 + (mainSettings.passphrases.size() * 4) + (mainSettings.mirrors.size() * 4) + myIPAddress.getBytes("UTF-8").length + relayAddress.getBytes("UTF-8").length + myPassphrases.getBytes("UTF-8").length + myMirrors.getBytes("UTF-8").length + signedHash.length + mirrorHash.length];
            len = 0;
            System.arraycopy(intToByteArray(mainSettings.devPriority), 0, rawReply, len, 4);
            len += 4;
            System.arraycopy(intToByteArray(di.getPublicPort()), 0, rawReply, len, 4);
            len += 4;
            System.arraycopy(intToByteArray(typeOfNat), 0, rawReply, len, 4);
            len += 4;
            System.arraycopy(intToByteArray(typeOfProtocol), 0, rawReply, len, 4);
            len += 4;
            System.arraycopy(intToByteArray(myIPAddress.getBytes("UTF-8").length), 0, rawReply, len, 4);
            len += 4;
            System.arraycopy(myIPAddress.getBytes("UTF-8"), 0, rawReply, len, myIPAddress.getBytes("UTF-8").length);
            len += myIPAddress.getBytes("UTF-8").length;
            System.arraycopy(intToByteArray(relayAddress.getBytes("UTF-8").length), 0, rawReply, len, 4);
            len += 4;
            System.arraycopy(relayAddress.getBytes("UTF-8"), 0, rawReply, len, relayAddress.getBytes("UTF-8").length);
            len += relayAddress.getBytes("UTF-8").length;
            System.arraycopy(intToByteArray(relayPort), 0, rawReply, len, 4);
            len += 4;
            System.arraycopy(intToByteArray(mainSettings.passphrases.size()), 0, rawReply, len, 4);
            len += 4;
            for (i = 0; i < mainSettings.passphrases.size(); i++){
                passphraseEntry p = new passphraseEntry(mainSettings.passphrases.get(i));
                System.arraycopy(intToByteArray(p.getPassphrase().getBytes("UTF-8").length), 0, rawReply, len, 4);
                len += 4;
                System.arraycopy(p.getPassphrase().getBytes("UTF-8"), 0, rawReply, len, p.getPassphrase().getBytes("UTF-8").length);
                len += p.getPassphrase().getBytes("UTF-8").length;
            }
            System.arraycopy(intToByteArray(mainSettings.mirrors.size()), 0, rawReply, len, 4);
            len += 4;
            for (i = 0; i < mainSettings.mirrors.size(); i++){
                mirror m = new mirror(mainSettings.mirrors.get(i));
                System.arraycopy(intToByteArray(m.getUsername().getBytes("UTF-8").length), 0, rawReply, len, 4);
                len += 4;
                System.arraycopy(m.getUsername().getBytes("UTF-8"), 0, rawReply, len, m.getUsername().getBytes("UTF-8").length);
                len += m.getUsername().getBytes("UTF-8").length;
            }
            System.arraycopy(signedHash, 0, rawReply, len, signedHash.length);
            len += signedHash.length;
            System.arraycopy(mirrorHash, 0, rawReply, len, signedHash.length);
            byte[] encryptedReply = encryptSession(rawReply);
            hash = hashMessage(rawReply, globalProperties.messageDigestAlgorithm);
            buffer = new byte[4 + 4 + encryptedReply.length + hash.length];
            len = 0;
            System.arraycopy(intToByteArray(4 + encryptedReply.length + hash.length), 0, buffer, len, 4);
            len += 4;
            System.arraycopy(intToByteArray(hdr.SESSIONKEY_RETRIEVED), 0, buffer, len, 4);
            len += 4;
            System.arraycopy(encryptedReply, 0, buffer, len, encryptedReply.length);
            len += encryptedReply.length;
            System.arraycopy(hash, 0, buffer, len, hash.length);
            outToRendServer.write(buffer, 0, buffer.length);
            outToRendServer.flush();
            /* server -> client: MSG_LENGTH|PEER_REGISTERED|E(SESSIONKEY)[FRIENDSHIP_REQUESTER_USERNAME.length|
             -> FRIENDSHIP_REQUESTER_USERNAME|
             -> 4+(E(P_peer)[PASSPHRASE]).length|PASSPHRASE.length|E(P_peer)[PASSPHRASE]]|hash
             OR
             MSG_LENGTH|PEER_NOT_REGISTERD
             */
            i = 0;
            lenBytes = new byte[4];
            while(i < 4){
                i += toRendServer.getInputStream().read(lenBytes, i, 4 - i);
                if (i < 0){
                    return false;
                }
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            len = byteArrayToInt(lenBytes);
            i = 0;
            msg = new byte[len];
            while(i < len){
                i += toRendServer.getInputStream().read(msg, i, len - i);
                if (i < 0){
                    return false;
                }
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            byte[] replyType = new byte[4];
            System.arraycopy(msg, 0, replyType, 0, 4);
            if (byteArrayToInt(replyType) != hdr.PEER_REGISTERD && byteArrayToInt(replyType) != hdr.PEER_REGISTERD_SEND_LOG){
                return false;
            }
            if (msg.length > 4){
                byte[] encryptedPart = new byte[msg.length - 4 - globalProperties.messageDigestSize];
                System.arraycopy(msg, 4, encryptedPart, 0, encryptedPart.length);
                byte[] plain = decryptSession(encryptedPart);
                hash = hashMessage(plain, globalProperties.messageDigestAlgorithm);
                recvdHash = new byte[globalProperties.messageDigestSize];
                System.arraycopy(msg, 4 + encryptedPart.length, recvdHash, 0, recvdHash.length);
                hashStr = new sun.misc.BASE64Encoder().encode(hash);
                recvdHashStr = new sun.misc.BASE64Encoder().encode(recvdHash);
                if (!hashStr.equals(recvdHashStr))
                {
                    return false;
                }
                extractFriendshipRequests(plain);
                for (i = 0; i < friendRequests.size(); i++){
                    friendshipRequest req = new friendshipRequest(friendRequests.get(i));
                    mainSettings.receiveFriendshipRequest(req.requesterID, req.passphrase);
                }
            }
            if (byteArrayToInt(replyType) == hdr.PEER_REGISTERD_SEND_LOG){
                File logFile = new File (prefix + "tmp/log.csv");
                if (logFile.exists()){
                    byte [] logbytearray  = new byte [(int)logFile.length()];
                    fis = new FileInputStream(logFile);
                    bis = new BufferedInputStream(fis);
                    bis.read(logbytearray,0,logbytearray.length);
                    os = toRendServer.getOutputStream();
                    if (DEBUG){
                        System.out.println("DEBUG: line 809 of MyZoneEngine.java. Sending log ...");
                    }
                    os.write(logbytearray,0,logbytearray.length);
                    os.flush();
                    os.close();
                    bis.close();
                    fis.close();
                    if (DEBUG){
                        System.out.println("DEBUG: line 818 of MyZoneEngine.java. log was sent successfully");
                    }
                    logFile.delete();
                }
            }
            return true;
        }catch(IOException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }finally{
            try{
                if (bis != null){
                    bis.close();
                }
                if (os != null){
                    os.close();
                }
                outToRendServer.close();
                toRendServer.close();
                if (fis != null){
                    fis.close();
                }
            }catch(IOException e){
                if (DEBUG){
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
    
    
    private void extractFriendshipRequests(byte[] requests){
        /* FRIENDSHIP_REQUESTER_USERNAME.length|
         -> FRIENDSHIP_REQUESTER_USERNAME|
         -> 4+(E(P_peer)[PASSPHRASE]).length|PASSPHRASE.length|E(P_peer)[PASSPHRASE]
         */
        try{
            friendRequests.clear();
            int i = 0;
            byte[] requesterIDBytes;
            byte[] lenBytes;
            int len;
            byte[] encryptedPassphraseWithLen;
            while(i < requests.length){
                lenBytes = new byte[4];
                System.arraycopy(requests, i, lenBytes, 0, 4);
                i += 4;
                len = byteArrayToInt(lenBytes); // FRIENDSHIP_REQUESTER_USERNAME.length
                requesterIDBytes = new byte[len];
                System.arraycopy(requests, i, requesterIDBytes, 0, len); // FRIENDSHIP_REQUESTER_USERNAME
                i += len;
                lenBytes = new byte[4];
                System.arraycopy(requests, i, lenBytes, 0, 4); // 4+(E(P_peer)[PASSPHRASE]).length
                i += 4;
                len = byteArrayToInt(lenBytes);
                encryptedPassphraseWithLen = new byte[len];
                System.arraycopy(requests, i, encryptedPassphraseWithLen, 0, len); // PASSPHRASE.length|E(P_peer)[PASSPHRASE]
                i += len;
                byte[] rawPassphrase = decryptWithPriKey(encryptedPassphraseWithLen, globalProperties.asymCipher);
                friendshipRequest req = new friendshipRequest();
                req.requesterID = new String(requesterIDBytes, "UTF-8");
                req.passphrase = new String(rawPassphrase, "UTF-8");
                friendRequests.add(req);
            }
        }catch(UnsupportedEncodingException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return;
    }
    
    private boolean sendFriendshipRequest(String friendUsername, String passphrase){
        /* client -> server: MSG_LENGTH|REQUEST_FRIENDSHIP|CERTIFICATE.length|CERTIFICATE
         server -> client: MSG_LENGTH|SESSIONKEY.length|E(P_peer)[SESSIONKEY]|hash
         client -> server: MSG_LENGTH|SESSIONKEY_RETRIEVED|E(SESSIONKEY)[USERNAME]|hash : USERNAME is the peer whom is requested for friendship
         server -> client: MSG_LENGTH|E(SESSIONKEY)[USER_CERTIFICATE]|hash : user certificate of the peer whom is requested for friendship
         OR
         server -> client: MSG_LENGTH|PEER_NOT_FOUND
         client -> server: MSG_LENGTH|E(SESSIONKEY)[PASSPHRASE.length|
         -> E(P_peer)[PASSPHRASE]]|hash
         server -> client: MSG_LENGTH|FRIENDSHIP_REQ_RECEIVED
         OR
         server -> client: MSG_LENGTH|PEER_NOT_FOUND
         */
        try{
            Socket toRendServer = new Socket(mainSettings.rendezvousServerAddress, mainSettings.rendezvousServerPort);
            toRendServer.setSoTimeout(IDLE_LIMIT);
            DataOutputStream outToRendServer = new DataOutputStream(toRendServer.getOutputStream());
            byte[] certBytes = readMyCert();
            if (certBytes == null){
                return false;
            }
            byte[] buffer = new byte[4 + 4 + 4 + certBytes.length];
            int len = 0;
            // client -> server: MSG_LENGTH|REQUEST_FRIENDSHIP|CERTIFICATE.length|CERTIFICATE
            System.arraycopy(intToByteArray(4 + 4 + certBytes.length), 0, buffer, len, 4);
            len += 4;
            System.arraycopy(intToByteArray(hdr.REQUEST_FRIENDSHIP), 0, buffer, len, 4);
            len += 4;
            System.arraycopy(intToByteArray(certBytes.length), 0, buffer, len, 4);
            len += 4;
            System.arraycopy(certBytes, 0, buffer, len, certBytes.length);
            outToRendServer.write(buffer, 0, buffer.length);
            outToRendServer.flush();
            int i = 0;
            byte[] lenBytes = new byte[4];
            while(i < 4){
                i += toRendServer.getInputStream().read(lenBytes, i, 4 - i);
                if (i < 0){
                    return false;
                }
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            len = byteArrayToInt(lenBytes);
            byte[] msg = new byte[len];
            i = 0;
            while(i < len){
                i += toRendServer.getInputStream().read(msg, i, len - i);
                if (i < 0){
                    return false;
                }
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            // server -> client: MSG_LENGTH|SESSIONKEY.length|E(P_peer)[SESSIONKEY]|hash
            byte[] encryptedSessionKeyWithLength = new byte[msg.length - globalProperties.messageDigestSize];
            System.arraycopy(msg, 0, encryptedSessionKeyWithLength, 0, encryptedSessionKeyWithLength.length);
            byte[] sessionKeyBytes = decryptWithPriKey(encryptedSessionKeyWithLength, globalProperties.asymCipher);
            if (sessionKeyBytes == null){
                return false;
            }
            sessionKey = new SecretKeySpec(sessionKeyBytes, globalProperties.sessionKeyType);
            byte[] hash = hashMessage(sessionKeyBytes, globalProperties.messageDigestAlgorithm);
            byte[] recvdHash = new byte[globalProperties.messageDigestSize];
            System.arraycopy(msg, encryptedSessionKeyWithLength.length, recvdHash, 0, recvdHash.length);
            String hashStr = new sun.misc.BASE64Encoder().encode(hash);
            String recvdHashStr = new sun.misc.BASE64Encoder().encode(recvdHash);
            if (!hashStr.equals(recvdHashStr))
            {
                return false;
            }
            // client -> server: MSG_LENGTH|SESSIONKEY_RETRIEVED|E(SESSIONKEY)[USERNAME]|hash : USERNAME is the peer whom is requested for friendship
            byte[] encryptedFriendUsername = encryptSession(friendUsername.getBytes("UTF-8"));
            hash = hashMessage(friendUsername.getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
            buffer = new byte[4 + 4 + encryptedFriendUsername.length + hash.length];
            System.arraycopy(intToByteArray(4 + encryptedFriendUsername.length + hash.length), 0, buffer, 0, 4);
            System.arraycopy(intToByteArray(hdr.SESSIONKEY_RETRIEVED), 0, buffer, 4, 4);
            System.arraycopy(encryptedFriendUsername, 0, buffer, 8, encryptedFriendUsername.length);
            System.arraycopy(hash, 0, buffer, 8 + encryptedFriendUsername.length, hash.length);
            outToRendServer.write(buffer, 0, buffer.length);
            outToRendServer.flush();
            /* server -> client: MSG_LENGTH|E(SESSIONKEY)[USER_CERTIFICATE]|hash : user certificate of the peer whom is requested for friendship
             OR
             server -> client: MSG_LENGTH|PEER_NOT_FOUND
             */
            i = 0;
            lenBytes = new byte[4];
            while(i < 4){
                i += toRendServer.getInputStream().read(lenBytes, i, 4 - i);
                if (i < 0){
                    return false;
                }
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            len = byteArrayToInt(lenBytes);
            msg = new byte[len];
            i = 0;
            while(i < len){
                i += toRendServer.getInputStream().read(msg, i, len - i);
                if (i < 0){
                    return false;
                }
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            if(msg.length == 4 && byteArrayToInt(msg) == hdr.PEER_NOT_FOUND){
                return false;
            }
            byte[] encryptedCertBytes = new byte[msg.length - globalProperties.messageDigestSize];
            System.arraycopy(msg, 0, encryptedCertBytes, 0, encryptedCertBytes.length);
            certBytes = decryptSession(encryptedCertBytes);
            if (certBytes == null){
                return false;
            }
            hash = hashMessage(certBytes, globalProperties.messageDigestAlgorithm);
            recvdHash = new byte[globalProperties.messageDigestSize];
            System.arraycopy(msg, encryptedCertBytes.length, recvdHash, 0, recvdHash.length);
            hashStr = new sun.misc.BASE64Encoder().encode(hash);
            recvdHashStr = new sun.misc.BASE64Encoder().encode(recvdHash);
            if (!hashStr.equals(recvdHashStr))
            {
                return false;
            }
            CertVerifier y = new CertVerifier();
            userCertificate uc = y.verifyCertificate(certBytes, globalProperties.caCertPath, globalProperties.keyPairAlgorithm, globalProperties.certSigAlgorithm);
            if (uc == null){
                return false;
            }
            /* client -> server: MSG_LENGTH|E(SESSIONKEY)[PASSPHRASE.length|
             -> E(P_peer)[PASSPHRASE]]|hash
             */
            byte[] encryptedPassphrase = encryptWithPubKey(passphrase.getBytes("UTF-8"), globalProperties.asymCipher, uc.publicKey);
            byte[] rawReply = new byte[4 + encryptedPassphrase.length];
            len = 0;
            System.arraycopy(intToByteArray(passphrase.getBytes("UTF-8").length), 0, rawReply, len, 4);
            len += 4;
            System.arraycopy(encryptedPassphrase, 0, rawReply, len, encryptedPassphrase.length);
            hash = hashMessage(rawReply, globalProperties.messageDigestAlgorithm);
            byte[] encryptedMsg = encryptSession(rawReply);
            buffer = new byte[4 + encryptedMsg.length + globalProperties.messageDigestSize];
            len = 0;
            System.arraycopy(intToByteArray(encryptedMsg.length + globalProperties.messageDigestSize), 0, buffer, len, 4);
            len += 4;
            System.arraycopy(encryptedMsg, 0, buffer, len, encryptedMsg.length);
            len += encryptedMsg.length;
            System.arraycopy(hash, 0, buffer, len, hash.length);
            outToRendServer.write(buffer, 0, buffer.length);
            outToRendServer.flush();
            /* server -> client: MSG_LENGTH|FRIENDSHIP_REQ_RECEIVED
             OR
             server -> client: MSG_LENGTH|PEER_NOT_FOUND
             */
            i = 0;
            lenBytes = new byte[4];
            while(i < 4){
                i += toRendServer.getInputStream().read(lenBytes, i, 4 - i);
                if (i < 0){
                    return false;
                }
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            len = byteArrayToInt(lenBytes);
            msg = new byte[len];
            i = 0;
            while(i < len){
                i += toRendServer.getInputStream().read(msg, i, len - i);
                if (i < 0)
                {
                    return false;
                }
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            if (byteArrayToInt(msg) == hdr.FRIENDSHIP_REQ_RECEIVED){
                return true;
            }
        }catch(UnknownHostException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(IOException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
        
    }
    
    public boolean start(){
        boolean disconnected = true;
        while (disconnected){
            try{
                mainSettings.refresh(mainSettings.BASIC_INFO);
                while (mainSettings.username == null || mainSettings.username.equals("") || !mainSettings.keyFound || !mainSettings.CAKeyFound || !mainSettings.certFound){
                    mainSettings.refresh(mainSettings.BASIC_INFO);
                    Thread.sleep(10000);
                }
                if (!selectInterface()){
                    if (DEBUG){
                        System.out.println("DEBUG: line 1124 of MyZoneEngine. was not able to obtain a network interface.");
                    }
                    return false;
                }
                mainSettings.refresh(mainSettings.ALL);
                for (int i = 0; i < mainSettings.friends.size(); i++){
                    friend f = mainSettings.friends.get(i);
                    if (f.getStatus().equals("connected")){
                        f.setStatus("disconnected");
                        mainSettings.friends.add(i, f);
                        mainSettings.friends.remove(i + 1);
                    }
                }
                for (int i = 0; i < mainSettings.originals.size(); i++){
                    mirror m = mainSettings.originals.get(i);
                    if (m.getStatus().equals("syncing")){
                        m.setStatus("idle");
                        mainSettings.originals.add(i, m);
                        mainSettings.originals.remove(i + 1);
                    }
                }
                for (int i = 0; i < mainSettings.mirrors.size(); i++){
                    mirror m = mainSettings.mirrors.get(i);
                    if (m.getStatus().equals("syncing")){
                        m.setStatus("idle");
                        mainSettings.mirrors.add(i, m);
                        mainSettings.mirrors.remove(i + 1);
                    }
                }
                while(!registerPeer()){
                    mainSettings.refresh(mainSettings.BASIC_INFO);
                    System.out.println("peer could not register !!!");
                    Thread.sleep(10000);
                }
                if (server == null){
                    System.out.println("Starting Serving Thread ...");
                    server = new servingThread(prefix, di, relay);
                    server.start();
                }else if (!server.isAlive()){
                    System.out.println("Starting Serving Thread ...");
                    server = new servingThread(prefix, di, relay);
                    server.start();
                }
                Thread.sleep(100);
                if (client == null){
                    System.out.println("Starting Client Thread ...");
                    client = new clientThread(prefix);
                    client.start();
                }else if (!client.isAlive()){
                    System.out.println("Starting Client Thread ...");
                    client = new clientThread(prefix);
                    client.start();
                }
                Thread.sleep(100);
                long timePassed = System.currentTimeMillis();
                disconnected = false;
                while(!disconnected){
                    try{
                        for (int i = 0; i < mainSettings.pendingFriendships.size(); i++){
                            friend pf = new friend(mainSettings.pendingFriendships.get(i));
                            if (pf.getSent().equals("true")){
                                if (mainSettings.isFriend(pf.getUser().getUsername())){
                                    mainSettings.pendingFriendships.remove(i);
                                    mainSettings.save(mainSettings.PENDINGFRIENDSHIPS);
                                }
                                continue;
                            }
                            if (mainSettings.devPriority == 0){
                                if (sendFriendshipRequest(pf.getUser().getUsername(), pf.getPassphrase())){
                                    pf.setSent("true");
                                    pf.setLastUpdateTime(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis());
                                    mainSettings.pendingFriendships.remove(i);
                                    if (!mainSettings.isFriend(pf.getUser().getUsername()))
                                        mainSettings.pendingFriendships.add(i, pf);
                                }
                            }
                        }
                        mainSettings.save(mainSettings.PENDINGFRIENDSHIPS);
                        Thread.sleep(120000);// register every 2 minutes;
                        mainSettings.refresh(mainSettings.ALL);
                        while(!registerPeer()){
                            System.out.println("peer could not register !!!");
                            Thread.sleep(10000);
                        }
                        if (!server.isAlive()){
                            server = new servingThread(prefix, di, relay);
                            server.start();
                        }
                        timePassed = System.currentTimeMillis() - timePassed;
                        if (timePassed > 150000){
                            disconnected = true;
                        }else{
                            timePassed = System.currentTimeMillis();
                        }
                    }catch(Exception ex){
                        if (DEBUG){
                            ex.printStackTrace();
                        }
                    }
                }
            }catch(Exception e){
                if (DEBUG){
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
    
    public static void main(String args[]) {
        System.out.println("Starting MyZone Engine ...");
        MyZoneEngine engine = new MyZoneEngine("../../");
        engine.start();
    }
}



