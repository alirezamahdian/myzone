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

import java.net.*;
import java.io.*;
import java.security.*;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest; 
import java.security.SecureRandom;

import javax.crypto.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import utils.attributes.*;

/*
 This file implements a SecureSocket on top of TCP or Reliable UDP.
 The socket is constructed by the (Relay)TLS Server/Client sockets by
 encrypting all the data transmitted through the socket passed to this class.
 the data are encrypted using a symmetric encryption specified by
 cipherAlgorithm that uses the session key stored in key.
 */
public class SecureSocket{
	public SecureSocket(Socket sock, int type, String cipherAlgorithm, Key key) throws IOException, SocketException{
        
        globalProperties.init("../../");
        connectionSocket = sock;
        connectionSocket.setSoTimeout(TLSSocketProperties.IDLE_LIMIT);
        sessionKey = key;
        this.cipherAlgorithm = cipherAlgorithm;
        this.in = sock.getInputStream();
        this.out = new DataOutputStream(sock.getOutputStream());
        this.type = type;
	}
    
    private final static boolean DEBUG = false;
	private TLSSocketAttributes TLSSocketProperties = new TLSSocketAttributes();
	private Socket connectionSocket;
    private String cipherAlgorithm;
	private Key sessionKey;
	private InputStream in;
	private DataOutputStream out;
    private int type;
    private globalAttributes globalProperties = new globalAttributes();
    
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
	
    // this function encrypts the data before they are sent.
	private byte[] encrypt(byte[] data){
		try{
            // Get a cipher object.
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance(cipherAlgorithm, "BC");
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
	
    // this function decrypts the data before they are sent.
	private byte[] decrypt(byte[] encrypted){
		
        try{
            // Get a cipher object.
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance(cipherAlgorithm, "BC");
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
	
    // this function computes the hash for the byte array stored in data
    // using the message digest algorithm given by mdAlgorithm.
    // the default digest algorithm is MD5.
	private byte[] hashMessage(byte[] data, String mdAlgorithm){ 
		try{
            MessageDigest md;
            Security.addProvider(new BouncyCastleProvider());
            md = MessageDigest.getInstance(mdAlgorithm, "BC");
            byte[] hash;
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
	
    // this function gets the raw data in form of a binary array
    // encrypts it using the session key and sends it through the socket.
	public int send(byte[] data) throws IOException{
		int len = 0;
        byte[] hash = hashMessage(data, globalProperties.messageDigestAlgorithm);
        byte[] encryptedMessage = encrypt(data);
        len = hash.length + encryptedMessage.length;
        byte[] lenBytes = intToByteArray(len);
        byte[] buffer = new byte[lenBytes.length + encryptedMessage.length + hash.length];
        System.arraycopy(lenBytes, 0, buffer, 0, lenBytes.length);
        System.arraycopy(encryptedMessage, 0, buffer, lenBytes.length, encryptedMessage.length);
        System.arraycopy(hash, 0, buffer, encryptedMessage.length + lenBytes.length, hash.length);
        out.write(buffer, 0, buffer.length);
        out.flush();
        return 1;
	}
	
    // this function receives the encrypted data from the socket and
    // decrypts them and returns the raw byte array to the calling function.
	public byte[] receive() throws IOException{
		if (type == globalProperties.TCP){
            long idleTime = 0;
            long initTime = System.currentTimeMillis();
            while(in.available() <= 0 && idleTime < TLSSocketProperties.IDLE_LIMIT){
                try{
                    Thread.sleep(10);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
                idleTime = System.currentTimeMillis() - initTime;
            }
            if (idleTime >= TLSSocketProperties.IDLE_LIMIT){
                throw new SocketException("Timed Out in Receive()");
            }
        }
        int len = 0;
        byte[] lenBytes = new byte[4];
        int i = 0;
        
        while(i < 4){
            i += in.read(lenBytes, i, 4 - i);
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
        len = byteArrayToInt(lenBytes);
        byte[] encryptedMessage = new byte[len - globalProperties.messageDigestSize];
        i = 0;
        while(i < encryptedMessage.length){
            i += in.read(encryptedMessage, i, encryptedMessage.length - i);
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
        byte[] hash = new byte[globalProperties.messageDigestSize];
        i = 0;
        while(i < hash.length){
            i += in.read(hash, i, hash.length - i);
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
        byte[] data = decrypt(encryptedMessage);
        String recvdHashStr = new sun.misc.BASE64Encoder().encode(hash);
        String hashStr = new sun.misc.BASE64Encoder().encode(hashMessage(data, globalProperties.messageDigestAlgorithm));
        if (hashStr.equals(recvdHashStr))
        {
            return data;
        }
        else {
            if (DEBUG){
                System.out.println("DEBUG: line 266 of SecureSocket.java. Hash value did not match: Packet was modified");
            }
            return null;
        }
    }
	
    // closes the socket.
	public boolean close(){
        try{
            in.close();
            out.close();
            connectionSocket.close();
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
            return false;
        }
		return true;
	}
    
    public boolean isClosed(){
        return connectionSocket.isClosed();
    }
    
    public String toString(){
        return connectionSocket.toString();
    }
    
}
