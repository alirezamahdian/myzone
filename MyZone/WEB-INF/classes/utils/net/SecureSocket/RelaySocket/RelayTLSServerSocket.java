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

package utils.net.SecureSocket.RelaySocket;

import javax.crypto.*;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.net.*;
import java.util.Calendar;
import java.util.TimeZone;

import sun.misc.BASE64Encoder;

import javax.crypto.spec.SecretKeySpec;

import utils.attributes.*;
import utils.net.rudp.*;
import utils.net.SecureSocket.TLSServerSocket;
import utils.net.SecureSocket.SecureSocket;
import utils.security.KeyPairUtil.*;
import utils.security.CertUtil.*;
/*
 This file implements the Relayed TLS Server Socket.
 Relayed TLS Server Socket provides an end to end secure socket (the traffic
 is relayed over a relay server without being decrypted at the relay server) to
 communicate with each client.
 The Relayed TLS Server Socket is instantiated using the following parameters:
 - relayAddress: the address of the relay server.
 - relayPort: the port number of the relay server.
 - username: the username of the person who is running the server.
 - certPath: the path to the directory where the user certificate is held.
 - keyPath: the path to the directory where the private and public keys
 of the server user are stored.
 - cipherAlgorithm: the algorithm that is used for the asymmetric encryption.
 The server socket is used by instantiating an object and calling the accept
 member function. Upon receiving a connection, accept return a secure socket
 which can communicate with the client by calling send or receive functions.
 */

public class RelayTLSServerSocket extends TLSServerSocket{
	
    // the constructor.
	public RelayTLSServerSocket(String relayAddress, int relayPort, String username, String certPath, String keyPath, String cipherAlgorithm) throws SocketException{
        super(username, certPath, keyPath, cipherAlgorithm);
        globalProperties.init("../../");
		this.username = username;
		this.relayAddress = relayAddress;
		this.relayPort = relayPort;
        gotRequest = false;
        int attempts = 0;
        synchronized(registerLock){
            while (registerServer() == 0 && attempts < 10){
                attempts++;
                try{
                    Thread.sleep(10);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
        if (attempts == 10){
            throw new SocketException("Could not register with the relay server");
        }
        st = new servingThread();
        st.start();
	}
	
    private final static boolean DEBUG = false;
    private servingThread st;
	private Socket servingSocket;
	private String username;
	private String relayAddress;
	private int relayPort;
	private header hdr = new header();
    private relayAttributes relayProperties = new relayAttributes();
	private boolean gotRequest;
    private int pingInterval;
    
    // this function registers the server socket in the relay server. 
    // the traffic can only be directed to this server socket after this server is
    // registered with the relay server.
    private int registerServer(){
		try{
            servingSocket = new Socket(relayAddress, relayPort);
            servingSocket.setSoTimeout(relayProperties.IDLE_LIMIT);
			/* client -> server: IS_SERVER|CERTIFICATE.length|CERTIFICATE
             server -> client: MSG_LENGTH|timestamp.length|E(P_peer)[timestamp]
             client -> server: timestamp
             */
            CertVerifier x = new CertVerifier();
            byte[] certBytes = x.readRawFile(certPath, username + ".cert");
            if (certBytes == null){
                if (DEBUG){
                    System.out.println("DEBUG: line 112 of RelayTLSServerSocket.java. Could not find the certificate.");
                }
                return 0;
            }
            byte[] buffer = new byte[1 + 4 + certBytes.length];
			buffer[0] = hdr.IS_SERVER;
            System.arraycopy(intToByteArray(certBytes.length) , 0, buffer, 1, 4);
			System.arraycopy(certBytes, 0, buffer, 1 + 4, certBytes.length);
			DataOutputStream outToRelay = new DataOutputStream(servingSocket.getOutputStream());
			outToRelay.write(buffer, 0, buffer.length);
			outToRelay.flush();
            // MSG_LENGTH|timestamp.length|E(P_peer)[timestamp]
            byte[] lenBytes = new byte[4];
            int i = 0;
            while(i < 4){
                i += servingSocket.getInputStream().read(lenBytes, i, 4 - i);
                if (i < 0){
                    return 0;
                }
                try{
                    Thread.sleep(10);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            int len = byteArrayToInt(lenBytes);
            byte[] encryptedTimeStampWithRawLength = new byte[len];
            i = 0;
            while(i < len){
                i += servingSocket.getInputStream().read(encryptedTimeStampWithRawLength, i, len - i);
                if (i < 0){
                    return 0;
                }
                try{
                    Thread.sleep(10);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            // client -> server: timestamp
            byte[] timestampBytes = decrypt(encryptedTimeStampWithRawLength, globalProperties.asymCipher, privateKey);
            outToRelay.write(timestampBytes, 0, timestampBytes.length);
            outToRelay.flush();
            //outToRelay.close();
            byte[] ack = new byte[1];
            byte[] keepAlive = new byte[4];
            servingSocket.getInputStream().read(ack, 0, 1);
            if (ack[0] == hdr.SERVER_NOT_REGISTERD){
                return 0;
            }
            servingSocket.getInputStream().read(keepAlive, 0, 4);
            pingInterval = byteArrayToInt(keepAlive);
            return 1;
		}catch(IOException e){
            if (DEBUG){
                System.out.println("DEBUG: line 167 of RelayTLSServerSocket.java. No Connection to the Relay Server");
            }
			return 0;
		}
    }
	private boolean acceptIsAlive = false;
    private Object serve = new Object();
    private boolean killSocket = false;
    private Object registerLock = new Object();
    // this is the overloaded accept function from the TLSServerSocket class which 
    // facilitates the emulation of the end to end secure connection with the client.
	public synchronized SecureSocket accept() throws SocketException{
        
        Socket servingSocketCopy = null;
        SecureSocket ssock = null;
        if (killSocket){
            throw new SocketException("Could not register with the relay server");
        }
        synchronized(serve){
            try{
                acceptIsAlive = true;
                serve.wait();
                acceptIsAlive = false;
                if (killSocket){
                    throw new SocketException("Could not register with the relay server");
                }
                servingSocketCopy = servingSocket;
                int attempts = 0;
                synchronized(registerLock){
                    while (registerServer() == 0 && attempts < 20){
                        attempts++;
                        try{
                            Thread.sleep(10);
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                }
                if (attempts == 20){
                    throw new SocketException("Could not register with the relay server");
                }
                serve.notify();
            }catch(InterruptedException e){
                if (DEBUG){
                    e.printStackTrace();
                }
            }
        }
        try{
            DataOutputStream outToRelay = new DataOutputStream(servingSocketCopy.getOutputStream());
            byte[] message = new byte[1];
            message[0] = hdr.SERVER_ACCEPTS_REQUEST;
            outToRelay.write(message, 0, message.length);
            outToRelay.flush();
            PublicKey clientPubKey = recvCertificate(servingSocketCopy.getInputStream());
            byte[] ack = new byte[1];
            if (clientPubKey == null){
                ack[0] = hdr.TLS_CERTIFICATE_NOT_RETRIEVED;
                outToRelay.write(ack, 0, 1);
                outToRelay.flush();
                servingSocketCopy.close();
                return null;
            }
            ack[0] = hdr.TLS_CERTIFICATE_RETRIEVED;
            outToRelay.write(ack, 0, 1);
            outToRelay.flush();
            sendEncryptedCertificate(outToRelay, clientPubKey);
            Key sessionKey = retrieveSessionKey(servingSocketCopy.getInputStream(), clientPubKey, globalProperties.asymCipher);
            ack = new byte[1];
            if (sessionKey == null){
                ack[0] = hdr.TLS_SESSIONKEY_NOT_RETRIEVED;
                outToRelay.write(ack, 0, 1);
                outToRelay.flush();
                servingSocketCopy.close();
                return null;
            }
            ack[0] = hdr.TLS_SESSIONKEY_RETRIEVED;
            outToRelay.write(ack, 0, 1);
            outToRelay.flush();
            ssock = new SecureSocket(servingSocketCopy, globalProperties.TCP, cipherAlgorithm, sessionKey);
            return ssock;
        }catch(IOException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
	}
	
	// this class is being used as a background thread that reads the messages sent by the relay
    // server from an idle socket conencted to the relay server, and decides whether the message
    // is a "server is alive" query packet in which case a SERVER_IS_ALIVE is sent back to the 
    // relay to keep the socket registered in the relay server, or if the message is a request to
    // establish a secure connection using this particular socket.  
    class servingThread extends Thread{
        public servingThread()
        {
            super("servingThread");
            setDaemon(true);
        }
        
        public void run(){
            long idleTime = 0;
            long startOfIdleTime = 0;
            while(true){
                try{
                    if (servingSocket.isClosed()){
                        int attempts = 0;
                        synchronized(registerLock){
                            while (registerServer() == 0 && attempts < 10){
                                attempts++;
                                try{
                                    Thread.sleep(100);
                                }catch(Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }
                        if (attempts == 10){
                            killSocket = true;
                            if (acceptIsAlive){
                                synchronized(serve){
                                    serve.notify();
                                }
                            }
                            return;
                        }
                    }
                    DataOutputStream outToRelay = new DataOutputStream(servingSocket.getOutputStream());
                    int i = 0;
                    byte[] message = new byte[1];
                    startOfIdleTime = System.currentTimeMillis();
                    while(servingSocket.getInputStream().available() < 1){
                        idleTime = System.currentTimeMillis() - startOfIdleTime;
                        if (idleTime > pingInterval + relayProperties.GRACE_PERIOD)
                        {
                            servingSocket.close();
                            int attempts = 0;
                            synchronized(registerLock){
                                while (registerServer() == 0 && attempts < 10){
                                    attempts++;
                                    try{
                                        Thread.sleep(100);
                                    }catch(Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            }
                            if (attempts == 10){
                                killSocket = true;
                                if (acceptIsAlive){
                                    synchronized(serve){
                                        serve.notify();
                                    }
                                }
                            }
                            outToRelay = new DataOutputStream(servingSocket.getOutputStream());
                            startOfIdleTime = System.currentTimeMillis();
                        }
                        try{
                            Thread.sleep(10);
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                    servingSocket.getInputStream().read(message, 0, 1);
                    if (message[0] == hdr.SERVER_GOT_REQUEST)
                    {
                        while(!acceptIsAlive){
                            try{
                                Thread.sleep(10);
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                        synchronized(serve){
                            serve.notify();
                            try{
                                serve.wait();
                            }catch(InterruptedException e){
                                if (DEBUG){
                                    e.printStackTrace();
                                }
                            }
                        }
                    }else if (message[0] == hdr.SERVER_IS_ALIVE){
                        message[0] = hdr.SERVER_IS_ALIVE;
                        outToRelay.write(message, 0, message.length);
                        outToRelay.flush();
                    }
                }catch(IOException e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

