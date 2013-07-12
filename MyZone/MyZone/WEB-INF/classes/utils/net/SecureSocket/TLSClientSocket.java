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

package utils.net.SecureSocket;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import javax.crypto.*;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.net.*;
import sun.misc.BASE64Encoder;

import java.util.Random;

import utils.attributes.*;
import utils.net.rudp.*;
import utils.security.KeyPairUtil.*;
import utils.security.CertUtil.*;
import utils.security.CertUtil.userCertificate;

/*
 This file implements the TLS Client Socket.
 TLS Client Socket provides a secure socket to communicate with a TLS Server Socket. 
 The TLS Client Socket is instantiated using the following parameters:
 - username: the username of the server user. 
 - type: this is the type of the protocol that is going to be used at the
 transport layer. It can either be TCP or UDP.
 - srvAddress: this is the server address.
 - port: this is the server port.
 - myusername: the username of the person who is using this socket (this is 
 to retrieve the certificate and private key).
 - certPath: the path to the directory where the certificate is held.
 - keyPath: the path to the directory where the private and public keys
 of this user are stored.
 - cipherAlgorithm: the algorithm that is used for the asymmetric encryption.
 - sessionKeyType: the type of the symmetric encryption being used for the
 session encryption.
 The client socket is used by instantiating an object and calling the initiate
 member function. Upon successful initiation of the socket a secure connection
 is established between the client and the server. client and server can communicate
 with each other by calling send and receive functions.
 */


public class TLSClientSocket{
	
    private final static boolean DEBUG = false;
    public globalAttributes globalProperties = new globalAttributes();
    private TLSSocketAttributes TLSSocketProperties = new TLSSocketAttributes();
    private header hdr = new header();
    private String cipherAlgorithm;
    private String sessionKeyType;
    private PrivateKey privateKey;
    private String serverUsername;
    private String myUsername;
    private int type;
    
    // the constructor.
	public TLSClientSocket(String username, int type, String srvAddress, int port, String myusername, String certPath, String keyPath, String cipherAlgorithm, String sessionKeyType) throws SocketTimeoutException, SocketException, UnknownHostException, IOException {
		
        globalProperties.init("../../");
        myUsername = myusername;
        serverUsername = username;
        this.cipherAlgorithm = cipherAlgorithm;
        this.sessionKeyType = sessionKeyType;
        this.certPath = certPath;
        this.type = type;
        if (type == globalProperties.UDP){
            clientSocket = new ReliableSocket(srvAddress, port);
            clientSocket.setSoTimeout(TLSSocketProperties.IDLE_LIMIT);
        }
        else if (type == globalProperties.TCP){
            clientSocket = new Socket(srvAddress, port);
            clientSocket.setSoTimeout(TLSSocketProperties.IDLE_LIMIT);
        }
        else 
        {
            if (DEBUG){
                System.out.println("DEBUG: line 96 of TLSClientSocket.java. Unsupported Protocol Socket");
            }
        }
        KeyPairUtil x = new KeyPairUtil();
        privateKey = x.readPriKey(keyPath, myusername, globalProperties.keyPairAlgorithm);
        
	}
    
    public boolean close(){
        try{
            if (clientSocket != null){
                clientSocket.close();
            }
            return true;
        }catch(IOException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return false;
    }
	// this function converts a 32 bit integer to a byte array of length 4.
	protected static final byte[] intToByteArray(int value) {
        return new byte[] {
			(byte)(value >>> 24),
			(byte)(value >>> 16),
			(byte)(value >>> 8),
			(byte)value};
	}
	
    // this function converts a byte array of length 4 to a 32 bit integer.
	private static final int byteArrayToInt(byte [] b) {
        return (b[0] << 24)
		+ ((b[1] & 0xFF) << 16)
		+ ((b[2] & 0xFF) << 8)
		+ (b[3] & 0xFF);
	}
	
	protected Socket clientSocket;
	private String certPath;
	
    // this function encrypts a raw array of bytes stored in plain and 
    // encrypts it using the asymmetric algorithm specified by keyAlgorithm
    // and the asymmetric key stored in key.
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
    private byte[] decrypt(byte[] encryptedMsgWithLength, String keyAlgorithm, Key key){
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
	
    // this function sends the certificate of the client user to the server.
    // the certificate is not encrypted.
	private int sendCertificate(Socket s){
        DataOutputStream outToServer = null;
		try{
            outToServer = new DataOutputStream(s.getOutputStream());
            CertVerifier x = new CertVerifier();
            byte[] certBytes = x.readRawFile(certPath, myUsername + ".cert");
            byte[] msgLenBytes = intToByteArray(certBytes.length);
            byte[] buffer = new byte[msgLenBytes.length + certBytes.length];
            System.arraycopy(msgLenBytes, 0, buffer, 0, msgLenBytes.length);
            System.arraycopy(certBytes, 0, buffer, msgLenBytes.length, certBytes.length);
            outToServer.write(buffer, 0, buffer.length);
            outToServer.flush();
        }catch(IOException e){
            if (DEBUG){
                e.printStackTrace();
            }
            
        }
        return 1;
	}
	
    // this function receives the server's certificate encrypted by the client's public 
    // key and extracts the server's certificate and returns the server's public key 
    // after verifyig the server's certificate.
	private PublicKey recvEncryptedCertificate(InputStream inFromServer){
		try{
            int i = 0;
            byte[] msgLenBytes = new byte[4];
            while(i < 4){
                i += inFromServer.read(msgLenBytes, i, 4 - i);
                if (i < 0){
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
            byte[] encryptedCertBytes = new byte[len];
            i = 0;
            while(i < len){
                i += inFromServer.read(encryptedCertBytes, i, len - i);
                if (i < 0){
                    if (DEBUG){
                        System.out.println("DEBUG: line 291 of TLSClientSocket.java");
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
            byte[] certBytes = decrypt(encryptedCertBytes, globalProperties.asymCipher, privateKey);
            if (certBytes == null){
                if (DEBUG){
                    System.out.println("DEBUG: line 306 of TLSClientSocket.java");
                }
            }
            CertVerifier x = new CertVerifier();
            userCertificate usrCert = x.verifyCertificate(certBytes, globalProperties.caCertPath, globalProperties.keyPairAlgorithm, globalProperties.certSigAlgorithm);
            if (usrCert.username.equals(serverUsername)){
                
                return usrCert.publicKey;
            }
            if (DEBUG){
                System.out.println("DEBUG: line 316 of TLSClientSocket.java");
            }
            return null;
        }catch(IOException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
	}
	
	private SecretKey sendSessionKey(Socket clientSocket, PublicKey srvPubKey){
        try{
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            Security.addProvider(new BouncyCastleProvider());
            KeyGenerator kgen = KeyGenerator.getInstance(sessionKeyType, "BC");
            kgen.init(globalProperties.sessionKeySize);
            SecretKey skey = kgen.generateKey();
            byte[] raw = skey.getEncoded();
            byte[] rawLenBytes = intToByteArray(raw.length);
            byte[] encryptedKey = encrypt(raw, globalProperties.asymCipher, srvPubKey);
            if (encryptedKey == null){
                if (DEBUG){
                    System.out.println("DEBUG: line 338 of TLSClientSocket.java");
                }
            }
            byte[] encryptedKeyWithRawLength = new byte[rawLenBytes.length + encryptedKey.length];
            System.arraycopy(rawLenBytes, 0, encryptedKeyWithRawLength, 0, rawLenBytes.length);
            System.arraycopy(encryptedKey, 0, encryptedKeyWithRawLength, rawLenBytes.length, encryptedKey.length);
            byte[] encryptedKeyWithRawLengthLenBytes = intToByteArray(encryptedKeyWithRawLength.length);
            byte[] wrappedEncryptedKey = encrypt(encryptedKeyWithRawLength, globalProperties.asymCipher, privateKey);
            if (wrappedEncryptedKey == null){
                if (DEBUG){
                    System.out.println("DEBUG: line 348 of TLSClientSocket.java");
                }
            }
            byte[] wrappedEncryptedKeyWithLength = new byte[wrappedEncryptedKey.length + encryptedKeyWithRawLengthLenBytes.length];
            System.arraycopy(encryptedKeyWithRawLengthLenBytes, 0, wrappedEncryptedKeyWithLength, 0, encryptedKeyWithRawLengthLenBytes.length);
            System.arraycopy(wrappedEncryptedKey, 0, wrappedEncryptedKeyWithLength, encryptedKeyWithRawLengthLenBytes.length, wrappedEncryptedKey.length);
            byte[] keyHash = hashMessage(raw, globalProperties.messageDigestAlgorithm);
            int len = wrappedEncryptedKeyWithLength.length + keyHash.length;
            byte[] msgLenBytes = intToByteArray(len);
            byte[] buffer = new byte[msgLenBytes.length + len];
            System.arraycopy(msgLenBytes, 0, buffer, 0, msgLenBytes.length);
            System.arraycopy(wrappedEncryptedKeyWithLength, 0, buffer, msgLenBytes.length, wrappedEncryptedKeyWithLength.length);
            System.arraycopy(keyHash, 0, buffer, msgLenBytes.length + wrappedEncryptedKeyWithLength.length, keyHash.length);
            outToServer.write(buffer, 0, buffer.length);
            outToServer.flush();
            return skey;
        }catch(NoSuchAlgorithmException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(IOException e){
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
    
    // this function initiates a secure connection with a TLS Server Socket and 
    // returns a secure connection upon successful execution.
    public SecureSocket initiate(){
		try{
            if (clientSocket == null){
                return null;
            }
            byte[] ack = new byte[1];
            sendCertificate(clientSocket);
            int i = 0;
            while(i < 1){
                i += clientSocket.getInputStream().read(ack, 0, 1);
                if (i < 0){
                    if (DEBUG){
                        System.out.println("DEBUG: line 390 of TLSClientSocket.java");
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
            if (ack[0] == hdr.TLS_CERTIFICATE_NOT_RETRIEVED){
                if (DEBUG){
                    System.out.println("DEBUG: line 404 of TLSClientSocket.java");
                }
                return null;
            }
            PublicKey srvPubKey = recvEncryptedCertificate(clientSocket.getInputStream());
            if (srvPubKey == null){
                if (DEBUG){
                    System.out.println("DEBUG: line 411 of TLSClientSocket.java");
                }
                clientSocket.close();
                return null;
            }
            SecretKey skey = sendSessionKey(clientSocket, srvPubKey);
            i = 0;
            ack = new byte[1];
            while(i < 1){
                i += clientSocket.getInputStream().read(ack, 0, 1);
                if (i < 0){
                    if (DEBUG){
                        System.out.println("DEBUG: line 423 of TLSClientSocket.java");
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
            if (ack[0] == hdr.TLS_SESSIONKEY_RETRIEVED){
                SecureSocket ssocket = new SecureSocket(clientSocket, type, cipherAlgorithm, skey);
                return ssocket;
            }
            if (DEBUG){
                System.out.println("DEBUG: line 440 of TLSClientSocket.java. successfully initiated");
            }
            clientSocket.close();
        }catch(IOException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
	}
	
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
    
}