package backend;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
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
	ConstantValues CV = new ConstantValues();
	
	public Backend() {
		db = new Database();
		ca = new CertAuth();
	}

	/**
	 * Register a new Smartcard. Should be called by the personalisation terminal
	 * @param i 
	 * 
	 * @param customerId
	 *            customer id, link card to this customer
	 * @return certificate and secret key of the card
	 */
	public InitData registerNewCard() {
		// generate a new (random) keypair
		KeyPair keypair = new KeyPair();
		//System.out.println(keypair.getPublic());
		//System.out.println(keypair.getPrivate());
		long exp = getExpirationDate();

		// get a certificate from the CA
		byte[] cert = ca.makeCert(CertAuth.TYPE.SMARTCARD, keypair.getPublic(),
				exp);

		// get the CA verification key
		RSAPublicKey certVerifKey = ca.getVerificationKey();

		// Dont need to store it since we have not given out the card yet!
		
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

		return new InitData(cert, keypair.getPrivate()); //Dont need pubkey because it is in the cert
	}

	/**
	 * Register a new Vehicle Terminal
	 * 
	 * @return certificate, private key and certificate verification key
	 */
	public InitData registerVehicleTerminal() {
		// generate a new (random) keypair
		KeyPair keypair = new KeyPair();

		// get a certificate from the CA
		byte[] cert = ca
				.makeCert(CertAuth.TYPE.RENTALTERM, keypair.getPublic());
		
		// get the CA public verification key
		RSAPublicKey certVerifKey = ca.getVerificationKey();

		// register vehicle terminal in the database
		return new InitData(cert, keypair.getPrivate(), certVerifKey);
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
		byte[] publicKey = new byte[CV.RSAPUBLICKEYLENGTH];
		System.arraycopy(cert, 1, publicKey, 0, CV.RSAPUBLICKEYLENGTH); // bytes 1...162 are pubKey
		
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
		RSAPublicKey rsaPublicKey = null;
		// first get the pubkey fromt the cert
		byte[] publicKey = new byte[CV.RSAPUBLICKEYLENGTH];
		System.arraycopy(cert, 1, publicKey, 0, CV.RSAPUBLICKEYLENGTH);
		System.out.println(publicKey.length);
		
		Serialization serialize = new Serialization();
		String strPublicKey = serialize.SerializeByteKey(publicKey);
		
		if (db.isRevoked(strPublicKey)) {
			throw new RevokedException();
		}
		
		// get updated expiration date
		long exp = getExpirationDate();
		
		// Convert bytes to RSAPublicKey
		/*X509EncodedKeySpec pubspec = new X509EncodedKeySpec(publicKey);
		KeyFactory factory;
		try {
			factory = KeyFactory.getInstance("RSA");
			rsaPublicKey = (RSAPublicKey) factory.generatePublic(pubspec);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		rsaPublicKey = generatePK(publicKey);
		
		
		// return the new certificate
		return ca.makeCert(CertAuth.TYPE.SMARTCARD, rsaPublicKey, exp);
	}
	
	//generate public key from modulo
	private RSAPublicKey generatePK(byte[] bytePubKey){
		// Convert bytekey (public Modulus into a RSAPublicKey
		byte[] padded = new byte[129];
		padded[0] = 0;
		System.arraycopy(bytePubKey, 0, padded, 1, 128);
		RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(padded), CV.PUBEXPONENT_BYTE);
		
		RSAPublicKey publicKey = null;
		try {
			KeyFactory factory = KeyFactory.getInstance("RSA");
			publicKey = (RSAPublicKey) factory.generatePublic(spec);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return publicKey;
		
	}
	
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
	
   private static void writeKey(Key key, String filename) {
	      FileOutputStream file;
		try {
			file = new FileOutputStream(filename);
		    file.write(key.getEncoded());
		    file.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
}
