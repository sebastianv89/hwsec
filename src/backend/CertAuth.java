package backend;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;




/** Certificate authority
 * Will generate signatures ("certificates" in our document) 
 * */
public class CertAuth {

	public enum TYPE {
		SMARTCARD, RENTALTERM, VEHICLETERM
	};

//	private RSAPublicKey capubkey;
	private static RSAPrivateKey caprivkey;
	
	public CertAuth() {
			String CAPrivateKeyFile = "CAPrivateKey"; // Path to the CA private key

			// Get the private key from file.
			caprivkey = (RSAPrivateKey) readPrivateKey(CAPrivateKeyFile);

	}

//	public byte[] getVerificationKey() {
//		return verifKey.clone();
//	}

	public byte[] makeCert(TYPE type, RSAPublicKey publicKey) {
		// TODO: encode certificate
		byte[] encoded = new byte[64];
		return signRaw(encoded);
	}

	public byte[] makeCert(TYPE type, RSAPublicKey publicKey, long exp) {
		// TODO: encode certificate
		byte[] encoded = new byte[64];
		return signRaw(encoded);
	}

	/** Sign a raw piece of bytes
	 * 
	 * To verify use:
	 * Signature sig = Signature.getInstance("MD5WithRSA");
	 * sig.initVerify(capubkey);
	 * sig.update(rawData);
	 * sig.verify(signRaw(rawData))
	 * 
	 * @input byte array rawData
	 * @return byte array signature
	 */
	public static byte[] signRaw(byte[] rawData) {
		byte[] signatureBytes = null; // This will contain the signature
	    try {
	    	Signature sig = Signature.getInstance("MD5WithRSA");
			sig.initSign(caprivkey);
		    sig.update(rawData);
		    signatureBytes = sig.sign();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
		return signatureBytes;
	}

	
   public static RSAPrivateKey readPrivateKey(String filename) { 
	    FileInputStream file;
	    RSAPrivateKey privkey = null;
		try {
			file = new FileInputStream(filename);
			byte[] bytes = new byte[file.available()];
			file.read(bytes);
			file.close();
			privkey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
  
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return privkey;
   }
   
   /* This function is not used here but you can copy-paste it to your other classes if you need to read a public keyfile
    * 	   public static RSAPublicKey readPublicKey(String filename) { 
	   RSAPublicKey pubkey = null;
	   FileInputStream file;
		try {
			file = new FileInputStream(filename);
			byte[] bytes = new byte[file.available()];
			file.read(bytes);
			file.close();
			X509EncodedKeySpec pubspec = new X509EncodedKeySpec(bytes);
			KeyFactory factory = KeyFactory.getInstance("RSA");
			pubkey = (RSAPublicKey) factory.generatePublic(pubspec);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pubkey;
   }
    */

   
   
}
