package terminal;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;


public class VTCases {

	protected static Shell shell;
	private String message = "";

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			VTCases window = new VTCases();
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
	public void setText(String text){
		this.message = text;
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell = new Shell();
		shell.setSize(450, 300);
		shell.setText("SWT Application");
		
		Label lbl_responses = new Label(shell, SWT.NONE);
		lbl_responses.setBounds(116, 94, 170, 17);
		lbl_responses.setText(message);

	}

}
