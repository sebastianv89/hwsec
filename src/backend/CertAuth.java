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
import java.security.spec.X509EncodedKeySpec;




/** Certificate authority
 * Will generate signatures ("certificates" in our document) 
 * */
public class CertAuth {

	public enum TYPE {
		SMARTCARD, RENTALTERM, VEHICLETERM
	};

	private RSAPublicKey capubkey;
	private RSAPrivateKey caprivkey;
	private String CAPrivateKeyFile = "CAPrivateKey"; // Path to the CA private key
	private String CAPublicKeyFile = "CAPublicKey";
	
	public CertAuth() {
			// Get the private key from file.
			readPrivateKey(CAPrivateKeyFile);
			readPublicKey(CAPublicKeyFile);

	}

	public RSAPublicKey getVerificationKey() {
		return capubkey;
	}

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

	/* Sign a raw piece of bytes
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
	public byte[] signRaw(byte[] rawData) {
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

	
   private void readPrivateKey(String filename) { 
	    FileInputStream file;
		try {
			file = new FileInputStream(filename);
			byte[] bytes = new byte[file.available()];
			file.read(bytes);
			file.close();
			caprivkey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
  
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
   }
   
   
   private void readPublicKey(String filename) { 
	   FileInputStream file;
		try {
			file = new FileInputStream(filename);
			byte[] bytes = new byte[file.available()];
			file.read(bytes);
			file.close();
			X509EncodedKeySpec pubspec = new X509EncodedKeySpec(bytes);
			KeyFactory factory = KeyFactory.getInstance("RSA");
			capubkey = (RSAPublicKey) factory.generatePublic(pubspec);
			
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
   }
   
   
}
