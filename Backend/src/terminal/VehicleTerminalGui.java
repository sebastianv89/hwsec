package terminal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.events.SelectionAdapter;


public class VehicleTerminalGui {

	protected Shell shell;
	protected static VehicleTerminalGui window;
	VehicleTerminal vt = new VehicleTerminal();

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			window = new VehicleTerminalGui();
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
		shell.setSize(507, 531);
		shell.setText("Smartcar Application - Vehicle Terminal");
		
		final Label lblMessage = new Label(shell, SWT.NONE);
		lblMessage.setBounds(24, 125, 434, 348);
		lblMessage.setText("Message: ");
		
		Label lblVehicleTerminal = new Label(shell, SWT.NONE);
		lblVehicleTerminal.setBounds(112, 22, 130, 17);
		lblVehicleTerminal.setText("Vehicle Terminal");
		
		Button btnNewButton = new Button(shell, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String msg = "";
				msg = msg +  vt.startIgnition() + "\n ";
				// "drive" 5 km
				for (int i = 0; i < 5; ++i) {
					msg = msg + vt.driving() + "\n";
				}
				msg = msg + vt.stopVehicle() + "\n";
				lblMessage.setText(msg);
			}
		});
		btnNewButton.setBounds(77, 64, 190, 29);
		btnNewButton.setText("Start Vehicle Terminal");
		
		

	}
	
	public void close(){
		Display display = Display.getDefault();
		while(!shell.isDisposed()){
			display.dispose();
		}
	}
}
