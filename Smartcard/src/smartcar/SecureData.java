package smartcar;

import javacard.framework.Util;

/**
 * Class for holding all persistent security data
 */
public class SecureData {

	// size (in bytes) of the values
	public static final short SIZE_SIGNATURE_KEY = 634;
	public static final short SIZE_CA_VERIFICATION_KEY = 162;
	public static final short SIZE_CERTIFICATE = 293;

	private byte[] signatureKey;
	private byte[] caVerificationKey;
	private byte[] certificate;

	/** Caller should verify that buffer length is equal to totalLength */
	SecureData(byte[] buffer, short ofs) {
		// allocate data
		signatureKey = new byte[SIZE_SIGNATURE_KEY];
		caVerificationKey = new byte[SIZE_CA_VERIFICATION_KEY];
		certificate = new byte[SIZE_CERTIFICATE];

		// TODO: use transaction here?
		Util.arrayCopy(buffer, ofs, signatureKey, (short) 0, SIZE_SIGNATURE_KEY);
		ofs += SIZE_SIGNATURE_KEY;
		Util.arrayCopy(buffer, ofs, caVerificationKey, (short) 0, SIZE_CA_VERIFICATION_KEY);
		ofs += SIZE_CA_VERIFICATION_KEY;
		Util.arrayCopy(buffer, ofs, certificate, (short) 0, SIZE_CERTIFICATE);
	}

	short getTotalLength() {
		return SIZE_SIGNATURE_KEY + SIZE_CA_VERIFICATION_KEY + SIZE_CERTIFICATE;
	}
	
	/** Caller should ensure that buffer length is equal to SIZE_CERTIFICATE */
	void setCertificate(byte[] buffer, short ofs) {
		Util.arrayCopy(buffer, ofs, certificate, (short) 0, SIZE_CERTIFICATE);
	}
}
