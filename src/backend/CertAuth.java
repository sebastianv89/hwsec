package backend;

/** Certificate authority */
public class CertAuth {

	public enum TYPE {
		SMARTCARD, RENTALTERM, VEHICLETERM
	};

	/**
	 * Hard-coded(!) signature key of the CA (SK_{CA}) 
	 */
	private static final byte[] signKey = new byte[] { (byte) 0xca,
			(byte) 0xfe, (byte) 0xba, (byte) 0xbe };
	private byte[] verifKey;

	public CertAuth() {
		// TODO: verifKey = derivePublicKey(signKey)S
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
