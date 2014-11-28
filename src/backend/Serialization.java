package backend;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

/*
 * To serialize and deserialize the key
 */
public class Serialization {
	public Serialization(){
		
	}
	
	/**
	 * return string of "1234|5678" 
	 * where 1234 is publicKeyModulus and 5678 is publicKeyExponent
	 * @param kp keypair
	 * @return serialized keystring
	 */
	public String SerializePublicKey(KeyPair kp){
		RSAPublicKey publicKey = (RSAPublicKey)kp.getPublic();
		return publicKey.getModulus().toString() + "|" +
		    publicKey.getPublicExponent().toString();
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
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pubkey;
	}
	

}
