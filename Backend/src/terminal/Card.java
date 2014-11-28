package terminal;

public class Card {
	int id;
	int customerID; 
	String customerName;
	int kilometers;
	long expiration;
	int revocation;
	byte[] cardPublicKey;
	
	public Card(){
		
	}
	
	public Card(int ID, int custID, String custName, int km, long exp, 
			int revoke, byte[] cardPK){
		this.id = ID;
		this.customerID = custID;
		this.customerName = custName;
		this.kilometers = km;
		this.expiration = exp;
		this.revocation = revoke;
		this.cardPublicKey = cardPK;
	}
	
	public void setKilometers(int kmNew){
		this.kilometers = kmNew;
	}
	
	public void setExpiration(long expNew){
		this.expiration = expNew;
	}
	
	public int getID(){
		return this.id;
	}
	
	public int getCustomerID(){
		return this.customerID;
	}
	
	public String getCustomerName(){
		return this.customerName;
	}
	
	public int getKilometers(){
		return this.kilometers;
	}
	
	public long getExpDate(){
		return this.expiration;
	}
	
	public int getRevocationStates(){
		return this.revocation;
	}
	public byte[] getCardPublicKey(){
		return this.cardPublicKey;
	}
	

}
