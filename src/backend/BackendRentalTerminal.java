package backend;

import java.sql.ResultSet;
import java.sql.SQLException;

import terminal.Card;

public class BackendRentalTerminal {
	Database db = new Database();
	Backend be = new Backend();
	
	public BackendRentalTerminal(){
		
	}
	
	public boolean MutualAuthenticationRT_S(){
		boolean states = true;
		//TODO:
		//Check Mutual Authentication between Rental Terminal and SmartCards
		//if succeed then states = true
		return states;
		
	}
	
	public void RegisterNewCustomer(String name){
		//add to database customer
		Integer customerId = db.insertCustomer(name);
		
		//add new smartcard
		InitData init = be.registerNewCard(customerId);
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
		
		ResultSet rs = db.selectCard(cardPublicKey);
		try {
			if(rs.next()){
				card = new Card(rs.getInt("id"), rs.getInt("custID"), rs.getString("custName"), 
						rs.getInt("km"), rs.getLong("exp") , rs.getInt("revoke"), rs.getBytes("cardPK"));
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
