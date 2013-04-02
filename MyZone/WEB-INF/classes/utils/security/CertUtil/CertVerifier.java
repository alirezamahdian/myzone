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

package utils.security.CertUtil;

import java.io.*;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import utils.attributes.*;
import utils.security.KeyPairUtil.*;
import utils.security.CertUtil.CertAuthUtil.*;

/*
 This file implements the class CertVerifier. The main purpose of this class is
 to verify the authenticity of the certificate of a certificate authority or 
 the certificate of another user.
 */

public class CertVerifier{
    
    private final static boolean DEBUG = false;
    private globalAttributes globalProperties = new globalAttributes();
	
    // this function converts a 32 bit integer to a byte array of size 4.
    private static final byte[] intToByteArray(int value) {
        return new byte[] {
			(byte)(value >>> 24),
			(byte)(value >>> 16),
			(byte)(value >>> 8),
			(byte)value};
	}
	
    // this function converts a byte array of size 4 into a 32 bit integer.
	private static final int byteArrayToInt(byte [] b) {
        return (b[0] << 24)
		+ ((b[1] & 0xFF) << 16)
		+ ((b[2] & 0xFF) << 8)
		+ (b[3] & 0xFF);
	}
    
    // this functions verifies the certificate of a certificate authority(CA).
    // the CA certificate is given in the form of a byte array stored in certBytes.
    // the keyAlgorithm is the specified asymmetric encryption algorithm used to
    // generate the public and private keys. finally, sigAlgorithm is the algorithm used
    // to sign the certificate. note that a CA would generate its own public and private
    // keys and generates its own certificate and signs it.
    // upon successful verification of the CA's certificate its public key will be stored
    // in the certPath directory using the following filename format: CA@caName.pub
    public boolean verifyCertAuth(byte[] certBytes, String certPath, String keyAlgorithm, String sigAlgorithm){
        if (certPath == null)
            return false;
		try{
            boolean verified = false;
            KeyPairUtil z = new KeyPairUtil();
            if (certPath == null){
                return false;
            }
            CertAuthUtil x = new CertAuthUtil();
            byte[] lenBytes = new byte[4];
            
            System.arraycopy(certBytes, 0, lenBytes, 0, 4);
            byte[] caNameBytes = new byte[byteArrayToInt(lenBytes)];
            System.arraycopy(certBytes, 12, caNameBytes, 0, caNameBytes.length);
            String caName = new String(caNameBytes, "UTF-8");
            
            System.arraycopy(certBytes, 4, lenBytes, 0, 4);
            byte[] publicKeyBytes = new byte[byteArrayToInt(lenBytes)];
            System.arraycopy(certBytes, 12 + caNameBytes.length, publicKeyBytes, 0, publicKeyBytes.length);
            
            System.arraycopy(certBytes, 8, lenBytes, 0, 4);
            byte[] sigBytes = new byte[byteArrayToInt(lenBytes)];
            System.arraycopy(certBytes, 12 + caNameBytes.length + publicKeyBytes.length, sigBytes, 0, sigBytes.length);
            Security.addProvider(new BouncyCastleProvider());
            KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm, "BC");
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
            Signature sig = Signature.getInstance(sigAlgorithm, "BC");
            sig.initVerify(publicKey);
            sig.update(caName.getBytes("UTF-8"));
            sig.update(publicKeyBytes);
            verified = sig.verify(sigBytes);
            if (verified){
                if (!certPath.equals("./")){
                    if (!(new File(certPath).exists())){
                        boolean success = (new File(certPath)).mkdir();
                        if (!success)
                            return false;
                    }
                }
                FileOutputStream CAKey = new FileOutputStream(certPath + "CA@"+caName+".pub");
                CAKey.write(publicKeyBytes);
                CAKey.close();
            }else{
                if (DEBUG){
                    System.out.println("DEBUG: line 112 of CertVerifier.java. Certificate Authority Could not be verified.");
                }
            }
            return verified;
        }catch(NoSuchAlgorithmException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(InvalidKeySpecException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(IOException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(InvalidKeyException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(SignatureException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(java.security.NoSuchProviderException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
		return false;
	}
	
    // reads the content of a file and returns it as a byte array.
	public byte[] readRawFile(String path, String fileName){
		try{
            byte[] fileBytes = null;
            if (path == null){
                return null;
            }
            if (!(new File(path + fileName).exists()))
                return null;
            // read in
            FileInputStream file = new FileInputStream(path + fileName);
            fileBytes = new byte[file.available()];  
            file.read(fileBytes);
            file.close();
            fileBytes = new sun.misc.BASE64Decoder().decodeBuffer(new String(fileBytes, "UTF-8"));
            return fileBytes;
        }catch(FileNotFoundException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(IOException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return null;
    }
	
    // this function verifies the authenticity of a certificate given as a byte array 
    // stored in certBytes, using the CA public key stored in the caPath directory and
    // the asymmetric encryption algorithm used to generate the public and private keys
    // and the sigAlgorithm used to sign the certificate by the CA.
	public userCertificate verifyCertificate(byte[] certBytes, String caPath, String keyAlgorithm, String sigAlgorithm){
        userCertificate usrCert = null;
		try{
            boolean verified = false;
            KeyPairUtil z = new KeyPairUtil();
            if (caPath == null){
                if (DEBUG){
                    System.out.println("DEBUG: line 179 of CertVerfier.java");
                }
                return null;
            }
            
            byte[] lenBytes = new byte[4];
            System.arraycopy(certBytes, 0, lenBytes, 0, 4);
            byte[] caNameBytes = new byte[byteArrayToInt(lenBytes)];
            System.arraycopy(certBytes, 16, caNameBytes, 0, caNameBytes.length);
            String caName = new String(caNameBytes, "UTF-8");
            System.arraycopy(certBytes, 4, lenBytes, 0, 4);
            byte[] clientNameBytes = new byte[byteArrayToInt(lenBytes)];
            System.arraycopy(certBytes, 16 + caNameBytes.length, clientNameBytes, 0, clientNameBytes.length);
            String clientName = new String(clientNameBytes, "UTF-8");
            System.arraycopy(certBytes, 8, lenBytes, 0, 4);
            byte[] publicKeyBytes = new byte[byteArrayToInt(lenBytes)];
            System.arraycopy(certBytes, 16 + caNameBytes.length + clientNameBytes.length, publicKeyBytes, 0, publicKeyBytes.length);
            
            System.arraycopy(certBytes, 12, lenBytes, 0, 4);
            byte[] sigBytes = new byte[byteArrayToInt(lenBytes)];
            System.arraycopy(certBytes, 16 + caNameBytes.length + clientNameBytes.length +publicKeyBytes.length, sigBytes, 0, sigBytes.length);
            
            byte[] caPubKeyBytes = readRawFile(caPath, "CA@" + caName + ".pub");
            if (caPubKeyBytes == null){
                if (DEBUG){
                    System.out.println("DEBUG: line 204 of CertVerfier.java");
                }
                return null;
            }
            Security.addProvider(new BouncyCastleProvider());
            KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm, "BC");
            EncodedKeySpec caKeySpec = new X509EncodedKeySpec(caPubKeyBytes);
            PublicKey caPubKey = keyFactory.generatePublic(caKeySpec);
            Signature sig = Signature.getInstance(sigAlgorithm, "BC");
            
            sig.initVerify(caPubKey);
            sig.update(caNameBytes);
            sig.update(clientNameBytes);
            sig.update(publicKeyBytes);
            verified = sig.verify(sigBytes);
            
            if (verified){
                PublicKey key = null;
                // The bytes can be converted back to public and private key objects
                keyFactory = KeyFactory.getInstance(keyAlgorithm, "BC");
                EncodedKeySpec KeySpec = new X509EncodedKeySpec(publicKeyBytes);
                key = keyFactory.generatePublic(KeySpec);
                usrCert = new userCertificate();
                usrCert.publicKey = key;
                usrCert.username = clientName;
                return usrCert;
            }
            else{
                return usrCert;
            }
        }catch(NoSuchAlgorithmException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(InvalidKeySpecException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(InvalidKeyException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(SignatureException e){
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
        return usrCert;
		
	}
	
}
