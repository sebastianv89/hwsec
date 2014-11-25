package backend;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;

/** Certificate authority */
public class CertAuth {

	public enum TYPE {
		SMARTCARD, RENTALTERM, VEHICLETERM
	};

	private RSAPublicKey capubkey;
	private RSAPrivateKey caprivkey;

	public CertAuth() {
		// TODO: read keys from files
		try {
			/* Get the secret message from file. */
			FileInputStream plainfile = new FileInputStream(inFileName);
			byte[] plaintext = new byte[plainfile.available()];
			plainfile.read(plaintext);
			plainfile.close();

			/* Get the public key from file. */
			PublicKey publickey = readPublicKey("publickey");

			/* Create a cipher for encrypting. */
			Cipher encrypt_cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			encrypt_cipher.init(Cipher.ENCRYPT_MODE, publickey);

			/* Encrypt the secret message and store in file. */
			byte[] ciphertext = encrypt_cipher.doFinal(plaintext);
			FileOutputStream cipherfile = new FileOutputStream(outFileName);
			cipherfile.write(ciphertext);
			cipherfile.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public byte[] getVerificationKey() {
		return verifKey.clone();
	}

	public byte[] makeCert(TYPE type, byte[] publicKey) {
		// TODO: encode certificate
		byte[] encoded = new byte[64];
		return signRaw(encoded);
	}

	public byte[] makeCert(TYPE type, byte[] publicKey, long exp) {
		// TODO: encode certificate
		byte[] encoded = new byte[64];
		return signRaw(encoded);
	}

	private byte[] signRaw(byte[] rawData) {
		byte[] sig = new byte[64];
		// TODO: implement
		return sig;
	}

}
