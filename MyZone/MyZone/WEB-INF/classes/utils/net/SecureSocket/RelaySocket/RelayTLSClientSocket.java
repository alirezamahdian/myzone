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

package utils.net.SecureSocket.RelaySocket;

import javax.crypto.*;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.net.*;
import java.util.Random;

import sun.misc.BASE64Encoder;

import utils.attributes.*;
import utils.net.rudp.*;
import utils.net.SecureSocket.TLSClientSocket;
import utils.net.SecureSocket.SecureSocket;
import utils.security.KeyPairUtil.*;
import utils.security.CertUtil.*;
/*
 This file implements the TLS Client Socket relayed using a relayed server.
 Relayed TLS Client Socket provides an end to end secure socket to communicate
 with a TLS Server Socket. 
 The Relyed TLS Client Socket is instantiated using the following parameters:
 - dstUsername: the username of the server user. 
 - relayAddress: the address of the relay server.
 - port: the port number of the relay server.
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


public class RelayTLSClientSocket extends TLSClientSocket{
    
    private final static boolean DEBUG = false;
	private header hdr = new header();
    private relayAttributes relayProperties = new relayAttributes();
    private String dstUsername;
	
	public RelayTLSClientSocket(String dstUsername, String relayAddress, int port, String myusername, String certPath, String keyPath, String cipherAlgorithm, String sessionKeyType) throws IOException, SocketException{
        super(dstUsername, 1, relayAddress, port, myusername, certPath, keyPath, cipherAlgorithm, sessionKeyType);
        try{
            this.dstUsername = dstUsername;
            DataOutputStream outToRelay = new DataOutputStream(clientSocket.getOutputStream());
            int len = dstUsername.length();
            byte[] lenBytes = intToByteArray(len);
            byte[] message = new byte[1];
            byte[] buffer = new byte[1 + lenBytes.length + len];
            message[0] = hdr.IS_CLIENT;
            System.arraycopy(message, 0, buffer, 0, 1);
            System.arraycopy(lenBytes, 0, buffer, 1, lenBytes.length);
            System.arraycopy(dstUsername.getBytes("UTF-8"), 0, buffer, 1 + lenBytes.length, len);
            outToRelay.write(buffer, 0, buffer.length);
            outToRelay.flush();
            byte[] ack = new byte[1];
            long idleTime = 0;
            long lastTransmissionTime = System.currentTimeMillis();
            while(clientSocket.getInputStream().available() < 1){
                idleTime = System.currentTimeMillis() - lastTransmissionTime;
                if (idleTime > relayProperties.IDLE_LIMIT)
                {
                    outToRelay.close();
                    clientSocket.close();
                    throw new SocketException("client socket closed.");
                }
                try{
                    Thread.sleep(10);
                }catch(Exception e){
                    if (DEBUG){
                        e.printStackTrace();
                    }
                }
            }
            clientSocket.getInputStream().read(ack, 0, 1);
            if (ack[0] != hdr.CLIENT_CONNECTED_TO_SERVER){
                clientSocket.close();
                clientSocket = null;
                if (DEBUG){
                    System.out.println("DEBUG: line 106 of RelayTLSClientSocket.java. Failed to Connect to " + dstUsername);
                }
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
	}
}