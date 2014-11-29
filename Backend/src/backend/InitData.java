package backend;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Wrapper class for the initial data that is to be sent to the
 * smartcard/terminal upon initialization.
 * 
 * TODO/WARNING: values can be null in current implementation
 */
public class InitData {
	
	public byte[] certificate;
	public RSAPrivateKey privateKey;
	public RSAPublicKey caVerifKey;

	public InitData(byte[] certificate, RSAPrivateKey privateKey) {
		this.certificate = certificate;
		this.privateKey = privateKey;
	}
	

	public InitData(byte[] certificate, RSAPrivateKey privateKey, RSAPublicKey caVerifKey) {
		this.certificate = certificate;
		this.privateKey = privateKey;
		this.caVerifKey = caVerifKey;
	}
}
