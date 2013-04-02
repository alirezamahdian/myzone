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

/* This is the header file that contains the definition of all message types.
 In addition, it also defines the protocol in terms of the messages sent 
 between each entity in the system.
 This file is divided into different parts based on the communicating
 parties.
 For each message type the body of each message and the sequence of
 interactions between the entities are defined.
 This file should be used as a reference for the protocol and all future 
 changes and extensions to the protocol need to be reflected in this file.
 */


package utils.attributes;

public class header{
    public final static int BAD_REQUEST = 0x0000;
    
    public final static int SESSIONKEY_RETRIEVED = 0x2220;
    public final static int SESSIONKEY_NOT_RETRIEVED = 0x2221;
    
    /* <---- Peer With the Certificate Authority Server ----> */
    public final static int ISSUE_CERTIFICATE = 0x0001;     
    /* client -> server: MSG_LENGTH|ISSUE_CERTIFICATE|(4 + 4 + USERNAME.length() + PUBLIC_KEY.length)
     ->|E(P_CA)[USERNAME.length()|PUBLIC_KEY.length|USERNAME|PUBLIC_KEY]|hash 
     */ 
    public final static int CERTIFICATE_ISSUED = 0x0002;    
    /* server -> client: MSG_LENGTH|CERTIFICATE_ISSUED|CERTIFICATE.length|E(P_peer)[CERTIFICATE]|hash */ 
    public final static int CERTIFICATE_NOT_ISSUED = 0x0003;
    /* MSG_LENGTH|CERTIFICATE_NOT_ISSUED */
    
    /* <---- Peer With the Rendevous Server ----> */
    public final static int REGISTER_PEER = 0x0011;   // modified      
    /* client -> server: MSG_LENGTH|REGISTER_PEER|CERTIFICATE.length|CERTIFICATE
     server -> client: MSG_LENGTH|SESSIONKEY.length|E(P_peer)[SESSIONKEY]|hash
     client -> server: MSG_LENGTH|SESSIONKEY_RETRIEVED|E(SESSIONKEY)[PORT|TYPE_OF_NAT|TYPE_OF_PROTOCOL|
     ->RELAY_ADDRESS.length|RELAY_ADDRESS|RELAY_PORT|PASSPHRASE.length|
     ->PASSPHRASE|ENCRYPTED_MIRRORS.length|{E(MTK_peer)[(MIRROR_USERNAME.length+MIRROR_USERNAME)*]}|E(Q_peer)[MD]]|hash 
     MD = hash(IP_ADDRESS+PORT+TYPE_OF_PROTOCOL+RELAY_ADDRESS+RELAY_PORT+PASSPHRASE+{E(MTK_peer)[(MIRROR_USERNAME.length+MIRROR_USERNAME)*]}
     */
    public final static int PEER_REGISTERD = 0x0012;     
    public final static int PEER_REGISTERD_SEND_LOG = 0x0014;   
    /* server -> client: MSG_LENGTH|PEER_REGISTERED|E(SESSIONKEY)[FRIENDSHIP_REQUESTER_USERNAME.length|
     ->FRIENDSHIP_REQUESTER_USERNAME|
     ->4+(E(P_peer)[PASSPHRASE]).length|PASSPHRASE.length|E(P_peer)[PASSPHRASE]]|hash
     */
    public final static int PEER_NOT_REGISTERD = 0x0013;    
    /* server -> client: MSG_LENGTH|PEER_NOT_REGISTERD */
    public final static int REQUEST_RELAY_SERVER = 0x0101;  
    /* client -> server: MSG_LENGTH|REQUEST_RELAY_SERVER */
    public final static int RELAY_SERVER_SENT = 0x0102;     
    /* server -> client: MSG_LENGTH|RELAY_SERVER_SENT|RELAY_ADDRESS.length|RELAY_ADDRESS|RELAY_PORT */
    public final static int NO_RELAY_AVAILABLE = 0x0103;    
    /* server -> client: MSG_LENGTH|NO_RELAY_AVAILABLE */
    public final static int FIND_PEER = 0x1011;             
    /* client -> server: MSG_LENGTH|FIND_PEER|CERTIFICATE.length|CERTIFICATE
     server -> client: MSG_LENGTH|SESSIONKEY.length|E(P_peer)[SESSIONKEY]|hash
     client -> server: MSG_LENGTH|SESSIONKEY_RETRIEVED|E(SESSIONKEY)[PASSPHRASE]|hash
     */
    public final static int PEER_FOUND = 0x1012;   // modified           
    /* server -> client: MSG_LENGTH|PEER_FOUND|E(SESSIONKEY)[IP_ADDRESS.length|IP_ADDRESS|PORT|TYPE_OF_PROTOCOL|
     ->RELAY_ADDRESS.length|RELAY_ADDRESS|RELAY_PORT|ENCRYPTED_MIRRORS.length|{E(MTK_peer)[(MIRROR_USERNAME.length+MIRROR_USERNAME)*]}|CERTIFICATE.length|CERTIFICATE|E(Q_peer)[MD]]|hash
     MD = hash(IP_ADDRESS+PORT+TYPE_OF_PROTOCOL+RELAY_ADDRESS+RELAY_PORT+PASSPHRASE+{E(MTK_peer)[(MIRROR_USERNAME.length+MIRROR_USERNAME)*]}
     */
    public final static int PEER_NOT_FOUND = 0x1013;        
    /* server -> client: MSG_LENGTH|PEER_NOT_FOUND */
    
    public final static int REQUEST_FRIENDSHIP = 0x1014;    
    /* client -> server: MSG_LENGTH|REQUEST_FRIENDSHIP|CERTIFICATE.length|CERTIFICATE
     server -> client: MSG_LENGTH|SESSIONKEY.length|E(P_peer)[SESSIONKEY]|hash
     client -> server: MSG_LENGTH|SESSIONKEY_RETRIEVED|E(SESSIONKEY)[USERNAME]|hash : USERNAME is the peer whom is requested for friendship
     --
     |server -> client: MSG_LENGTH|E(SESSIONKEY)[USER_CERTIFICATE]|hash : user certificate of the peer whom is requested for friendship
     |OR
     |server -> client: MSG_LENGTH|PEER_NOT_FOUND
     --
     client -> server: MSG_LENGTH|E(SESSIONKEY)[PASSPHRASE.length|
     ->E(P_peer)[PASSPHRASE]]|hash
     */    
    public final static int FRIENDSHIP_REQ_RECEIVED = 0x1015; 
    /* --
     |server -> client: MSG_LENGTH|FRIENDSHIP_REQ_RECEIVED
     |OR
     |server -> client: MSG_LENGTH|PEER_NOT_FOUND
     --
     */
    
    
    /* <---- Relay Server With the Rendevous Server ----> */
    public final static int REGISTER_RELAY = 0x1101;        
    /* MSG_LENGTH|REGISTER_RELAY|PORT|CAPACITY */
    public final static int RELAY_REGISTERD = 0x1102;       
    /* MSG_LENGTH|RELAY_REGISTERD|UPDATEINTERVAL */
    public final static int RELAY_NOT_REGISTERD = 0x1103;   
    /* MSG_LENGTH|RELAY_NOT_REGISTERED */
    public final static int RELAY_UPDATE = 0x1111;          
    /* MSG_LENGTH|RELAY_UPDATE|PORT|CAPACITY|LOAD */
    public final static int RELAY_NOT_FOUND = 0x1112;       
    /* MSG_LENGTH|RELAY_NOT_FOUND */
    public final static int RELAY_UPDATED = 0x1113;         
    /* MSG_LENGTH|RELAY_UPDATED */
    
    
    /* <---- Peer With the Relay Server ----> */
    public final static byte IS_SERVER = 0x0A;
    /* client -> server: IS_SERVER|CERTIFICATE.length|CERTIFICATE
     server -> client: MSG_LENGTH|timestamp.length|E(P_peer)[timestamp]
     client -> server: timestamp
     */
    public final static byte KEEPALIVE_INTERVAL = 0x08;
    public final static byte SERVER_NOT_REGISTERD = 0x07;
	public final static byte IS_CLIENT = 0x0B;              
    public final static byte SERVER_IS_ALIVE = 0x0C;
	public final static byte SERVER_GOT_REQUEST = 0x0D;
	public final static byte CLIENT_CONNECTED_TO_SERVER = 0x0E;
	public final static byte CLIENT_NOT_CONNECTED_TO_SERVER = 0x0F;
    public final static byte SERVER_ACCEPTS_REQUEST = 0x09;
    
    
    /* <---- TLSSocket ----> */
    public final static byte TLS_CERTIFICATE_RETRIEVED = 0x10;
    public final static byte TLS_CERTIFICATE_NOT_RETRIEVED = 0x11;
    public final static byte TLS_SESSIONKEY_RETRIEVED = 0x20;
    public final static byte TLS_SESSIONKEY_NOT_RETRIEVED = 0x21;
    
}
