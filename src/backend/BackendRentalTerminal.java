package backend;

public class BackendRentalTerminal {
	Database db = new Database();
	Backend be = new Backend();
	
	public BackendRentalTerminal(){
		
	}
	
	private boolean MutualAuthenticationRTS(){
		boolean states = false;
		//TODO:
		//Check Mutual Authentication between Rental Terminal and SmartCards
		//if succeed then states = true
		return states;
		
	}
	
	public void RegisterNewCustomer(String name, short km){
		//add to database customer
		db.insertCustomer(name);
		//get the customer ID that just inserted
		int id = db.getLastID();		
		
		//add new smartcard
		InitData init = be.registerNewCard(id, km);
	}

}
