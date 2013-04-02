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

package utils.net.SecureSocket;

import java.util.ArrayList;
import java.util.List;

import java.io.*;
import java.io.DataInputStream;
import java.net.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import sun.misc.BASE64Encoder;

import utils.attributes.*;
import utils.net.rudp.*;
import utils.security.CertUtil.*;
import utils.security.KeyPairUtil.*;
/*
 This file implements the TLS Server Socket.
 TLS Server Socket provides a secure socket to communicate with each client. 
 The TLS Server Socket is instantiated using the following parameters:
 - type: this is the type of the protocol that is going to be used at the
 transport layer. It can either be TCP or UDP.
 - port: this is the port that the server listens for incoming connections.
 - username: the username of the person who is running the server.
 - certPath: the path to the directory where the certificate is held.
 - keyPath: the path to the directory where the private and public keys
 of this user are stored.
 - cipherAlgorithm: the algorithm that is used for the asymmetric encryption.
 The server socket is used by instantiating an object and calling the accept
 member function. Upon receiving a connection, accept return a secure socket
 which can communicate with the client by calling send or receive functions.
 */

public class TLSServerSocket{
    
    private final static boolean DEBUG = false;
    private header hdr = new header();
    private TLSSocketAttributes TLSSocketProperties = new TLSSocketAttributes();
    public globalAttributes globalProperties = new globalAttributes();
    protected String cipherAlgorithm;
    protected PrivateKey privateKey;
    private PublicKey clientPubKey;
    private String username;
    private List<String> accessList;
    private int type;
    public String clientUsername;
	
    // constructor
    public TLSServerSocket(int type, int port, String username, String certPath, String keyPath, String cipherAlgorithm){
		try{
            globalProperties.init("../../");
            clientPubKey = null;
            accessList = new ArrayList();
            this.username = username;
            this.cipherAlgorithm = cipherAlgorithm;
            this.certPath = certPath;
            this.type = type;
            if (type == globalProperties.UDP){
                serverSocket = new ReliableServerSocket(port);
            }
            else if (type == globalProperties.TCP){
                serverSocket = new ServerSocket(port);
            }
            else{ 
                if (DEBUG){
                    System.out.println("DEBUG: line 89 of TLSServerSocket.java. Unsupported Protocol Socket");
                }
            }
            KeyPairUtil x = new KeyPairUtil();
            privateKey = x.readPriKey(keyPath, username, globalProperties.keyPairAlgorithm);
        }catch(IOException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
	}
    // this function adds users to the acces lisst of this particular
    // server socket.
    public void grantAccess(String username){
        for (int i = 0; i < accessList.size(); i++){
            String uname = (String)accessList.get(i);
            if (uname.equals(username)){
                return;
            }
        }
        accessList.add(username);
    }
    
    public void clearAccessList(){
        accessList.clear();
    }
    // this function revokes access privilages for the given user from 
    // the server socket.
    public void revokeAccess(String username){
        for (int i = 0; i < accessList.size(); i++){
            String uname = (String)accessList.get(i);
            if (uname.equals(username)){
                accessList.remove(i);
            }
        }
        return;
    }
    
	// constructor to be used by the child class RelayTLSServerSocket.
	public TLSServerSocket(String username, String certPath, String keyPath, String cipherAlgorithm){
        clientPubKey = null;
        accessList = new ArrayList();
        this.username = username;
		this.certPath = certPath;
        type = globalProperties.TCP;
        this.cipherAlgorithm = cipherAlgorithm;
        KeyPairUtil x = new KeyPairUtil();
        privateKey = x.readPriKey(keyPath, username, globalProperties.keyPairAlgorithm);
		serverSocket = null;
	}
	
	private ServerSocket serverSocket;
	protected String certPath;
	
    // This function computes the hash of the data represented as byte array 
    // using the given message digest algorithm. The default message digest 
    // algorithm is MD5.
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
    
    // converts a 32 bit integer to a byte array of size 4.
	protected static final byte[] intToByteArray(int value) {
        return new byte[] {
			(byte)(value >>> 24),
			(byte)(value >>> 16),
			(byte)(value >>> 8),
			(byte)value};
	}
	
    // converts a byte array of size 4 into an integer.
	protected static final int byteArrayToInt(byte [] b) {
        return (b[0] << 24)
		+ ((b[1] & 0xFF) << 16)
		+ ((b[2] & 0xFF) << 8)
		+ (b[3] & 0xFF);
	}
	
    // this function return a secure socket upon exchanging certificates and session key 
    // and establishing an encrypted connection using symmetric encryption
	public SecureSocket accept() throws SocketException{
        try{
            Socket connectionSocket = serverSocket.accept();
            connectionSocket.setSoTimeout(TLSSocketProperties.IDLE_LIMIT);
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            clientPubKey = recvCertificate(connectionSocket.getInputStream());
            byte[] ack = new byte[1];
            if (clientPubKey == null){
                if (DEBUG){
                    System.out.println("DEBUG: line 190 of TLSServerSocket.java");
                }
                ack[0] = hdr.TLS_CERTIFICATE_NOT_RETRIEVED;
                outToClient.write(ack, 0, 1);
                outToClient.flush();
                //outToClient.close();
                connectionSocket.close();
                return null;
            }
            ack[0] = hdr.TLS_CERTIFICATE_RETRIEVED;
            outToClient.write(ack, 0, 1);
            outToClient.flush();
            sendEncryptedCertificate(outToClient, clientPubKey);
            Key sessionKey = retrieveSessionKey(connectionSocket.getInputStream(), clientPubKey, globalProperties.asymCipher);
            ack = new byte[1];
            if (sessionKey == null){
                if (DEBUG){
                    System.out.println("DEBUG: line 207 of TLSServerSocket.java");
                }
                ack[0] = hdr.TLS_SESSIONKEY_NOT_RETRIEVED;
                outToClient.write(ack, 0, 1);
                outToClient.flush();
                connectionSocket.close();
                return null;
            }
            ack[0] = hdr.TLS_SESSIONKEY_RETRIEVED;
            outToClient.write(ack, 0, 1);
            outToClient.flush();
            SecureSocket ssock = new SecureSocket(connectionSocket, type,  cipherAlgorithm, sessionKey);
            return ssock;
        }catch(IOException e){
            if (DEBUG){
                e.printStackTrace();
            }
            return null;
        }
	}
	
    // this function encrypts the byte array stored in argument plain using the specified asymmetric
    // algorithm given by argument keyAlgorithm and the assymetring key stored in argument key.
    private byte[] encrypt(byte[] plain, String keyAlgorithm, Key key){
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
    
    // this function takes as input an encrypted byte array prefixed by the length of its 
    // decrypted form and decrypts it using the asymmetric algorithm specified by keyAlgorithm
    // and the asymmetric key stored at key.
    protected byte[] decrypt(byte[] encryptedMsgWithLength, String keyAlgorithm, Key key){
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
	
    // this function send the server user certificate to the client using the asymmetric key 
    // stored in key.
	protected int sendEncryptedCertificate(DataOutputStream outToClient, Key key){
		try{
            CertVerifier x = new CertVerifier();
            byte[] certBytes = x.readRawFile(certPath, username + ".cert");
            if (certBytes == null){
                if (DEBUG){
                    System.out.println("DEBUG: line 339 of TLSServerSocket.java");
                }
                return 0;
            }
            byte[] encryptedCert = encrypt(certBytes, globalProperties.asymCipher, key); 
            byte[] certLenBytes = intToByteArray(certBytes.length);
            byte[] msgLenBytes = intToByteArray(encryptedCert.length + certLenBytes.length);
            
            byte[] buffer = new byte[msgLenBytes.length + certLenBytes.length +  encryptedCert.length];
            System.arraycopy(msgLenBytes, 0, buffer, 0, msgLenBytes.length);
            System.arraycopy(certLenBytes, 0, buffer, msgLenBytes.length, certLenBytes.length);
            System.arraycopy(encryptedCert, 0, buffer, msgLenBytes.length + certLenBytes.length, encryptedCert.length);
            outToClient.write(buffer, 0, buffer.length);
            outToClient.flush();
        }catch(IOException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return 1;
	}
	
    // this function receives the certificate of the client and verifies that the certificate is 
    // authenticated.
	protected PublicKey recvCertificate(InputStream inFromClient){
		try{
            byte[] msgLenBytes = new byte[4];
            int i = 0;
            while(i < 4){
                i += inFromClient.read(msgLenBytes, i, 4 - i);
                if (i < 0){
                    if (DEBUG){
                        System.out.println("DEBUG: line 371 of TLSServerSocket.java");
                    }
                    return null;
                }
                try{
                    Thread.sleep(10);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            int len = byteArrayToInt(msgLenBytes);
            byte[] certBytes = new byte[len];
            i = 0;
            while(i < len){
                i += inFromClient.read(certBytes, i, len - i);
                if (i < 0){
                    if (DEBUG){
                        System.out.println("DEBUG: line 390 of TLSServerSocket.java");
                    }
                    return null;
                }
                try{
                    Thread.sleep(10);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            CertVerifier x = new CertVerifier();
            userCertificate clientCert = x.verifyCertificate(certBytes, globalProperties.caCertPath, globalProperties.keyPairAlgorithm, globalProperties.certSigAlgorithm);
            if (clientCert != null){
                if (isFriend(clientCert.username)){
                    clientUsername = clientCert.username;
                    return clientCert.publicKey;
                }
            }
            if (DEBUG){
                System.out.println("DEBUG: line 411 of TLSServerSocket.java");
            }
            return null;
        }catch(IOException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
	}
	
    private boolean isFriend(String username){
        for (int i = 0; i < accessList.size(); i++){
            String uname = (String)accessList.get(i);
            if (uname.equals(username)){
                return true;
            }
        }
        return false;
    }
    
    // this function retrieves the session key encrypted by the client's private key and decrypts it
    // using the client's public key and the specified asymmetric algorithm specified by keyAlgorithm.
	protected Key retrieveSessionKey(InputStream inFromClient, PublicKey clientPubKey, String keyAlgorithm){
		try{
            byte[] msgLenBytes = new byte[4];
            int i = 0;
            while(i < 4){
                i += inFromClient.read(msgLenBytes, i, 4 - i);
                if (i < 0){
                    if (DEBUG){
                        System.out.println("DEBUG: line 442 of TLSServerSocket.java");
                    }
                    return null;
                }
                try{
                    Thread.sleep(10);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            int len = byteArrayToInt(msgLenBytes);
            byte[] wrappedEncryptedKey = new byte[len - globalProperties.messageDigestSize];
            i = 0;
            while(i < wrappedEncryptedKey.length){
                i += inFromClient.read(wrappedEncryptedKey, i, wrappedEncryptedKey.length - i);
                if (i < 0){
                    if (DEBUG){
                        System.out.println("DEBUG: line 461 of TLSServerSocket.java");
                    }
                    return null;
                }
                try{
                    Thread.sleep(10);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            byte[] hash = new byte[globalProperties.messageDigestSize];
            i = 0;
            while(i < globalProperties.messageDigestSize){
                i += inFromClient.read(hash, i, hash.length - i);
                if (i < 0){
                    if (DEBUG){
                        System.out.println("DEBUG: line 479 of TLSServerSocket.java");
                    }
                    return null;
                }
                try{
                    Thread.sleep(10);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            byte[] encryptedKey = decrypt(wrappedEncryptedKey, keyAlgorithm, clientPubKey);
            byte[] key = decrypt(encryptedKey, keyAlgorithm, privateKey);
            String hashStr = new sun.misc.BASE64Encoder().encode(hashMessage(key, globalProperties.messageDigestAlgorithm));
            String recvdHashStr = new sun.misc.BASE64Encoder().encode(hash);
            
            if (hashStr.equals(recvdHashStr))
            {
                Key decryptionKey = new SecretKeySpec(key, globalProperties.sessionKeyType);
                return decryptionKey;
            }
            else {
                return null;
            }
        }catch(IOException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
	}
    
}