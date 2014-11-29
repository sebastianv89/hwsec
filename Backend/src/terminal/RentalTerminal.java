package terminal;

import java.sql.SQLException;
import java.util.Scanner;

import backend.BackendRentalTerminal;

/*
 * This is the main class of Rental Terminal
 * contains all the code that will interact with the user
 * This class is related to RentalTerminal_classes that contains the functions executed in this class
 * 
 */
public class RentalTerminal {
	public RentalTerminal(){
		
	}
	
	public static void main(String[] args) throws SQLException {
		//Call the classes that contains all the script needed
		BackendRentalTerminal rt = new BackendRentalTerminal();
		
		System.out.println("Welcome to the Cars-Rental");
		System.out.println("1. Register (for new customer)");
		System.out.println("2. Top Up");
		System.out.println("3. Refund");
		System.out.print("Choose a menu number: ");
		Scanner scan = new Scanner(System.in);
	    String menu = scan.nextLine();
		if(menu.equals("1")){
			System.out.println("Menu Register New Customer");
			
			System.out.println("Input your name: ");
			Scanner scName = new Scanner(System.in);
		    String custName = scName.nextLine();
		    
		    rt.RegisterNewCustomer(custName);
		    System.out.println("Congratulation you have been registered");
		    System.out.println("Name : " + custName);
		    
		}else if(menu.equals("2")){
			System.out.println("Menu Top Up");
			
			System.out.print("Input the kilometers that you want to add: ");
		    Scanner scKm = new Scanner(System.in);
		    short kilometers = (short) scKm.nextInt();
			
		    Card card = rt.TopUpCard(kilometers);
		    System.out.println("Name: "+ card.getCustomerName());
		    System.out.println("Kilometers: " + card.getStringKilometers());
		    System.out.println("Expiration date" + card.getExpDate());
			
		}else if(menu.equals("3")){
			//Refund is topup with 0
			System.out.println("Menu Refund");
			
			Card card = rt.refundKilometers();
		    System.out.println("Name: "+ card.getCustomerName());
		    System.out.println("Kilometers: " + card.getStringKilometers());
		    System.out.println("Expiration date" + card.getExpDate());
		}

	}
}
