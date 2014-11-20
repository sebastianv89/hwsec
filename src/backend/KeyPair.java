package backend;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Wrapper class for a public crypto keypair.
 */
public class KeyPair {
	
	private int KEYSIZE = 32;

	private byte[] privateKey;
	private byte[] publicKey;

	/**
	 * Generate a new random public keypair
	 */
	public KeyPair() {
		privateKey = new byte[KEYSIZE];
		FileInputStream urandom = null;
		try {
			urandom = new FileInputStream("/dev/urandom");
			// read from /dev/urandom until the buffer privateKey is full
			urandom.read(privateKey);
		} catch (IOException e) {
			System.err.println("Unable to read from /dev/urandom: "
					+ e.getMessage());
			System.exit(1);
		} finally {
			try {
				if (urandom != null) {
					urandom.close();
				}
			} catch (IOException e) {
				// not really a problem
				System.err.println("Unable to close /dev/unrandom: "
						+ e.getMessage());
			}
		}
		derivePublicKey();
	}
	
	/**
	 * Generate a new keypair, computes the corresponding public key 
	 * 
	 * @param secretKey secret key of the keypair
	 */
	public KeyPair(byte[] privateKey) {
		this.privateKey = privateKey;
		derivePublicKey();
	}
	
	/**	Initialize a keypair with the provided values */
	public KeyPair(byte[] privateKey, byte[] publicKey) {
		this.privateKey = privateKey;
		this.publicKey = publicKey;
	}

	public byte[] getPrivate() {
		return privateKey;
	}

	public byte[] getPublic() {
		return publicKey;
	}
	
	private void derivePublicKey() {
		// TODO: implement
		publicKey = new byte[KEYSIZE];
	}

}