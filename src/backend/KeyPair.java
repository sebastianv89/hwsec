package backend;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;

/**
 * Wrapper class for a public crypto keypair.
 * 
 * Make a new keypair to generate new keys. These are not stored anywhere so make sure
 * to store the public key in the database (this should only be done by the backend!!!)
 */
public class KeyPair {
	
//	private int KEYSIZE = 32;

	private RSAPublicKey publickey;
	private RSAPrivateKey privatekey;

	/**
	 * Generate a new random public keypair
	 */
	public KeyPair() {
		
	      try {
	          /* Generate keypair. */
	          KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
	          generator.initialize(1024);
	          java.security.KeyPair keypair = generator.generateKeyPair();
	          publickey = (RSAPublicKey)keypair.getPublic();
	          privatekey = (RSAPrivateKey)keypair.getPrivate();

	       } catch (Exception e) {
	          e.printStackTrace();
	       }
		
	}
	
	/**
	 * Generate a new keypair, computes the corresponding public key 
	 * 
	 * Max: This wont work? we can't derive the private key from the pubkey if we use official RSA keys
	 * 
	 * @param secretKey secret key of the keypair
	 */
//	public KeyPair(byte[] privateKey) {
//		this.privateey = privateKey;
//		derivePublicKey();
//	}
	
	/**	Initialize a keypair with the provided values 
	 * 
	 * */
	public KeyPair(byte[] privateKey, byte[] publicKey) {
		//TODO: Decode the byte arrays into RSA...Key
//		this.privatekey = privateKey;
//		this.publickey = publicKey;
	}

	public RSAPrivateKey getPrivate() {
		return privatekey;
	}

	public RSAPublicKey getPublic() {
		return publickey;
	}

}