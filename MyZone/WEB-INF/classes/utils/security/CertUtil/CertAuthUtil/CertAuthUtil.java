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

package utils.security.CertUtil.CertAuthUtil;

import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import sun.misc.BASE64Encoder;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import utils.attributes.*;
import utils.security.KeyPairUtil.*;
import utils.security.CertUtil.*;


// this file implements the CertAuthUtil class which implements two utility 
// functions needed for a certificate authority:
// 1. generateSelfCertificate: generates its own certificate and signs it.
// 2. issueCertificate: issues certificate for other users.
public class CertAuthUtil {
    
    private final static boolean DEBUG = false;
    private globalAttributes globalProperties = new globalAttributes();;
    
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
    
    // this function generates a pair of keys of size keySize using the
    // keyAlgorithm and stores them in the keyPath directory while the actual
    // certificate is stored in the certPath directory using the filename
    // caName.cert. the sigAlgorithm is used to sign the certificate.s
    public void generateSelfCertificate(String keyPath, String certPath, String caName, String keyAlgorithm, int keySize, String sigAlgorithm){
		try{
            if (keyPath == null || certPath == null)
                return;
            if (!certPath.equals("./")){
                if (!(new File(certPath).exists())){
                    boolean success = (new File(certPath)).mkdir();
                    if (!success)
                        return;
                }
            }
            
            File file = new File(keyPath + caName + ".pub");
            boolean pubKeyExists = file.exists();
            file = new File(keyPath + caName + ".pri");
            boolean priKeyExists = file.exists();
            KeyPairUtil x = new KeyPairUtil();
            KeyPair keys = null;
            PublicKey publicKey = null;
            PrivateKey privateKey = null;
            
            if (!pubKeyExists || !priKeyExists) {
                keys = x.generateKeys(keyPath, caName, keyAlgorithm, keySize);
                publicKey = keys.getPublic();
                privateKey = keys.getPrivate();
            }else{
                publicKey = x.readPubKey(keyPath, caName, keyAlgorithm);
                privateKey = x.readPriKey(keyPath, caName, keyAlgorithm);
            }
            
            byte[] publicKeyBytes = publicKey.getEncoded();
            
            FileOutputStream certFile = new FileOutputStream(certPath + caName + ".cert");
            Security.addProvider(new BouncyCastleProvider());
            Signature sig = Signature.getInstance(sigAlgorithm, "BC");
            sig.initSign(privateKey);
            sig.update(caName.getBytes("UTF-8"));
            sig.update(publicKeyBytes);
            byte[] signatureBytes = sig.sign();
            /* CA Certificate Format */
            // caName.length()+publicKeyBytes.length+signatureBytes.length+caName+publicKeyBytes+signatureBytes 
            byte[] a = new byte[12 + caName.length() + publicKeyBytes.length + signatureBytes.length];
            System.arraycopy(intToByteArray(caName.getBytes("UTF-8").length), 0, a, 0, 4);
            System.arraycopy(intToByteArray(publicKeyBytes.length), 0, a, 4, 4);
            System.arraycopy(intToByteArray(signatureBytes.length), 0, a, 8, 4);
            System.arraycopy(caName.getBytes("UTF-8"), 0, a, 12, caName.getBytes("UTF-8").length);
            System.arraycopy(publicKeyBytes, 0, a, 12 + caName.getBytes("UTF-8").length, publicKeyBytes.length);
            System.arraycopy(signatureBytes, 0, a, 12 + caName.getBytes("UTF-8").length + publicKeyBytes.length, signatureBytes.length);
            String str = new sun.misc.BASE64Encoder().encode(a);
            certFile.write(str.getBytes("UTF-8"));
            certFile.close();
		}catch(NoSuchAlgorithmException e){
            if (DEBUG){
                e.printStackTrace();
            }
        }catch(FileNotFoundException e){
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
    }
	
	// this function issues a certificate stored in a byte array.
    // the information included in the certificate are the username
    // of the certificate requester in addition to their public key stored as
    // a byte array in clientPubKey. the keyAlgorithm is needed in order to
    // read the public and private keys of the CA from raw files.
    // the certificate is signed using sigAlgorithm.
	public byte[] issueCertificate(String caKeyPath, String caName, String clientName, byte[] clientPubKey, String keyAlgorithm, String sigAlgorithm){
		try{
            byte[] cert = null;
            KeyPairUtil x = new KeyPairUtil();
            PrivateKey caPriKey = x.readPriKey(caKeyPath, caName, keyAlgorithm);
            PublicKey caPubKey = x.readPubKey(caKeyPath, caName, keyAlgorithm);
            Security.addProvider(new BouncyCastleProvider());
            Signature sig = Signature.getInstance(sigAlgorithm, "BC");
            sig.initSign(caPriKey);
            
            /* Certificate Format */
            // caName.length()+clientName.length()+clientPubKey.length+signatureBytes.length+caName+clientName+clientPubKey+signatureBytes 
            
            sig.update(caName.getBytes("UTF-8"));
            sig.update(clientName.getBytes("UTF-8"));
            sig.update(clientPubKey);
            byte[] signatureBytes = sig.sign();
            /* append the signature to the certificate */
            cert = new byte[caName.getBytes("UTF-8").length + clientName.getBytes("UTF-8").length + clientPubKey.length + signatureBytes.length + (4 * 4) ];
            byte[] lenBytes = intToByteArray(caName.getBytes("UTF-8").length);
            System.arraycopy(lenBytes, 0, cert, 0, 4);
            lenBytes = intToByteArray(clientName.getBytes("UTF-8").length);
            System.arraycopy(lenBytes, 0, cert, 4, 4);
            lenBytes = intToByteArray(clientPubKey.length);
            System.arraycopy(lenBytes, 0, cert, 8, 4);
            lenBytes = intToByteArray(signatureBytes.length);
            System.arraycopy(lenBytes, 0, cert, 12, 4);
            System.arraycopy(caName.getBytes("UTF-8"), 0, cert, 16, caName.getBytes("UTF-8").length);
            System.arraycopy(clientName.getBytes("UTF-8"), 0, cert, 16 + caName.getBytes("UTF-8").length, clientName.getBytes("UTF-8").length);
            System.arraycopy(clientPubKey, 0, cert, 16 + caName.getBytes("UTF-8").length + clientName.getBytes("UTF-8").length, clientPubKey.length);
            System.arraycopy(signatureBytes, 0, cert, 16 + caName.getBytes("UTF-8").length + clientName.getBytes("UTF-8").length + clientPubKey.length, signatureBytes.length);
            String str = new sun.misc.BASE64Encoder().encode(cert);
            cert = str.getBytes("UTF-8");
            return cert;
        }catch(NoSuchAlgorithmException e){
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
        }catch(UnsupportedEncodingException e){
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
}



