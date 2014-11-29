package backend;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
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

	ConstantValues cv = new ConstantValues();
	
	public enum TYPE {
		SMARTCARD ((byte)0), RENTALTERM ((byte)1), VEHICLETERM ((byte)2);
		
		public final byte code;
		TYPE(byte code) {
			this.code = code;
		}
		
		
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
	
	/*
	 * Certificate for the terminals should not have an expdate
	 * Certificate WITOUT expdate is a byte array that looks like:
	 * cert[0] = type (0 = smartcard, 1 = rentalterm, 2 = vehicleterm)
	 * cert[1..129] = public modulus (128 bytes)
	 * cert[130...248] = Signature (128bytes)
	 */
	public byte[] makeCert(TYPE type, RSAPublicKey publicKey) {
		byte[] byteTuple = new byte[1 + cv.PUBMODULUS];
		byteTuple[0] = type.code;
		byte[] pk = publicKey.getModulus().toByteArray();
		System.arraycopy(pk, 1, byteTuple, 1, pk.length - 1);
		
		byte[] signature = signRaw(byteTuple);
		
		byte[] cert = new byte[byteTuple.length + signature.length];
		System.arraycopy(byteTuple, 0, cert, 0, byteTuple.length);
		System.arraycopy(signature, 0, cert, byteTuple.length, signature.length);
		return cert;
	}

	/* TODO: Change exp to short
	 * Certificate with expdate is a byte array that looks like:
	 * cert[0] = type (0 = smartcard, 1 = rentalterm, 2 = vehicleterm)
	 * cert[1..129] = public modulus (128 bytes)
	 * cert[130...138] = expdate (long = 8bytes)
	 * cert[139...267] = Signature (128bytes)
	 */
	public byte[] makeCert(TYPE type, RSAPublicKey publicKey, long exp) {
		byte[] byteTuple = new byte[1 + cv.PUBMODULUS + cv.EXP_LENGTH];
		byteTuple[0] = type.code;
		byte[] pk = publicKey.getModulus().toByteArray(); 
		System.arraycopy(pk, 1, byteTuple, 1, pk.length - 1);
		byte[] expbytes = ByteBuffer.allocate(8).putLong(exp).array(); // long -> bytes
		
		System.arraycopy(expbytes, 0, byteTuple, pk.length, expbytes.length);
		
		byte[] signature = signRaw(byteTuple); //signature = 128bytes
		
		byte[] cert = new byte[1 + cv.PUBMODULUS + cv.EXP_LENGTH + signature.length];
		System.arraycopy(byteTuple, 0, cert, 0, byteTuple.length);
		System.arraycopy(signature, 0, cert, byteTuple.length, signature.length);
		return cert;
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
