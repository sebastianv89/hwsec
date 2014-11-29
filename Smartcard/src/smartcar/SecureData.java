package smartcar;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.KeyBuilder;
import javacard.security.RSAPrivateKey;
import javacard.security.RSAPublicKey;
import javacard.security.RandomData;
import javacard.security.Signature;
import javacardx.crypto.Cipher;

//TODO: transactions

/**
 * Class for holding all persistent security data
 */
public class SecureData {

	// sizes of data
	public static final short SIZE_RSA_KEY_PRIV_EXP = 128;
	public static final short SIZE_RSA_KEY_MOD = 128;
	public static final short SIZE_RSA_KEY_PUB_EXP = 3;
	public static final short SIZE_RSA_SIG = 128;
	public static final short SIZE_NONCE = 16;
	public static final short SIZE_CERT_TYPE = 1;
	public static final short SIZE_CERT_EXP = 8;
	public static final short SIZE_CERT_DATA_CARD = (short) ((short) (SIZE_CERT_TYPE + SIZE_RSA_KEY_MOD) + SIZE_CERT_EXP);
	public static final short SIZE_CERT_DATA_TERM = (short) (SIZE_CERT_TYPE + SIZE_RSA_KEY_MOD);
	public static final short SIZE_CERT_CARD = (short) (SIZE_CERT_DATA_CARD + SIZE_RSA_SIG);
	public static final short SIZE_CERT_TERM = (short) (SIZE_CERT_TYPE + SIZE_RSA_SIG);
	public static final short SIZE_PUBENC_PLAIN = (short) (SIZE_RSA_KEY_MOD - 11);
	public static final short SIZE_PUBENC_CIPH = 128;

	private static final byte[] RSA_PUB_EXP = { 0x01, 0x00, 0x01 };

	// permanent crypto objects
	private RSAPrivateKey signatureKey; // TODO: replace completely with signer
	private RSAPublicKey caVerificationKey; // TODO: replace completely with
	// caVerifier
	private byte[] certificate; // raw data
	private RandomData rng;

	private Signature signer;
	private Signature caVerifier;
	private Cipher termEncrypter;

	// transient crypto objects
	private RSAPublicKey[] termEncryptKey;

	/** Constructor, allocates data, initializes crypto */
	SecureData() {
		// allocate data
		certificate = new byte[SIZE_CERT_CARD];
		termEncryptKey = (RSAPublicKey[]) JCSystem.makeTransientObjectArray(
				(short) 1, JCSystem.CLEAR_ON_DESELECT);

		// initialize crypto
		signatureKey = (RSAPrivateKey) KeyBuilder.buildKey(
				KeyBuilder.TYPE_RSA_PRIVATE, KeyBuilder.LENGTH_RSA_1024, false);
		caVerificationKey = (RSAPublicKey) KeyBuilder.buildKey(
				KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, false);
		// set the constant public exponent for the CAKey
		caVerificationKey.setExponent(RSA_PUB_EXP, (short) 0,
				SIZE_RSA_KEY_PUB_EXP);

		// initialize "the workers"
		caVerifier = Signature.getInstance(Signature.ALG_RSA_MD5_PKCS1, false);
		signer = Signature.getInstance(Signature.ALG_RSA_MD5_PKCS1, false);
		termEncrypter = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
		rng = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
	}

	void publicEncrypt(byte[] plaintext, byte counter, byte[] ciphertext) {
		short inOffset = (short) (counter * SIZE_PUBENC_PLAIN);
		short inSize = (counter == 4 ? 1024 : SIZE_PUBENC_PLAIN); // FIXME:
																	// length
		termEncrypter.doFinal(plaintext, inOffset, inSize, ciphertext,
				(short) 0);
	}

	/** Create the response for mutual authentication in the tmp buffer */
	void createAuthResponse(byte[] buffer) {
		// place the certificate
		Util.arrayCopy(certificate, (short) 0, buffer, (short) 0,
				SIZE_CERT_CARD);

		// generate temporary key
		rng.generateData(buffer, (short) (SIZE_CERT_CARD + SIZE_NONCE),
				SIZE_NONCE);
		// sign nonce + tmpkey
		signer.sign(buffer, SIZE_CERT_CARD, (short) (2 * SIZE_NONCE), buffer,
				(short) (SIZE_CERT_CARD + (short) (2 * SIZE_NONCE)));
	}

	/** Validate a terminal certificate */
	boolean validateCert(byte[] data, short dataOfs, byte[] sig, short sigOfs) {
		return caVerifier.verify(data, dataOfs, SIZE_CERT_DATA_TERM, sig,
				sigOfs, SIZE_RSA_SIG);
	}

	/** Initialize the workers after personalization */
	void init() {
		signer.init(signatureKey, Signature.MODE_SIGN);
		caVerifier.init(caVerificationKey, Signature.MODE_VERIFY);
	}

	/** Set the (public) key modulus (N) for the encryption key */
	void setTermEncryptKeyMod(byte[] buffer, short offset) {
		termEncryptKey[0].setModulus(buffer, offset, SIZE_RSA_KEY_MOD);
		termEncryptKey[0].setExponent(RSA_PUB_EXP, (short) 0,
				SIZE_RSA_KEY_PUB_EXP);
		termEncrypter.init(termEncryptKey[0], Cipher.MODE_ENCRYPT);
	}

	/** Set the (private) key exponent (d) for the signature key */
	void setSignKeyExp(byte[] buffer, short offset) {
		signatureKey.setExponent(buffer, offset, SIZE_RSA_KEY_PRIV_EXP);
	}

	/** Set the (public) key modulus (N) for the signature key */
	void setSignKeyMod(byte[] buffer, short offset) {
		signatureKey.setModulus(buffer, offset, SIZE_RSA_KEY_MOD);
	}

	/** Set the (public) key modulus (N) for the CA verification key */
	void setCAVerifKeyMod(byte[] buffer, short offset) {
		caVerificationKey.setModulus(buffer, offset, SIZE_RSA_KEY_MOD);
	}

	/** Set the data part of the smartcard certificate */
	void setCertData(byte[] buffer, short ofs) {
		Util
				.arrayCopy(buffer, ofs, certificate, (short) 0,
						SIZE_CERT_DATA_CARD);
	}

	/** Set the CA signature part of the certificate */
	void setCertSig(byte[] buffer, short ofs) {
		Util.arrayCopy(buffer, ofs, certificate, SIZE_CERT_DATA_CARD,
				SIZE_RSA_SIG);
	}

}
