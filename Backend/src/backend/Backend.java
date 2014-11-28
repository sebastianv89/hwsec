package backend;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;

/* Add function: createCustomer -> return the customer id
 * 
 */

/**
 * Wrapper class for the Backend System. This class serves as an interface for
 * the {@link PersonalTerminal} and {@link RentalTerminal} classes.
 */
public class Backend {

	private Database db;
	private CertAuth ca;

	public Backend() {
		db = new Database();
		ca = new CertAuth();
	}

	/**
	 * Register a new Smartcard. Should be called by the personalisation terminal
	 * 
	 * @param customerId
	 *            customer id, link card to this customer
	 * @return certificate and secret key of the card
	 */
	public InitData registerNewCard() {
		// generate a new (random) keypair
		KeyPair keypair = new KeyPair();
		long exp = getExpirationDate();

		// get a certificate from the CA
		byte[] cert = ca.makeCert(CertAuth.TYPE.SMARTCARD, keypair.getPublic(),
				exp);

		// get the CA verification key
		byte[] certVerifKey = ca.getVerificationKey().getEncoded();

//		// add smartcard to database
//		db.addSmartcard(customerId, exp, keypair.getPublic());

		return new InitData(cert, keypair.getPrivate(), certVerifKey);
	}

	/**
	 * Register a new Rental Terminal
	 * 
	 * Ideally, this should only have to be done once.
	 * 
	 * @return certificate and private key
	 */
	public InitData registerRentalTerminal() {
		// generate a new (random) keypair
		KeyPair keypair = new KeyPair();

		// get a certificate from the CA
		byte[] cert = ca
				.makeCert(CertAuth.TYPE.RENTALTERM, keypair.getPublic());

		return new InitData(cert, keypair.getPrivate());
	}

	/**
	 * Register a new Vehicle Terminal
	 * 
	 * @return certificate, private key and certificate verification key
	 */
	public InitData registerVehicleTerminal() {
		// generate a new (random) keypair
		KeyPair keypair = new KeyPair();
		// generate a secret key (used for logging)
		RSAPrivateKey secretKey = keypair.getPrivate();

		// get a certificate from the CA
		byte[] cert = ca
				.makeCert(CertAuth.TYPE.RENTALTERM, keypair.getPublic());

		// add vehicle terminal to the database
		Serialization serialize = new Serialization();
		String strPublicKey = serialize.SerializePublicKey(keypair.getPublic());
		String strPrivateKey = serialize.SerializePrivateKey(secretKey);
		db.addVehicleTerminal(strPublicKey, secretKey);

		// get the CA public verification key
		byte[] certVerifKey = ca.getVerificationKey().getEncoded();

		// register vehicle terminal in the database
		return new InitData(cert, keypair.getPrivate(), secretKey, certVerifKey);
	}

	/**
	 * Revoke the smartcard
	 * 
	 * Use case 2.6
	 * 
	 * @param cert
	 *            Used to identify the smartcard
	 */
	public void revokeSmartcard(byte[] cert) {
		byte[] publicKey;
		System.arraycopy(cert, 1, publicKey, 0, 162); // bytes 1...162 are pubKey
		
		Serialization serialize = new Serialization();
		String strPublicKey = serialize.SerializeByteKey(publicKey);
		
		db.revokeSmartcard(strPublicKey);
	}

	/**
	 * Renew the certificate of the smartcard (specifically: the expiration
	 * date)
	 * 
	 * Use case 2.7 (protocol 6.9)
	 * 
	 * @param cert
	 *            The old certificate
	 * @return the new certificate if the card is not revoked
	 * @throws RevokedException
	 *             If the smartcard was revoked
	 */
	public byte[] renewCertificate(byte[] cert) throws RevokedException {
		
		// first get the pubkey fromt the cert
		byte[] publicKey;
		System.arraycopy(cert, 1, publicKey, 0, 162);
		
		Serialization serialize = new Serialization();
		String strPublicKey = serialize.SerializeByteKey(publicKey);
		
		if (db.isRevoked(strPublicKey)) {
			throw new RevokedException();
		}
		
		// get updated expiration date
		long exp = getExpirationDate();
		
		// return the new certificate
		return ca.makeCert(CertAuth.TYPE.SMARTCARD, publicKey, exp);
	}
	
	// TODO: topup (protocol 6.10), lots of steps in that protocol
	// TODO: refund (protocol 6.11), similar to payment
	
	/**
	 * Simulation of "checking with the bank" if the payment was valid
	 */
	private boolean checkPayment() {
		try {
		    Thread.sleep(500); // milliseconds
		} catch (InterruptedException e) {
		    Thread.currentThread().interrupt();
		}
		return true;
	}

	/**
	 * Get a new expiration date, three weeks from today
	 * 
	 * @return long timestamp of the new expiration date
	 */
	private long getExpirationDate() {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.DATE, 21);
		return c.getTimeInMillis(); // TODO: can't we make this a short, maybe a
									// counter in days
	}
	
}
