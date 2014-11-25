package backend;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.math.BigInteger;
import java.util.List;

import javax.swing.*;

import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;

import javax.smartcardio.*;

public class PersonalTerminal {
	// the personal terminal should upload a fresh secret key and certificate to a smartcard
	// the environment is secure by assumption, so no auth is required
	//Get certificate from the backend and send it to the card by addCertificate
	
	// use: Backend.registerNewCard(customerId)
	// call this function "InitData registerNewCard"
	// create smartcard object 
	
	public static void main(String[] args){
		//InitData nc = new InitData(cert, keypair.getPrivate(), certVerifKey);
		Backend bk = new Backend();
		InitData nc = bk.registerNewCard(9, (short) 1);
		
		byte[] pk = nc.privateKey;
		byte[] c = nc.certificate;
		byte[] ca = nc.caVerifKey;
		
		
		
		
	}
}
