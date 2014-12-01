package smartcar;

import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.AESKey;
import javacard.security.CryptoException;
import javacard.security.KeyBuilder;
import javacard.security.RSAPrivateKey;
import javacard.security.RSAPublicKey;
import javacard.security.RandomData;
import javacard.security.Signature;
import javacardx.crypto.Cipher;

/**
 * Class for holding all persistent security data
 */
public class SecureData {

	// sizes of data
	public static final short SIZE_RSA_KEY_PRIV_EXP = 128;
	public static final short SIZE_RSA_KEY_MOD = 128;
	public static final short SIZE_RSA_KEY_PUB_EXP = 3;
	public static final short SIZE_RSA_SIG = 128;
	public static final short SIZE_RSA_ENC = 128;
	public static final short SIZE_NONCE = 16;
	public static final short SIZE_AES_KEY = 16;
	public static final short SIZE_CERT_TYPE = 1;
	public static final short SIZE_CERT_EXP_DATE = 8;
	public static final short SIZE_CERT_DATA_CARD = (short) ((short) (SIZE_CERT_TYPE + SIZE_RSA_KEY_MOD) + SIZE_CERT_EXP_DATE);
	public static final short SIZE_CERT_DATA_TERM = (short) (SIZE_CERT_TYPE + SIZE_RSA_KEY_MOD);
	public static final short SIZE_CERT_CARD = (short) (SIZE_CERT_DATA_CARD + SIZE_RSA_SIG);
	public static final short SIZE_CERT_TERM = (short) (SIZE_CERT_TYPE + SIZE_RSA_SIG);
	public static final short SIZE_PUBENC_PLAIN = (short) (SIZE_RSA_KEY_MOD - 11);
	public static final short SIZE_PUBENC_CIPH = 128;
	public static final short SIZE_AES_BLOCKSIZE = 16;

	private static final byte[] RSA_PUB_EXP = { 0x01, 0x00, 0x01 };

	// permanent crypto objects
	private RSAPrivateKey signatureKey;
	private RSAPublicKey caVerificationKey;
	private AESKey sessionKey;
	private byte[] certificate; // raw data

	// transient crypto objects
	private Object[] pubEncKey;

	// crypto workers
	private Signature signer;
	private Signature caVerifier;
	private Cipher pubEncrypter;
	private Cipher secretEncrypter;
	private Cipher secretDecrypter;
	private RandomData rng;

	/** Constructor, allocates data, initializes crypto */
	SecureData() {
		// allocate data
		certificate = new byte[SIZE_CERT_CARD];
		pubEncKey = JCSystem.makeTransientObjectArray((short) 1,
				JCSystem.CLEAR_ON_DESELECT);

		// initialize crypto
		signatureKey = (RSAPrivateKey) KeyBuilder.buildKey(
				KeyBuilder.TYPE_RSA_PRIVATE, KeyBuilder.LENGTH_RSA_1024, false);
		caVerificationKey = (RSAPublicKey) KeyBuilder.buildKey(
				KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, false);
		// set the constant public exponent for the CAKey
		caVerificationKey.setExponent(RSA_PUB_EXP, (short) 0,
				SIZE_RSA_KEY_PUB_EXP);
		sessionKey = (AESKey) KeyBuilder.buildKey(
				KeyBuilder.TYPE_AES_TRANSIENT_DESELECT,
				KeyBuilder.LENGTH_AES_128, false);

		// initialize "the workers"
		caVerifier = Signature.getInstance(Signature.ALG_RSA_MD5_PKCS1, false);
		signer = Signature.getInstance(Signature.ALG_RSA_MD5_PKCS1, false);
		pubEncrypter = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
		secretEncrypter = Cipher.getInstance(
				Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
		secretDecrypter = Cipher.getInstance(
				Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
		rng = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
	}

	/**
	 * Public key encryption
	 * 
	 * @return length of encrypted data
	 */
	short publicEncrypt(byte[] plaintext, byte counter, byte[] ciphertext) {
		short inOffset = (short) (counter * SIZE_PUBENC_PLAIN);
		short fourthSize = (SIZE_CERT_CARD + SIZE_NONCE + SIZE_AES_KEY + SIZE_RSA_SIG)
				% SIZE_PUBENC_PLAIN;
		short inSize = (counter < 4 ? SIZE_PUBENC_PLAIN : fourthSize);
		return pubEncrypter.doFinal(plaintext, inOffset, inSize, ciphertext,
				(short) 0);
	}

	/**
	 * Create the response for mutual authentication in the tmp buffer, This
	 * method also sets the temporary key
	 * 
	 * @return size of full message
	 */
	short createAuthResponse(byte[] buffer) {
		short len = SIZE_CERT_CARD;
		// place the certificate
		Util.arrayCopy(certificate, (short) 0, buffer, (short) 0, len);

		len += SIZE_NONCE;

		// generate temporary key
		rng.generateData(buffer, len, SIZE_AES_KEY);
		setTmpKey(buffer, len);

		len += SIZE_AES_KEY;

		// sign nonce + tmpkey
		len += signer.sign(buffer, SIZE_CERT_CARD,
				(short) (SIZE_NONCE + SIZE_AES_KEY), buffer, len);

		return len;
	}

	boolean validateOwnCert() {
		return caVerifier.verify(certificate, (short) 0, SIZE_CERT_DATA_CARD,
				certificate, SIZE_CERT_DATA_CARD, SIZE_RSA_SIG);
	}

	/** Validate a terminal certificate */
	boolean validateCert(byte[] data, short dataOfs, byte[] sig, short sigOfs) {
		return caVerifier.verify(data, dataOfs, SIZE_CERT_DATA_TERM, sig,
				sigOfs, SIZE_RSA_SIG);
	}

	/** Check a certificate for update */
	boolean checkCertUpdate(byte[] newCert, short ofs) {
		return Util.arrayCompare(certificate, (short) 0, newCert, ofs,
				(short) (SIZE_CERT_TYPE + SIZE_RSA_KEY_MOD)) == 0
				&& Util
						.arrayCompare(
								certificate,
								(short) (SIZE_CERT_TYPE + SIZE_RSA_KEY_MOD),
								newCert,
								(short) (ofs + (short) (SIZE_CERT_TYPE + SIZE_RSA_KEY_MOD)),
								SIZE_CERT_EXP_DATE) <= 0;
	}

	/**
	 * Sign a message
	 * 
	 * @return size of signature
	 */
	short sign(byte[] data, short dataOfs, short dataLen, byte[] sig,
			short sigOfs) {
		return signer.sign(data, dataOfs, dataLen, sig, sigOfs);
	}

	/** Encrypt with the session key */
	short sessionEncrypt(byte[] plain, short plainOfs, short plainLen,
			byte[] cipher, short cipherOfs) {
		return secretEncrypter.doFinal(plain, plainOfs, plainLen, cipher,
				cipherOfs);
	}

	/** Decrypt with the session key */
	short sessionDecrypt(byte[] cipher, short cipherOfs, short cipherLen,
			byte[] plain, short plainOfs) {
		return secretDecrypter.doFinal(cipher, cipherOfs, cipherLen, plain,
				plainOfs);
	}

	/** Initialize the workers after personalization */
	boolean init() {
		signer.init(signatureKey, Signature.MODE_SIGN);
		caVerifier.init(caVerificationKey, Signature.MODE_VERIFY);
		return validateOwnCert();
	}

	/** Set the (symmetric) key for encryption and decryption */
	void setSessionKey(byte[] buffer, short offset) {
		sessionKey.setKey(buffer, offset);
		secretEncrypter.init(sessionKey, Cipher.MODE_ENCRYPT);
		secretDecrypter.init(sessionKey, Cipher.MODE_DECRYPT);
	}

	/** @return whether the session key is initialized */
	boolean hasSessionKey() {
		return sessionKey.isInitialized();
	}

	/** Destroy the session key */
	void destroySessionKey() {
		sessionKey.clearKey();
	}

	/** Set the (symmetric) key for decryption */
	void setTmpKey(byte[] buffer, short offset) {
		sessionKey.setKey(buffer, offset);
		secretDecrypter.init(sessionKey, Cipher.MODE_DECRYPT);
	}

	/** Set the (public) key modulus (N) for the encryption key */
	void setPubEncryptKeyMod(byte[] buffer, short offset) {
		RSAPublicKey encKey = (RSAPublicKey) (pubEncKey[0]);
		encKey = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC,
				KeyBuilder.LENGTH_RSA_1024, false);
		encKey.setModulus(buffer, offset, SIZE_RSA_KEY_MOD);
		encKey.setExponent(RSA_PUB_EXP, (short) 0, SIZE_RSA_KEY_PUB_EXP);
		pubEncrypter.init(encKey, Cipher.MODE_ENCRYPT);
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

	/** Set the certificate */
	void setCert(byte[] buffer, short offset) {
		Util.arrayCopy(buffer, offset, certificate, (short) 0, SIZE_CERT_CARD);
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

	byte[] getCert() {
		return certificate;
	}

}
