package backend;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

/*
 * To serialize and deserialize the key
 */
public class Serialization {
	ConstantValues CV = new ConstantValues();
	
	public Serialization(){
		
	}
	
	/**
	 * return string of "1234|5678" 
	 * where 1234 is publicKeyModulus and 5678 is publicKeyExponent
	 * @param kp keypair
	 * @return serialized keystring
	 */
	public String SerializePublicKey(RSAPublicKey publicKey){
		return publicKey.getModulus().toString() + "|" +
		    publicKey.getPublicExponent().toString();
	}
		
	
	public String SerializeByteKey(byte[] bytePubKey){
		X509EncodedKeySpec pubspec = new X509EncodedKeySpec(bytePubKey);
		KeyFactory factory;
		RSAPublicKey publicKey = null;
		try {
			factory = KeyFactory.getInstance("RSA");
			publicKey = (RSAPublicKey) factory.generatePublic(pubspec);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return publicKey.getModulus().toString() + "|" +
		    publicKey.getPublicExponent().toString();
	}
	
	/**
	 * Serialize the private key into string
	 * @param privateKey
	 * @return the string of "modulus|exponent" of private key
	 */
	public String SerializePrivateKey(RSAPrivateKey privateKey){
		return privateKey.getModulus().toString() + "|" +
		privateKey.getPrivateExponent().toString();
	}
	
	/**
	 * Deserialize keystring to RSAPublicKey 
	 * @param keyString
	 * @return RSAPublicKey
	 */
	public RSAPublicKey DeserializePublicKey(String pubKeyString){
		String []Parts = pubKeyString.split("\\|");
		RSAPublicKeySpec Spec = new RSAPublicKeySpec(
		        new BigInteger(Parts[0]),
		        new BigInteger(Parts[1]));
		RSAPublicKey pubkey = null;
		
		try {
			pubkey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(Spec);
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return pubkey;
	}
	
	
	/**
	 * Function to combine the two byte arrays into one byte array 
	 * @param package1 byte[]
	 * @param package2 byte[]
	 * @return
	 */
	public byte[] combineThePackage(byte[] package1, byte[] package2){
		byte[] newPack = new byte[package1.length + package2.length];
		
		System.arraycopy(package1, 0, newPack, 0, package1.length);
		System.arraycopy(package2, 0, newPack, package1.length, package2.length);
		
		return newPack;
	}

	
	//get the expiration date from the certificate
	public byte[] getExpFromCert(byte[] cert){
		byte[] exp = new byte[CV.EXP_LENGTH];
		System.arraycopy(cert, CV.EXPIRATIONSTARTPOS, exp, 0, CV.EXP_LENGTH);
		return exp;
	}

	//get public key from certificate
	public byte[] getPublicKeyFromCert(byte[] cert){
		byte[] pubKey = new byte[CV.RSAPUBLICKEYLENGTH];
		System.arraycopy(cert, 1, pubKey, 0, CV.RSAPUBLICKEYLENGTH);
		return pubKey;		
	}


}
