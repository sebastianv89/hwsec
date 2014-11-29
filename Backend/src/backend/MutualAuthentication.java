package backend;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;

public class MutualAuthentication {
	ByteUtils util = new ByteUtils();
	ConstantValues CV = new ConstantValues();
	Serialization serial = new Serialization();
	
	int NOUNCE_LENGTH = 16;
	
	public MutualAuthentication(){
		
	}
	
	/**
	 * Rental Terminal Authentication:
	 * 1. Read RT Certificate from file
	 * 2. Generate Nonce
	 * 3. Combine those two and split them into two package;
	 * 4. Send those two package to card
	 * 5. Received 4 packages from smartcard (ask the card for the every next package; mentioned the package number in P2)
	 * 6. Read RT Priv Key from file
	 * 7. Decrypt every packages
	 * 8. Combine the decrypted packages
	 * 9. Split the smartcard's certificate (certS) and Data (N, Ktmp) from the decrypted packages
	 * 10.Verify the Certificate (with the CASignKey - from file - and check the revocation status)
	 * 11. Verify the card Signature (data, cardPubKey, dataSignature)
	 * 12. Send the session key to card (later)
	 */
	
	public void TerminalMutualAuth(byte[] cert, RSAPrivateKey privKey){
		
		//byte[] rtCert = readFiles(certFilename);  //Read Certificate for  Terminal
		byte[] nounce = util.GenerateRandomBytes(NOUNCE_LENGTH); //generate nonces
		
		//Split the certificate into two package
		byte[] pack1 = new byte[CV.PUBMODULUS + 1];  //129
		System.arraycopy(cert, 0, pack1, 0, pack1.length); 
		byte[] pack2 = new byte[CV.SIG_LENGTH + NOUNCE_LENGTH]; //SIGNATURE + NOUNCE = 144
		System.arraycopy(cert, CV.PUBMODULUS+1, pack2, 0, CV.SIG_LENGTH);
		System.arraycopy(nounce, 0, pack2, CV.SIG_LENGTH, nounce.length);
		
		//TODO: send to card
		
		//TODO: GET PACKAGE FROM CARD
		byte[] scPack1 = new byte[128]; //received 1st package from sc
		byte[] scPack2 = new byte[128]; //received 2nd package from sc
		byte[] scPack3 = new byte[128]; //received 3nd package from sc
		byte[] scPack4 = new byte[128]; //received 4nd package from sc
		
		//get The private Key
		//RSAPrivateKey rtPrivKey = GetPrivateKeyFromFile("RTPrivateKey");
		byte[] scDataPack1 = RSADecrypt(scPack1, privKey);
		byte[] scDataPack2 = RSADecrypt(scPack2, privKey);
		byte[] scDataPack3 = RSADecrypt(scPack3, privKey);
		byte[] scDataPack4 = RSADecrypt(scPack4, privKey);
		
		//combine the decrypted package
		byte[] scPack = serial.combineThePackage(scDataPack1, scDataPack2, scDataPack3, scDataPack4);
		
		//split card certificate and the card data {N, Ktmp}
		
		
		
		
		/*//for testing purposes
		new Random().nextBytes(scPack1);
		new Random().nextBytes(scPack2);
		RSAPublicKey rtPubKey = GetPublicKeyFromFile("RTPublicKey");
		System.out.println(Arrays.toString(scPack));
		byte[] enc = RSAEncrypt(scPack, rtPubKey);
		System.out.println(Arrays.toString(enc));
		System.out.println(enc.length);*/
		
		
		
		
		
	}
	
	public static void main(String[] args) {
		MutualAuthentication ma = new MutualAuthentication();
		ma.TerminalMutualAuth(null, null);
	}
	
	private byte[] readFiles(String filename) {
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
	
	private RSAPrivateKey GetPrivateKeyFromFile(String filename){
		byte[] privateKeyBytes = readFiles(filename);
		RSAPrivateKey privKey = null;
		try {
			privKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return privKey;
	}
	
	
	private RSAPublicKey GetPublicKeyFromFile(String filename){
		byte[] publicKeyBytes = readFiles(filename);
		RSAPublicKey pubKey = null;
		X509EncodedKeySpec pubspec = new X509EncodedKeySpec(publicKeyBytes);
		KeyFactory factory;
		
		try {
			factory = KeyFactory.getInstance("RSA");
			pubKey = (RSAPublicKey) factory.generatePublic(pubspec);
		}catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return pubKey;
	}
	
	
	public byte[] RSADecrypt(byte[] ciphertext, RSAPrivateKey privatekey) {
		byte[] plaintext = null;
		try {
			/* Create cipher for decryption. */
			Cipher decrypt_cipher = Cipher.getInstance("ALG_RSA_ISO14888");
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
	
	
	
	public byte[] RSAEncrypt(byte[] plaintext, RSAPublicKey publicKey) {
		 byte[] ciphertext = null;
	      try {
	         /* Create a cipher for encrypting. */
	         Cipher encrypt_cipher = Cipher.getInstance("ALG_RSA_ISO14888" );
	         encrypt_cipher.init(Cipher.ENCRYPT_MODE, publicKey);

	         /* Encrypt the secret message and store in file. */
	        ciphertext = encrypt_cipher.doFinal(plaintext);
	      } catch (Exception e) {
	         e.printStackTrace();
	      }
	      return ciphertext;
	   }

}
