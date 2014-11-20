package backend;

/**
 * Wrapper class for the initial data that is to be sent to the
 * smartcard/terminal upon initialization.
 * 
 * TODO/WARNING: values can be null in current implementation
 */
public class InitData {
	
	public byte[] certificate;
	public byte[] privateKey;
	public byte[] secretKey;
	public byte[] caVerifKey;

	public InitData(byte[] certificate, byte[] privateKey) {
		this(certificate, privateKey, null, null);
	}
	
	public InitData(byte[] certificate, byte[] privateKey, byte[] caVerifKey) {
		this(certificate, privateKey, null, caVerifKey);
	}

	public InitData(byte[] certificate, byte[] privateKey, byte[] secretKey,
			byte[] caVerifKey) {
		this.certificate = certificate;
		this.privateKey = privateKey;
		this.secretKey = secretKey;
		this.caVerifKey = caVerifKey;
	}
}
