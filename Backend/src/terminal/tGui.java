package terminal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
/***
 * References: http://www.mkyong.com/swt/
 * ***/


public class tGui {

	
	
	/******
	 * ms == 1 for displaying the current balance
	 * ms == 2 showing error window and starting "safe stopping"
	 * ms == 3 showing error for response time exceed and starting "safe stopping"
	 * ms == 4 message for -it is ok to remove the card //stopping the car
	 * ms == 5 message for -no balance --safe stopping
	 * ms == 6 message for -ok to start driving -turn the key
	 * 
	 * ********/

 	public static void message(String ms){
 		
 		
 		if(ms.equals("1")){
 		Display display = new Display ();
		Shell shell = new Shell(display);
		shell.setText("Vehicle Terminal");
			Text helloWorldTest = new Text(shell, SWT.NONE);
			helloWorldTest.setText("Welcome to the Vehicle Terminal");
			helloWorldTest.pack();
		shell.open ();
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose ();
 		}
 		
 		if(ms.equals("2")){
 	 		Display display = new Display ();
 			Shell shell = new Shell(display);
 			shell.setText("Vehicle Terminal");
 				final Text t = new Text(shell, SWT.NONE);
 				t.setText("Error Occured. Safe stopping now...");
 				t.pack();
 			
 				Button pushButton = new Button(shell, SWT.PUSH);
 	 			pushButton.setLocation(50, 50);
 	 			pushButton.setText("Drive 1 km");
 	 			pushButton.pack();
 	 			
 	 		  pushButton.addSelectionListener(new SelectionListener() {

 	 		      public void widgetSelected(SelectionEvent event) {
 	 		        // call driving();
 	 		      }

 	 		      public void widgetDefaultSelected(SelectionEvent event) {
 	 		    	   // call driving();
 	 		      }
 	 		    });
 	 			
 	 			
 	 			shell.setSize(500,500);
 				
 				shell.open ();
 			
 			
 			while (!shell.isDisposed ()) {
 				if (!display.readAndDispatch ()) display.sleep ();
 			}
 			display.dispose ();
 			
 			
 			
 			
 	 		}
 		
 		if(ms.equals("3")){
 	 		Display display = new Display ();
 			Shell shell = new Shell(display);
 			shell.setText("Vehicle Terminal");
 				Text helloWorldTest = new Text(shell, SWT.NONE);
 				helloWorldTest.setText("Response time exceeded. Safe stopping now...");
 				helloWorldTest.pack();
 				Button pushButton = new Button(shell, SWT.PUSH);
 	 			pushButton.setLocation(50, 50);
 	 			pushButton.setText("Drive 1 km");
 	 			pushButton.pack();
 	 			
 	 			 pushButton.addSelectionListener(new SelectionListener() {

 	 	 		      public void widgetSelected(SelectionEvent event) {
 	 	 		       // call driving();
 	 	 		      }

 	 	 		      public void widgetDefaultSelected(SelectionEvent event) {
 	 	 		       // call driving();
 	 	 		      }
 	 	 		    });
 	 			
 	 			shell.setSize(500,500);
 			shell.open ();
 			while (!shell.isDisposed ()) {
 				if (!display.readAndDispatch ()) display.sleep ();
 			}
 			display.dispose ();
 	 		}
 		
 		if(ms.equals("4")){
 	 		Display display = new Display ();
 			Shell shell = new Shell(display);
 			shell.setText("Vehicle Terminal");
 				Text helloWorldTest = new Text(shell, SWT.NONE);
 				helloWorldTest.setText("It is ok to remove the card.");
 				helloWorldTest.pack();
 				Button pushButton = new Button(shell, SWT.PUSH);
 	 			pushButton.setLocation(50, 50);
 	 			pushButton.setText("Drive 1 km");
 	 			pushButton.pack();
 	 			
 	 			
 	 			 pushButton.addSelectionListener(new SelectionListener() {

 	 	 		      public void widgetSelected(SelectionEvent event) {
 	 	 		       // call driving();
 	 	 		      }

 	 	 		      public void widgetDefaultSelected(SelectionEvent event) {
 	 	 		       // call driving();
 	 	 		      }
 	 	 		    });
 	 			shell.setSize(500,500);
 			shell.open ();
 			while (!shell.isDisposed ()) {
 				if (!display.readAndDispatch ()) display.sleep ();
 			}
 			display.dispose ();
 	 		}
 		
 		if(ms.equals("5")){
 	 		Display display = new Display ();
 			Shell shell = new Shell(display);
 			shell.setText("Vehicle Terminal");
 				Text helloWorldTest = new Text(shell, SWT.NONE);
 				helloWorldTest.setText("No Balance left. Safe stopping now...");
 				helloWorldTest.pack();
 				Button pushButton = new Button(shell, SWT.PUSH);
 	 			pushButton.setLocation(50, 50);
 	 			pushButton.setText("Drive 1 km");
 	 			pushButton.pack();
 	 			
 	 			 pushButton.addSelectionListener(new SelectionListener() {

 	 	 		      public void widgetSelected(SelectionEvent event) {
 	 	 		       // call driving();
 	 	 		      }

 	 	 		      public void widgetDefaultSelected(SelectionEvent event) {
 	 	 		       // call driving();
 	 	 		      }
 	 	 		    });
 	 			shell.setSize(500,500);
 			shell.open ();
 			while (!shell.isDisposed ()) {
 				if (!display.readAndDispatch ()) display.sleep ();
 			}
 			display.dispose ();
 	 		}
 		
 		if(ms.equals("6")){
 	 		Display display = new Display ();
 			Shell shell = new Shell(display);
 			shell.setText("Vehicle Terminal");
 				Text helloWorldTest = new Text(shell, SWT.NONE);
 				helloWorldTest.setText("It is ok to start driving now.");
 				helloWorldTest.pack();
 				Button pushButton = new Button(shell, SWT.PUSH);
 	 			pushButton.setLocation(50, 50);
 	 			pushButton.setText("Drive 1 km");
 	 			pushButton.pack();
 	 			
 	 			
 	 			 pushButton.addSelectionListener(new SelectionListener() {

 	 	 		      public void widgetSelected(SelectionEvent event) {
 	 	 		       // call driving();
 	 	 		      }

 	 	 		      public void widgetDefaultSelected(SelectionEvent event) {
 	 	 		       // call driving();
 	 	 		      }
 	 	 		    });
 	 			shell.setSize(500,500);
 			shell.open ();
 			while (!shell.isDisposed ()) {
 				if (!display.readAndDispatch ()) display.sleep ();
 			}
 			display.dispose ();
 	 		}
 		
 		
 	}
 
	public static void main(String[] args) {
	
		//Display display = new Display ();
		//Shell shell = new Shell(display);
		Integer balance = 100;
		
		
		String ms = "1";
		message("2");
	 
      
	}
	


}

