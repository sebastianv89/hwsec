package backend;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class MutualAuthentication {
	ByteUtils util = new ByteUtils();
	ConstantValues CV = new ConstantValues();
	Serialization serial = new Serialization();
	CardTerminalCommunication CT = new CardTerminalCommunication();

	int NONCE_LENGTH = 16;

	public MutualAuthentication(){

	}

	/**
	 * Terminal Mutual Authentication:
	 * 1. Read RT Certificate from file (OK)
	 * 2. Generate Nonce (OK)
	 * 3. Combine those two and split them into two package; (OK)
	 * 4. Send those two package to card --
	 * 5. Received 4 packages from smartcard --
	 * (ask the card for the every next package; differentiate the package number in INS) --
	 * 6. Read RT Priv Key from file (OK)
	 * 7. Decrypt every packages (OK)
	 * 8. Combine the decrypted packages (OK)
	 * 9. Split the smartcard's certificate (certS) and Data (N, Ktmp) from the decrypted packages (OK)
	 * 10.Verify the Certificate (with the CAPublicKey - from file - and check the revocation status) (OK)
	 * 11. Verify the card Signature (data, cardPubKey, dataSignature) (Max help. OK)
	 * 12. Send the session key to card  --
	 */

	public byte[] TerminalMutualAuth(byte[] cert, RSAPrivateKey privKey){
		byte[] cardCert = null;
		byte[] scPack1 = null;
		byte[] sessionKey = null;


		//byte[] rtCert = readFiles(certFilename);  //Read Certificate for  Terminal
		byte[] nounce = util.GenerateRandomBytes(NONCE_LENGTH); //generate nonces

		//Split the certificate into two package
		byte[] pack1 = new byte[CV.PUBMODULUS + 1];  //129
		System.arraycopy(cert, 0, pack1, 0, pack1.length); 
		byte[] pack2 = new byte[CV.SIG_LENGTH + NONCE_LENGTH]; //SIGNATURE + NONCE = 144
		System.arraycopy(cert, CV.PUBMODULUS+1, pack2, 0, CV.SIG_LENGTH);
		System.arraycopy(nounce, 0, pack2, CV.SIG_LENGTH, nounce.length);
		
		byte[] sig = new byte[CV.SIG_LENGTH];
		System.arraycopy(pack2, 0, sig, 0, CV.SIG_LENGTH);
		/* DEBUG
		try {
			FileInputStream file = new FileInputStream("CAPublicKey");
			byte[] bytes = new byte[file.available()];
			file.read(bytes);
			X509EncodedKeySpec pubspec = new X509EncodedKeySpec(bytes);
			KeyFactory factory = KeyFactory.getInstance("RSA");
			RSAPublicKey capubkey = (RSAPublicKey) factory.generatePublic(pubspec);
			System.err.println("checking signature of terminal: " + sigVerif(pack1, util.getBytes(capubkey.getModulus()), sig));
			file.close();
			
			CertAuth ca = new CertAuth();
			System.err.println("check again: " + sigVerif(pack1, util.getBytes(ca.getVerificationKey().getModulus()), sig));
		} catch (Exception e) {
			System.err.println(e);
			System.exit(1);
		}
		*/

		//TODO: send to card  // consider while loop. if the card is not responding ?
		CT.sendToCard(pack1, CT.INS_AUTH_1);
		scPack1 = CT.sendToCard(pack2, CT.INS_AUTH_2);

		//TODO: GET PACKAGE FROM CARD -- SIZE 128
		byte[] scPack2 = CT.sendToCard(null, CT.INS_AUTH_3); //received 2nd package from sc
		byte[] scPack3 = CT.sendToCard(null, CT.INS_AUTH_4); //received 3nd package from sc
		byte[] scPack4 = CT.sendToCard(null, CT.INS_AUTH_5); //received 4nd package from sc

		//get The private Key
		//RSAPrivateKey rtPrivKey = GetPrivateKeyFromFile("RTPrivateKey");
		byte[] scDataPack1 = RSADecrypt(scPack1, privKey);
		byte[] scDataPack2 = RSADecrypt(scPack2, privKey);
		byte[] scDataPack3 = RSADecrypt(scPack3, privKey);
		byte[] scDataPack4 = RSADecrypt(scPack4, privKey);

		//combine the decrypted package
		byte[] scPack = serial.combineThePackage(scDataPack1, scDataPack2, scDataPack3, scDataPack4);
		System.out.println(util.toHexString(scPack));
		
		//split card certificate and the card data {N, Ktmp}Sig
		cardCert = new byte[CV.CARDCERT_LENGTH];
		System.arraycopy(scPack, 0, cardCert, 0, CV.CARDCERT_LENGTH);		
		byte[] cardData = new byte[CV.CARDDATA_LENGTH];
		System.arraycopy(scPack, CV.CARDCERT_LENGTH, cardData, 0, CV.CARDDATA_LENGTH);
		//get card public key (from cardCert)
		byte[] certPubKey = serial.getPublicKeyFromCert(cardCert);
		//get signature from cardData
		byte[] cardDataSig = serial.getSigFromCert(cardData);
		//Verify the certificate
		if(certVerify(cardCert)){
			//Verify the cardData {N, Ktmp} signature
			if(sigVerif(cardData, certPubKey, cardDataSig)){
				//TODO send session key to card
				CT.sendToCard(null, CT.INS_AUTH_6, sessionKey);
			}else{
				//Emptying the card certificate --> the card cert is not valid
				cardCert = null;
			}
		}else{
			//Emptying the card certificate --> the card cert is not valid
			cardCert = null;
		}

		return cardCert;
	}

	/**
	 * Init Verify with CApubKey
	 * Update with card cert data 
	 * sig.verify with card signature
	 * @param certSC
	 * @return
	 */
	public boolean certVerify(byte[] cardCert){
		boolean result = false;
		byte[] CApubKey = readFiles("CAPublicKey");
		//split the certificate data and the signature 
		byte[] certData = serial.getCardCertDataFromCert(cardCert); //without signature
		byte[] certSig = serial.getSigFromCert(cardCert); //card signature

		if(sigVerif(certData, CApubKey, certSig)){
			result = true;
		}

		return result;
	}

	public boolean sigVerif(byte[] data, byte[] pubKey, byte[] signature) {
		// Convert bytekey (public Modulus into a RSAPublicKey
		RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(pubKey), CV.PUBEXPONENT_BYTE);
		RSAPublicKey pub = null;
		try {
			KeyFactory factory = KeyFactory.getInstance("RSA");
			pub = (RSAPublicKey) factory.generatePublic(spec);
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Signature sig;
		boolean result = false;
		try {
			sig = Signature.getInstance("MD5WithRSA");
			sig.initVerify(pub);
			sig.update(data);
			result = sig.verify(signature);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}

	public static void main(String[] args) {
		MutualAuthentication ma = new MutualAuthentication();
		ma.TerminalMutualAuth(null, null);
	}

	public byte[] readFiles(String filename) {
		FileInputStream file;
		byte[] bytes = null;
		try {
			file = new FileInputStream(filename);
			bytes = new byte[file.available()];
			file.read(bytes);
			file.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bytes;
	}





	public byte[] RSADecrypt(byte[] ciphertext, RSAPrivateKey privatekey) {
		byte[] plaintext = null;
		try {
			/* Create cipher for decryption. */
			Cipher decrypt_cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			decrypt_cipher.init(Cipher.DECRYPT_MODE, privatekey);

			/* Reconstruct the plaintext message. */
			byte[] temp = decrypt_cipher.doFinal(ciphertext);
			plaintext  = new byte[temp.length];
			plaintext = temp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return plaintext;
	}


	/*
	public byte[] RSAEncrypt(byte[] plaintext, RSAPublicKey publicKey) {
		 byte[] ciphertext = null;
	      try {
	         // Create a cipher for encrypting.
	         Cipher encrypt_cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
	         encrypt_cipher.init(Cipher.ENCRYPT_MODE, publicKey);

	         // Encrypt the secret message and store in file.
	        ciphertext = encrypt_cipher.doFinal(plaintext);
	      } catch (Exception e) {
	         e.printStackTrace();
	      }
	      return ciphertext;
	   }*/

}
