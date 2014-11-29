package backend;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import terminal.Card;


/*ALGO:  
 * 		1. get cardCert from smartcard and do mutual authentication between Rental Terminal - Smartcard 
 * 		2. get card data from database
 * 		3. do the change (register, top up or refund)
 * 		4. update database + update card certificate (if needed)
 */


public class BackendRentalTerminal {
	Database db = new Database();
	Backend be = new Backend();
	Serialization serial = new Serialization();
	ByteUtils byteUtil = new ByteUtils();
	
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
		byte[] exp = serial.getExpFromCert(newCert);
		byte[] pubKey = serial.getPublicKeyFromCert(newCert);
		String strPubKey = serial.SerializeByteKey(pubKey);
		long expLong = byteUtil.bytesToLong(exp);
		
		//3. add to database customer
		Integer customerId = db.insertCustomer(name);
		db.addSmartcard(customerId, expLong, strPubKey);
		
	}
	
	/**
	 * 
	 * @param cardKm: the kilometers amount that stored in the card
	 * NOTE: for the database, the kilometers saved is the total kilometers that ever added to the card
	 * without the ticking
	 */
	public Card TopUpCard(short cardKm){
		//1. read from the card and do mutual authentication
		byte[] cert = MutualAuthenticationRT_S();
		short km = 0; //read this from card
		byte[] newCert = null;
		
		/* Get card from database  */
		Card card = getCardDB(cert);
		
		try {
			/* renew the certificate */
			newCert = be.renewCertificate(cert);
			
			//extract expiration date from the new certificate and convert it to Long 
			long expNew  = byteUtil.bytesToLong(serial.getExpFromCert(newCert));
			//convert the long date to string
			String expString = convertLongDateToString(expNew);
			card.setExpiration(expNew);
			card.setStringExpiration(expString);
			
			/* update to database  */
			db.updateKilometersExpiration(card.getKilometers()+ cardKm, expNew, card.getID());
				
			//TODO update to the smartcard 
			card.setKilometers(km + cardKm);
			
		} catch (RevokedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return card;
	}
	
	
	//Refund the kilometers
	public Card refundKilometers(){
		byte[] cert = MutualAuthenticationRT_S(); 		//1. read from the card and do mutual authentication
		Card card = getCardDB(cert); //2. get card data from database
		card.setKilometers(0); 		//3. do the refund
		db.updateKilometersExpiration(0, card.getExpDate(), card.getID());  /*4. update to database  */
				
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
	

	//convert long date to string date
	private String convertLongDateToString(long expDate){
	    Date date=new Date(expDate);
	    SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yy");
	    String dateText = df2.format(date);
	    System.out.println(dateText);
	    return dateText;
	}
	
	

}
