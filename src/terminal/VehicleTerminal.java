package terminal;

import java.util.List;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;

import backend.Backend;
import backend.InitData;

public class VehicleTerminal {
	
	public static void main(String[] args){
		Backend bk = new Backend();
		InitData id = bk.registerVehicleTerminal();
		
		byte[] c = id.certificate;
		byte[] vk = id.caVerifKey;
		byte[] sk = id.secretKey;
		byte[] pk = id.privateKey;
	}
	
		
		public boolean isCardPresent() {
			
			SmartCard sc = bk.
			List<SmartCard> sc;
			try {
				sc = ct.list(SmartCard.State.CARD_PRESENT);
				for (CardTerminal c : sc) {
					if (c.isCardPresent()){
						return this.ready;
					}
				}
			} catch (CardException e) {
				// Do nothing
			}
			
			return false;
		}

		
		

	

}
