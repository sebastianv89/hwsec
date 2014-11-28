package backend;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.math.BigInteger;
import java.util.List;
import java.util.Scanner;

import javax.swing.*;

import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;

import javax.smartcardio.*;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class PersonalTerminal {
	private static final String ch = null;
	// the personal terminal should upload a fresh secret key and certificate to a smartcard
	// the environment is secure by assumption, so no auth is required
	//Get certificate from the backend and send it to the card by addCertificate
	
	// use: Backend.registerNewCard(customerId)
	// call this function "InitData registerNewCard"
	// create smartcard object 
	
	public static void main(String[] args){
		//InitData nc = new InitData(cert, keypair.getPrivate(), certVerifKey);
		Backend bk = new Backend();
		InitData nc = bk.registerNewCard(8);
		
		byte[] pk = nc.privateKey;
		byte[] c = nc.certificate;
		byte[] ca = nc.caVerifKey;
		
		
		//issuing certificate to the terminal on request by user either rental terminal or vehicle terminal
		PersonalTerminal pt = new PersonalTerminal();
		
		System.out.println("Welcome to the Personilization Terminal");
		System.out.println("1. Issue certificate for Rental Terminal");
		System.out.println("2. Issue certificate for Vehicle Terminal");
		System.out.println("3. Issue certificate for SmartCard");
		System.out.println("4.. EXIT");
		System.out.println("Enter a number");
		Scanner scan = new Scanner(System.in);
		String menu = scan.nextLine();
		if (menu.equals("1")){
			bk.registerRentalTerminal();
			byte[] c1 = nc.certificate;
			byte[] pk1 = nc.privateKey;
			System.out.println("Certificate issued successfully");
			
		}
		else if (menu.equals("2")){
			bk.registerVehicleTerminal();
			byte[] c2 = nc.certificate;
			byte[] pk2 = nc.privateKey;
			byte[] sc2 = nc.secretKey;
			byte[] ca2 = nc.caVerifKey;
			System.out.println("Certificate issued successfully");
			
		}
		else if(menu.equals("3")){
			//add new smartcard
			InitData init = bk.registerNewCard(0);
		}
		else	
		{
			System.out.println("Thank You");
		}
	}
		
		public ResponseAPDU transmitAPDU(final CommandAPDU cAPDU);
			throws CardException, CardNotFoundException{
				
				
				ResponseAPDU resp = ch.transmit(new CommandAPDU(0X00, 0XA4, 0X00, 0X90, new byte[]{0x54, 0x01}) );
				}
			
		}
		
		
		
	}
}
