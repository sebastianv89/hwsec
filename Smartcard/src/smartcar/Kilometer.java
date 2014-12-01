package smartcar;

import javacard.framework.JCSystem;

/**
 * Kilometer counter, with a round-robin memory implementation to avoid wear on
 * the EEPROM
 */
public class Kilometer {

	public static final short MAX_KM = 32767; // 0x7fff

	private static final byte N = 8;

	private short[] value;
	private byte i;

	Kilometer() {
		i = 0;
		value = new short[N];
	}

	short getKm() {
		return value[i];
	}

	private void setKm(short km) {
		byte j = (byte) ((i + 1) % N);
		JCSystem.beginTransaction();
		value[j] = km;
		i = j;
		JCSystem.commitTransaction();
	}

	/**
	 * Tick one km
	 * 
	 * @return false if the counter is zero
	 */
	boolean tick() {
		short km = getKm();
		if (km <= 0) {
			return false;
		}
		setKm((short) (km - 1));
		return true;
	}

	/**
	 * Tick two km
	 * 
	 * @return false if the counter is smaller than 2
	 */
	boolean start() {
		short km = getKm();
		if (km <= 1) {
			return false;
		}
		setKm((short) (km - 2));
		return true;
	}

	/**
	 * Add one km
	 * 
	 * @return false if the counter will overflow
	 */
	boolean stop() {
		if (value[i] == MAX_KM) {
			return false;
		}
		setKm((short) (getKm() + 1));
		return true;
	}

	boolean topupAllowed(short amount) {
		return amount >= 0 && amount < MAX_KM;
	}

	void topup(short amount) {
		if (!topupAllowed(amount)) {
			return;
		}
		setKm(amount);
	}

	void reset() {
		byte j;
		JCSystem.beginTransaction();
		for (j = 0; j < N; ++j) {
			value[j] = (short) 0;
		}
		i = 0;
		JCSystem.commitTransaction();
	}
}
