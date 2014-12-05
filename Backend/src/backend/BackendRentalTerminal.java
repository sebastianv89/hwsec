package backend;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
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
	CardTerminalCommunication CT = new CardTerminalCommunication();
	MutualAuthentication MA = new MutualAuthentication();
	ConstantValues CV = new ConstantValues();
	
	public BackendRentalTerminal(){
		
	}
	
	private byte[] GetRTCert(){
		String RTcertFile = "RTCert";
		byte[] rtCert = MA.readFiles(RTcertFile);  //Read Certificate from Rental Terminal
		return rtCert;
	}
	
	private RSAPrivateKey GetRTPrivateKey(){
		byte[] privKeyByte = MA.readFiles("RTPrivateKey");
		RSAPrivateKey rtPrivKey = null;
		try {
			rtPrivKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privKeyByte));
		} catch ( NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rtPrivKey;
	}
	
	/**
	 * REGISTER NEW CUSTOMER
	 * 1. Read cert from card
	 * 2. renew the certificate to expand the expiration date
	 * 3. Add customer and card data to database. NOTE: pubkey database is string
	 */
	public void RegisterNewCustomer(String name){		
		//1. read from the card and do mutual authentication
		byte[] cert = MA.TerminalMutualAuth(GetRTCert(), GetRTPrivateKey());
		
		//2a. renew the certificate
		byte[] newCert = null;
		try {
			newCert = be.renewCertificate(cert);
		} catch (RevokedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//2b. extract the expiration date and pubkey from card certificate
		byte[] exp = new byte[CV.EXP_LENGTH];
		System.arraycopy(newCert, 129, exp, 0, exp.length);
		byte[] pubKey = new byte[CV.PUBMODULUS];
		System.arraycopy(newCert, 1, pubKey, 0, pubKey.length);
		System.out.println(pubKey);
		
		long expLong = byteUtil.bytesToLong(exp);
		
		//3. add to database customer
		Integer customerId = db.insertCustomer(name);
		db.addSmartcard(customerId, expLong, pubKey);
		
	}
	
	/**
	 * 
	 * @param cardKm: the kilometers amount that stored in the card
	 * NOTE: for the database, the kilometers saved is the total kilometers that ever added to the card
	 * without the ticking
	 */
	
	public Card AuthenticateCard(){
		//1. read from the card and do mutual authentication
		byte[] cert = MA.TerminalMutualAuth(GetRTCert(), GetRTPrivateKey());
		byte[] newCert = null;
		Card card = new Card();
		/* renew the certificate */
		try {
			newCert = be.renewCertificate(cert);
			card.setCertificate(newCert);
			
			//split the certificate into two packages
			byte[] pack1 = new byte[CV.PUBMODULUS + 1 + CV.EXP_LENGTH];  //1 + 128 + 8
			System.arraycopy(newCert, 0, pack1, 0, pack1.length); 
			System.out.println(byteUtil.toHexString(pack1));
			byte[] pack2 = new byte[CV.SIG_LENGTH]; //SIGNATURE 128
			System.arraycopy(newCert, CV.PUBMODULUS + 1 + CV.EXP_LENGTH, pack2, 0, CV.SIG_LENGTH);
			System.out.println(byteUtil.toHexString(pack2));
			
			//send the new cert to card
			byte[] scPack1 = CT.sendToCard(pack1, CT.INS_RT_RENEW_CERT_1);
			byte[] scPack2 = CT.sendToCard(pack2, CT.INS_RT_RENEW_CERT_2); //here I receive the km signed and encrypted
			
			//get the km from the card
			byte[] km = new byte[2];
			System.out.println("p2: "+ byteUtil.toHexString(scPack2));
			System.arraycopy(scPack2, 0, km, 0, km.length);
			short skm = byteUtil.bytesToShort(km);
			card.setKilometers(skm);
			
			//extract expiration date from the new certificate and convert it to Long 
			long expNew  = byteUtil.bytesToLong(serial.getExpFromCert(newCert));
			card.setExpiration(expNew);
			
			
		} catch (RevokedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return card;
	}
	
	public Card TopUpCard(Card card, short cardKm){
		
		byte[] data = new byte[3];
		data[0] = CT.MSG_TOPUP;
		byte[] ukmB = byteUtil.shortToBytes(cardKm);
		card.setKilometers((short)(card.getKilometers() + cardKm));
		System.arraycopy(ukmB, 0, data, 1, 2);
		//System.out.print("data: "+ byteUtil.toHexString(data));
		byte[] scTopUp = CT.sendToCard(data, CT.INS_RT_TOPUP_KM);
		//System.out.println(byteUtil.toHexString(scTopUp));
		
		byte[] sig = new byte[CV.SIG_LENGTH]; //SIGNATURE 128
		System.arraycopy(card.getCertificate(), CV.PUBMODULUS + 1 + CV.EXP_LENGTH, sig, 0, CV.SIG_LENGTH);
		//get card public key
		//split the certificate into two packages
		byte[] pubkey = new byte[CV.PUBMODULUS];  //128 
		System.arraycopy(card.getCertificate(), 1, pubkey, 0, pubkey.length); 
		card.setCardModulus(pubkey);
		
		//add to the log
		addLogEntry(ukmB, sig);
		//update to database  
		db.updateKilometersExpiration(cardKm, card.getExpDate(), pubkey);
			
		return card;
	}
	
	
	private void addLogEntry(byte[] data, byte[] signature) {
		FileOutputStream file;
		try {
			file = new FileOutputStream("RTLogFile", true); //True to append
			file.write(data);
			file.write(signature);
			file.close();
		} catch ( IOException e) {
			e.printStackTrace();
		}
	}
	
	
	//Refund the kilometers
	public Card refundKilometers(Card card){
		byte[] data = new byte[1];
		data[0] = CT.MSG_REFUND;
		byte[] scRefund = CT.sendToCard(data, CT.INS_RT_REFUND_KM);
		
		byte[] sig = new byte[CV.SIG_LENGTH]; //SIGNATURE 128
		System.arraycopy(card.getCertificate(), CV.PUBMODULUS + 1 + CV.EXP_LENGTH, sig, 0, CV.SIG_LENGTH);
		//get card public key
		//split the certificate into two packages
		byte[] pubkey = new byte[CV.PUBMODULUS];  //128 
		System.arraycopy(card.getCertificate(), 1, pubkey, 0, pubkey.length); 
		card.setCardModulus(pubkey);
		//add to the log
		addLogEntry(data, sig);
		//update to database  
		db.updateKilometersExpiration(0, card.getExpDate(), pubkey);
		
	
		card.setKilometers((short)0); 		//3. do the refund
				
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
		//String strPubKey = serial.SerializeByteKey(cardPublicKey);
		
		ResultSet rs = db.selectCard(cardPublicKey);
		try {
			if(rs.next()){
				card = new Card(rs.getInt("id"), rs.getInt("custID"), rs.getString("custName"), 
						 rs.getShort("km"), rs.getLong("exp") , rs.getInt("revoke"), rs.getString("cardPK"));
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
	

	
	
	

}
