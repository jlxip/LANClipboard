package net.jlxip.lanclipboard;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

public class EnterPassword {

	public static String run() {	// SOURCE: https://goo.gl/5pFXao
		JPanel panel = new JPanel();
		JLabel label = new JLabel("This server is protected with a password.");
		JPasswordField pass = new JPasswordField(10);
		panel.add(label);
		panel.add(pass);
		String[] options = new String[]{"OK", "Cancel"};
		int option = JOptionPane.showOptionDialog(null, panel, "Enter password",
				JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
				null, options, options[0]);
		if(option == 0) { // OK button
			char[] password = pass.getPassword();
			return new String(password);
		} else {
			return null;
		}
	}
}
