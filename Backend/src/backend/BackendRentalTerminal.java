package backend;

import java.sql.ResultSet;
import java.sql.SQLException;

import terminal.Card;

public class BackendRentalTerminal {
	Database db = new Database();
	Backend be = new Backend();
	Serialization serial = new Serialization();
	ByteUtils byteUtil = new ByteUtils();
	
	int EXP_LENGTH = 8;
	int PUBKEY_LENGTH = 162;
	
	public BackendRentalTerminal(){
		
	}
	
	public byte[] ReadCertFromSmartCard(){
		byte[] cert = null;
		
		
		return cert;
	}
	
	public boolean MutualAuthenticationRT_S(){
		boolean states = true;
		//TODO:
		//Check Mutual Authentication between Rental Terminal and SmartCards
		//if succeed then states = true
		return states;
		
	}
	
	/* TODO: Change exp to short
	 * Certificate with expdate is a byte array that looks like:
	 * cert[0] = type (0 = smartcard, 1 = rentalterm, 2 = vehicleterm)
	 * cert[1..163] = rsapublickey (length 162bytes)
	 * cert[164..172] = expiration date of type long (8bytes)
	 * cert[173...301] = Signature (128bytes)
	 */ 
	
	public void RegisterNewCustomer(String name){
		/**
		 * REGISTER NEW CUSTOMER
		 * 1. Read cert from card
		 * 2. renew the certificate to expand the expiration date
		 * 3. Add customer and card data to database. NOTE: pubkey database is string
		 */
		
		//1. read from the card 
		byte[] cert = ReadCertFromSmartCard();
		//2a. renew the certificate
		byte[] newCert = null;
		try {
			newCert = be.renewCertificate(cert);
		} catch (RevokedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//2b. extract the expiration date and pubkey from card certificate
		byte[] exp = new byte[EXP_LENGTH];
		byte[] pubKey = new byte[PUBKEY_LENGTH];
		System.arraycopy(newCert, 164, exp, 0, EXP_LENGTH);
		System.arraycopy(newCert, 1, pubKey, 0, PUBKEY_LENGTH);
		String strPubKey = serial.SerializeByteKey(pubKey);
		long expLong = byteUtil.bytesToLong(exp);
		
		//3. add to database customer
		Integer customerId = db.insertCustomer(name);
		db.addSmartcard(customerId, expLong, strPubKey);
		
	}
	
	/**
	 * 
	 * @param cardPublicKey: the public key that stored in the card
	 * @param cardKm: the kilometers amount that stored in the card
	 */
	public Card topUpCard(byte[] cert, Card card, short cardKm){
		byte[] newCert = null;
		
		try {
			/* renew the certificate */
			newCert = be.renewCertificate(cert);
			
			/* Update the Card data (in a struct) */
			card.setKilometers(card.getKilometers() + cardKm);
			//TODO EXTRACT EXP FROM CERTIFICATES
			//card.setExpiration(expNew);
			
			/* update to database  */
			db.updateKilometersExpiration(card.getKilometers(), card.getExpDate(), card.getID());
				
			//TODO update to Card
			
		} catch (RevokedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return card;
	}
	
	public Card getCardDB(byte[] cardPublicKey){
		Card card = new Card();
		String strPubKey = serial.SerializeByteKey(cardPublicKey);
		
		ResultSet rs = db.selectCard(strPubKey);
		try {
			if(rs.next()){
				card = new Card(rs.getInt("id"), rs.getInt("custID"), rs.getString("custName"), 
						rs.getInt("km"), rs.getLong("exp") , rs.getInt("revoke"), rs.getString("cardPK"));
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
	
	//Refund the kilometers
	public void refundKilometers(byte[] cert, Card card){
		/* update to database  */
		db.updateKilometersExpiration(0, card.getExpDate(), card.getID());
			
		//TODO update to Card
	}

}
