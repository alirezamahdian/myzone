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


/*
 This file includes all the global variables and configurations needed to
 run the infrustructure.
 */
package utils.attributes;

import MyZone.Settings;

public class globalAttributes{
    public Settings mainSettings;
    public final String messageDigestAlgorithm = "SHA-256";
    public final int messageDigestSize = 32;
    public final String keyPairAlgorithm = "RSA";
    public final String asymCipher = "RSA/NONE/PKCS1PADDING";
    public final int certificateKeySize = 1024;
    public final int plainBlockSize = 117;
    public final String certSigAlgorithm = "MD5WITHRSA";
    public final String sessionKeyType = "AES";
    public final int sessionKeySize = 128;
    public final String sessionCipher = "AES/ECB/PKCS5Padding";
    public final int PUBLIC_IP = 1;
    public final int FULL_CONE_NAT = 2;
    public final int SYMMETRIC_NAT = 3;
    public final int TCP = 1;
    public final int UDP = 2;
    public final int caPort = 81;
    public final String caAddress = "www.joinmyzone.com";
    public String caName;
    public String rendezvousServerAddress;
    public int rendezvousServerPort;
    public String stunServerAddress;
    public int stunServerPort;
    public String myCertPath;
    public String caCertPath;
    public String myKeyPath;
    public globalAttributes(){
        
    }
    
    public void init(String prefix){
        mainSettings = new Settings(prefix);
        mainSettings.refresh(mainSettings.BASIC_INFO);
        caName = mainSettings.CAServerName;
        rendezvousServerAddress = mainSettings.rendezvousServerAddress;
        rendezvousServerPort = mainSettings.rendezvousServerPort;
        stunServerAddress = mainSettings.STUNServerAddress;
        stunServerPort = mainSettings.STUNServerPort;
        myCertPath = prefix + mainSettings.username + "/cert/";
        caCertPath = prefix + "CAs/";
        myKeyPath = prefix + mainSettings.username + "/keys/";
    }
}
