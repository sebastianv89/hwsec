package backend;

import java.io.FileInputStream;
import java.io.IOException;

public class SecretKey {
	
	private int KEYSIZE = 32;
	
	public byte[] secretKey;
	
	/**
	 * Generate a new random secret key
	 */
	public SecretKey() {
		secretKey = new byte[KEYSIZE];
		
		FileInputStream urandom = null;
		try {
			urandom = new FileInputStream("/dev/urandom");
			// read from /dev/urandom until the buffer secretKey is full
			urandom.read(secretKey);
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
				// not really a problem...
				System.err.println("Unable to close /dev/unrandom: "
						+ e.getMessage());
			}
		}
	}
	
	public SecretKey(byte[] secretKey) {
		this.secretKey = secretKey;
	}
}
