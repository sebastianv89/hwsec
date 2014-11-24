package terminal;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
			System.out.println("Input your name: ");
			Scanner scName = new Scanner(System.in);
		    String custName = scName.nextLine();
		    
		    System.out.println("Input the kilometers: ");
		    Scanner scKm = new Scanner(System.in);
		    int kilometers = scKm.nextInt();
		    
		    rt.RegisterNewCustomer(custName, (short)kilometers);
		    
		}else if(menu.equals("2")){
			//Insert SmartCard, get cardID and km from card
			//HardCoded CardID
			byte[] card = new byte[] { (byte) 0xca,	(byte) 0xfb, (byte) 0xba, (byte) 0xbe };
		}
	}
}
