package terminal;

public class Card {
	int id;
	int customerID; 
	String customerName;
	int kilometers;
	String expiration;
	int revocation;
	String cardPublicKey;
	
	public Card(){
		
	}
	
	public Card(int ID, int custID, String custName, int km, String exp, 
			int revoke, String cardPK){
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
	
	public void setExpiration(String expNew){
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
	
	public String getExpDate(){
		return this.expiration;
	}
	
	public int getRevocationStates(){
		return this.revocation;
	}
	public String getCardPublicKey(){
		return this.cardPublicKey;
	}
	

}
