package smartcar;

import javacard.framework.JCSystem;

/**
 * Kilometer counter, with a round-robin memory implementation to avoid wear on
 * the EEPROM
 */
public class Kilometer {

	private static final byte N = 8;

	private short[] km;
	private byte i;

	Kilometer() {
		i = 0;
		km = new short[N];
	}

	/**
	 * Tick one km
	 * 
	 * @return false if the counter is zero
	 */
	boolean tick() {
		if (km[i] == 0) {
			return false;
		}
		byte j = (byte) ((i + 1) % N);
		JCSystem.beginTransaction();
		km[j] = (short) (km[i] - 1);
		i = j;
		JCSystem.commitTransaction();
		return true;
	}
	
	/**
	 * Tick two km
	 * @return false if the counter is smaller than 2
	 */
	boolean start() {
		if (km[i] <= 1) {
			return false;
		}
		byte j = (byte) ((i + 1) % N);
		JCSystem.beginTransaction();
		km[j] = (short) (km[i] - 2);
		i = j;
		JCSystem.commitTransaction();
		return true;
	}
	
	void stop() {
		byte j = (byte) ((i + 1) % N);
		JCSystem.beginTransaction();
		km[j] = (short) (km[i] + 1);
		i = j;
		JCSystem.commitTransaction();
	}

	short getKm() {
		return km[i];
	}

	void setKm(short km) {
		this.km[i] = km;
	}

	void reset() {
		byte j;
		JCSystem.beginTransaction();
		for (j = 0; j < N; ++j) {
			km[j] = (short) 0;
		}
		i = 0;
		JCSystem.commitTransaction();
	}
}
