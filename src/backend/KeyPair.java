package backend;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;

/**
 * Wrapper class for a public crypto keypair.
 */
public class KeyPair {
	
//	private int KEYSIZE = 32;

	private RSAPublicKey publickey;
	private RSAPrivateKey privatekey;

	/**
	 * Generate a new random public keypair
	 */
	public KeyPair() {
//		privateKey = new byte[KEYSIZE];
//		FileInputStream urandom = null;
//		try {
//			urandom = new FileInputStream("/dev/urandom");
//			// read from /dev/urandom until the buffer privateKey is full
//			urandom.read(privateKey);
//		} catch (IOException e) {
//			System.err.println("Unable to read from /dev/urandom: "
//					+ e.getMessage());
//			System.exit(1);
//		} finally {
//			try {
//				if (urandom != null) {
//					urandom.close();
//				}
//			} catch (IOException e) {
//				// not really a problem
//				System.err.println("Unable to close /dev/unrandom: "
//						+ e.getMessage());
//			}
//		}
//		derivePublicKey();
		
	      try {
	          /* Generate keypair. */
	          KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
	          generator.initialize(1024);
	          java.security.KeyPair keypair = generator.generateKeyPair();
	          RSAPublicKey publickey = (RSAPublicKey)keypair.getPublic();
	          RSAPrivateKey privatekey = (RSAPrivateKey)keypair.getPrivate();

	          //TODO: Now we have a pub and priv key, how to store that?

	          System.out.println("modulus = " + publickey.getModulus());
	          System.out.println("pubexpint = " + publickey.getPublicExponent());
	          System.out.println("privexpint = " + privatekey.getPrivateExponent());
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
	 * Max: I think this can go as well
	 * 
	 * */
//	public KeyPair(byte[] privateKey, byte[] publicKey) {
//		this.privatekey = privateKey;
//		this.publickey = publicKey;
//	}

	public RSAPrivateKey getPrivate() {
		return privatekey;
	}

	public RSAPublicKey getPublic() {
		return publickey;
	}
	
	private void derivePublicKey() {
		// TODO: implement
		publicKey = new byte[KEYSIZE];
	}

}