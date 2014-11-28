package backend;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import terminal.Card;

/* TODO: Change exp to short
 * Certificate with expdate is a byte array that looks like:
 * cert[0] = type (0 = smartcard, 1 = rentalterm, 2 = vehicleterm)
 * cert[1..163] = rsapublickey (length 162bytes)
 * cert[164..172] = expiration date of type long (8bytes)
 * cert[173...301] = Signature (128bytes)
 */ 

/*ALGO:  
 * 		1. get cardCert from smartcard and do mutual authentication between Rental Terminal - Smartcard 
 * 		2. get card data from database
 * 		3. do the change (register, top up or refund)
 * 		4. update database + update card certificate (if needed)
 */

/*
 * 		KeyPair kp = new KeyPair();
		System.out.println(kp.getPublic().getPublicExponent());
		System.out.println(getDigitCount(kp.getPublic().getPublicExponent())); //5bytes
		System.out.println(kp.getPublic().getModulus()); //309bytes
		System.out.println(getDigitCount(kp.getPublic().getModulus())); //309bytes
		System.out.println(kp.getPrivate().getPrivateExponent()); //308bytes
		System.out.println(getDigitCount(kp.getPrivate().getPrivateExponent()));
		System.out.println(kp.getPrivate().getModulus());
 */

public class BackendRentalTerminal {
	Database db = new Database();
	Backend be = new Backend();
	Serialization serial = new Serialization();
	ByteUtils byteUtil = new ByteUtils();
	
	int EXP_LENGTH = 8;
	int PUBKEY_LENGTH = 162;
	
	public BackendRentalTerminal(){
		
	}
	
	private byte[] ReadCertFromSmartCard(){
		byte[] cert = null;
		
		
		return cert;
	}
	
	private byte[] MutualAuthenticationRT_S(){
		byte[] cert = ReadCertFromSmartCard();
		//TODO:
		//Check Mutual Authentication between Rental Terminal and SmartCards
		//Need to do a revocation check as well		
	
		return cert;
		
	}
	
	/**
	 * REGISTER NEW CUSTOMER
	 * 1. Read cert from card
	 * 2. renew the certificate to expand the expiration date
	 * 3. Add customer and card data to database. NOTE: pubkey database is string
	 */
	public void RegisterNewCustomer(String name){		
		//1. read from the card and do mutual authentication
		byte[] cert = MutualAuthenticationRT_S();
		
		//2a. renew the certificate
		byte[] newCert = null;
		try {
			newCert = be.renewCertificate(cert);
		} catch (RevokedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//2b. extract the expiration date and pubkey from card certificate
		byte[] exp = getExpFromCert(newCert);
		byte[] pubKey = getPublicKeyFromCert(newCert);
		String strPubKey = serial.SerializeByteKey(pubKey);
		long expLong = byteUtil.bytesToLong(exp);
		
		//3. add to database customer
		Integer customerId = db.insertCustomer(name);
		db.addSmartcard(customerId, expLong, strPubKey);
		
	}
	
	/**
	 * 
	 * @param cardKm: the kilometers amount that stored in the card
	 */
	public Card TopUpCard(short cardKm){
		//1. read from the card and do mutual authentication
		byte[] cert = MutualAuthenticationRT_S();
		
		byte[] newCert = null;
		Card card = getCardDB(cert);
		
		try {
			/* renew the certificate */
			newCert = be.renewCertificate(cert);
			
			/* Update the Card class data  */
			card.setKilometers(card.getKilometers() + cardKm);
			//extract expiration date from the new certificate and convert it to Long 
			long expNew  = byteUtil.bytesToLong(getExpFromCert(newCert));
			//convert the long date to string
			String expString = convertLongDateToString(expNew);
			card.setExpiration(expString);
			
			/* update to database  */
			db.updateKilometersExpiration(card.getKilometers(), expNew, card.getID());
				
			//TODO update to the smartcard
			
		} catch (RevokedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return card;
	}
	
	
	//Refund the kilometers
	public Card refundKilometers(){
		//1. read from the card and do mutual authentication
		byte[] cert = MutualAuthenticationRT_S();
		
		//2. get card data from database
		Card card = getCardDB(cert);
		
		
		/* update to database  */
		db.updateKilometersExpiration(0, card.getExpDate(), card.getID());
				
		//TODO update to Card
		return card;
	}
	
	/**
	 * Get card data from database
	 * @param cardPublicKey
	 * @return
	 */
	public Card getCardDB(byte[] cardPublicKey){
		Card card = new Card();
		String strPubKey = serial.SerializeByteKey(cardPublicKey);
		
		ResultSet rs = db.selectCard(strPubKey);
		try {
			if(rs.next()){
				card = new Card(rs.getInt("id"), rs.getInt("custID"), rs.getString("custName"), 
						rs.getInt("km"), convertLongDateToString(rs.getLong("exp")) , rs.getInt("revoke"), rs.getString("cardPK"));
			}else{
				System.out.println("That card has not been issued");
				card = null;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return card;
	}
	
	
	
	
	//get the expiration date from the certificate
	private byte[] getExpFromCert(byte[] cert){
		byte[] exp = new byte[EXP_LENGTH];
		System.arraycopy(cert, 164, exp, 0, EXP_LENGTH);
		return exp;
	}
	
	//get public key from certificate
	private byte[] getPublicKeyFromCert(byte[] cert){
		byte[] pubKey = new byte[PUBKEY_LENGTH];
		System.arraycopy(cert, 1, pubKey, 0, PUBKEY_LENGTH);
		return pubKey;		
	}
	
	//convert long date to string date
	private String convertLongDateToString(long expDate){
	    Date date=new Date(expDate);
	    SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yy");
	    String dateText = df2.format(date);
	    System.out.println(dateText);
	    return dateText;
	}

}
