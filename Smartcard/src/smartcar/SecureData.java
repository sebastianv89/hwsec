package smartcar;

import javacard.security.Signature;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.KeyBuilder;
import javacard.security.RSAPrivateKey;
import javacard.security.RSAPublicKey;
import javacardx.crypto.Cipher;

/**
 * Class for holding all persistent security data
 */
public class SecureData {

	// sizes of data
	public static final short SIZE_CARD_CERTIFICATE_DATA = 137;
	public static final short SIZE_TERM_CERTIFICATE_DATA = 129;
	public static final short SIZE_CERTIFICATE_SIG = 128;
	public static final short SIZE_RSAKEY_EXP = 128;
	public static final short SIZE_RSAKEY_MOD = 128;

	// crypt objects
	private RSAPrivateKey signatureKey; // TODO: replace completely with signer
	RSAPublicKey caVerificationKey; // TODO: replace completely with caVerifier
	RSAPublicKey[] termEncryptKey; // RAM
	Signature signer;
	Signature caVerifier;
	Cipher termEncrypter;

	// TODO: check if the public RSA exponent is always the same
	private static final byte[] RSA_EXP = { 0x01, 0x00, 0x01 };
	private static final short RSA_EXP_SIZE = 0x0003;

	// certificate is stored raw, easiest for just sending data
	private byte[] certificate;

	/** Constructor, allocates data, initializes crypto */
	SecureData() {
		// allocate data
		certificate = new byte[SIZE_CARD_CERTIFICATE_DATA
				+ SIZE_CERTIFICATE_SIG];
		termEncryptKey = (RSAPublicKey[]) JCSystem.makeTransientObjectArray(
				(short) 1, JCSystem.CLEAR_ON_DESELECT);

		// initialize crypto
		signatureKey = (RSAPrivateKey) KeyBuilder.buildKey(
				KeyBuilder.TYPE_RSA_PRIVATE, KeyBuilder.LENGTH_RSA_1024, false);
		caVerificationKey = (RSAPublicKey) KeyBuilder.buildKey(
				KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, false);
		// set the constant public exponent for the CAKey
		caVerificationKey.setExponent(RSA_EXP, (short) 0, RSA_EXP_SIZE);

		// initialize "the workers"
		caVerifier = Signature.getInstance(Signature.ALG_RSA_MD5_PKCS1, false);
		signer = Signature.getInstance(Signature.ALG_RSA_MD5_PKCS1, false);
		termEncrypter = Cipher.getInstance(Cipher.ALG_RSA_ISO14888, false);
		Cipher.ALG_RSA
	}

	/** Validate a terminal certificate */
	boolean validateCert(byte[] data, short dataOfs, byte[] sig, short sigOfs,
			byte type) {
		// validate certificate type
		if (data[dataOfs++] != type) {
			return false;
		}
		return caVerifier.verify(data, dataOfs, SIZE_TERM_CERTIFICATE_DATA,
				sig, sigOfs, SIZE_CERTIFICATE_SIG);
	}

	/** Initialize the workers after personalization */
	void init() {
		signer.init(signatureKey, Signature.MODE_SIGN);
		caVerifier.init(caVerificationKey, Signature.MODE_VERIFY);
	}

	/** Set the (public) key modulus (N) for the encryption key */
	void setTermEncryptKeyMod(byte[] buffer, short offset) {
		termEncryptKey[0].setModulus(buffer, offset, SIZE_RSAKEY_MOD);
		termEncryptKey[0].setExponent(RSA_EXP, (short) 0, RSA_EXP_SIZE);
		termEncrypter.init(termEncryptKey[0], Cipher.MODE_ENCRYPT);
	}

	/** Set the (private) key exponent (d) for the signature key */
	void setSignKeyExp(byte[] buffer, short offset) {
		signatureKey.setExponent(buffer, offset, SIZE_RSAKEY_EXP);
	}

	/** Set the (public) key modulus (N) for the signature key */
	void setSignKeyMod(byte[] buffer, short offset) {
		signatureKey.setModulus(buffer, offset, SIZE_RSAKEY_MOD);
	}

	/** Set the (public) key modulus (N) for the CA verification key */
	void setCAVerifKeyMod(byte[] buffer, short offset) {
		caVerificationKey.setModulus(buffer, offset, SIZE_RSAKEY_MOD);
	}

	/** Set the data part of the certificate */
	void setCertData(byte[] buffer, short ofs) {
		Util.arrayCopy(buffer, ofs, certificate, (short) 0,
				SIZE_CERTIFICATE_DATA);
	}

	/** Set the CA signature part of the certificate */
	void setCertSig(byte[] buffer, short ofs) {
		Util.arrayCopy(buffer, ofs, certificate, SIZE_CERTIFICATE_DATA,
				SIZE_CERTIFICATE_SIG);
	}

	/** Get the certificate in the buffer data */
	short getCert(byte[] buf, short ofs) {
		short len = SIZE_CERTIFICATE_DATA + SIZE_CERTIFICATE_SIG;
		Util.arrayCopy(certificate, (short) 0, buf, ofs, len);
		return len;
	}

}
