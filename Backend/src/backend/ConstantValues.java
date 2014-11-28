package backend;

public class ConstantValues {
	/*
	 * Certificate for the terminals should not have an expdate
	 * Certificate WITOUT expdate is a byte array that looks like:
	 * cert[0] = type (0 = smartcard, 1 = rentalterm, 2 = vehicleterm)
	 * cert[1..163] = rsapublickey (length 162bytes)
	 * cert[164...292] = Signature (128bytes)
	 */
	
	/* TODO: Change exp to short
	 * Certificate with expdate is a byte array that looks like:
	 * cert[0] = type (0 = smartcard, 1 = rentalterm, 2 = vehicleterm)
	 * cert[1..163] = rsapublickey (length 162bytes)
	 * cert[164..172] = expiration date of type long (8bytes)
	 * cert[173...301] = Signature (128bytes)
	 */
	public int RSAPUBLICKEYLENGTH = 162;
	public int EXP_LENGTH = 8;
	public int PK_EXP_LENGTH = 170; //162+8
	public int SIG_LENGTH = 128;
	


}
