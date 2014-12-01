package terminal;

public class Card {
	int id;
	int customerID; 
	String customerName;
	short kilometers;
	long expiration;
	String expString;
	int revocation;
	String cardPublicKey;
	
	byte[] cardModulus;
	byte[] cardCert;
	byte[] sessionKey;
	
	
	public Card(){
		
	}
	
	public Card(int ID, int custID, String custName, short km, long exp, 
			int revoke, String cardPK){
		this.id = ID;
		this.customerID = custID;
		this.customerName = custName;
		this.kilometers = km;
		this.expiration = exp;
		this.revocation = revoke;
		this.cardPublicKey = cardPK;
	}
	
	public void setKilometers(short kmNew){
		this.kilometers = kmNew;
	}
	
	public void setExpiration(long expNew){
		this.expiration = expNew;
	}
	public void setStringExpiration(String strExpNew){
		this.expString = strExpNew;
	}
	public void setCardModulus(byte[] cardByteModulus){
		this.cardModulus = cardByteModulus;
	}
	
	public void setSessionKey(byte[] sessionkey){
		this.sessionKey = sessionkey;
	}
	
	public void setCertificate(byte[] cardcert){
		this.cardCert = cardcert;
	}
	
	public byte[] getCertificate(){
		return this.cardCert;
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
	
	public String getStringKilometers(){
		return this.expString;
	}
	
	public long getExpDate(){
		return this.expiration;
	}
	
	public int getRevocationStates(){
		return this.revocation;
	}
	public String getCardPublicKey(){
		return this.cardPublicKey;
	}
	
	public byte[] getCardModulus(){
		return this.cardModulus;
	}
	

}
