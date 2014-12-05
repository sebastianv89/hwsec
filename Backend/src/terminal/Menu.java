package terminal;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import backend.BackendRentalTerminal;

public class Menu {

	protected Shell shell;
	protected static Menu window;

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			window = new Menu();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	
	public void close(){
		Display display = Display.getDefault();
		while(!shell.isDisposed()){
			display.dispose();
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		final RegisterNewCustomer regWindow = new RegisterNewCustomer();
		final TopUp topUpwindow = new TopUp();
		final Refund refundWindow = new Refund();
		final BackendRentalTerminal rt = new BackendRentalTerminal();
		
		shell = new Shell();
		shell.setSize(346, 300);
		shell.setText("SWT Application");
		
		Label label = new Label(shell, SWT.NONE);
		label.setText("Welcome to Cars Rental");
		label.setAlignment(SWT.CENTER);
		label.setBounds(10, 10, 324, 27);
		
		Label lblNewLabel = new Label(shell, SWT.NONE);
		lblNewLabel.setBounds(28, 68, 390, 17);
		lblNewLabel.setText("Insert your card and choose the menu below");
		
		final Button btnRadioButton_Register = new Button(shell, SWT.RADIO);
		btnRadioButton_Register.setBounds(28, 108, 211, 22);
		btnRadioButton_Register.setText("Register New Customer");
		
		final Button btnRadioButton_TopUp = new Button(shell, SWT.RADIO);
		btnRadioButton_TopUp.setBounds(28, 136, 108, 22);
		btnRadioButton_TopUp.setText("Top Up");
		
		final Button btnRadioButton_Refund = new Button(shell, SWT.RADIO);
		btnRadioButton_Refund.setBounds(28, 164, 108, 22);
		btnRadioButton_Refund.setText("Refund");
		
		Button btnSubmit = new Button(shell, SWT.NONE);
		btnSubmit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String m = "";
				if(btnRadioButton_Register.getSelection()){
					m = "register";
					window.close();
					regWindow.open();
				}else if(btnRadioButton_TopUp.getSelection()){
					m = "top up";
					window.close();
					Card card = rt.AuthenticateCard();
					topUpwindow.setCard(card);
					topUpwindow.open();
				}else if(btnRadioButton_Refund.getSelection()){
					m = "refund";
					window.close();
					Card card = rt.AuthenticateCard();
					refundWindow.setCard(card);
					refundWindow.open();
				}
				//JOptionPane.showMessageDialog(null, m);
			}
		});
		btnSubmit.setBounds(114, 204, 88, 29);
		btnSubmit.setText("Submit");

	}
}
