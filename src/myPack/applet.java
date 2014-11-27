package myPack;
/*****************
 * **************
 * REFERENCES:
 * https://community.oracle.com/thread/1751790?start=0&tstart=0
 * http://www.wrankl.de/Javacard/Javacard.html#JavaD_JC
 */


import applet.RandomData;
import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

public class applet extends Applet {

	 counter c1 = new counter();

		
	 final static short MAX_BALANCE = 10000;
	
	final static byte    CLASS       = (byte) 0x80;     // class of the APDU commands
	  final static byte    INS_READ    = (byte) 0x06;     // instruction for the READ APDU command
	  final static byte    INS_diseng   = (byte) 0x04;     // instruction for the WRITE APDU command
	  final static byte    INS_ded   = (byte) 0x02;     // instruction for the WRITE APDU command
	  final static byte    INS_cnt   = (byte) 0x03; 
	  final static byte    INS_add   = (byte) 0x05;
	  
	  final static short   SIZE_MEMORY = (short)   9;     // size of the data storage area
	  static byte[]        memory;                        // this is the data memory for the application

	  final static short SW_INVALID_TRANSACTION_AMOUNT = 0x6E84;
	  final static short SW_NEGATIVE_BALANCE = 0x6E85;
	  
	  //**Exceptions**//
	 final static short NONCE_FAILURE = (short) 13000;
	 final static short SIGNATURE_FAILURE = (short) 13001;
	 //creating temp buffer on card
	 byte[] tmp;
		
		short temp_short_1;
		short temp_short_2;
		
		// The RandomData instance.
		private RandomData random;
	  
	//----- installation and registration of the applet -----
	  public static void install(byte[] buffer, short offset, byte length) {
	    memory = new byte[SIZE_MEMORY];  // this is the data storage area
	    new applet().register();
	  }  // install
	  
	  
	  //----- this is the command dispatcher -----
	  public void process(APDU apdu) {
	    byte[] cmd_apdu = apdu.getBuffer();

	    if (cmd_apdu[ISO7816.OFFSET_CLA] == CLASS) {  // it is the rigth class
	      switch(cmd_apdu[ISO7816.OFFSET_INS]) {      // check the instruction byte
	        case INS_READ:                            // it is a READ instruction
	          cmdREAD(apdu);
	          break;
	        case INS_diseng:                           // it is a WRITE instruction
	          cmdWRITE(apdu);
	          break;
	        case INS_ded:                           // it is a WRITE instruction
		          deduct(apdu);
		          break;
	        case INS_cnt:                           // it is a WRITE instruction
	        	getCnt(apdu);
		          break;
		          
	        case INS_add:
	             add(apdu);
		           break;
	        default :                                 // the instruction in the command apdu is not supported
	          ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
	      }  // switch
	    }  // if
	    else {                                        // the class in the command apdu is not supported
	      ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
	    }  // else
	  }  // process

private void add(APDU apdu) {
		  
		  byte[] buffer = apdu.getBuffer();
		  byte numBytes = buffer[ISO7816.OFFSET_LC];
		
          byte byteRead = (byte)(apdu.setIncomingAndReceive());
          short shortAmount = 0;
        
          if (numBytes == 2){
               shortAmount = (short) Util.getShort(buffer, ISO7816.OFFSET_CDATA);
          }
          else if (numBytes == 1) {
               shortAmount = (short) buffer[ISO7816.OFFSET_CDATA];
          }

          
          
          c1.counter = (short)(c1.counter + shortAmount);
        

          return;
		  
	  }
	  
	  private void deduct(APDU apdu) {
		  
		  byte[] buffer = apdu.getBuffer();
		  byte numBytes = buffer[ISO7816.OFFSET_LC];
		
          byte byteRead = (byte)(apdu.setIncomingAndReceive());
          short shortAmount = 0;
        
          if (numBytes == 2){
               shortAmount = (short) Util.getShort(buffer, ISO7816.OFFSET_CDATA);
          }
          else if (numBytes == 1) {
               shortAmount = (short) buffer[ISO7816.OFFSET_CDATA];
          }

          // check the debit amount
          if (( shortAmount < 0 )) {
               ISOException.throwIt(SW_INVALID_TRANSACTION_AMOUNT);
          }
          // check the new balance
          if ((short)( c1.counter - shortAmount)  < 0) {
               ISOException.throwIt(SW_NEGATIVE_BALANCE);
          }
          // debit the amount
          c1.counter = (short)(c1.counter - shortAmount);

          return;
		  
	  }
	  
	  private void getCnt(APDU apdu) {

          byte[] buffer = apdu.getBuffer();

          // inform the JCRE that the applet has data to return
          short le = apdu.setOutgoing();

          // set the actual number of the outgoing data bytes
          apdu.setOutgoingLength((byte)2);


          // write the balance into the APDU buffer at the offset 0
          Util.setShort(buffer, (short)0, (short) (c1.counter+1));

          // send the 2-byte balance at the offset
          // 0 in the apdu buffer
          apdu.sendBytes((short)0, (short)2);

     }

	  
	  //----- program code for the APDU command READ -----
	  private void cmdREAD(APDU apdu) {
	    byte[] cmd_apdu = apdu.getBuffer();
	    //----- check the preconditions -----
	    // check if P1=0
	    if (cmd_apdu[ISO7816.OFFSET_P1] != 0) ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
	    // check if offset P2 is inside the bound of the memory array
	    short offset = (short) (cmd_apdu[ISO7816.OFFSET_P2] & 0x00FF); // calculate offset
	    if (offset >= SIZE_MEMORY) ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
	    // check if offset P2 and expected length Le is inside the bounds of the memory array
	    short le = (short)(cmd_apdu[ISO7816.OFFSET_LC] & 0x00FF);  // calculate Le (expected length)
	    if ((offset + le) > SIZE_MEMORY) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
	    // check if expected length Le is 0
	    if (le == 0) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

	    //----- now all preconditions are fulfilled, the data can be send to the IFD -----
	    apdu.setOutgoing();                                   // set transmission to outgoing data
	    apdu.setOutgoingLength((short)le);                    // set the number of bytes to send to the IFD
	    apdu.sendBytesLong(memory, (short)offset, (short)le); // send the requested number of bytes to the IFD
	  }  // cmdREAD

	
	  private void cmdWRITE(APDU apdu) {
	    byte[] cmd_apdu = apdu.getBuffer();
	    //----- check the preconditions -----
	    // check if P1=0
	    if (cmd_apdu[ISO7816.OFFSET_P1] != 0) ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
	    // check if offset P2 is inside the bound of the memory array
	    short offset = (short) (cmd_apdu[ISO7816.OFFSET_P2] & 0x00FF); // calculate offset
	    if (offset >= SIZE_MEMORY) ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
	    // check if offset P2 and expected length Le is inside the bounds of the memory array
	    short lc = (short)(cmd_apdu[ISO7816.OFFSET_LC] & 0x00FF);  // calculate Lc (expected length)
	    if ((offset + lc) > SIZE_MEMORY) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
	    // check if command length Lc is 0
	    if (lc == 0) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

	    receiveAPDUBody(apdu);    // receive now the rest of the APDU
	    //----- now all preconditions are fulfilled, the data can be copied to the memory -----
	    Util.arrayCopy(cmd_apdu, (short)((ISO7816.OFFSET_CDATA) & 0x00FF), memory, offset, lc);  // this copy precedure is atomic
	   
	    ISOException.throwIt(ISO7816.SW_NO_ERROR);   // command proper executed
	  }  // cmdWRITE

	  //----- receive the body of the command APDU
	  public void receiveAPDUBody(APDU apdu) {
	    byte[] buffer = apdu.getBuffer();
	    short lc = (short)(buffer[ISO7816.OFFSET_LC] & 0x00FF);  // calculate Lc (expected length)
	    // check if Lc != number of received bytes of the command APDU body
	    if (lc != apdu.setIncomingAndReceive()) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
	  }  // receiveAPDUBody

}
