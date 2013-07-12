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


import java.io.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
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
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Calendar;
import java.util.TimeZone;

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
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Text;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;


import utils.attributes.*;
import utils.peer.*;
import utils.net.SecureSocket.*;
import utils.net.SecureSocket.RelaySocket.*;
import utils.security.KeyPairUtil.*;
import utils.security.CertUtil.*;
import utils.security.CertUtil.userCertificate;

import MyZone.*;
import MyZone.elements.*;

/* this file implements the clientThread class and its components namely, syncingThread, postingThread and updatingThread.
 The clientThread is instantiated and started from MyZoneEngine class and is responsible for all activities that are
 done on behalf of the host in the role of a client. 
 The clientThread has three components: 
 1. syncingThread: which initiates syncing process with lower priority machines and mirrors. it is a sequential process
 which means that syncing starts from the immediately lower priority machine and ends at the mirror with the lowest priority.
 this process is invoked based on the syncPeriod value from the settings class.
 2. postingThread: which is responsible for posting all the pending changes to friend's profiles. The pending changes are read from
 pendingChanges.xml.
 3. updatingThread: which is responsible for getting updates from friend's profiles. 
 */

public class clientThread extends Thread{
    
    private final static boolean DEBUG = false;
    private String prefix;
    private Settings mainSettings;
    private globalAttributes globalProperties;
    private PrivateKey priKey;
    private header hdr = new header();
    private final static int IDLE_LIMIT = 60000;
    
    clientThread(String prefix){
        this.prefix = prefix;
        mainSettings = new Settings(prefix);
        globalProperties = new globalAttributes();
        globalProperties.init(prefix);
    }
    
    // used to collect status of connections to other peers for experimental purposes.
    private boolean appendLog(String id, String action, String destination, String machine, String filesize, String status, String timestamp){
        mainSettings.refresh(mainSettings.BASIC_INFO);
        if (!mainSettings.logUsage){
            return false;
        }
        try{
            if (!(new File(prefix + "tmp")).exists()){
                (new File(prefix + "tmp")).mkdirs();
            }
            FileWriter fstream = new FileWriter(prefix + "tmp/log.csv", true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(id + "," + action + "," + destination + "," + machine + "," + filesize + "," + status + "," + timestamp + "\n");
            out.close();
            fstream.close();
            return true;
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
        
    }
    
    private byte[] readXML(String filename){
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
            byte[] b = new byte[100000];
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
    
    private byte[] readMyCert(){
        CertVerifier y = new CertVerifier();
        String myCertPath = prefix + mainSettings.username + "/cert/";
        byte[] certBytes = y.readRawFile(myCertPath, mainSettings.username + ".cert");
        return certBytes;
    }
    
    private userCertificate extractCertificate(byte[] certBytes){
        CertVerifier x = new CertVerifier();
        userCertificate clientCert = x.verifyCertificate(certBytes, globalProperties.caCertPath, globalProperties.keyPairAlgorithm, globalProperties.certSigAlgorithm);
        if (clientCert != null){
            return clientCert;
        }
        return null;
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
                if (DEBUG){
                    e.printStackTrace();
                }
            }
        }catch(java.security.NoSuchProviderException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
    }
    
    private byte[] signHash(byte[] rawBytes){
        byte[] hash = hashMessage(rawBytes, globalProperties.messageDigestAlgorithm);
        byte[] signedHash;
        signedHash = encryptWithPriKey(hash, globalProperties.asymCipher);
        return signedHash;
    }
    
    private byte[] extractHash(byte[] encryptedHash, Key key){
        byte[] hash;
        hash = decryptWithPubKey(encryptedHash, globalProperties.asymCipher, key);
        return hash;
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
            KeyPairUtil x = new KeyPairUtil();
            priKey = x.readPriKey(prefix + mainSettings.username + "/keys/", mainSettings.username, globalProperties.keyPairAlgorithm);
            if (priKey == null){
                if (DEBUG){
                    System.out.println("DEBUG: line 268 of clientThread.java. private key is null");
                }
                return null;
            }
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
    
    private byte[] decryptWithPubKey(byte[] encryptedMsg, String keyAlgorithm, Key key){
		try{
            int msgLength = globalProperties.messageDigestSize;
            int chunks = (int)(encryptedMsg.length / (int)(globalProperties.certificateKeySize/8));
            byte[] decryptedMsg = new byte[chunks * (int)(globalProperties.certificateKeySize/8)];
            byte[] encryptedChunk = new byte[(int)(globalProperties.certificateKeySize/8)];
            byte[] decryptedChunk = null;
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance(keyAlgorithm, "BC");
            cipher.init(Cipher.DECRYPT_MODE, key);
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
    
    private byte[] encryptSession(byte[] data, Key sessionKey){
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
    
    private byte[] decryptSession(byte[] encrypted, Key sessionKey){
        
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
    
    private byte[] encryptWithPriKey(byte[] plain, String keyAlgorithm){
        try{
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance(keyAlgorithm, "BC");
            KeyPairUtil x = new KeyPairUtil();
            priKey = x.readPriKey(prefix + mainSettings.username + "/keys/", mainSettings.username, globalProperties.keyPairAlgorithm);
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
    
    private friend locatePeer(friend f, int priority, boolean strict){
        /* client -> server: MSG_LENGTH|FIND_PEER|CERTIFICATE.length|CERTIFICATE
         server -> client: MSG_LENGTH|SESSIONKEY.length|E(P_peer)[SESSIONKEY]|hash
         client -> server: MSG_LENGTH|SESSIONKEY_RETRIEVED|E(SESSIONKEY)[PRIORITY|PASSPHRASE]|hash
         server -> client: MSG_LENGTH|PEER_FOUND|E(SESSIONKEY)[PRIORITY|IP_ADDRESS.length|IP_ADDRESS|PORT|TYPE_OF_PROTOCOL|
         ->RELAY_ADDRESS.length|RELAY_ADDRESS|RELAY_PORT|MIRRORS.length|[(MIRROR_USERNAME.length|MIRROR_USERNAME)*]|CERTIFICATE.length|CERTIFICATE|E(Q_peer)[MD]|E(Q_peer)[MIRRORS_HASH]]|hash
         MD = hash(PRIORITY+IP_ADDRESS+PORT+TYPE_OF_PROTOCOL+RELAY_ADDRESS+RELAY_PORT)
         MIRRORS_HASH = hash([MIRROR_USERNAME]*)
         OR
         server -> client: MSG_LENGTH|PEER_NOT_FOUND
         */
        if (f == null)
            return null;
        try{
            if (mainSettings.rendezvousServerAddress == null || mainSettings.rendezvousServerAddress.equals("")){
                if (DEBUG){
                    System.out.println("DEBUG: line 489 of clientThread.java. Could not find the address of the Rendezvous Server");
                }
                //if (strict){
                //    return null;
                //}
                return f;
            }
            Socket toRendServer = new Socket(mainSettings.rendezvousServerAddress, mainSettings.rendezvousServerPort);
            toRendServer.setSoTimeout(IDLE_LIMIT);
            DataOutputStream outToRendServer = new DataOutputStream(toRendServer.getOutputStream());
            byte[] certBytes = readMyCert();
            if (certBytes == null){
                if (DEBUG){
                    System.out.println("DEBUG: line 502 of clientThread.java. Could not read in your certificate");
                }
                //if (strict){
                //    return null;
                //}
                return f;
            }
            // client -> server: MSG_LENGTH|FIND_PEER|CERTIFICATE.length|CERTIFICATE
            byte[] buffer = new byte[4 + 4 + 4 + certBytes.length];
            int len = 0;
            System.arraycopy(intToByteArray(4 + 4 + certBytes.length), 0, buffer, len, 4);
            len += 4;
            System.arraycopy(intToByteArray(hdr.FIND_PEER), 0, buffer, len, 4);
            len += 4;
            System.arraycopy(intToByteArray(certBytes.length), 0, buffer, len, 4);
            len += 4;
            System.arraycopy(certBytes, 0, buffer, len, certBytes.length);
            outToRendServer.write(buffer, 0, buffer.length);
            outToRendServer.flush();
            // server -> client: MSG_LENGTH|SESSIONKEY.length|E(P_peer)[SESSIONKEY]|hash
            int i = 0;
            byte[] lenBytes = new byte[4];
            while(i < 4){
                i += toRendServer.getInputStream().read(lenBytes, i, 4 - i);
                if (i < 0){
                    if (DEBUG){
                        System.out.println("DEBUG: line 528 of clientThread.java");
                    }
                    //if (strict){
                    //    return null;
                    //}
                    return f;
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
                    if (DEBUG){
                        System.out.println("DEBUG: line 550 of clientThread.java");
                    }
                   // if (strict){
                   //     return null;
                   // }
                    return f;
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
                if (DEBUG){
                    System.out.println("DEBUG: line 570 of clientThread.java. sessionKeyBytes == null");
                }
               // if (strict){
               //     return null;
               // }
                return f;
            }
            Key sessionKey = new SecretKeySpec(sessionKeyBytes, globalProperties.sessionKeyType);
            byte[] hash = hashMessage(sessionKeyBytes, globalProperties.messageDigestAlgorithm);
            byte[] recvdHash = new byte[globalProperties.messageDigestSize];
            System.arraycopy(msg, encryptedSessionKeyWithLength.length, recvdHash, 0, recvdHash.length);
            String hashStr = new sun.misc.BASE64Encoder().encode(hash);
            String recvdHashStr = new sun.misc.BASE64Encoder().encode(recvdHash);
            if (!hashStr.equals(recvdHashStr))
            {
                if (DEBUG){
                    System.out.println("DEBUG: line 586 of clientThread.java. hashStr != recvdHashStr");
                }
              //  if (strict){
              //      return null;
               // }
                return f;
            }
            // client -> server: MSG_LENGTH|SESSIONKEY_RETRIEVED|E(SESSIONKEY)[PRIORITY|PASSPHRASE]|hash
            
            byte[] rawReply = new byte[4 + f.getPassphrase().getBytes("UTF-8").length];
            System.arraycopy(intToByteArray(priority), 0, rawReply, 0, 4);
            System.arraycopy(f.getPassphrase().getBytes("UTF-8"), 0, rawReply, 4, f.getPassphrase().getBytes("UTF-8").length);
            byte[] encryptedMsg = encryptSession(rawReply, sessionKey);
            byte[] toBeSend = new byte[4 + 4 + encryptedMsg.length + globalProperties.messageDigestSize];
            hash = hashMessage(rawReply, globalProperties.messageDigestAlgorithm);
            len = 0;
            System.arraycopy(intToByteArray(4 + encryptedMsg.length + globalProperties.messageDigestSize), 0, toBeSend, len, 4);
            len += 4;
            System.arraycopy(intToByteArray(hdr.SESSIONKEY_RETRIEVED), 0, toBeSend, len, 4);
            len += 4;
            System.arraycopy(encryptedMsg, 0, toBeSend, len, encryptedMsg.length);
            len += encryptedMsg.length;
            System.arraycopy(hash, 0, toBeSend, len, hash.length);
            outToRendServer.write(toBeSend, 0, toBeSend.length);
            outToRendServer.flush();
            /* server -> client: MSG_LENGTH|PEER_FOUND|E(SESSIONKEY)[PRIORITY|IP_ADDRESS.length|IP_ADDRESS|PORT|TYPE_OF_PROTOCOL|
             ->RELAY_ADDRESS.length|RELAY_ADDRESS|RELAY_PORT|MIRRORS.length|[(MIRROR_USERNAME.length|MIRROR_USERNAME)*]|CERTIFICATE.length|CERTIFICATE|E(Q_peer)[MD]|E(Q_peer)[MIRRORS_HASH]]|hash
             OR
             server -> client: MSG_LENGTH|PEER_NOT_FOUND
             */
            lenBytes = new byte[4];
            i = 0;
            while(i < 4){
                i += toRendServer.getInputStream().read(lenBytes, i, 4 - i);
                if (i < 0){
                    if (DEBUG){
                        System.out.println("DEBUG: line 622 of clientThread.java");
                    }
                 //   if (strict){
                 //       return null;
                 //   }
                    return f;
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
                    if (DEBUG){
                        System.out.println("DEBUG: line 644 of clientThread.java");
                    }
                 //   if (strict){
                 //       return null;
                 //   }
                    return f;
                }
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            if (len == 4){
                if (byteArrayToInt(msg) == hdr.PEER_NOT_FOUND){
                    if (DEBUG){
                        System.out.println("DEBUG: line 662 of clientThread.java. PEER_NOT_FOUND");
                    }
                    return null;
                }else{
                    return f;
                }
            }
            encryptedMsg = new byte[msg.length - 4 - globalProperties.messageDigestSize];
            System.arraycopy(msg, 4, encryptedMsg, 0, encryptedMsg.length);
            rawReply = decryptSession(encryptedMsg, sessionKey);
            recvdHash = new byte[globalProperties.messageDigestSize];
            System.arraycopy(msg, 4 + encryptedMsg.length, recvdHash, 0, recvdHash.length);
            hash = hashMessage(rawReply, globalProperties.messageDigestAlgorithm);
            hashStr = new sun.misc.BASE64Encoder().encode(hash);
            recvdHashStr = new sun.misc.BASE64Encoder().encode(recvdHash);
            if (!hashStr.equals(recvdHashStr))
            {
                if (DEBUG){
                    System.out.println("DEBUG: line 680 of clientThread.java. hashStr != recvdHashStr");
                }
              //  if (strict){
              //      return null;
              //  }
                return f;
            }
            /* PRIORITY|IP_ADDRESS.length|IP_ADDRESS|PORT|TYPE_OF_PROTOCOL|
             ->RELAY_ADDRESS.length|RELAY_ADDRESS|RELAY_PORT|MIRRORS.length|[(MIRROR_USERNAME.length|MIRROR_USERNAME)*]|CERTIFICATE|E(Q_peer)[MD]|E(Q_peer)[MIRRORS_HASH]
             */
            lenBytes = new byte[4];
            System.arraycopy(rawReply, 0, lenBytes, 0, 4);
            len = byteArrayToInt(lenBytes);
            int recvdPriority = len;
            System.arraycopy(rawReply, 4, lenBytes, 0, 4);
            len = byteArrayToInt(lenBytes);
            byte[] ipAddressBytes = new byte[len];
            System.arraycopy(rawReply, 8, ipAddressBytes, 0, len);
            byte[] portBytes = new byte[4];
            len += 8;
            System.arraycopy(rawReply, len, portBytes, 0, 4);
            len += 4;
            byte[] typeOfProtocolBytes = new byte[4];
            System.arraycopy(rawReply, len, typeOfProtocolBytes, 0, 4);
            len += 4;
            byte[] relayAddressLenBytes = new byte[4];
            System.arraycopy(rawReply, len, relayAddressLenBytes, 0, 4);
            len += 4;
            int relayAddressLen = byteArrayToInt(relayAddressLenBytes);
            byte[] relayAddressBytes = new byte[relayAddressLen];
            System.arraycopy(rawReply, len, relayAddressBytes, 0, relayAddressLen);
            len += relayAddressLen;
            byte[] relayPortBytes = new byte[4];
            System.arraycopy(rawReply, len, relayPortBytes, 0, 4);
            len += 4;
            byte[] mirrorsLength = new byte[4];
            System.arraycopy(rawReply, len, mirrorsLength, 0, 4);
            len += 4;
            List<String> mirrors = new ArrayList<String>();
            int x = 0;
            if (DEBUG){
                System.out.println("DEBUG: line 721 of clientThread.java. number of mirrors = " + byteArrayToInt(mirrorsLength));
            }
            for (i = 0; i < byteArrayToInt(mirrorsLength); i++){
                byte[] mirrorLength = new byte[4];
                System.arraycopy(rawReply, len, mirrorLength, 0, 4);
                len += 4;
                byte[] mirror = new byte[byteArrayToInt(mirrorLength)];
                System.arraycopy(rawReply, len, mirror, 0, mirror.length);
                len += mirror.length;
                mirrors.add(new String(mirror, "UTF-8"));
                if (DEBUG){
                    System.out.println("DEBUG: line 732 of clientThread.java. mirror name = " + new String(mirror, "UTF-8"));
                }
                x += mirror.length;
            }
            byte[] certLen = new byte[4];
            System.arraycopy(rawReply, len, certLen, 0, 4);
            certBytes = new byte[byteArrayToInt(certLen)];
            len += 4;
            System.arraycopy(rawReply, len, certBytes, 0, certBytes.length);
            len += certBytes.length;
            CertVerifier y = new CertVerifier();
            userCertificate uc = y.verifyCertificate(certBytes, globalProperties.caCertPath, globalProperties.keyPairAlgorithm, globalProperties.certSigAlgorithm);
            if (uc == null){
                if (DEBUG){
                    System.out.println("DEBUG: line 746 of clientThread.java. userCertificate == null");
                }
              //  if (strict){
              //      return null;
              //  }
                return f;
            }
            if (!uc.username.equals(f.getUser().getUsername()) && priority < 3)
            {
             //   if (strict){
             //       return null;
             //   }
                return f;
            }
            if (priority < 3 && !f.getUser().getUsername().equals(mainSettings.username)){
                File file = new File(prefix + mainSettings.username + "/friends/" + uc.username + "/cert/" + uc.username + ".cert");
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(certBytes);
                fos.flush();
                fos.close();
            }
            byte[] recvdSignedHash = new byte[(rawReply.length - len)/2];
            byte[] recvdMirrorsSignedHash = new byte[(rawReply.length - len)/2];
            byte[] recvdMirrorsHash = new byte[globalProperties.messageDigestSize];
            System.arraycopy(rawReply, len, recvdSignedHash, 0, recvdSignedHash.length);
            len += recvdSignedHash.length;
            System.arraycopy(rawReply, len, recvdMirrorsSignedHash, 0, recvdMirrorsSignedHash.length);
            // MD = hash(PRIORITY+IP_ADDRESS+PORT+TYPE_OF_PROTOCOL+RELAY_ADDRESS+RELAY_PORT+[MIRROR_USERNAME]*
            byte[] rawBytes = new byte[4 + ipAddressBytes.length + 4 + 4 + relayAddressBytes.length + 4];
            len = 0;
            System.arraycopy(intToByteArray(recvdPriority), 0, rawBytes, len, 4);
            len += 4;
            System.arraycopy(ipAddressBytes, 0, rawBytes, len, ipAddressBytes.length);
            len += ipAddressBytes.length;
            System.arraycopy(portBytes, 0, rawBytes, len, 4);
            len += 4;
            System.arraycopy(typeOfProtocolBytes, 0, rawBytes, len, 4);
            len += 4;
            System.arraycopy(relayAddressBytes, 0, rawBytes, len, relayAddressBytes.length);
            len += relayAddressBytes.length;
            System.arraycopy(relayPortBytes, 0, rawBytes, len, 4);
            len = 0;
            byte[] mirrorsRawBytes = null;
            if (x > 0){
                mirrorsRawBytes = new byte[x];
                for (i = 0; i < mirrors.size(); i++){
                    String mirror = (String)mirrors.get(i);
                    System.arraycopy(mirror.getBytes("UTF-8"), 0, mirrorsRawBytes, len, mirror.getBytes("UTF-8").length);
                    len += mirror.getBytes("UTF-8").length;
                }
            }
            hash = hashMessage(rawBytes, globalProperties.messageDigestAlgorithm);
            recvdHash = extractHash(recvdSignedHash, extractCertificate(certBytes).publicKey);
            recvdMirrorsHash = extractHash(recvdMirrorsSignedHash, extractCertificate(certBytes).publicKey);
            hashStr = new sun.misc.BASE64Encoder().encode(hash);
            byte[] mirrorsHash = null;
            String mirrorsHashStr = null;
            if (x > 0){
                mirrorsHash = hashMessage(mirrorsRawBytes, globalProperties.messageDigestAlgorithm);
                mirrorsHashStr = new sun.misc.BASE64Encoder().encode(mirrorsHash);
            }
            recvdHashStr = new sun.misc.BASE64Encoder().encode(recvdHash);
            String recvdMirrorsHashStr = new sun.misc.BASE64Encoder().encode(recvdMirrorsHash);
            if (!hashStr.equals(recvdHashStr) && extractCertificate(certBytes).username.equals(f.getUser().getUsername()))
            {
                if (DEBUG){
                    System.out.println("DEBUG: line 813 of clientThread.java");
                }
             //   if (strict){
             //       return null;
             //   }
                return f;
            }
            f.setIPAddress(new String(ipAddressBytes, "UTF-8"));
            f.setPort(byteArrayToInt(portBytes));
            f.setRelayAddress(new String(relayAddressBytes, "UTF-8"));
            f.setRelayPort(byteArrayToInt(relayPortBytes));
            f.setTypeOfProtocol(byteArrayToInt(typeOfProtocolBytes));
            if (priority < 3 && x > 0 && recvdMirrorsHashStr.equals(mirrorsHashStr)){
                f.setMirrors(mirrors);
                if (DEBUG){
                    System.out.println("DEBUG: line 828 of clientThread.java. updating mirrors");
                }
            }else{
                if (DEBUG){
                    System.out.println("DEBUG: line 832 of clientThread.java. not updating mirrors");
                }
            }
            f.setLastUpdateTime(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis());
            if (DEBUG){
                System.out.println("DEBUG: line 837 of clientThread.java. value of friend returned from the rendezvous server = " + f);
            }
            return f;
        }catch(UnknownHostException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(IOException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
       // if (strict){
       //     return null;
       // }
        return f;
    }
    private SecureSocket connectToFriend(friend f){
        SecureSocket conn = null;
        TLSClientSocket client = null;
        RelayTLSClientSocket relayClient = null;
        if (f == null)
            return null;
        if (f.getUser() == null || f.getIPAddress() == null || mainSettings.username == null || globalProperties.myCertPath == null || globalProperties.myKeyPath == null || globalProperties.sessionCipher == null || globalProperties.sessionKeyType == null)
            return null;
        try{
            if(f.getTypeOfProtocol()  == globalProperties.TCP && f.getRelayAddress().equals("0.0.0.0"))
            {
                // friend with public ip address
                client = new TLSClientSocket(f.getUser().getUsername(), globalProperties.TCP, f.getIPAddress(), f.getPort(), mainSettings.username, globalProperties.myCertPath, globalProperties.myKeyPath, globalProperties.sessionCipher, globalProperties.sessionKeyType);
                conn = client.initiate();
                return conn;
            }
            if(f.getTypeOfProtocol() == globalProperties.TCP && !f.getRelayAddress().equals("0.0.0.0"))
            {
                // friend is behind a symetric NAT (firewall)
                relayClient = new RelayTLSClientSocket(f.getUser().getUsername(), f.getRelayAddress(), f.getRelayPort(), mainSettings.username, globalProperties.myCertPath, globalProperties.myKeyPath, globalProperties.sessionCipher, globalProperties.sessionKeyType);
                conn = relayClient.initiate();
                return conn;
            }
            if(f.getTypeOfProtocol() == globalProperties.UDP){
                // friend is behind a full cone NAT and UDP hole punching is possible
                client = new TLSClientSocket(f.getUser().getUsername(), globalProperties.UDP, f.getIPAddress(), f.getPort(), mainSettings.username, globalProperties.myCertPath, globalProperties.myKeyPath, globalProperties.sessionCipher, globalProperties.sessionKeyType);
                conn = client.initiate();
                return conn;
            }
        }catch(SocketException e){
            if (client != null){
                client.close();
            }
            if (relayClient != null){
                relayClient.close();
            }
            if (conn != null){
                if (!conn.isClosed()){
                    conn.close();
                }
            }
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(SocketTimeoutException e){
            if (client != null){
                client.close();
            }
            if (relayClient != null){
                relayClient.close();
            }
            if (conn != null){
                if (!conn.isClosed()){
                    conn.close();
                }
            }
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(IOException e){
            if (client != null){
                client.close();
            }
            if (relayClient != null){
                relayClient.close();
            }
            if (conn != null){
                if (!conn.isClosed()){
                    conn.close();
                }
            }
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
    }
    public void run(){
        postingThread pThread = null;
        syncingThread sThread = null;
        List<String> currentZones = new ArrayList();
        while(true){
            mainSettings.refresh(mainSettings.ALL);
            if (pThread != null){
                if (!pThread.isAlive()){
                    pThread = new postingThread(prefix);
                    pThread.start();
                }
            }else{
                pThread = new postingThread(prefix);
                pThread.start();
            }
            try{
                Thread.sleep(10000);
            }catch(Exception e){
                if (DEBUG){
                    e.printStackTrace();
                }
            }
            if (sThread != null){
                if (!sThread.isAlive()){
                    sThread = new syncingThread(prefix);
                    sThread.start();
                }
            }else{
                sThread = new syncingThread(prefix);
                sThread.start();
            }
            try{
                Thread.sleep(10000);
            }catch(Exception e){
                if (DEBUG){
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < mainSettings.zones.size(); i++){
                zone z = new zone(mainSettings.zones.get(i));
                int j = 0;
                for (j = 0; j < currentZones.size(); j++){
                    String zoneName = new String(currentZones.get(j));
                    if (zoneName.equals(z.getName())){
                        break;
                    }
                }
                if (j == currentZones.size()){
                    currentZones.add(new String(z.getName()));
                    new updatingThread(prefix, z).start();
                }
            }
            try{
                Thread.sleep(60000); // once every minute
            }catch(java.lang.InterruptedException e){
                if (DEBUG){
                    e.printStackTrace();
                }
                continue;
            }
        }
    }
    
    class syncingThread extends Thread{
        String prefix;
        SecureSocket conn;
        Settings syncingThreadSettings;
        syncingThread(String prefix){
            this.prefix = prefix;
            syncingThreadSettings = new Settings(prefix);
        }
        
        public void run(){
            // this needs to be sequential. starting at the top most priority mirror.
            // receive updates from the mirror.
            // merge with your own version and send back the updates to the mirror.
            // send correctImage.xml too.
            // update sync time. 
            // do this periodically. and eventually all mirrors would have the correct image of the profile.
            if (DEBUG){
                System.out.println("DEBUG: line 1011 of clientThread.java. ***** In Syncing Thread *****");
            }
            syncingThreadSettings.refresh(syncingThreadSettings.ALL);
            conn = null;
            int x = syncingThreadSettings.devPriority + 1;
            while (x < 3){
                user usr = new user(syncingThreadSettings.username, null, null, 0);
                friend f = new friend(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "NONE", usr, null, null, 0, null, 0, 0, 0);
                friend tmp = locatePeer(f, x, true);
                if (tmp != null){
                    f = new friend(tmp);
                    conn = connectToFriend(f);
                }
                if (conn == null){
                    if (x == 1){
                        System.out.println("It appears that your secondary device is not available to sync!!!");
                    }else{
                        System.out.println("It appears that your tertiary device is not available to sync!!!");
                    }
                    x++;
                    continue;
                }
                if (x == 1){
                    System.out.println("Syncing with the Secondary Device ... please do not disconnect!!!");
                }else{
                    System.out.println("Syncing with the Tertiary Device ... please do not disconnect!!!");
                }
                String s = "SYNC|" + syncingThreadSettings.username + "|" + syncingThreadSettings.getLastSyncTime(x) + "|";
                byte[] response = null;
                try{
                    response = s.getBytes("UTF-8");
                }catch(IOException e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                    x++;
                    continue;
                }
                try{
                    conn.send(response);
                }catch(IOException e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                    if (!conn.isClosed()){
                        conn.close();
                    }
                    x++;
                    continue;
                }
                mirror m = new mirror(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), syncingThreadSettings.username, "NONE", -1, -1, 0, syncingThreadSettings.getLastSyncTime(x), 0);
                long usedCapacity = receiveUpdates(m);
                if (usedCapacity >= 0)
                    syncingThreadSettings.setLastSyncTime(x, m.getLastSyncTime());
                conn.close();
                System.out.println("Syncing Completed ... you can safely disconnect now.");
                x++;
            }
            syncingThreadSettings.refresh(syncingThreadSettings.BASIC_INFO);
            syncingThreadSettings.refresh(syncingThreadSettings.MIRRORS);
            syncingThreadSettings.refresh(syncingThreadSettings.FRIENDS);
            for (int i = 0; i < syncingThreadSettings.mirrors.size(); i++){
                mirror m = new mirror(syncingThreadSettings.mirrors.get(i));
                for (int j = 0; j < syncingThreadSettings.friends.size(); j++){
                    friend f = new friend(syncingThreadSettings.friends.get(j)); 
                    if (f.getUser().getUsername().equals(m.getUsername())){
                        friend tmp = locatePeer(f, 0, true);
                        if (tmp != null){
                            f = new friend(tmp);
                            conn = connectToFriend(f);
                        }
                        if (syncingThreadSettings.isMirroringFor(f.getUser().getUsername()) > 0 && syncingThreadSettings.isMirroringFor(f.getUser().getUsername()) < f.getLastUpdateTime()){
                            int k = 0;
                            List<String> mirrors = f.getMirrors();
                            for (k = 0; k < mirrors.size(); k++){
                                String fm = new String(mirrors.get(k));
                                if (fm.equals(syncingThreadSettings.username)) // still mirroring
                                    break;
                            }
                            if (k == f.getMirrors().size()) // the other user deleted me as their mirror
                            {
                                if (DEBUG){
                                    System.out.println("DEBUG: line 1093 of clientThread.java. DELETING ORIGINAL --------------->>>>>>");
                                }
                                syncingThreadSettings.deleteOriginal(f.getUser().getUsername());
                            }
                        }
                        break;
                    }
                }
                if (conn == null){
                    System.out.println("It appears that your mirror " + m.getUsername() + " is not available to sync.");
                    continue;
                }
                System.out.println("Syncing with the Mirror " + m.getUsername() + " ... please do not disconnect!!!");
                String s = "SYNC|" + syncingThreadSettings.username + "|" + m.getLastSyncTime() + "|";
                byte[] response = null;
                try{
                    response = s.getBytes("UTF-8");
                }catch(IOException e){
                    System.out.println("Syncing failed ... but you can safely disconnect now.");
                    if (DEBUG){
                        e.printStackTrace();
                    }
                    continue;
                }
                try{
                    conn.send(response);
                }catch(IOException e){
                    System.out.println("Syncing failed ... but you can safely disconnect now.");
                    if (DEBUG){
                        e.printStackTrace();
                    }
                    if (!conn.isClosed()){
                        conn.close();
                    }
                    continue;
                }
                long usedCapacity = receiveUpdates(m);
                if (usedCapacity >= 0){
                    m.setUsed(usedCapacity);
                    syncingThreadSettings.updateMirror(m);
                    System.out.println("Syncing Completed ... you can safely disconnect now.");
                }else{
                    System.out.println("Syncing failed ... but you can safely disconnect now.");
                }
                if (!conn.isClosed()){
                    conn.close();
                }
            }
            try{
                if (syncingThreadSettings.syncPeriod < 900){
                    syncingThreadSettings.syncPeriod = 900;
                }
                Thread.sleep(syncingThreadSettings.syncPeriod * 1000);
            }catch(Exception e){
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
        // this function builds a list of all files that has been modified since the last sync time
        // with this mirror.
        private List<String> getModifiedFiles(long lastVersion){
            try{
                String path = "";
                path = prefix + syncingThreadSettings.username;
                
                List<String> filenames = new ArrayList();
                if (new File(path + "/correctImage.xml").exists())
                    filenames.add(new String(path + "/correctImage.xml"));
                if (new File(path + "/existingImage.xml").exists())
                    filenames.add(new String(path + "/existingImage.xml"));
                if (new File(path + "/friends.xml").exists())
                    filenames.add(new String(path + "/friends.xml"));
                if (new File(path + "/zones.xml").exists())
                    filenames.add(new String(path + "/zones.xml"));
                if (new File(path + "/inbox.xml").exists())
                    filenames.add(new String(path + "/inbox.xml"));
                if (new File(path + "/photos.xml").exists())
                    filenames.add(new String(path + "/photos.xml"));
                if (new File(path + "/videos.xml").exists())
                    filenames.add(new String(path + "/videos.xml"));
                if (new File(path + "/audios.xml").exists())
                    filenames.add(new String(path + "/audios.xml"));
                if (new File(path + "/events.xml").exists())
                    filenames.add(new String(path + "/events.xml"));
                if (new File(path + "/notifications.xml").exists())
                    filenames.add(new String(path + "/notifications.xml"));
                if (new File(path + "/awaitingFriendships.xml").exists())
                    filenames.add(new String(path + "/awaitingFriendships.xml"));
                if (new File(path + "/receivedMirroringRequests.xml").exists())
                    filenames.add(new String(path + "/receivedMirroringRequests.xml"));
                for (int i = 0; i < syncingThreadSettings.zones.size(); i++){
                    zone z = new zone(syncingThreadSettings.zones.get(i));
                    File file = new File(path + "/zones/" + z.getName() + "/modifiedFiles.xml");
                    boolean exists = file.exists();
                    if (!exists){
                        continue;
                    }
                    filenames.add(new String(path + "/zones/" + z.getName() + "/modifiedFiles.xml"));
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document dom;
                    
                    byte[] readIn = null;
                    while((readIn = readXML(path + "/zones/" + z.getName() + "/modifiedFiles.xml")) == null){
                        Thread.sleep(100);
                    }
                    if (readIn.length > 0){
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
                                    boolean found = false; // avoid duplicates
                                    for (int k = 0; k < filenames.size(); k++){
                                        String name = new String(filenames.get(k));
                                        if (name.equals(ce.getFilename())){
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found){
                                        filenames.add(ce.getFilename());
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
            }
            return null;
        }
        // this function is responsible for receiving all the updates from mirror and merging them with 
        // the original profile.
        private long receiveUpdates(mirror m){
            FileChannel channel = null;
            FileLock lock = null;
            FileInputStream fis = null;
            long usedCapacity = -1;
            try{
                new File(prefix + syncingThreadSettings.username + "/tmp").mkdirs();
                // receiving updates
                if (DEBUG){
                    System.out.println("DEBUG: line 1251 of clientThread.java. receiving last updates from mirror " + m);
                }
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document dom;
                byte[] response = conn.receive();
                String s = new String(response, "UTF-8");
                syncingThreadSettings.refresh(syncingThreadSettings.ALL);
                if (s.indexOf("DELETED") == 0){
                    syncingThreadSettings.deleteMirror(m.getUsername());
                    if (DEBUG){
                        System.out.println("DEBUG: line 1262 of clientThread.java. " + m.getUsername() + " deleted your mirror copy and was removed from your mirror list");
                    }
                    return -1;
                }
                if (s.indexOf("GET_READY_FOR_UPDATES") != 0){
                    if (DEBUG){
                        System.out.println("DEBUG: line 1268 of clientThread.java. the value of s is: " + s + " instead of GET_READY_FOR_UPDATES");
                    }
                    return -1;
                }
                s = "READY";
                conn.send(s.getBytes("UTF-8"));
                response = conn.receive();
                // SENDING_FILE|FILENAME|FILE_LENGTH|
                s = new String(response, "UTF-8");
                List<String> receivedFiles = new ArrayList();
                while(s.indexOf("DONE") != 0){
                    if (s.indexOf("SENDING_FILE") != 0){
                        if (DEBUG){
                            System.out.println("DEBUG: line 1281 of clientThread.java. the value of s is: " + s);
                        }
                        return -1;
                    }
                    long fileLength = 0;
                    String filename = "";
                    if (s.indexOf("|") < s.indexOf("|", s.indexOf("|") + 1) && s.indexOf("|", s.indexOf("|") + 1) < s.lastIndexOf("|")){
                        filename = s.substring(s.indexOf("|") + 1, s.indexOf("|", s.indexOf("|") + 1));
                        if (DEBUG){
                            System.out.println("DEBUG: line 1290 of clientThread.java. receiving filename = " + filename);
                        }
                        fileLength = Long.parseLong(s.substring(s.indexOf("|", s.indexOf("|") + 1) + 1, s.lastIndexOf("|")));
                        if (DEBUG){
                            System.out.println("DEBUG: line 1294 of clientThread.java. receiving filelength = " + fileLength);
                        }
                    }else{
                        if (DEBUG){
                            System.out.println("DEBUG: line 1298 of clientThread.java. the value of s is: " + s);
                        }
                        return -1;
                    }
                    if (filename.indexOf("/") < filename.lastIndexOf("/")){
                        String dirName = filename.substring(filename.indexOf("/") + 1, filename.lastIndexOf("/"));
                        if (filename.contains(".xml")){
                            boolean success = (new File(prefix + syncingThreadSettings.username + "/tmp/" + dirName)).mkdirs();
                            if (!success && !(new File(prefix + syncingThreadSettings.username + "/tmp/" + dirName)).exists()){
                                if (DEBUG){
                                    System.out.println("DEBUG: line 1308 of clientThread.java.");
                                }
                                return -1;
                            }
                        }else{
                            boolean success = (new File(prefix + syncingThreadSettings.username + "/" + dirName)).mkdirs();
                            if (!success && !(new File(prefix + syncingThreadSettings.username + "/" + dirName)).exists()){
                                if (DEBUG){
                                    System.out.println("DEBUG: line 1316 of clientThread.java.");
                                }
                                return -1;
                            }
                        }
                    }
                    filename = filename.substring(filename.indexOf("/") + 1, filename.length());
                    if (filename.contains(".xml")){
                        filename = prefix + syncingThreadSettings.username + "/tmp/" + filename;
                    }else{
                        filename = prefix + syncingThreadSettings.username + "/" + filename;
                        if ((new File(filename)).exists()){
                            if ((new File(filename)).length() == fileLength){
                                s = "SKIP";
                                response = s.getBytes("UTF-8");
                                conn.send(response);
                                response = conn.receive();
                                s = new String(response, "UTF-8");
                                continue;
                            }
                        }
                    }
                    receivedFiles.add(filename);
                    File file = new File(filename);
                    int retries = 0;
                    FileOutputStream fos = new FileOutputStream(file);
                    s = "READY";
                    response = s.getBytes("UTF-8");
                    conn.send(response);
                    while (fileLength > 0 && retries < 20){
                        byte[] buf;
                        buf = conn.receive();
                        fos.write(buf, 0, buf.length);
                        fileLength -= buf.length;
                        if (buf.length > 0){
                            retries = 0;
                            response = new String("ACK").getBytes("UTF-8");
                            conn.send(response);
                        }else{
                            response = new String("NACK").getBytes("UTF-8");
                            conn.send(response);
                            retries++;
                        }
                    }
                    fos.flush();
                    fos.close();
                    response = conn.receive();
                    // SENDING_FILE|FILENAME|FILE_LENGTH|
                    s = new String(response, "UTF-8");
                }
                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                List<String> updatedFiles = new ArrayList();
                long correctSyncTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
                synchronized(syncingThreadSettings.mirrors){
                    syncingThreadSettings.refresh(syncingThreadSettings.MIRRORS);
                    int i = 0;
                    for (i = 0; i < syncingThreadSettings.mirrors.size(); i++){
                        mirror mi = new mirror(syncingThreadSettings.mirrors.get(i));
                        if (mi.getUsername().equals(m.getUsername())){
                            break;
                        }
                    }
                    if (i < syncingThreadSettings.mirrors.size()){
                        syncingThreadSettings.updateMirror(m);
                    }
                }
                // merging stage: Merging each received file with the existing one.
                for (int i = 0; i < receivedFiles.size(); i++){
                    String filename = new String(receivedFiles.get(i));
                    if (filename.contains("friends.xml")){
                        File file = new File(filename);
                        dom = db.parse(file);
                        Element docEle = dom.getDocumentElement();
                        NodeList fl = docEle.getElementsByTagName("friend");
                        boolean updated = false;
                        if(fl != null && fl.getLength() > 0) {
                            for (int j = 0; j < fl.getLength(); j++){
                                Element fel = (Element)fl.item(j);
                                friend f = new friend();
                                f.create(fel);
                                int k = 0;
                                syncingThreadSettings.refresh(syncingThreadSettings.BASIC_INFO);
                                syncingThreadSettings.refresh(syncingThreadSettings.FRIENDS);
                                for (k = 0; k < syncingThreadSettings.friends.size(); k++){
                                    friend ef = new friend(syncingThreadSettings.friends.get(k));
                                    if (ef.getUser().getUsername().equals(f.getUser().getUsername())){
                                        if (f.getLastUpdateTime() > ef.getLastUpdateTime()){
                                            f.setLatestVersion(ef.getLatestVersion());
                                            syncingThreadSettings.friends.add(k, f); // existing friend was modified
                                            syncingThreadSettings.friends.remove(k + 1);
                                            updated = true;
                                        }
                                        break;
                                    }
                                }
                                if (k == syncingThreadSettings.friends.size()){
                                    f.setLatestVersion(0);
                                    syncingThreadSettings.friends.add(f); // add as a new friend
                                    syncingThreadSettings.refresh(syncingThreadSettings.PENDINGFRIENDSHIPS);
                                    if (!(new File(prefix + syncingThreadSettings.username + "/friends/" + f.getUser().getUsername()).exists())){
                                        boolean success = (new File(prefix + syncingThreadSettings.username + "/friends/" + f.getUser().getUsername())).mkdirs();
                                    }
                                    if (!(new File(prefix + syncingThreadSettings.username + "/friends/" + f.getUser().getUsername() + "/audios").exists())){
                                        boolean success = (new File(prefix + syncingThreadSettings.username + "/friends/" + f.getUser().getUsername() + "/audios")).mkdirs();
                                    }
                                    if (!(new File(prefix + syncingThreadSettings.username + "/friends/" + f.getUser().getUsername() + "/videos").exists())){
                                        boolean success = (new File(prefix + syncingThreadSettings.username + "/friends/" + f.getUser().getUsername() + "/videos")).mkdirs();
                                    }
                                    if (!(new File(prefix + syncingThreadSettings.username + "/friends/" + f.getUser().getUsername() + "/photos").exists())){
                                        boolean success = (new File(prefix + syncingThreadSettings.username + "/friends/" + f.getUser().getUsername() + "/photos")).mkdirs();
                                    }
                                    if (!(new File(prefix + syncingThreadSettings.username + "/friends/" + f.getUser().getUsername() + "/wall").exists())){
                                        boolean success = (new File(prefix + syncingThreadSettings.username + "/friends/" + f.getUser().getUsername() + "/wall")).mkdirs();
                                    }
                                    if (!(new File(prefix + syncingThreadSettings.username + "/friends/" + f.getUser().getUsername() + "/cert").exists())){
                                        boolean success = (new File(prefix + syncingThreadSettings.username + "/friends/" + f.getUser().getUsername() + "/cert")).mkdirs();
                                    }
                                    for (int l = syncingThreadSettings.pendingFriendships.size() - 1; l >= 0; l--){
                                        friend pf = new friend(syncingThreadSettings.pendingFriendships.get(l));
                                        if (pf.getUser().getUsername().equals(f.getUser().getUsername())){
                                            syncingThreadSettings.pendingFriendships.remove(l);
                                        }
                                        break;
                                    }
                                    syncingThreadSettings.save(syncingThreadSettings.PENDINGFRIENDSHIPS);
                                    updated = true;
                                }
                            }
                        }
                        if (updated){
                            syncingThreadSettings.save(syncingThreadSettings.FRIENDS);
                            updatedFiles.add(new String(prefix + syncingThreadSettings.username + "/friends.xml"));
                        }
                        file.delete();
                    }else if (filename.contains("mirrors.xml")){
                        File file = new File(filename);
                        dom = db.parse(file);
                        Element docEle = dom.getDocumentElement();
                        NodeList ml = docEle.getElementsByTagName("mirror");
                        boolean updated = false;
                        if(ml != null && ml.getLength() > 0) {
                            for (int j = 0; j < ml.getLength(); j++){
                                Element mel = (Element)ml.item(j);
                                mirror mi = new mirror();
                                mi.create(mel);
                                int k = 0;
                                syncingThreadSettings.refresh(syncingThreadSettings.BASIC_INFO);
                                syncingThreadSettings.refresh(syncingThreadSettings.MIRRORS);
                                for (k = 0; k < syncingThreadSettings.mirrors.size(); k++){
                                    mirror em = new mirror(syncingThreadSettings.mirrors.get(k));
                                    if (em.getUsername().equals(mi.getUsername())){
                                        if (mi.getLastUpdateTime() > em.getLastUpdateTime()){
                                            mi.setLastSyncTime(em.getLastSyncTime());
                                            mi.setUsed(em.getUsed());
                                            syncingThreadSettings.mirrors.remove(k);
                                            syncingThreadSettings.mirrors.add(mi); // append to the end for now
                                            updated = true;
                                        }
                                        break;
                                    }
                                }
                                if (k == syncingThreadSettings.mirrors.size()){
                                    mi.setLastSyncTime(0);
                                    mi.setUsed(0);
                                    syncingThreadSettings.mirrors.add(mi); // add as a new mirror
                                    updated = true;
                                }
                            }
                        }
                        if (updated){
                            for (int j = 0; j < syncingThreadSettings.mirrors.size(); j++){
                                mirror em = new mirror(syncingThreadSettings.mirrors.get(j));
                                if (em.getPriority() != j){
                                    if (em.getPriority() > j){
                                        syncingThreadSettings.mirrors.remove(j);
                                        syncingThreadSettings.mirrors.add(em); // append it to the end for now
                                    }else{
                                        syncingThreadSettings.mirrors.remove(j);
                                        syncingThreadSettings.mirrors.add(em.getPriority(), em); // the exisitng one at this position must have a priority >= em.getPriority()
                                    }
                                }
                            }
                            for (int j = 0; j < syncingThreadSettings.mirrors.size(); j++){
                                mirror em = new mirror(syncingThreadSettings.mirrors.get(j));
                                if (em.getPriority() != j){
                                    em.setPriority(j);
                                    syncingThreadSettings.mirrors.add(j, em);
                                    syncingThreadSettings.mirrors.remove(j + 1);
                                }
                            }
                            syncingThreadSettings.save(syncingThreadSettings.MIRRORS);
                            updatedFiles.add(new String(prefix + syncingThreadSettings.username + "/mirrors.xml"));
                        }
                        file.delete();
                    }else if (filename.contains("originals.xml")){
                        File file = new File(filename);
                        dom = db.parse(file);
                        Element docEle = dom.getDocumentElement();
                        NodeList ml = docEle.getElementsByTagName("mirror");
                        boolean updated = false;
                        if(ml != null && ml.getLength() > 0) {
                            for (int j = 0; j < ml.getLength(); j++){
                                Element mel = (Element)ml.item(j);
                                mirror mi = new mirror();
                                mi.create(mel);
                                int k = 0;
                                syncingThreadSettings.refresh(syncingThreadSettings.BASIC_INFO);
                                syncingThreadSettings.refresh(syncingThreadSettings.ORIGINALS);
                                for (k = 0; k < syncingThreadSettings.originals.size(); k++){
                                    mirror em = new mirror(syncingThreadSettings.originals.get(k));
                                    if (em.getUsername().equals(mi.getUsername())){
                                        if (mi.getLastUpdateTime() > em.getLastUpdateTime()){
                                            mi.setUsed(em.getUsed());
                                            mi.setLastSyncTime(em.getLastSyncTime());
                                            syncingThreadSettings.originals.remove(k);
                                            syncingThreadSettings.originals.add(mi); // append to the end for now
                                            updated = true;
                                        }
                                        break;
                                    }
                                }
                                if (k == syncingThreadSettings.originals.size()){
                                    mi.setUsed(0);
                                    mi.setLastSyncTime(0);
                                    syncingThreadSettings.originals.add(mi); // add as a new mirror
                                    updated = true;
                                }
                            }
                        }
                        if (updated){
                            for (int j = 0; j < syncingThreadSettings.originals.size(); j++){
                                mirror em = new mirror(syncingThreadSettings.originals.get(j));
                                if (em.getPriority() != j){
                                    if (em.getPriority() > j){
                                        syncingThreadSettings.originals.remove(j);
                                        syncingThreadSettings.originals.add(em); // append it to the end for now
                                    }else{
                                        syncingThreadSettings.originals.remove(j);
                                        syncingThreadSettings.originals.add(em.getPriority(), em); // the exisitng one at this position must have a priority >= em.getPriority()
                                    }
                                }
                            }
                            for (int j = 0; j < syncingThreadSettings.originals.size(); j++){
                                mirror em = new mirror(syncingThreadSettings.originals.get(j));
                                if (em.getPriority() != j){
                                    em.setPriority(j);
                                    syncingThreadSettings.originals.add(j, em);
                                    syncingThreadSettings.originals.remove(j + 1);
                                }
                            }
                            syncingThreadSettings.save(syncingThreadSettings.ORIGINALS);
                            updatedFiles.add(new String(prefix + syncingThreadSettings.username + "/originals.xml"));
                        }
                        file.delete();
                    }else if (filename.contains("outbox.xml")){
                        profile userProfile = new profile(syncingThreadSettings.username, syncingThreadSettings.username, prefix);
                        File file = new File(filename);
                        dom = db.parse(file);
                        Element docEle = dom.getDocumentElement();
                        NodeList ml = docEle.getElementsByTagName("message");
                        boolean updated = false;
                        if(ml != null && ml.getLength() > 0) {
                            for (int j = 0; j < ml.getLength(); j++){
                                Element mel = (Element)ml.item(j);
                                message me = new message();
                                me.create(mel);
                                if (userProfile.sendMessage(me)){
                                    updated = true;
                                }
                            }
                        }
                        if (updated){
                            updatedFiles.add(new String(prefix + syncingThreadSettings.username + "/outbox.xml"));
                        }
                        file.delete();
                    }else if (filename.contains("inbox.xml")){
                        File file = new File(filename);
                        profile userProfile = new profile(syncingThreadSettings.username, syncingThreadSettings.username, prefix);
                        userProfile.loadInbox();
                        dom = db.parse(file);
                        Element docEle = dom.getDocumentElement();
                        NodeList ml = docEle.getElementsByTagName("message");
                        boolean updated = false;
                        if(ml != null && ml.getLength() > 0) {
                            for (int j = 0; j < ml.getLength(); j++){
                                Element mel = (Element)ml.item(j);
                                message me = new message();
                                me.create(mel);
                                int k = 0;
                                for (k = 0; k < userProfile.receivedMsgs.size(); k++){
                                    message em = new message(userProfile.receivedMsgs.get(k));
                                    if (em.getId() == me.getId()){
                                        break;
                                    }
                                }
                                if (k == userProfile.receivedMsgs.size()){ // new message
                                    int l = 0;
                                    for (l = 0; l < userProfile.receivedMsgs.size(); l++){
                                        message em = new message(userProfile.receivedMsgs.get(l));
                                        if (em.getId() < me.getId()){
                                            break;
                                        }
                                    }
                                    if (l < userProfile.receivedMsgs.size()){
                                        userProfile.receivedMsgs.add(l, me); // new message
                                    }else{
                                        userProfile.receivedMsgs.add(me); // new message
                                    }
                                    updated = true;
                                    userProfile.saveInbox();
                                }
                            }
                        }
                        if (updated){
                            updatedFiles.add(new String(prefix + syncingThreadSettings.username + "/inbox.xml"));
                        }
                        file.delete();
                    }else if (filename.contains("events.xml")){
                        File file = new File(filename);
                        profile userProfile = new profile(syncingThreadSettings.username, syncingThreadSettings.username, prefix);
                        dom = db.parse(file);
                        Element docEle = dom.getDocumentElement();
                        NodeList evl = docEle.getElementsByTagName("event");
                        if(evl != null && evl.getLength() > 0) {
                            for (int j = 0; j < evl.getLength(); j++){
                                Element eel = (Element)evl.item(j);
                                event ev = new event();
                                ev.create(eel);
                                userProfile.addEvent(ev, false);
                            }
                        }
                        updatedFiles.add(new String(prefix + syncingThreadSettings.username + "/events.xml"));
                        file.delete();
                    }else if (filename.contains("notifications.xml")){
                        File file = new File(filename);
                        profile userProfile = new profile(syncingThreadSettings.username, syncingThreadSettings.username, prefix);
                        dom = db.parse(file);
                        Element docEle = dom.getDocumentElement();
                        NodeList nl = docEle.getElementsByTagName("notification");
                        if(nl != null && nl.getLength() > 0) {
                            for (int j = 0; j < nl.getLength(); j++){
                                Element nel = (Element)nl.item(j);
                                notification n = new notification();
                                n.create(nel);
                                userProfile.insertNotificaton(n);
                            }
                        }
                        updatedFiles.add(new String(prefix + syncingThreadSettings.username + "/notifications.xml"));
                        file.delete();
                    }else if (filename.contains("passphrases.xml")){
                        File file = new File(filename);
                        dom = db.parse(file);
                        Element docEle = dom.getDocumentElement();
                        NodeList pl = docEle.getElementsByTagName("passphraseEntry");
                        boolean updated = false;
                        if(pl != null && pl.getLength() > 0) {
                            for (int j = 0; j < pl.getLength(); j++){
                                Element pel = (Element)pl.item(j);
                                passphraseEntry p = new passphraseEntry();
                                p.create(pel);
                                int k = 0;
                                syncingThreadSettings.refresh(syncingThreadSettings.BASIC_INFO);
                                syncingThreadSettings.refresh(syncingThreadSettings.PASSPHRASES);
                                for (k = 0; k < syncingThreadSettings.passphrases.size(); k++){
                                    passphraseEntry ep = new passphraseEntry(syncingThreadSettings.passphrases.get(k));
                                    if (ep.getUsername().equals(p.getUsername())){
                                        break;
                                    }
                                }
                                if (k == syncingThreadSettings.passphrases.size()){
                                    syncingThreadSettings.passphrases.add(p); // new passphrase
                                    updated = true;
                                }
                            }
                        }
                        if (updated){
                            syncingThreadSettings.save(syncingThreadSettings.PASSPHRASES);
                            updatedFiles.add(new String(prefix + syncingThreadSettings.username + "/passphrases.xml"));
                        }
                        file.delete();
                    }else if (filename.contains("pendingFriendships.xml")){
                        File file = new File(filename);
                        dom = db.parse(file);
                        Element docEle = dom.getDocumentElement();
                        NodeList fl = docEle.getElementsByTagName("friend");
                        boolean updated = false;
                        if(fl != null && fl.getLength() > 0) {
                            for (int j = 0; j < fl.getLength(); j++){
                                Element fel = (Element)fl.item(j);
                                friend f = new friend();
                                f.create(fel);
                                if (syncingThreadSettings.isFriend(f.getUser().getUsername()))
                                    continue;
                                int k = 0;
                                syncingThreadSettings.refresh(syncingThreadSettings.BASIC_INFO);
                                syncingThreadSettings.refresh(syncingThreadSettings.PENDINGFRIENDSHIPS);
                                for (k = 0; k < syncingThreadSettings.pendingFriendships.size(); k++){
                                    friend ef = new friend(syncingThreadSettings.pendingFriendships.get(k));
                                    if (ef.getUser().getUsername().equals(f.getUser().getUsername())){
                                        if (f.getLastUpdateTime() > ef.getLastUpdateTime()){
                                            syncingThreadSettings.pendingFriendships.add(k, f);
                                            syncingThreadSettings.pendingFriendships.remove(k + 1);
                                            updated = true;
                                        }
                                        break;
                                    }
                                }
                                if (k == syncingThreadSettings.pendingFriendships.size()){
                                    syncingThreadSettings.pendingFriendships.add(f); // new pendingfriendship request.
                                    updated = true;
                                }
                            }
                        }
                        if (updated){
                            syncingThreadSettings.save(syncingThreadSettings.PENDINGFRIENDSHIPS);
                            updatedFiles.add(new String(prefix + syncingThreadSettings.username + "/pendingFriendships.xml"));
                        }
                        file.delete();
                    }else if (filename.contains("awaitingFriendships.xml")){
                        File file = new File(filename);
                        dom = db.parse(file);
                        Element docEle = dom.getDocumentElement();
                        NodeList fl = docEle.getElementsByTagName("friend");
                        boolean updated = false;
                        if(fl != null && fl.getLength() > 0) {
                            for (int j = 0; j < fl.getLength(); j++){
                                Element fel = (Element)fl.item(j);
                                friend f = new friend();
                                f.create(fel);
                                if (syncingThreadSettings.isFriend(f.getUser().getUsername()))
                                    continue;
                                int k = 0;
                                syncingThreadSettings.refresh(syncingThreadSettings.BASIC_INFO);
                                syncingThreadSettings.refresh(syncingThreadSettings.AWAITINGFRIENDSHIPS);
                                for (k = 0; k < syncingThreadSettings.awaitingFriendships.size(); k++){
                                    friend ef = new friend(syncingThreadSettings.awaitingFriendships.get(k));
                                    if (ef.getUser().getUsername().equals(f.getUser().getUsername())){
                                        if (f.getLastUpdateTime() > ef.getLastUpdateTime()){
                                            syncingThreadSettings.awaitingFriendships.add(k, f);
                                            syncingThreadSettings.awaitingFriendships.remove(k + 1);
                                            updated = true;
                                        }
                                        break;
                                    }
                                }
                                if (k == syncingThreadSettings.awaitingFriendships.size()){
                                    syncingThreadSettings.awaitingFriendships.add(f); // new awaitingfriendship request.
                                    updated = true;
                                }
                            }
                        }
                        if (updated){
                            syncingThreadSettings.save(syncingThreadSettings.AWAITINGFRIENDSHIPS);
                            updatedFiles.add(new String(prefix + syncingThreadSettings.username + "/awaitingFriendships.xml"));
                        }
                        file.delete();
                    }else if (filename.contains("sentMirroringRequests.xml")){
                        File file = new File(filename);
                        dom = db.parse(file);
                        Element docEle = dom.getDocumentElement();
                        NodeList ml = docEle.getElementsByTagName("mirror");
                        boolean updated = false;
                        if(ml != null && ml.getLength() > 0) {
                            for (int j = 0; j < ml.getLength(); j++){
                                Element mel = (Element)ml.item(j);
                                mirror mi = new mirror();
                                mi.create(mel);
                                if (syncingThreadSettings.isMirrored(mi.getUsername()))
                                    continue;
                                int k = 0;
                                syncingThreadSettings.refresh(syncingThreadSettings.BASIC_INFO);
                                syncingThreadSettings.refresh(syncingThreadSettings.SENTMIRRORINGREQUESTS);
                                mirror em = new mirror();
                                for (k = 0; k < syncingThreadSettings.sentMirroringRequests.size(); k++){
                                    em = new mirror(syncingThreadSettings.sentMirroringRequests.get(k));
                                    if (mi.getId() >= em.getId()){
                                        break;
                                    }
                                    
                                }
                                if (k == syncingThreadSettings.sentMirroringRequests.size()){
                                    syncingThreadSettings.sentMirroringRequests.add(mi); // new request
                                    updated = true;
                                }else{
                                    if (em.getUsername().equals(mi.getUsername())){
                                        if (mi.getLastUpdateTime() > em.getLastUpdateTime()){
                                            syncingThreadSettings.sentMirroringRequests.add(k, mi); 
                                            syncingThreadSettings.sentMirroringRequests.remove(k + 1);
                                            updated = true;
                                        }
                                    }else{
                                        syncingThreadSettings.sentMirroringRequests.add(k, mi);
                                        updated = true;
                                    }
                                }
                            }
                        }
                        if (updated){
                            syncingThreadSettings.save(syncingThreadSettings.SENTMIRRORINGREQUESTS);
                            updatedFiles.add(new String(prefix + syncingThreadSettings.username + "/sentMirroringRequests.xml"));
                        }
                        file.delete();
                    }else if (filename.contains("receivedMirroringRequests.xml")){
                        File file = new File(filename);
                        dom = db.parse(file);
                        Element docEle = dom.getDocumentElement();
                        NodeList ml = docEle.getElementsByTagName("mirror");
                        boolean updated = false;
                        if(ml != null && ml.getLength() > 0) {
                            for (int j = 0; j < ml.getLength(); j++){
                                Element mel = (Element)ml.item(j);
                                mirror mi = new mirror();
                                mi.create(mel);
                                if (syncingThreadSettings.isMirroringFor(mi.getUsername()) > -1)
                                    continue;
                                int k = 0;
                                syncingThreadSettings.refresh(syncingThreadSettings.BASIC_INFO);
                                syncingThreadSettings.refresh(syncingThreadSettings.RECEIVEDMIRRORINGREQUESTS);
                                mirror em = new mirror();
                                for (k = 0; k < syncingThreadSettings.receivedMirroringRequests.size(); k++){
                                    em = new mirror(syncingThreadSettings.receivedMirroringRequests.get(k));
                                    if (mi.getId() >= em.getId()){
                                        break;
                                    }
                                    
                                }
                                if (k == syncingThreadSettings.receivedMirroringRequests.size()){
                                    syncingThreadSettings.receivedMirroringRequests.add(mi); // new request
                                    updated = true;
                                }else{
                                    if (em.getUsername().equals(mi.getUsername())){
                                        if (mi.getLastUpdateTime() > em.getLastUpdateTime()){
                                            syncingThreadSettings.receivedMirroringRequests.add(k, mi); 
                                            syncingThreadSettings.receivedMirroringRequests.remove(k + 1);
                                            updated = true;
                                        }
                                    }else{
                                        syncingThreadSettings.receivedMirroringRequests.add(k, mi);
                                        updated = true;
                                    }
                                }
                            }
                        }
                        if (updated){
                            syncingThreadSettings.save(syncingThreadSettings.RECEIVEDMIRRORINGREQUESTS);
                            updatedFiles.add(new String(prefix + syncingThreadSettings.username + "/receivedMirroringRequests.xml"));
                        }
                        file.delete();
                    }else if (filename.contains("zones.xml")){
                        File file = new File(filename);
                        dom = db.parse(file);
                        Element docEle = dom.getDocumentElement();
                        NodeList zl = docEle.getElementsByTagName("zone");
                        boolean updated = false;
                        if(zl != null && zl.getLength() > 0) {
                            for (int j = 0; j < zl.getLength(); j++){
                                Element zel = (Element)zl.item(j);
                                zone z = new zone();
                                z.create(zel);
                                int k = 0;
                                zone ez = new zone();
                                syncingThreadSettings.refresh(syncingThreadSettings.BASIC_INFO);
                                syncingThreadSettings.refresh(syncingThreadSettings.ZONES);
                                for (k = 0; k < syncingThreadSettings.zones.size(); k++){
                                    ez = new zone(syncingThreadSettings.zones.get(k));
                                    if (z.getId() >= ez.getId()){
                                        break;
                                    }
                                }
                                if (k == syncingThreadSettings.zones.size()){
                                    syncingThreadSettings.zones.add(z); // new zone
                                    if (!(new File(prefix + syncingThreadSettings.username + "/zones/" + z.getName() + "/wall").exists())){
                                        (new File(prefix + syncingThreadSettings.username + "/zones/" + z.getName() + "/wall")).mkdirs();
                                    }
                                    updated = true;
                                }else{
                                    if (ez.getName().equals(z.getName())){
                                        if (z.getLastUpdateTime() > ez.getLastUpdateTime()){
                                            syncingThreadSettings.zones.add(k, z);
                                            syncingThreadSettings.zones.remove(k + 1);
                                            updated = true;
                                        }
                                    }else{
                                        syncingThreadSettings.zones.add(k, z);
                                        updated = true;
                                    }
                                }
                            }
                        }
                        if (updated){
                            syncingThreadSettings.save(syncingThreadSettings.ZONES);
                            updatedFiles.add(new String(prefix + syncingThreadSettings.username + "/zones.xml"));
                        }
                        file.delete();
                    }else if (filename.contains("wall")){
                        profile tmpProfile = new profile(syncingThreadSettings.username, syncingThreadSettings.username, prefix);
                        tmpProfile.loadWall(filename);
                        List<timelineEntry> wall = new ArrayList();
                        for (int j = 0; j < tmpProfile.wall.size(); j++){
                            timelineEntry tle = new timelineEntry(tmpProfile.wall.get(j));
                            wall.add(tle);
                        }
                        profile userProfile = new profile(syncingThreadSettings.username, syncingThreadSettings.username, prefix);
                        for (int j = wall.size() - 1; j >= 0; j--){
                            timelineEntry entry = new timelineEntry(wall.get(j));
                            if (entry.type.equals("link")){
                                userProfile.addLink(entry.l, true);
                            }else if (entry.type.equals("wallPost")){
                                userProfile.addWallPost(entry.s, true);
                            }else if (entry.type.equals("audioAlbum")){
                                userProfile.addAudioAlbum(entry.aa, true);
                            }else if (entry.type.equals("videoAlbum")){
                                userProfile.addVideoAlbum(entry.va, true);
                            }else if (entry.type.equals("photoAlbum")){
                                userProfile.addPhotoAlbum(entry.pa, true);
                            }else if (entry.type.equals("audio")){
                                userProfile.addAudio(entry.a, true);
                            }else if (entry.type.equals("video")){
                                userProfile.addVideo(entry.v, true);
                            }else if (entry.type.equals("photo")){
                                userProfile.addPhoto(entry.p, true);
                            }else if (entry.type.equals("deletedEntry")){
                                if (entry.de.getType().equals("link")){
                                    userProfile.removeLink(entry.de.getId(), entry.de.getShareWith(), entry.de.getPostedBy(), true);
                                }else if (entry.de.getType().equals("wallPost")){
                                    userProfile.removewallPost(entry.de.getId(), entry.de.getShareWith(), entry.de.getPostedBy(), true);
                                }else if (entry.de.getType().equals("audioAlbum")){
                                    userProfile.removeAudioAlbum(entry.de.getId(), entry.de.getShareWith(), entry.de.getPostedBy(), true);
                                }else if (entry.de.getType().equals("videoAlbum")){
                                    userProfile.removeVideoAlbum(entry.de.getId(), entry.de.getShareWith(), entry.de.getPostedBy(), true);
                                }else if (entry.de.getType().equals("photoAlbum")){
                                    userProfile.removePhotoAlbum(entry.de.getId(), entry.de.getShareWith(), entry.de.getPostedBy(), true);
                                }else if (entry.de.getType().equals("audio")){
                                    userProfile.removeAudio(entry.de.getParent(), entry.de.getId(), entry.de.getShareWith(), entry.de.getPostedBy(), true);
                                }else if (entry.de.getType().equals("video")){
                                    userProfile.removeVideo(entry.de.getParent(), entry.de.getId(), entry.de.getShareWith(), entry.de.getPostedBy(), true);
                                }else if (entry.de.getType().equals("photo")){
                                    userProfile.removePhoto(entry.de.getParent(), entry.de.getId(), entry.de.getShareWith(), entry.de.getPostedBy(), true);
                                }
                                
                            }
                        }
                        File file = new File(filename);
                        if (file.exists()){
                            file.delete();
                        }
                    }else if (filename.contains("info.xml")){
                        profile userProfile = new profile(syncingThreadSettings.username, syncingThreadSettings.username, prefix);
                        dbf = DocumentBuilderFactory.newInstance();
                        db = dbf.newDocumentBuilder();
                        File file = new File(filename);
                        byte[] readIn = null;
                        while((readIn = readXML(filename)) == null){
                            Thread.sleep(100);
                        }
                        if (readIn.length > 0){
                            dom = db.parse(new ByteArrayInputStream(readIn));
                            Element docEle = dom.getDocumentElement();
                            NodeList nl = docEle.getElementsByTagName("info");
                            if(nl != null && nl.getLength() > 0) {
                                Element el = (Element)nl.item(0);
                                userProfile.userInfo.create(el);
                            }
                            userProfile.saveUserInfo();
                        }
                        file.delete();
                    }
                }
                long passed = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() - now.getTimeInMillis();
                if (DEBUG){
                    System.out.println("DEBUG: line 1986 of clientThread.java. Time that has passed for receiving updated files while syncing with this mirror: " + passed);
                }
                // wait 2 minutes before sending the latest profile to the mirror. This is to make sure that the merging is done in time
                // so that the mirror does not timeout on the receiving socket.
                if (passed <= 120000)
                    Thread.sleep(120000 - passed);
                if (DEBUG){
                    System.out.println("DEBUG: line 1993 of clientThread.java. WOKE UP AFTER SLEEP");
                }
                s = "GET_READY_FOR_UPDATES";
                response = s.getBytes("UTF-8");
                conn.send(response);
                response = conn.receive();
                s = new String(response, "UTF-8");
                if (s.indexOf("READY") != 0){
                    if (DEBUG){
                        System.out.println("DEBUG: line 2002 of clientThread.java. value of s is: " + s);
                    }
                    return -1;
                }
                List<String> files = getModifiedFiles(m.getLastSyncTime());
                files.addAll(updatedFiles);
                if (m.getUsername().equals(syncingThreadSettings.username)){
                    String path = prefix + syncingThreadSettings.username;
                    if (new File(path + "/mirrors.xml").exists())
                        files.add(new String(path + "/mirrors.xml"));
                    if (new File(path + "/originals.xml").exists())
                        files.add(new String(path + "/originals.xml"));
                    if (new File(path + "/sentMirroringRequests.xml").exists())
                        files.add(new String(path + "/sentMirroringRequests.xml"));
                    if (new File(path + "/pendingFriendships.xml").exists())
                        files.add(new String(path + "/pendingFriendships.xml"));
                    if (new File(path + "/outbox.xml").exists())
                        files.add(new String(path + "/outbox.xml"));
                    if (new File(path + "/passphrases.xml").exists())
                        files.add(new String(path + "/passphrases.xml"));
                }
                for (int i = 0; i < files.size(); i++){
                    if (DEBUG){
                        System.out.println("DEBUG: line 2025 of clientThread.java. NUMBER OF FILES REMAINING TO BE SENT IN THE SYNC PRIOCESS = " + (files.size() - i));
                    }
                    String absoluteFilename = (String)files.get(i);
                    String filename = "";
                    filename = absoluteFilename.substring(absoluteFilename.indexOf(syncingThreadSettings.username), absoluteFilename.length());
                    absoluteFilename = prefix + filename;
                    if (DEBUG){
                        System.out.println("DEBUG: line 2032 of clientThread.java. absoluteFilename = " + absoluteFilename);
                    }
                    File toBeSent = new File(absoluteFilename);
                    if (!toBeSent.exists())
                        continue;
                    fis = new FileInputStream(toBeSent);
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
                    s = "SENDING_FILE|" + filename + "|" + toBeSent.length() + "|";
                    response = s.getBytes("UTF-8");
                    conn.send(response);
                    byte[] ackBytes = conn.receive();
                    String ack = new String(ackBytes, "UTF-8");
                    int retries = 0;
                    if (ack.equals("SKIP")){
                        continue;
                    }
                    while (!ack.equals("ACK") && retries < 20){
                        conn.send(response);
                        ackBytes = conn.receive();
                        ack = new String(ackBytes, "UTF-8");
                        retries++;
                    }
                    if (retries == 20){
                        if (DEBUG){
                            System.out.println("DEBUG: line number 2072 of clientThread.java.");
                        }
                        if (lock != null){
                            lock.release();
                        }
                        channel.close();
                        fis.close();
                        return -1;
                    }
                    int sentBytes = 0;
                    while (-1 != (bytesRead = channel.read(buf))){
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
                        if (retries == 20){
                            if (DEBUG){
                                System.out.println("DEBUG: line number 2097 of clientThread.java.");
                            }
                            if (lock != null){
                                lock.release();
                            }
                            channel.close();
                            fis.close();
                            return -1;
                        }
                        buf.rewind();
                        sentBytes += bytesRead;
                    }
                    if (lock != null){
                        lock.release();
                    }
                    channel.close();
                    fis.close();
                }
                s = "DONE";
                response = s.getBytes("UTF-8");
                conn.send(response);
                response = conn.receive();
                s = new String(response, "UTF-8");
                usedCapacity = Long.parseLong(s);
                m.setLastSyncTime(correctSyncTime);
                deleteDir(new File(prefix + syncingThreadSettings.username + "/tmp"));
            }catch(Exception e){
                if (DEBUG){
                    e.printStackTrace();
                }
                usedCapacity = -1;
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
            return usedCapacity;
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
    }
    
    class postingThread extends Thread{
        Settings postingThreadSettings;
        String prefix;
        postingThread(String prefix){
            this.prefix = prefix;
            postingThreadSettings = new Settings(prefix);
        }
        
        // for the purpose of efficiency all the posts that need to be send to a particular user are appended together.
        private List<Long> indicesOfSimilarPosts(List<pendingChange> pendingChanges, String belongsTo){
            List<Long> indices = new ArrayList<Long>();
            if (pendingChanges.size() > 0){
                for (int j = pendingChanges.size() - 1; j >= 0; j--){
                    if (pendingChanges.get(j).getType().equals("link")){
                        if (pendingChanges.get(j).getEntry().l.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("wallPost")){
                        if (pendingChanges.get(j).getEntry().s.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("photo")){
                        if (pendingChanges.get(j).getEntry().p.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("photoAlbum")){
                        if (pendingChanges.get(j).getEntry().pa.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("video")){
                        if (pendingChanges.get(j).getEntry().v.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("videoAlbum")){
                        if (pendingChanges.get(j).getEntry().va.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("audio")){
                        if (pendingChanges.get(j).getEntry().a.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("audioAlbum")){
                        if (pendingChanges.get(j).getEntry().aa.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("linkComment")){
                        if (pendingChanges.get(j).getEntry().l.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("wallPostComment")){
                        if (pendingChanges.get(j).getEntry().s.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("audioAlbumComment")){
                        if (pendingChanges.get(j).getEntry().aa.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("videoAlbumComment")){
                        if (pendingChanges.get(j).getEntry().va.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("photoAlbumComment")){
                        if (pendingChanges.get(j).getEntry().pa.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("audioComment")){
                        if (pendingChanges.get(j).getEntry().a.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("videoComment")){
                        if (pendingChanges.get(j).getEntry().v.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("photoComment")){
                        if (pendingChanges.get(j).getEntry().p.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("linkLike")){
                        if (pendingChanges.get(j).getEntry().l.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("wallPostLike")){
                        if (pendingChanges.get(j).getEntry().s.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("audioAlbumLike")){
                        if (pendingChanges.get(j).getEntry().aa.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("videoAlbumLike")){
                        if (pendingChanges.get(j).getEntry().va.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("photoAlbumLike")){
                        if (pendingChanges.get(j).getEntry().pa.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("audioLike")){
                        if (pendingChanges.get(j).getEntry().a.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("videoLike")){
                        if (pendingChanges.get(j).getEntry().v.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("photoLike")){
                        if (pendingChanges.get(j).getEntry().p.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("linkDislike")){
                        if (pendingChanges.get(j).getEntry().l.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("wallPostDislike")){
                        if (pendingChanges.get(j).getEntry().s.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("audioAlbumDislike")){
                        if (pendingChanges.get(j).getEntry().aa.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("videoAlbumDislike")){
                        if (pendingChanges.get(j).getEntry().va.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("photoAlbumDislike")){
                        if (pendingChanges.get(j).getEntry().pa.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("audioDislike")){
                        if (pendingChanges.get(j).getEntry().a.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("videoDislike")){
                        if (pendingChanges.get(j).getEntry().v.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("photoDislike")){
                        if (pendingChanges.get(j).getEntry().p.getBelongsTo().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("message")){
                        if (pendingChanges.get(j).getEntry().m.getReceiver().getUsername().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("mirror")){
                        if (pendingChanges.get(j).getEntry().mi.getUsername().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("mirror")){
                        if (pendingChanges.get(j).getEntry().mi.getUsername().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId());
                        }
                    }else if (pendingChanges.get(j).getType().equals("event")){
                        if (pendingChanges.get(j).getEntry().e.getCreator().getUsername().equals(belongsTo)){
                            indices.add(pendingChanges.get(j).getId()); // replying to an event invitation
                        }else{
                            if (pendingChanges.get(j).getEntry().e.getCreator().getUsername().equals(postingThreadSettings.username)){ // the user is the creator and has updated the event
                                List<user> pending_users = pendingChanges.get(j).getEntry().e.getPendingNotification();
                                for (int i = pending_users.size() - 1; i >= 0; i--){
                                    user u = pending_users.get(i);
                                    if (u.getUsername().equals(belongsTo)){
                                        pending_users.remove(i);
                                    }
                                }
                                if (pending_users.size() == 0){
                                    indices.add(pendingChanges.get(j).getId());
                                }
                            }
                        }
                    }
                }
            }
            return indices;
        }
        
        private Element appendSimilarPosts(Document dom, Element rootEle, List<pendingChange> pendingChanges, String belongsTo){
            if (pendingChanges.size() > 0){
                for (int j = pendingChanges.size() - 1; j >= 0; j--){
                    if (pendingChanges.get(j).getType().equals("link")){
                        if (pendingChanges.get(j).getEntry().l.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("wallPost")){
                        if (pendingChanges.get(j).getEntry().s.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("photo")){
                        if (pendingChanges.get(j).getEntry().p.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("photoAlbum")){
                        if (pendingChanges.get(j).getEntry().pa.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("video")){
                        if (pendingChanges.get(j).getEntry().v.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("videoAlbum")){
                        if (pendingChanges.get(j).getEntry().va.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("audio")){
                        if (pendingChanges.get(j).getEntry().a.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("audioAlbum")){
                        if (pendingChanges.get(j).getEntry().aa.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("linkComment")){
                        if (pendingChanges.get(j).getEntry().l.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("wallPostComment")){
                        if (pendingChanges.get(j).getEntry().s.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("audioAlbumComment")){
                        if (pendingChanges.get(j).getEntry().aa.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("videoAlbumComment")){
                        if (pendingChanges.get(j).getEntry().va.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("photoAlbumComment")){
                        if (pendingChanges.get(j).getEntry().pa.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("audioComment")){
                        if (pendingChanges.get(j).getEntry().a.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("videoComment")){
                        if (pendingChanges.get(j).getEntry().v.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("photoComment")){
                        if (pendingChanges.get(j).getEntry().p.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("linkLike")){
                        if (pendingChanges.get(j).getEntry().l.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("wallPostLike")){
                        if (pendingChanges.get(j).getEntry().s.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("audioAlbumLike")){
                        if (pendingChanges.get(j).getEntry().aa.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("videoAlbumLike")){
                        if (pendingChanges.get(j).getEntry().va.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("photoAlbumLike")){
                        if (pendingChanges.get(j).getEntry().pa.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("audioLike")){
                        if (pendingChanges.get(j).getEntry().a.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("videoLike")){
                        if (pendingChanges.get(j).getEntry().v.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("photoLike")){
                        if (pendingChanges.get(j).getEntry().p.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("linkDislike")){
                        if (pendingChanges.get(j).getEntry().l.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("wallPostDislike")){
                        if (pendingChanges.get(j).getEntry().s.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("audioAlbumDislike")){
                        if (pendingChanges.get(j).getEntry().aa.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("videoAlbumDislike")){
                        if (pendingChanges.get(j).getEntry().va.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("photoAlbumDislike")){
                        if (pendingChanges.get(j).getEntry().pa.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("audioDislike")){
                        if (pendingChanges.get(j).getEntry().a.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("videoDislike")){
                        if (pendingChanges.get(j).getEntry().v.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("photoDislike")){
                        if (pendingChanges.get(j).getEntry().p.getBelongsTo().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("message")){
                        if (pendingChanges.get(j).getEntry().m.getReceiver().getUsername().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("mirror")){
                        if (pendingChanges.get(j).getEntry().mi.getUsername().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                        }
                    }else if (pendingChanges.get(j).getType().equals("event")){
                        if (pendingChanges.get(j).getEntry().e.getCreator().getUsername().equals(belongsTo)){
                            rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom)); // replying to an event invitation
                        }else{
                            if (pendingChanges.get(j).getEntry().e.getCreator().getUsername().equals(postingThreadSettings.username)){ // the user is the creator and has updated the event
                                List<user> pending_users = pendingChanges.get(j).getEntry().e.getPendingNotification();
                                for (int i = pending_users.size() - 1; i >= 0; i--){
                                    user u = pending_users.get(i);
                                    if (u.getUsername().equals(belongsTo)){
                                        rootEle.appendChild(pendingChanges.get(j).createDOMElement(dom));
                                        pending_users.remove(i);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return rootEle;
        }
        
        public synchronized void run(){
            // read in the pendingchanges.xml lock the file and send request starting from the oldest one and receive updates from that user.
            // remove the entry and save it right away. and release the lock.
            // *******************
            //FileChannel toSendFileChannel = null;
            //FileLock toSendFileLock = null;
            FileInputStream fis = null;
            ByteArrayOutputStream baos = null;
            FileChannel toBeReceivedChannel = null;
            FileLock toBeReceivedLock = null;
            FileOutputStream fos = null;
            SecureSocket conn = null;
            boolean disconnectAtTheEnd = false;
            int i = 0;
            friend f = null;
            try{
                if (DEBUG){
                    System.out.println("DEBUG: line 2518 of clientThread.java. *******  IN POSTING THREAD  *******");
                }
                postingThreadSettings.refresh(postingThreadSettings.ALL); 
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document dom;
                String path = prefix + postingThreadSettings.username;
                byte[] readIn = null;
                while((readIn = readXML(path + "/pendingChanges.xml")) == null){
                    Thread.sleep(100);
                }
                List<pendingChange> pendingChanges = new ArrayList<pendingChange>();
                List<pendingChange> updatedPendingChanges = new ArrayList<pendingChange>();
                if (readIn.length > 0){
                    dom = db.parse(new ByteArrayInputStream(readIn));
                    Element docEle = dom.getDocumentElement();
                    NodeList nl = docEle.getElementsByTagName("pendingChange");
                    if(nl != null && nl.getLength() > 0) {
                        for(int l = 0 ; l < nl.getLength(); l++) {
                            Element el = (Element)nl.item(l);
                            pendingChange pc = new pendingChange();
                            pc.create(el);
                            pendingChanges.add(pc);
                        }
                    }
                }
                if (pendingChanges.size() == 0){
                    return;
                }
                List<String> offlineFriends = new ArrayList<String>();
                int pindex = pendingChanges.size() - 1;
                while(pindex >= 0){
                    pendingChange oldest = new pendingChange(pendingChanges.get(pindex));
                    timelineEntry entry = new timelineEntry(oldest.getEntry());
                    List<String> sendTo = new ArrayList();
                    if (oldest.getType().equals("link")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.l.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.l.getBelongsTo());
                    }else if (oldest.getType().equals("wallPost")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.s.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.s.getBelongsTo());
                    }else if (oldest.getType().equals("photo")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.p.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.p.getBelongsTo());
                    }else if (oldest.getType().equals("photoAlbum")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.pa.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.pa.getBelongsTo());
                    }else if (oldest.getType().equals("video")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.v.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.v.getBelongsTo());
                    }else if (oldest.getType().equals("videoAlbum")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.va.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.va.getBelongsTo());
                    }else if (oldest.getType().equals("audio")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.a.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.a.getBelongsTo());
                    }else if (oldest.getType().equals("audioAlbum")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.aa.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.aa.getBelongsTo());
                    }else if (oldest.getType().equals("linkComment")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.l.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.l.getBelongsTo());
                    }else if (oldest.getType().equals("wallPostComment")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.s.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.s.getBelongsTo());
                    }else if (oldest.getType().equals("audioAlbumComment")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.aa.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.aa.getBelongsTo());
                    }else if (oldest.getType().equals("videoAlbumComment")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.va.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.va.getBelongsTo());
                    }else if (oldest.getType().equals("photoAlbumComment")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.pa.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.pa.getBelongsTo());
                    }else if (oldest.getType().equals("audioComment")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.a.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.a.getBelongsTo());
                    }else if (oldest.getType().equals("videoComment")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.v.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.v.getBelongsTo());
                    }else if (oldest.getType().equals("photoComment")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.p.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.p.getBelongsTo());
                    }else if (oldest.getType().equals("linkLike")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.l.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.l.getBelongsTo());
                    }else if (oldest.getType().equals("wallPostLike")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.s.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.s.getBelongsTo());
                    }else if (oldest.getType().equals("audioAlbumLike")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.aa.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.aa.getBelongsTo());
                    }else if (oldest.getType().equals("videoAlbumLike")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.va.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.va.getBelongsTo());
                    }else if (oldest.getType().equals("photoAlbumLike")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.pa.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.pa.getBelongsTo());
                    }else if (oldest.getType().equals("audioLike")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.a.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.a.getBelongsTo());
                    }else if (oldest.getType().equals("videoLike")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.v.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.v.getBelongsTo());
                    }else if (oldest.getType().equals("photoLike")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.p.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.p.getBelongsTo());
                    }else if (oldest.getType().equals("linkDislike")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.l.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.l.getBelongsTo());
                    }else if (oldest.getType().equals("wallPostDislike")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.s.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.s.getBelongsTo());
                    }else if (oldest.getType().equals("audioAlbumDislike")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.aa.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.aa.getBelongsTo());
                    }else if (oldest.getType().equals("videoAlbumDislike")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.va.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.va.getBelongsTo());
                    }else if (oldest.getType().equals("photoAlbumDislike")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.pa.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.pa.getBelongsTo());
                    }else if (oldest.getType().equals("audioDislike")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.a.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.a.getBelongsTo());
                    }else if (oldest.getType().equals("videoDislike")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.v.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.v.getBelongsTo());
                    }else if (oldest.getType().equals("photoDislike")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.p.getBelongsTo().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.p.getBelongsTo());
                    }else if (oldest.getType().equals("event")){
                        if (entry.e.getCreator().getUsername().equals(postingThreadSettings.username)){
                            List<user> pending_users = entry.e.getPendingNotification();
                            for (i = 0; i < pending_users.size(); i++){
                                user u = new user(pending_users.get(i));
                                sendTo.add(u.getUsername());
                            }
                        }else{
                            int k = 0;
                            for (k = 0; k < offlineFriends.size(); k++){
                                if (entry.e.getCreator().getUsername().equals(offlineFriends.get(k))){
                                    pindex--;
                                    break;
                                }
                            }
                            if (k < offlineFriends.size()){
                                continue;
                            }
                            sendTo.add(entry.e.getCreator().getUsername());
                        }
                    }else if (oldest.getType().equals("message")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.m.getReceiver().getUsername().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.m.getReceiver().getUsername());
                    }else if (oldest.getType().equals("mirror")){
                        int k = 0;
                        for (k = 0; k < offlineFriends.size(); k++){
                            if (entry.mi.getUsername().equals(offlineFriends.get(k))){
                                pindex--;
                                break;
                            }
                        }
                        if (k < offlineFriends.size()){
                            continue;
                        }
                        sendTo.add(entry.mi.getUsername());
                    }
                    for (int k = 0; k < sendTo.size(); k++){
                        String belongsTo = new String(sendTo.get(k));
                        if (DEBUG){
                            System.out.println("DEBUG: line 2985 of clientThread.java. post should be sent to " + belongsTo);
                        }
                        if (!postingThreadSettings.isFriend(belongsTo)){
                            List<Long> indices = new ArrayList<Long>();
                            updatedPendingChanges.clear();
                            readIn = null;
                            while((readIn = readXML(path + "/pendingChanges.xml")) == null){
                                Thread.sleep(100);
                            }
                            if (readIn.length > 0){
                                dom = db.parse(new ByteArrayInputStream(readIn));
                                Element docEle = dom.getDocumentElement();
                                NodeList nl = docEle.getElementsByTagName("pendingChange");
                                if(nl != null && nl.getLength() > 0) {
                                    for(int l = 0 ; l < nl.getLength(); l++) {
                                        Element el = (Element)nl.item(l);
                                        pendingChange pc = new pendingChange();
                                        pc.create(el);
                                        updatedPendingChanges.add(pc);
                                    }
                                }
                            }
                            indices.addAll(indicesOfSimilarPosts(pendingChanges, belongsTo));
                            for (int j = 0; j < indices.size(); j++){
                                long id = indices.get(j);
                                for (int l = 0; l < updatedPendingChanges.size(); l++){
                                    if (updatedPendingChanges.get(l).getId() == id){
                                        updatedPendingChanges.remove(l);
                                        pindex--;
                                    }
                                }
                            }
                            dom = db.newDocument();
                            
                            Element rootEle = dom.createElement("pendingChanges");
                            dom.appendChild(rootEle);
                            
                            for (int j = 0; j < updatedPendingChanges.size(); j++){
                                pendingChange p = new pendingChange(updatedPendingChanges.get(j));
                                rootEle.appendChild(p.createDOMElement(dom));
                            }
                            profile userProfile = new profile(postingThreadSettings.username, postingThreadSettings.username, prefix);
                            while (!userProfile.saveXML(path + "/pendingChanges.xml", dom)){
                                Thread.sleep(100);
                            }
                            continue;
                        }
                        for (i = 0; i < postingThreadSettings.friends.size(); i++){
                            f = new friend(postingThreadSettings.friends.get(i));
                            if (f.getUser().getUsername().equals(belongsTo)){
                                if (f.getStatus().equals("connected")){
                                    break;
                                }
                                List<String> mirrors = f.getMirrors();
                                conn = connectToFriend(f);
                                int index = -1;
                                boolean stillFriend = false;
                                if (conn != null){
                                    stillFriend = true;
                                }
                                friend dummy = null;
                                while(conn == null && index < (mirrors.size() + 3))
                                {
                                    index++;
                                    friend tmp = null;
                                    if (index < 3){
                                        tmp = locatePeer(f, index, false);
                                    }else{
                                        tmp = locatePeer(f, index, true);
                                    }
                                    if (tmp != null){
                                        stillFriend = true;
                                        f = new friend(tmp);
                                        if (index > 2){ // connecting to a mirror
                                            dummy = new friend(f);
                                            user dummyUsr = new user(f.getMirrors().get(index - 3), null, null, 0);
                                            dummy.setUser(dummyUsr);
                                            conn = connectToFriend(dummy);
                                        }else{
                                            conn = connectToFriend(f);
                                        }
                                    }
                                }
                                long logEntryId = 0;
                                if (conn != null){
                                    byte[] usernameHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    String usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                    byte[] machineHashBytes = null;
                                    String machineHash = "";
                                    if (index < 3){
                                        machineHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                        machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                    }else{
                                        machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                        machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                    }
                                    logEntryId = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
                                    appendLog(String.valueOf(logEntryId), "post", usernameHash, machineHash, "0", "connection established", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                                }else{
                                    byte[] usernameHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    String usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                    byte[] machineHashBytes = null;
                                    String machineHash = "";
                                    machineHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                    logEntryId = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
                                    appendLog(String.valueOf(logEntryId), "post", usernameHash, machineHash, "0", "connection not established", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                                }
                                // need to delete all posts for that friend too
                                if (!stillFriend){
                                    postingThreadSettings.deleteFriend(f.getUser().getUsername());
                                    List<Long> indices = new ArrayList<Long>();
                                    updatedPendingChanges.clear();
                                    readIn = null;
                                    while((readIn = readXML(path + "/pendingChanges.xml")) == null){
                                        Thread.sleep(100);
                                    }
                                    if (readIn.length > 0){
                                        dom = db.parse(new ByteArrayInputStream(readIn));
                                        Element docEle = dom.getDocumentElement();
                                        NodeList nl = docEle.getElementsByTagName("pendingChange");
                                        if(nl != null && nl.getLength() > 0) {
                                            for(int l = 0 ; l < nl.getLength(); l++) {
                                                Element el = (Element)nl.item(l);
                                                pendingChange pc = new pendingChange();
                                                pc.create(el);
                                                updatedPendingChanges.add(pc);
                                            }
                                        }
                                    }
                                    indices.addAll(indicesOfSimilarPosts(pendingChanges, belongsTo));
                                    for (int j = 0; j < indices.size(); j++){
                                        long id = indices.get(j);
                                        for (int l = 0; l < updatedPendingChanges.size(); l++){
                                            if (updatedPendingChanges.get(l).getId() == id){
                                                updatedPendingChanges.remove(l);
                                                pindex--;
                                            }
                                        }
                                    }
                                    dom = db.newDocument();
                                    Element rootEle = dom.createElement("pendingChanges");
                                    dom.appendChild(rootEle);
                                    
                                    for (int j = 0; j < updatedPendingChanges.size(); j++){
                                        pendingChange p = new pendingChange(updatedPendingChanges.get(j));
                                        rootEle.appendChild(p.createDOMElement(dom));
                                    }
                                    profile userProfile = new profile(postingThreadSettings.username, postingThreadSettings.username, prefix);
                                    while (!userProfile.saveXML(path + "/pendingChanges.xml", dom)){
                                        Thread.sleep(100);
                                    }
                                    break;
                                }else{
                                    if (postingThreadSettings.isMirroringFor(f.getUser().getUsername()) > 0 && postingThreadSettings.isMirroringFor(f.getUser().getUsername()) < f.getLastUpdateTime()){
                                        int l = 0;
                                        mirrors = f.getMirrors();
                                        for (l = 0; l < mirrors.size(); l++){
                                            String fm = new String(mirrors.get(l));
                                            if (fm.equals(postingThreadSettings.username)) // still mirroring
                                                break;
                                        }
                                        if (l == f.getMirrors().size()) // the other user deleted me as their mirror
                                        {
                                            if (DEBUG){
                                                System.out.println("DEBUG: line 3150 of clientThread.java. DELETING ORIGINAL --------------->>>>>>");
                                            }
                                            postingThreadSettings.deleteOriginal(f.getUser().getUsername());
                                        }
                                    }
                                }
                                if (conn == null){
                                    offlineFriends.add(new String(belongsTo));
                                    if (DEBUG){
                                        System.out.println("DEBUG: line 3159 of clientThread.java. adding offline friends " + belongsTo);
                                    }
                                    break;
                                }
                                if (DEBUG){
                                    System.out.println("DEBUG: line 3164 of clientThread.java. successfuly established a connection to : " + f.getUser().getUsername() + " from posting thread.");
                                }
                                f.setStatus("connected");
                                System.out.println("Sending your posts to " + f.getUser().getUsername() + ". please do not disconnect!!!");
                                disconnectAtTheEnd = true;
                                postingThreadSettings.updateFriend(f);
                                dom = db.newDocument();
                                Element rootEle = dom.createElement("pendingChanges");
                                dom.appendChild(rootEle);
                                List<Long> indices = new ArrayList<Long>();
                                rootEle = appendSimilarPosts(dom, rootEle, pendingChanges, belongsTo);
                                indices.addAll(indicesOfSimilarPosts(pendingChanges, belongsTo));
                                OutputFormat format = new OutputFormat(dom);
                                format.setIndenting(true);
                                baos = new ByteArrayOutputStream();
                                XMLSerializer serializer = new XMLSerializer(baos, format);
                                serializer.serialize(dom);
                                byte[] toBeSend = new String("POST|" + f.getUser().getUsername() + "|" + f.getLatestVersion() + "|").getBytes("UTF-8");
                                conn.send(toBeSend);
                                byte[] reply = conn.receive();
                                String s = new String(reply, "UTF-8");
                                if (s.indexOf("SEND_XML") != 0){
                                    if (DEBUG){
                                        System.out.println("DEBUG: line 3187 of clientThread.java. s = " + s);
                                    }
                                    baos.close();
                                    break;
                                }
                                toBeSend = baos.toByteArray();
                                byte[] usernameHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                String usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                byte[] machineHashBytes = null;
                                String machineHash = "";
                                if (index < 3){
                                    machineHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                }else{
                                    machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                }
                                appendLog(String.valueOf(logEntryId), "post", usernameHash, machineHash, String.valueOf(toBeSend.length), "sending file", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                                
                                conn.send(toBeSend);
                                baos.close();
                                
                                usernameHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                machineHashBytes = null;
                                machineHash = "";
                                if (index < 3){
                                    machineHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                }else{
                                    machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                }
                                appendLog(String.valueOf(logEntryId), "post", usernameHash, machineHash, String.valueOf(toBeSend.length), "sending file completed", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                                profile afterMergeProfile = new profile(f.getUser().getUsername(), postingThreadSettings.username, true, prefix);
                                reply = conn.receive();
                                s = new String(reply, "UTF-8");
                                while (s.indexOf("SEND_FILE") == 0){
                                    // SEND_FILE|FILENAME|
                                    String filename = s.substring(s.indexOf("|") + 1, s.lastIndexOf("|"));
                                    
                                    File toSendfile = new File(prefix + filename);
                                    if (!toSendfile.exists())
                                    {
                                        s = "NOT_FOUND";
                                        conn.send(s.getBytes("UTF-8"));
                                    }else{
                                        s = String.valueOf(toSendfile.length()) + "|";
                                        conn.send(s.getBytes("UTF-8"));
                                        reply = conn.receive();
                                        s = new String(reply, "UTF-8");
                                        if (s.indexOf("READY") != 0){ // optional feature to opt out of receiving huge files.
                                            reply = conn.receive();
                                            s = new String(reply, "UTF-8");
                                            continue;
                                        }
                                        fis = new FileInputStream(toSendfile);
                                        byte[] buf = new byte[100000];
                                        int bytesRead = 0;
                                        byte[] tosend;
                                        byte[] ackBytes;
                                        String ack;
                                        int retries = 0;
                                        usernameHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                        usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                        machineHashBytes = null;
                                        machineHash = "";
                                        if (index < 3){
                                            machineHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                            machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                        }else{
                                            machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                            machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                        }
                                        appendLog(String.valueOf(logEntryId), "post", usernameHash, machineHash, String.valueOf(toSendfile.length()), "sending file", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                                        while (-1 != (bytesRead = fis.read(buf, 0, buf.length)) && retries < 20){
                                            tosend = new byte[bytesRead];
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
                                        usernameHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                        usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                        machineHashBytes = null;
                                        machineHash = "";
                                        if (index < 3){
                                            machineHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                            machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                        }else{
                                            machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                            machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                        }
                                        appendLog(String.valueOf(logEntryId), "post", usernameHash, machineHash, String.valueOf(toSendfile.length()), "sending file completed", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                                    }
                                    reply = conn.receive();
                                    s = new String(reply, "UTF-8");
                                }
                                usernameHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                machineHashBytes = null;
                                machineHash = "";
                                if (index < 3){
                                    machineHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                }else{
                                    machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                }
                                appendLog(String.valueOf(logEntryId), "post", usernameHash, machineHash, "0", "all posts sent", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                                if (s.indexOf("DONE") != 0){
                                    if (DEBUG){
                                        System.out.println("DEBUG: line 3307 of clientThread.java. s = " + s);
                                    }
                                    break;
                                }
                                updatedPendingChanges.clear();
                                readIn = null;
                                while((readIn = readXML(path + "/pendingChanges.xml")) == null){
                                    Thread.sleep(100);
                                }
                                if (readIn.length > 0){
                                    dom = db.parse(new ByteArrayInputStream(readIn));
                                    Element docEle = dom.getDocumentElement();
                                    NodeList nl = docEle.getElementsByTagName("pendingChange");
                                    if(nl != null && nl.getLength() > 0) {
                                        for(int l = 0 ; l < nl.getLength(); l++) {
                                            Element el = (Element)nl.item(l);
                                            pendingChange pc = new pendingChange();
                                            pc.create(el);
                                            updatedPendingChanges.add(pc);
                                        }
                                    }
                                }
                                for (int j = 0; j < indices.size(); j++){
                                    long id = indices.get(j);
                                    for (int l = 0; l < updatedPendingChanges.size(); l++){
                                        if (updatedPendingChanges.get(l).getId() == id){
                                            updatedPendingChanges.remove(l);
                                            pindex--;
                                        }
                                    }
                                }
                                dom = db.newDocument();
                                rootEle = dom.createElement("pendingChanges");
                                dom.appendChild(rootEle);
                                for (int j = 0; j < updatedPendingChanges.size(); j++){
                                    pendingChange p = new pendingChange(updatedPendingChanges.get(j));
                                    rootEle.appendChild(p.createDOMElement(dom));
                                }
                                profile userProfile = new profile(postingThreadSettings.username, postingThreadSettings.username, prefix);
                                while (!userProfile.saveXML(path + "/pendingChanges.xml", dom)){
                                    Thread.sleep(100);
                                }
                                usernameHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                machineHashBytes = null;
                                machineHash = "";
                                if (index < 3){
                                    machineHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                }else{
                                    machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                }
                                appendLog(String.valueOf(logEntryId), "post", usernameHash, machineHash, "0", "receiving updates", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                                // receiving updates
                                s = "SEND_UPDATES";
                                byte[] response = s.getBytes("UTF-8");
                                conn.send(response);
                                response = conn.receive();
                                s = new String(response, "UTF-8");
                                if (s.indexOf("GET_READY_FOR_UPDATES") != 0){
                                    if (DEBUG){
                                        System.out.println("DEBUG: line 3369 of clientThread.java. s = " + s);
                                    }
                                    break;
                                }
                                s = "READY";
                                long now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
                                conn.send(s.getBytes("UTF-8"));
                                response = conn.receive();
                                s = new String(response, "UTF-8");
                                // SENDING_FILE|FILENAME|FILE_LENGTH|
                                while(s.indexOf("DONE") != 0){
                                    if (s.indexOf("SENDING_FILE") != 0){
                                        if (DEBUG){
                                            System.out.println("DEBUG: line 3382 of clientThread.java. s = " + s);
                                        }
                                        response = conn.receive();
                                        s = new String(response, "UTF-8");
                                        break;
                                    }
                                    long fileLength = 0;
                                    String filename = "";
                                    if (s.indexOf("|") < s.indexOf("|", s.indexOf("|") + 1) && s.indexOf("|", s.indexOf("|") + 1) < s.lastIndexOf("|")){
                                        filename = s.substring(s.indexOf("|") + 1, s.indexOf("|", s.indexOf("|") + 1));
                                        fileLength = Long.parseLong(s.substring(s.indexOf("|", s.indexOf("|") + 1) + 1, s.lastIndexOf("|")));
                                    }else{
                                        if (DEBUG){
                                            System.out.println("DEBUG: line 3395 of clientThread.java. s = " + s);
                                        }
                                        break;
                                    }
                                    
                                    if (filename.contains("thumbnails")){
                                        filename = prefix + postingThreadSettings.username + "/thumbnails/" + filename.substring(filename.lastIndexOf("/") + 1, filename.length());
                                        if (filename.contains(f.getUser().getUsername() + ".jpg")){
                                            userProfile.updateModifiedFiles("add", Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "./MyZone/" + postingThreadSettings.username + "/thumbnails/" + filename.substring(filename.lastIndexOf("/") + 1, filename.length()), "public");
                                        }
                                    }else{
                                        filename = prefix + postingThreadSettings.username + "/friends/" + filename;
                                    };
                                    
                                    if (filename.contains("wall")){
                                        filename = prefix + postingThreadSettings.username + filename.substring(filename.lastIndexOf("/friends/"), filename.indexOf("tmp/")) + "wall/wall.xml"; 
                                        // creating wall file.
                                    }
                                    // ************
                                    File toBeReceived = new File(filename);
                                    if (toBeReceived.exists()){
                                        if (!filename.contains(".xml") && toBeReceived.length() == fileLength){
                                            response = new String("SKIP").getBytes("UTF-8");
                                            conn.send(response);
                                            response = conn.receive();
                                            s = new String(response, "UTF-8");
                                            continue;
                                        }else{
                                            if (filename.contains(".xml")){
                                                fos = new FileOutputStream(toBeReceived);
                                                toBeReceivedChannel = fos.getChannel();
                                                toBeReceivedLock = null;
                                                while (toBeReceivedLock == null){
                                                    try{
                                                        toBeReceivedLock = toBeReceivedChannel.tryLock();
                                                    }catch(Exception e){
                                                        if (DEBUG){
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                    Thread.yield();
                                                }
                                            }
                                            int retries = 0;
                                            response = new String("READY").getBytes("UTF-8");
                                            conn.send(response);
                                            usernameHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                            usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                            machineHashBytes = null;
                                            machineHash = "";
                                            if (index < 3){
                                                machineHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                                machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                            }else{
                                                machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                                machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                            }
                                            appendLog(String.valueOf(logEntryId), "post", usernameHash, machineHash, String.valueOf(fileLength), "receiving update file", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                                            if (filename.contains(".xml")){
                                                while (fileLength > 0){
                                                    byte[] b = conn.receive();
                                                    ByteBuffer buf = ByteBuffer.wrap(b);
                                                    toBeReceivedChannel.write(buf);
                                                    fileLength -= b.length;
                                                    if (b.length > 0){
                                                        retries = 0;
                                                        response = new String("ACK").getBytes("UTF-8");
                                                        conn.send(response);
                                                    }else{
                                                        if (retries == 20){
                                                            if (DEBUG){
                                                                System.out.println("DEBUG: line 3466 of clientThread.java");
                                                            }
                                                            toBeReceivedLock.release();
                                                            toBeReceivedChannel.close();
                                                            fos.flush();
                                                            fos.close();
                                                            break;
                                                        }
                                                        response = new String("NACK").getBytes("UTF-8");
                                                        conn.send(response);
                                                        retries++;
                                                    }
                                                }
                                                toBeReceivedLock.release();
                                                toBeReceivedChannel.close();
                                                fos.flush();
                                                fos.close();
                                            }else{
                                                fos = new FileOutputStream(toBeReceived);
                                                while (fileLength > 0){
                                                    byte[] buf = conn.receive();
                                                    fos.write(buf, 0, buf.length);
                                                    fileLength -= buf.length;
                                                    if (buf.length > 0){
                                                        retries = 0;
                                                        response = new String("ACK").getBytes("UTF-8");
                                                        conn.send(response);
                                                    }else{
                                                        if (retries == 20){
                                                            if (DEBUG){
                                                                System.out.println("DEBUG: line 3496 of clientThread.java");
                                                            }
                                                            fos.flush();
                                                            fos.close();
                                                            break;
                                                        }
                                                        response = new String("NACK").getBytes("UTF-8");
                                                        conn.send(response);
                                                        retries++;
                                                    }
                                                }
                                                fos.flush();
                                                fos.close();
                                            }
                                            usernameHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                            usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                            machineHashBytes = null;
                                            machineHash = "";
                                            if (index < 3){
                                                machineHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                                machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                            }else{
                                                machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                                machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                            }
                                            appendLog(String.valueOf(logEntryId), "post", usernameHash, machineHash, String.valueOf(toBeReceived.length()), "update file received", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                                        }
                                    }else{
                                        int retries = 0;
                                        fos = new FileOutputStream(toBeReceived);
                                        response = new String("READY").getBytes("UTF-8");
                                        conn.send(response);
                                        String dirPath = filename.replaceFirst(prefix, "./MyZone/");
                                        dirPath = dirPath.substring(0, dirPath.lastIndexOf("/") + 1);
                                        fileListEntry fle = new fileListEntry(belongsTo, "N/A", dirPath, filename.substring(filename.lastIndexOf("/") + 1, filename.length()), fileLength);
                                        usernameHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                        usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                        machineHashBytes = null;
                                        machineHash = "";
                                        if (index < 3){
                                            machineHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                            machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                        }else{
                                            machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                            machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                        }
                                        appendLog(String.valueOf(logEntryId), "post", usernameHash, machineHash, String.valueOf(fileLength), "receiving update file", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                                        while (fileLength > 0){
                                            byte[] buf = conn.receive();
                                            fos.write(buf, 0, buf.length);
                                            fileLength -= buf.length;
                                            if (buf.length > 0){
                                                retries = 0;
                                                response = new String("ACK").getBytes("UTF-8");
                                                conn.send(response);
                                            }else{
                                                if (retries == 20){
                                                    if (DEBUG){
                                                        System.out.println("DEBUG: line 3554 of clientThread.java");
                                                    }
                                                    fos.flush();
                                                    fos.close();
                                                    break;
                                                }
                                                response = new String("NACK").getBytes("UTF-8");
                                                conn.send(response);
                                                retries++;
                                            }
                                        }
                                        fos.flush();
                                        fos.close();
                                        usernameHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                        usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                        machineHashBytes = null;
                                        machineHash = "";
                                        if (index < 3){
                                            machineHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                            machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                        }else{
                                            machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                            machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                        }
                                        appendLog(String.valueOf(logEntryId), "post", usernameHash, machineHash, String.valueOf(toBeReceived.length()), "update file received", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                                        if (!filename.contains(".xml")){
                                            userProfile.updateImage("correctImage", "add", fle);
                                            userProfile.updateImage("existingImage", "add", fle);
                                        }
                                    }
                                    if (filename.contains("wall")){
                                        if (DEBUG){
                                            System.out.println("DEBUG: line 3586 of clientThread.java. *********  calling removeUncommittedChanges from sendUpdates");
                                        }
                                        afterMergeProfile.removeUncommittedChanges(f.getLatestVersion());
                                        profile beforeMergeProfile = new profile(f.getUser().getUsername(), postingThreadSettings.username, true, prefix);
                                        beforeMergeProfile.loadWall(filename);
                                        for (int n = beforeMergeProfile.wall.size() - 1; n >= 0; n--){
                                            timelineEntry tle = new timelineEntry(beforeMergeProfile.wall.get(n));
                                            if (tle.type.equals("audio")){
                                                afterMergeProfile.addAudio(tle.a, true);
                                            }else if (tle.type.equals("video")){
                                                afterMergeProfile.addVideo(tle.v, true);
                                            }else if (tle.type.equals("photo")){
                                                afterMergeProfile.addPhoto(tle.p, true);
                                            }else if (tle.type.equals("audioAlbum")){
                                                afterMergeProfile.addAudioAlbum(tle.aa, true);
                                            }else if (tle.type.equals("photoAlbum")){
                                                afterMergeProfile.addPhotoAlbum(tle.pa, true);
                                            }else if (tle.type.equals("videoAlbum")){
                                                afterMergeProfile.addVideoAlbum(tle.va, true);
                                            }else if (tle.type.equals("link")){
                                                afterMergeProfile.addLink(tle.l, true);
                                            }else if (tle.type.equals("wallPost")){
                                                afterMergeProfile.addWallPost(tle.s, true);
                                            }else if (tle.type.equals("deletedEntry")){
                                                if (tle.de.getType().equals("link")){
                                                    afterMergeProfile.removeLink(tle.de.getId(), tle.de.getShareWith(), tle.de.getPostedBy(), true);
                                                }else if (tle.de.getType().equals("wallPost")){
                                                    afterMergeProfile.removewallPost(tle.de.getId(), tle.de.getShareWith(), tle.de.getPostedBy(), true);
                                                }else if (tle.de.getType().equals("audioAlbum")){
                                                    afterMergeProfile.removeAudioAlbum(tle.de.getId(), tle.de.getShareWith(), tle.de.getPostedBy(), true);
                                                }else if (tle.de.getType().equals("videoAlbum")){
                                                    afterMergeProfile.removeVideoAlbum(tle.de.getId(), tle.de.getShareWith(), tle.de.getPostedBy(), true);
                                                }else if (tle.de.getType().equals("photoAlbum")){
                                                    afterMergeProfile.removePhotoAlbum(tle.de.getId(), tle.de.getShareWith(), tle.de.getPostedBy(), true);
                                                }else if (tle.de.getType().equals("audio")){
                                                    afterMergeProfile.removeAudio(tle.de.getParent(), tle.de.getId(), tle.de.getShareWith(), tle.de.getPostedBy(), true);
                                                }else if (tle.de.getType().equals("video")){
                                                    afterMergeProfile.removeVideo(tle.de.getParent(), tle.de.getId(), tle.de.getShareWith(), tle.de.getPostedBy(), true);
                                                }else if (tle.de.getType().equals("photo")){
                                                    afterMergeProfile.removePhoto(tle.de.getParent(), tle.de.getId(), tle.de.getShareWith(), tle.de.getPostedBy(), true);
                                                }
                                                
                                            }
                                            
                                        }
                                        toBeReceived.delete();
                                    }else if (filename.contains("info.xml")){
                                        profile usrProfile = new profile(f.getUser().getUsername(), postingThreadSettings.username, prefix);
                                        usrProfile.loadUserInfo();
                                        user u = new user(f.getUser().getUsername(), usrProfile.userInfo.getFirstName(), usrProfile.userInfo.getLastName(), 0);
                                        f.setUser(u);
                                        postingThreadSettings.updateFriend(f);
                                    }
                                    response = conn.receive();
                                    s = new String(response, "UTF-8");
                                }
                                if (DEBUG){
                                    System.out.println("DEBUG: line 3643 of clientThread.java. $$$$$$$$$ setting latestversion here from sendUpdates $$$$$$$$$ for " + f.getUser().getUsername());
                                }
                                f.setLatestVersion(now);
                                f.setStatus("disconnected");
                                postingThreadSettings.updateFriend(f);
                                conn.close();
                                System.out.println("Your posts has been sent to " + f.getUser().getUsername() + ". you can safely disconnect now ...");
                                disconnectAtTheEnd = false;
                                usernameHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                machineHashBytes = null;
                                machineHash = "";
                                if (index < 3){
                                    machineHashBytes = hashMessage(f.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                }else{
                                    machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                }
                                appendLog(String.valueOf(logEntryId), "post", usernameHash, machineHash, "0", "successfully sent posts", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                            }
                        }
                        if (DEBUG){
                            System.out.println("DEBUG: line 3666 of clientThread.java. value of pindex at the end of loop " + pindex);
                        }
                    }
                    pindex--;
                }
            }catch(Exception e){
                if (DEBUG){
                    e.printStackTrace();
                }
            }finally{
                try{
                    if (f != null && postingThreadSettings.friends.size() > i && disconnectAtTheEnd){
                        f.setStatus("disconnected");
                        postingThreadSettings.updateFriend(f);
                        System.out.println("Your posts were not sent to " + f.getUser().getUsername() + " successfully and MyZoneEngine will try again later but for now you can safely disconnect ...");
                    }
                    if (baos != null)
                        baos.close();
                    if (toBeReceivedLock != null){
                        if (toBeReceivedChannel.isOpen()){
                            toBeReceivedLock.release();
                            toBeReceivedChannel.close();
                        }
                    }
                    if (toBeReceivedChannel != null){
                        toBeReceivedChannel.close();
                    }
                    if (fis != null)
                        fis.close();
                    if (fos != null)
                        fos.close();
                    if (conn != null){
                        if (!conn.isClosed()){
                            conn.close();
                        }
                    }
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    class updatingThread extends Thread{
        String prefix;
        zone z;
        List<String> runningThreads;
        Settings updatingThreadSettings;
        updatingThread(String prefix, zone z){
            this.prefix = prefix;
            this.z = z;
            updatingThreadSettings = new Settings(prefix);
        }
        
        public void run(){
            
            while(true){
                
                runningThreads = new ArrayList<String>();
                updatingThreadSettings.refresh(updatingThreadSettings.ALL);
                int i = 0;
                for (i = 0; i < updatingThreadSettings.zones.size(); i++){
                    zone tmp = new zone(updatingThreadSettings.zones.get(i));
                    if (tmp.getName().equals(z.getName())){
                        z = new zone(tmp);
                        break;
                    }
                }
                if (i == updatingThreadSettings.zones.size()){
                    return; // this zone has been deleted since this thread was called.
                }
                
                // update this zone by locating each member and contacting it and then receiving updates if necessary the last update time should be updated at the end. need a single thread for each client up to 10 threads at a time.
                List<user> members = z.getMembers();
                for (i = 0; i < members.size(); i++){
                    user u = new user(members.get(i));
                    int j = 0;
                    friend f = new friend();
                    for (j = 0; j < updatingThreadSettings.friends.size(); j++){
                        f = new friend(updatingThreadSettings.friends.get(j));
                        if (f.getUser().getUsername().equals(u.getUsername()))
                            break;
                    }
                    if (j == updatingThreadSettings.friends.size())
                        break;
                    while (runningThreads.size() >= 10){
                        if (DEBUG){
                            try{
                                Thread.sleep(100);
                            }catch(Exception e){
                                if (DEBUG){
                                    e.printStackTrace();
                                }
                            }
                        } // wait until threads free up.
                    }
                    synchronized(runningThreads){
                        for (j = 0; j < runningThreads.size(); j++){
                            String name = new String(runningThreads.get(j));
                            if (name.equals(f.getUser().getUsername())){
                                break;
                            }
                        }
                        if (j == runningThreads.size()){
                            runningThreads.add(f.getUser().getUsername());
                            new getUpdate(prefix, f).start();
                        }
                    }
                }
                long refreshInterval = z.getRefreshInterval() * 1000;
                if (refreshInterval < 900000)
                    refreshInterval = 900000;
                try{
                    Thread.sleep(refreshInterval);
                }catch(java.lang.InterruptedException e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                    continue;
                }
            }
            
        }
        
        class getUpdate extends Thread{
            public String prefix;
            public friend myFriend;
            public Settings getUpdateSettings;
            
            getUpdate(String prefix, friend f){
                this.prefix = prefix;
                this.myFriend = new friend(f);
                getUpdateSettings = new Settings(prefix);
                getUpdateSettings.refresh(getUpdateSettings.BASIC_INFO);
            }
            
            public void run(){
                FileChannel channel = null;
                FileLock lock = null;
                FileOutputStream fos = null;
                SecureSocket conn = null;
                int i = 0; 
                boolean disconnectAtTheEnd = false;
                try{
                    if (DEBUG){
                        System.out.println("DEBUG: line 3813 of clientThread.java. ^^^^^^^^^^ in get updates ^^^^^^^^^ ");
                    }
                    if (myFriend.getStatus() != null){
                        if (myFriend.getStatus().equals("connected")){
                            return;
                        }
                    }
                    getUpdateSettings.refresh(getUpdateSettings.ALL);
                    List<String> mirrors = myFriend.getMirrors();
                    conn = connectToFriend(myFriend);
                    int index = 0;
                    boolean stillFriend = false;
                    if (conn != null){
                        stillFriend = true;
                    }
                    friend dummy = null;
                    while(conn == null && index < (mirrors.size() + 3))
                    {
                        friend tmp = null;
                        if (index < 3){
                            tmp = locatePeer(myFriend, index, false);
                        }else{
                            tmp = locatePeer(myFriend, index, true);
                        }
                        if (tmp != null){
                            stillFriend = true;
                            myFriend = new friend(tmp);
                            if (index > 2){ // connecting to a mirror
                                dummy = new friend(myFriend);
                                user dummyUsr = new user(myFriend.getMirrors().get(index - 3), null, null, 0);
                                dummy.setUser(dummyUsr);
                                conn = connectToFriend(dummy);
                            }else{
                                conn = connectToFriend(myFriend);
                            }
                        }
                        index++;
                    }
                    long logEntryId = 0;
                    if (conn != null){
                        byte[] usernameHashBytes = hashMessage(myFriend.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                        String usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                        byte[] machineHashBytes = null;
                        String machineHash = "";
                        if (index < 3){
                            machineHashBytes = hashMessage(myFriend.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                            machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                        }else{
                            machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                            machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                        }
                        logEntryId = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
                        appendLog(String.valueOf(logEntryId), "get updates", usernameHash, machineHash, "0", "connection established", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                    }else{
                        byte[] usernameHashBytes = hashMessage(myFriend.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                        String usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                        byte[] machineHashBytes = null;
                        String machineHash = "";
                        machineHashBytes = hashMessage(myFriend.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                        machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                        logEntryId = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
                        appendLog(String.valueOf(logEntryId), "get updates", usernameHash, machineHash, "0", "connection not established", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                    }
                    if (DEBUG){
                        System.out.println("DEBUG: line 3877 of clientThread.java. value of stillFriend = " + stillFriend);
                    }
                    if (!stillFriend){
                        getUpdateSettings.deleteFriend(myFriend.getUser().getUsername());
                        if (DEBUG){
                            System.out.println("DEBUG: line 3882 of clientThread.java. about to delete friend");
                        }
                        return;
                    }else{
                        if (getUpdateSettings.isMirroringFor(myFriend.getUser().getUsername()) > 0 && getUpdateSettings.isMirroringFor(myFriend.getUser().getUsername()) < myFriend.getLastUpdateTime()){
                            int k = 0;
                            mirrors = myFriend.getMirrors();
                            for (k = 0; k < mirrors.size(); k++){
                                String fm = new String(mirrors.get(k));
                                if (fm.equals(getUpdateSettings.username)) // still mirroring
                                    break;
                            }
                            if (k == myFriend.getMirrors().size()) // the other user deleted me as their mirror
                            {
                                if (DEBUG){
                                    System.out.println("DEBUG: line 3897 of clientThread.java. DELETING ORIGINAL --------------->>>>>>");
                                }
                                getUpdateSettings.deleteOriginal(myFriend.getUser().getUsername());
                            }
                        }
                    }
                    if (conn == null){
                        return;
                    }
                    if (DEBUG){
                        System.out.println("DEBUG: line 3907 of clientThread.java. \\\\\\\\ successfully established a connection ///////////");
                    }
                    getUpdateSettings.refresh(getUpdateSettings.BASIC_INFO);
                    getUpdateSettings.refresh(getUpdateSettings.FRIENDS);
                    for (i = 0; i < getUpdateSettings.friends.size(); i++){
                        friend f = new friend(getUpdateSettings.friends.get(i));
                        if (f.getUser().getUsername().equals(myFriend.getUser().getUsername())){
                            break;
                        }
                    }
                    if (i == getUpdateSettings.friends.size()){
                        return;
                    }
                    myFriend.setStatus("connected");
                    disconnectAtTheEnd = true;
                    getUpdateSettings.updateFriend(myFriend);
                    profile afterMergeProfile = new profile(myFriend.getUser().getUsername(), getUpdateSettings.username, true, prefix);
                    String s = "GET|" + myFriend.getUser().getUsername() + "|" + String.valueOf(myFriend.getLatestVersion()) + "|";
                    byte[] response;
                    response = s.getBytes("UTF-8");
                    // GET|USERNAME|LATESTVERSION|
                    conn.send(response);
                    response = conn.receive();
                    s = new String(response, "UTF-8");
                    if (s.indexOf("GET_READY_FOR_UPDATES") == 0){
                        byte[] usernameHashBytes = hashMessage(myFriend.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                        String usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                        byte[] machineHashBytes = null;
                        String machineHash = "";
                        if (index < 3){
                            machineHashBytes = hashMessage(myFriend.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                            machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                        }else{
                            machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                            machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                        }
                        appendLog(String.valueOf(logEntryId), "get updates", usernameHash, machineHash, "0", "receiving updates", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                        s = "READY";
                        try{
                            response = s.getBytes("UTF-8");
                        }catch(java.io.UnsupportedEncodingException e){
                            if (DEBUG){
                                e.printStackTrace();
                            }
                            return;
                        }
                        conn.send(response);
                        long now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
                        response = conn.receive();
                        try{
                            s = new String(response, "UTF-8");
                        }catch(java.io.UnsupportedEncodingException e){
                            if (DEBUG){
                                e.printStackTrace();
                            }
                            return;
                        }
                        // SENDING_FILE|FILENAME|FILE_LENGTH|
                        while(s.indexOf("DONE") != 0){
                            if (s.indexOf("SENDING_FILE") != 0){
                                return;
                            }
                            long fileLength = 0;
                            String filename = "";
                            if (s.indexOf("|") < s.indexOf("|", s.indexOf("|") + 1) && s.indexOf("|", s.indexOf("|") + 1) < s.lastIndexOf("|")){
                                filename = s.substring(s.indexOf("|") + 1, s.indexOf("|", s.indexOf("|") + 1));
                                fileLength = Long.parseLong(s.substring(s.indexOf("|", s.indexOf("|") + 1) + 1, s.lastIndexOf("|")));
                            }else{
                                return;
                            }
                            
                            if (filename.contains("thumbnails")){
                                filename = prefix + getUpdateSettings.username + "/thumbnails/" + filename.substring(filename.lastIndexOf("/") + 1, filename.length());
                                if (filename.contains(myFriend.getUser().getUsername() + ".jpg")){
                                    profile userProfile = new profile(getUpdateSettings.username, getUpdateSettings.username, prefix);
                                    userProfile.updateModifiedFiles("add", Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), "./MyZone/" + getUpdateSettings.username + "/thumbnails/" + filename.substring(filename.lastIndexOf("/") + 1, filename.length()), "public");
                                }
                            }else{
                                filename = prefix + getUpdateSettings.username + "/friends/" + filename;
                            }
                            
                            if (filename.contains("wall")){
                                filename = prefix + getUpdateSettings.username + filename.substring(filename.lastIndexOf("/friends/"), filename.indexOf("tmp/")) + "wall/wall.xml"; 
                                // creating wall file.
                            }
                            
                            // ************
                            File file = new File(filename);
                            if (file.exists()){
                                if (!filename.contains(".xml") && file.length() == fileLength){
                                    response = new String("SKIP").getBytes("UTF-8");
                                    conn.send(response);
                                    response = conn.receive();
                                    s = new String(response, "UTF-8");
                                    continue;
                                }else{
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
                                        }
                                        Thread.yield();
                                    }
                                    int retries = 0;
                                    s = "READY";
                                    response = s.getBytes("UTF-8");
                                    conn.send(response);
                                    usernameHashBytes = hashMessage(myFriend.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                    machineHashBytes = null;
                                    machineHash = "";
                                    if (index < 3){
                                        machineHashBytes = hashMessage(myFriend.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                        machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                    }else{
                                        machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                        machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                    }
                                    appendLog(String.valueOf(logEntryId), "get updates", usernameHash, machineHash, String.valueOf(fileLength), "receiving file", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                                    while (fileLength > 0){
                                        byte[] b = conn.receive();
                                        ByteBuffer buf = ByteBuffer.wrap(b);
                                        channel.write(buf);
                                        fileLength -= b.length;
                                        if (b.length > 0){
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
                                    lock.release();
                                    channel.close();
                                    fos.flush();
                                    fos.close();
                                    usernameHashBytes = hashMessage(myFriend.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                    machineHashBytes = null;
                                    machineHash = "";
                                    if (index < 3){
                                        machineHashBytes = hashMessage(myFriend.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                        machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                    }else{
                                        machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                        machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                    }
                                    appendLog(String.valueOf(logEntryId), "get updates", usernameHash, machineHash, String.valueOf(file.length()), "file received", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                                }
                            }else{
                                int retries = 0;
                                fos = new FileOutputStream(file);
                                s = "READY";
                                response = s.getBytes("UTF-8");
                                conn.send(response);
                                profile userProfile = new profile(getUpdateSettings.username, getUpdateSettings.username, prefix);
                                String dirPath = filename.replaceFirst(prefix, "./MyZone/");
                                dirPath = dirPath.substring(0, dirPath.lastIndexOf("/") + 1);
                                fileListEntry fle = new fileListEntry(myFriend.getUser().getUsername(), "N/A", dirPath, filename.substring(filename.lastIndexOf("/") + 1, filename.length()), fileLength);
                                usernameHashBytes = hashMessage(myFriend.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                machineHashBytes = null;
                                machineHash = "";
                                if (index < 3){
                                    machineHashBytes = hashMessage(myFriend.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                }else{
                                    machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                }
                                appendLog(String.valueOf(logEntryId), "get updates", usernameHash, machineHash, String.valueOf(file.length()), "receiving file", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                                while (fileLength > 0){
                                    byte[] buf = conn.receive();
                                    fos.write(buf, 0, buf.length);
                                    fileLength -= buf.length;
                                    
                                    if (buf.length > 0){
                                        retries = 0;
                                        response = new String("ACK").getBytes("UTF-8");
                                        conn.send(response);
                                    }else{
                                        if (retries == 20){
                                            fos.flush();
                                            fos.close();
                                            break;
                                        }
                                        response = new String("NACK").getBytes("UTF-8");
                                        conn.send(response);
                                        retries++;
                                    }
                                }
                                fos.flush();
                                fos.close();
                                usernameHashBytes = hashMessage(myFriend.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                                machineHashBytes = null;
                                machineHash = "";
                                if (index < 3){
                                    machineHashBytes = hashMessage(myFriend.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                }else{
                                    machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                                    machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                                }
                                appendLog(String.valueOf(logEntryId), "get updates", usernameHash, machineHash, String.valueOf(file.length()), "file received", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                                if (!filename.contains(".xml")){
                                    userProfile.updateImage("correctImage", "add", fle);
                                    userProfile.updateImage("existingImage", "add", fle);
                                }
                            }
                            response = conn.receive();
                            s = new String(response, "UTF-8");
                            if (filename.contains("wall")){
                                if (DEBUG){
                                    System.out.println("DEBUG: line 4132 of clientThread.java. *********  calling removeUncommittedChanges from getUpdates");
                                }
                                afterMergeProfile.removeUncommittedChanges(myFriend.getLatestVersion());
                                profile beforeMergeProfile = new profile(myFriend.getUser().getUsername(), getUpdateSettings.username, true, prefix);
                                beforeMergeProfile.loadWall(filename);
                                for (int k = beforeMergeProfile.wall.size() - 1; k >= 0; k--){
                                    timelineEntry tle = new timelineEntry(beforeMergeProfile.wall.get(k));
                                    if (tle.type.equals("audio")){
                                        afterMergeProfile.addAudio(tle.a, true);
                                    }else if (tle.type.equals("video")){
                                        afterMergeProfile.addVideo(tle.v, true);
                                    }else if (tle.type.equals("photo")){
                                        afterMergeProfile.addPhoto(tle.p, true);
                                    }else if (tle.type.equals("audioAlbum")){
                                        afterMergeProfile.addAudioAlbum(tle.aa, true);
                                    }else if (tle.type.equals("photoAlbum")){
                                        afterMergeProfile.addPhotoAlbum(tle.pa, true);
                                    }else if (tle.type.equals("videoAlbum")){
                                        afterMergeProfile.addVideoAlbum(tle.va, true);
                                    }else if (tle.type.equals("link")){
                                        afterMergeProfile.addLink(tle.l, true);
                                    }else if (tle.type.equals("wallPost")){
                                        afterMergeProfile.addWallPost(tle.s, true);
                                    }else if (tle.type.equals("deletedEntry")){
                                        if (tle.de.getType().equals("link")){
                                            afterMergeProfile.removeLink(tle.de.getId(), tle.de.getShareWith(), tle.de.getPostedBy(), true);
                                        }else if (tle.de.getType().equals("wallPost")){
                                            afterMergeProfile.removewallPost(tle.de.getId(), tle.de.getShareWith(), tle.de.getPostedBy(), true);
                                        }else if (tle.de.getType().equals("audioAlbum")){
                                            afterMergeProfile.removeAudioAlbum(tle.de.getId(), tle.de.getShareWith(), tle.de.getPostedBy(), true);
                                        }else if (tle.de.getType().equals("videoAlbum")){
                                            afterMergeProfile.removeVideoAlbum(tle.de.getId(), tle.de.getShareWith(), tle.de.getPostedBy(), true);
                                        }else if (tle.de.getType().equals("photoAlbum")){
                                            afterMergeProfile.removePhotoAlbum(tle.de.getId(), tle.de.getShareWith(), tle.de.getPostedBy(), true);
                                        }else if (tle.de.getType().equals("audio")){
                                            afterMergeProfile.removeAudio(tle.de.getParent(), tle.de.getId(), tle.de.getShareWith(), tle.de.getPostedBy(), true);
                                        }else if (tle.de.getType().equals("video")){
                                            afterMergeProfile.removeVideo(tle.de.getParent(), tle.de.getId(), tle.de.getShareWith(), tle.de.getPostedBy(), true);
                                        }else if (tle.de.getType().equals("photo")){
                                            afterMergeProfile.removePhoto(tle.de.getParent(), tle.de.getId(), tle.de.getShareWith(), tle.de.getPostedBy(), true);
                                        }
                                        
                                    }
                                }
                                file.delete();
                            }else if (filename.contains("info.xml")){
                                profile userProfile = new profile(myFriend.getUser().getUsername(), getUpdateSettings.username, prefix);
                                userProfile.loadUserInfo();
                                user u = new user(myFriend.getUser().getUsername(), userProfile.userInfo.getFirstName(), userProfile.userInfo.getLastName(), 0);
                                myFriend.setUser(u);
                                getUpdateSettings.updateFriend(myFriend);
                            }
                        }
                        myFriend.setStatus("disconnected");
                        if (DEBUG){
                            System.out.println("DEBUG: line 4187 of clientThread.java. $$$$$$$$$ setting latestversion here from getUpdates $$$$$$$$$ for " + myFriend.getUser().getUsername());
                        }
                        myFriend.setLatestVersion(now);
                        getUpdateSettings.updateFriend(myFriend);
                        conn.close();
                        usernameHashBytes = hashMessage(myFriend.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                        usernameHash = new sun.misc.BASE64Encoder().encode(usernameHashBytes);
                        machineHashBytes = null;
                        machineHash = "";
                        if (index < 3){
                            machineHashBytes = hashMessage(myFriend.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                            machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                        }else{
                            machineHashBytes = hashMessage(dummy.getUser().getUsername().getBytes("UTF-8"), globalProperties.messageDigestAlgorithm);
                            machineHash = new sun.misc.BASE64Encoder().encode(machineHashBytes);
                        }
                        appendLog(String.valueOf(logEntryId), "get updates", usernameHash, machineHash, "0", "successfully got updates", String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()));
                        disconnectAtTheEnd = false;
                    }
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }finally{
                    try{
                        if (myFriend != null && getUpdateSettings.friends.size() > i && disconnectAtTheEnd){
                            myFriend.setStatus("disconnected");
                            getUpdateSettings.updateFriend(myFriend);
                        }
                        synchronized(runningThreads){
                            for (i = 0; i < runningThreads.size(); i++){
                                String name = runningThreads.get(i);
                                if (name.equals(myFriend.getUser().getUsername())){
                                    runningThreads.remove(i);
                                }
                            }
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
                        if (conn != null){
                            if (!conn.isClosed()){
                                conn.close();
                            }
                        }
                    }catch(Exception e){
                        
                    }
                }
                return;
            }
        }
    }
}
