package terminal;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;

import backend.BackendRentalTerminal;

public class Refund {

	protected Shell shell;
	private Text txt_CardKm;
	private Label label;
	private Text text;
	private Button btnRefund;
	Card card = new Card();
	BackendRentalTerminal rt = new BackendRentalTerminal();

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Refund window = new Refund();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setCard(Card card){
		this.card = card;
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
		shell.setSize(336, 274);
		shell.setText("SWT Application");
		
		Label lblNewLabel = new Label(shell, SWT.NONE);
		lblNewLabel.setBounds(23, 69, 163, 17);
		lblNewLabel.setText("Kilometer in the card");
		
		txt_CardKm = new Text(shell, SWT.BORDER);
		txt_CardKm.setEditable(false);
		txt_CardKm.setBounds(215, 59, 83, 27);
		txt_CardKm.setText(Short.toString(card.getKilometers()));
		
		Label lblRefund = new Label(shell, SWT.NONE);
		lblRefund.setBounds(134, 21, 56, 17);
		lblRefund.setText("Refund");
		
		label = new Label(shell, SWT.NONE);
		label.setText("Kilometer in the card");
		label.setBounds(23, 178, 163, 17);
		
		text = new Text(shell, SWT.BORDER);
		text.setEditable(false);
		text.setBounds(215, 168, 83, 27);
		
		btnRefund = new Button(shell, SWT.NONE);
		btnRefund.setBounds(121, 109, 88, 29);
		btnRefund.setText("Submit");
		btnRefund.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				card = rt.refundKilometers(card);
				text.setText(Short.toString(card.getKilometers()));
			}
		});

	}

}
