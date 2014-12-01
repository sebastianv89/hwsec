package backend;

import java.math.BigInteger;

public class ConstantValues {

	public int RSAPUBLICKEYLENGTH = 162;
	public int PUBMODULUS = 128; // after you strip off the first byte (which = 0)
	public int PUBEXPONENT = 3;
	public int PRIVMODULUS = 128; // after you strip off the first byte (which = 0)
	public BigInteger PUBEXPONENT_BYTE = new BigInteger("65537".getBytes());
	public int PRIVEXPONENT = 128; // after you strip off the first byte (which = 0)
	public int EXP_LENGTH = 8;
	public int PK_EXP_LENGTH = 170; //162+8
	public int SIG_LENGTH = 128;
	
	public int EXPIRATIONSTARTPOS = 129;
	
	public int CARDCERT_LENGTH = 265; //137 (type, modulo, expiration) +128 (signature)	
	public int TERMINALCERT_LENGTH = 257; //1 (type) + 128 (modulo) + 128 (signature)
	public int CARDCERTDATA_LENGTH = 137; //1 (type) + 128 (modulo) + 8 (expiration)
	public int CARDDATA_LENGTH = 160; //16 (nonce) + 16 (Ktemp) + 128 (SKs)
	


}
