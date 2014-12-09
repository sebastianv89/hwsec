package terminal;


import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import backend.BackendRentalTerminal;

public class RegisterNewCustomer {

	protected Shell shell;
	private Text txt_CustName;
	protected static RegisterNewCustomer window;
	
	BackendRentalTerminal rt = new BackendRentalTerminal();

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			window = new RegisterNewCustomer();
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

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell = new Shell();
		shell.setSize(335, 281);
		shell.setText("Smartcar Application - Register New Customer");
		
		final Label lblNewCustomer = new Label(shell, SWT.NONE);
		lblNewCustomer.setBounds(20, 199, 68, 17);

		
		Label lblCustomerName = new Label(shell, SWT.NONE);
		lblCustomerName.setBounds(20, 65, 122, 17);
		lblCustomerName.setText("Customer Name");
		
		Label lblCustomerRegistration = new Label(shell, SWT.NONE);
		lblCustomerRegistration.setAlignment(SWT.CENTER);
		lblCustomerRegistration.setText("Customer Registration");
		lblCustomerRegistration.setBounds(55, 10, 196, 27);
		
		txt_CustName = new Text(shell, SWT.BORDER);
		txt_CustName.setBounds(140, 65, 147, 27);
		
		Button btnSubmit = new Button(shell, SWT.NONE);
		btnSubmit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//JOptionPane.showMessageDialog(null, txt_CustName.getText());
				rt.RegisterNewCustomer(txt_CustName.getText());
				lblNewCustomer.setText(txt_CustName.getText());
			}
		});
		btnSubmit.setBounds(121, 126, 88, 29);
		btnSubmit.setText("Submit");
		
	}
}
