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

public class TopUp {

	protected Shell shell;
	private Text txt_kmCard;
	private Text txt_km;
	private Text txt_kmAfterTopUp;
	Card card = new Card();
	BackendRentalTerminal rt = new BackendRentalTerminal();

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			TopUp window = new TopUp();
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
	
	public void setCard(Card card){
		this.card = card;
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell = new Shell();
		shell.setSize(378, 373);
		shell.setText("SWT Application");
		
		Label lblKilometersInThe_1 = new Label(shell, SWT.NONE);
		lblKilometersInThe_1.setText("Kilometers in the card after top up");
		lblKilometersInThe_1.setBounds(59, 247, 245, 17);
		
		txt_kmAfterTopUp = new Text(shell, SWT.BORDER);
		txt_kmAfterTopUp.setEditable(false);
		txt_kmAfterTopUp.setBounds(118, 270, 142, 27);
		
		Label lblNewLabel = new Label(shell, SWT.NONE);
		lblNewLabel.setBounds(153, 10, 53, 17);
		lblNewLabel.setText("Top Up");
		
		Label lblKilometersInThe = new Label(shell, SWT.NONE);
		lblKilometersInThe.setBounds(26, 73, 157, 17);
		lblKilometersInThe.setText("Kilometers in the card");
		
		txt_kmCard = new Text(shell, SWT.BORDER);
		txt_kmCard.setEditable(false);
		txt_kmCard.setBounds(193, 73, 142, 27);
		txt_kmCard.setText(Short.toString(card.getKilometers()));
		
		Label lblTopUpKilometer = new Label(shell, SWT.NONE);
		lblTopUpKilometer.setText("Top Up Kilometer");
		lblTopUpKilometer.setBounds(26, 131, 157, 17);
		
		txt_km = new Text(shell, SWT.BORDER);
		txt_km.setBounds(193, 131, 142, 27);
		
		Button btnSubmit = new Button(shell, SWT.NONE);
		btnSubmit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				card = rt.TopUpCard(card, Short.valueOf(txt_km.getText()));
				txt_kmAfterTopUp.setText(Short.toString(card.getKilometers()));
			}
		});
		btnSubmit.setBounds(143, 191, 88, 29);
		btnSubmit.setText("Submit");
		
		

	}
}
