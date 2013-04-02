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

package utils.security.KeyPairUtil;

import java.io.*;
import java.security.*;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import utils.attributes.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;



/*
 This class implements the utility functions needed to generate a pair of keys
 and extracting the public and private keys stored in raw binary files.
 path is the path to the directoy where the public and private keys are stored. 
 the username is the name of the user who is creating the private and public 
 keys and is used to name the respective files for private and public keys:
 the private key is stored in username.pri whereas the public key is stored in
 username.pub file. keyAlgorithm is the asymmetric algorithm used to generate
 the keys and numBit is the number of bits of the keys.
 */

public class KeyPairUtil {
    
    private final static boolean DEBUG = false;
    public KeyPairUtil(){
	}
	
    public KeyPair generateKeys(String path, String username, String keyAlgorithm, int numBits){
		try{
            if (path == null)
                return null;
            if (!path.equals("./")){
                if (!(new File(path).exists())){
                    boolean success = (new File(path)).mkdirs();
                    if (!success)
                        return null;
                }
            }
            
            
            KeyPair keyPair = null;
            
            // Get the public/private key pair
            Security.addProvider(new BouncyCastleProvider());
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(keyAlgorithm, "BC");
            keyGen.initialize(numBits);
            keyPair = keyGen.genKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey  publicKey  = keyPair.getPublic();
            if (DEBUG){
                System.out.println("DEBUG: line 77 of KeyPairUtil.java. Generating key/value pair using " + privateKey.getAlgorithm() + " algorithm");
            }
            
            // Get the bytes of the public and private keys
            byte[] privateKeyBytes = privateKey.getEncoded();
            byte[] publicKeyBytes  = publicKey.getEncoded();
            
            String privateKeyStr = new sun.misc.BASE64Encoder().encode(privateKeyBytes);
            String publicKeyStr = new sun.misc.BASE64Encoder().encode(publicKeyBytes);
            
            
            // Store the public and private keys in separate files
            
            FileOutputStream pubKeyFile = new FileOutputStream(path + username + ".pub");
            pubKeyFile.write(publicKeyStr.getBytes("UTF-8"));
            pubKeyFile.close();
            
            FileOutputStream priKeyFile = new FileOutputStream(path + username + ".pri");
            priKeyFile.write(privateKeyStr.getBytes("UTF-8"));
            pubKeyFile.close();
            
            return keyPair;
        }catch(NoSuchAlgorithmException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(FileNotFoundException e){
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
	
	public PublicKey readPubKey( String path, String username, String keyAlgorithm)
	{
		PublicKey key = null;
        try{
            byte[] KeyBytes = null;
            if (path == null)
                return null;
            if (username == null)
                return null;
            
            FileInputStream KeyFile = new FileInputStream(path + username + ".pub");
            KeyBytes = new byte[KeyFile.available()];  
            KeyFile.read(KeyBytes);
            KeyFile.close();
            
            KeyBytes = new sun.misc.BASE64Decoder().decodeBuffer(new String(KeyBytes, "UTF-8"));
            // The bytes can be converted back to public and private key objects
            Security.addProvider(new BouncyCastleProvider());
            KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm, "BC");
            EncodedKeySpec KeySpec = new X509EncodedKeySpec(KeyBytes);
            key = keyFactory.generatePublic(KeySpec);
        }catch(InvalidKeySpecException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(NoSuchAlgorithmException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(FileNotFoundException e){
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
        return key;
    }
	
    public PrivateKey readPriKey( String path, String username, String keyAlgorithm){
        PrivateKey key = null;
		try{
            byte[] KeyBytes = null;
            if (path == null)
                return null;
            if (username == null)
                return null;
            FileInputStream KeyFile = new FileInputStream(path + username + ".pri");
            KeyBytes = new byte[KeyFile.available()];  
            KeyFile.read(KeyBytes);
            KeyFile.close();
            
            KeyBytes = new sun.misc.BASE64Decoder().decodeBuffer(new String(KeyBytes, "UTF-8"));
            Security.addProvider(new BouncyCastleProvider());
            // The bytes can be converted back to public and private key objects
            KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm, "BC");
            EncodedKeySpec KeySpec = new PKCS8EncodedKeySpec(KeyBytes);
            key = keyFactory.generatePrivate(KeySpec);
        }catch(InvalidKeySpecException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(NoSuchAlgorithmException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(FileNotFoundException e){
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
        return key;
    }
}


