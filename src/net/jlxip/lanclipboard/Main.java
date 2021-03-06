package net.jlxip.lanclipboard;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Main extends JFrame {
	static final int PORT = 24812;
	
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JPasswordField password;
	private JList<String> list;
	private JLabel state;
	private JCheckBox protectWithPassword;
	private JButton btnStartSharing;
	private JButton quickRun;
	private JCheckBox exitWhenFinishedClient;
	private JCheckBox exitWhenFinishedServer;
	private JSlider timeoutSlider;
	private JLabel lblMaxWrongPassword;
	private JSlider maxWrongPasswordAttemptsSlider;
	
	private static final Pattern Pfile = Pattern.compile(Pattern.quote("|"));
	private static final Pattern Pfilename = Pattern.compile(Pattern.quote("$"));
	private static final Pattern Pspace = Pattern.compile(Pattern.quote(" "));
	
	private static String encryptionPassword = "";
	private static String SALT = "";

	public static void main(String[] args) {
		try {
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if ("Nimbus".equals(info.getName())) {
		            UIManager.setLookAndFeel(info.getClassName());
		            break;
		        }
		    }
		} catch (Exception e) {
		    // If Nimbus is not available, you can set the GUI to another look and feel.
		}
		
		Main frame = new Main();
		frame.setVisible(true);
	}

	public Main() {
		setTitle("LANClipboard");
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 436, 415);
		setLocationRelativeTo(null);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setSize(432, 338);
		tabbedPane.setLocation(0, 42);
		contentPane.add(tabbedPane);
		
		JPanel panel = new JPanel();
		tabbedPane.addTab("Receive", null, panel, null);
		panel.setLayout(null);
		
		JButton btnUpdate = new JButton("Update");
		btnUpdate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				update();
			}
		});
		btnUpdate.setBounds(12, 13, 97, 25);
		panel.add(btnUpdate);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 51, 284, 161);
		panel.add(scrollPane);
		
		list = new JList<String>();
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(arg0.getButton() == MouseEvent.BUTTON1 && arg0.getClickCount() == 2 && !list.isSelectionEmpty()) {
					int timeout = 0;
					switch(timeoutSlider.getValue()) {
						case 0:
							timeout = 500;
							break;
						case 1:
							timeout = 1000;
							break;
						case 2:
							timeout = 2500;
							break;
						case 3:
							timeout = 5000;
							break;
					}
					CheckPortOpenThread.timeout = timeout;
					
					String host = Pspace.split(list.getSelectedValue())[Pspace.split(list.getSelectedValue()).length-1];	// LAST SPACE OF SELECTED VALUE
					
					try {
						Socket socket = new Socket(host, PORT);
						OutputStream os = socket.getOutputStream();
						os.write(0x00);
						
						InputStream is = socket.getInputStream();
						
						byte[] usePassword = new byte[1];
						is.read(usePassword);
						if(usePassword[0] == 0x01) {
							byte[] bSALT = new byte[CryptoFunctions.SALT_LENGTH];
							while(!(is.available() == CryptoFunctions.SALT_LENGTH)) { // Waiting until the whole salt is sent.
								try {
									Thread.sleep(10);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							is.read(bSALT);
							SALT = new String(bSALT);
							
							state.setText("State: asking password.");
							String password = EnterPassword.run();
							encryptionPassword = CryptoFunctions.hash(password, SALT);
							byte[] passwordConfirmation = CryptoFunctions.AESencrypt(encryptionPassword, SALT, CryptoFunctions.PASSWORD_CONFIRMATION_STRING.getBytes());
							passwordConfirmation = Base64.getEncoder().encode(passwordConfirmation);
							
							byte[] endedPasswordConfirmation = new byte[passwordConfirmation.length + 1];
							for(int i=0;i<passwordConfirmation.length;i++) endedPasswordConfirmation[i] = passwordConfirmation[i];
							endedPasswordConfirmation[endedPasswordConfirmation.length - 1] = (byte)0x90;
							
							os.write(endedPasswordConfirmation);
							byte[] correct = new byte[1];
							is.read(correct);
							if(correct[0] == 0x01) {
								state.setText("State: disconnected.");
								JOptionPane.showMessageDialog(null, "Incorrect password.");
								socket.close();
								return;
							}
						}
						
						byte[] firstByte = new byte[1];
						is.read(firstByte);
						
						ArrayList<Byte> allContent = new ArrayList<Byte>();
						
						while(true) {
							byte[] onebyte = new byte[1];
							is.read(onebyte);
							if(onebyte[0] != (byte)0x90) allContent.add(onebyte[0]);
							else break;
						}
						
						byte[] all = new byte[allContent.size()+1];
						all[0] = firstByte[0];
						for(int i=0;i<allContent.size();i++) {
							all[i+1] = allContent.get(i);
						}
						
						if(usePassword[0] == 0x01) {
							all = Base64.getDecoder().decode(all);	// It's in BASE64
							all = CryptoFunctions.AESdecrypt(encryptionPassword, SALT, all);	// Decrypt it
						}
						
						byte[] content = new byte[all.length-1];
						for(int i=0;i<content.length;i++) content[i] = all[i+1];
						
						if(all[0] == 0x00) {	// STRING
							String text = new String(Base64.getDecoder().decode(content));
							Clipboard.setClipboard(text);
						} else {	// FILE
							String Scontent = new String(content);
							String[] files = Pfile.split(Scontent);
							List<File> Lfiles = new ArrayList<File>();
							for(int i=0;i<files.length;i++) {
								String filename = new String(Base64.getDecoder().decode(Pfilename.split(files[i])[0].getBytes()));
								byte[] Bfile = Base64.getDecoder().decode(Pfilename.split(files[i])[1].getBytes());
								
								String TEMPORAL_PATH = "";
								if(System.getProperty("os.name").toLowerCase().contains("windows")) {
									TEMPORAL_PATH = System.getenv("TEMP");
								} else if(System.getProperty("os.name").toLowerCase().contains("linux")) {
									TEMPORAL_PATH = "/tmp";
								}	// TODO: Mac OS X
								
								File file = new File(TEMPORAL_PATH + File.separator + filename);
								FileOutputStream fos = new FileOutputStream(file);
								fos.write(Bfile);
								fos.close();
								
								Lfiles.add(file);
							}
							
							Clipboard.setClipboard(Lfiles);
						}
						
						socket.close();
						state.setText("State: finished.");
						
						if(exitWhenFinishedClient.isSelected()) {
							System.exit(0);
						}
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
		scrollPane.setViewportView(list);
		
		state = new JLabel("State: finished.");
		state.setBounds(12, 225, 405, 16);
		panel.add(state);
		
		exitWhenFinishedClient = new JCheckBox("Exit when finished");
		exitWhenFinishedClient.setSelected(true);
		exitWhenFinishedClient.setBounds(117, 13, 131, 25);
		panel.add(exitWhenFinishedClient);
		
		timeoutSlider = new JSlider();
		timeoutSlider.setPaintLabels(true);
		timeoutSlider.setPaintTicks(true);
		timeoutSlider.setMajorTickSpacing(1);
		timeoutSlider.setToolTipText("Timeout of 1000ms");
		timeoutSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				switch(timeoutSlider.getValue()) {
					case 0:
						timeoutSlider.setToolTipText("Timeout of 500ms");
						break;
					case 1:
						timeoutSlider.setToolTipText("Timeout of 1000ms");
						break;
					case 2:
						timeoutSlider.setToolTipText("Timeout of 2500ms");
						break;
					case 3:
						timeoutSlider.setToolTipText("Timeout of 5000ms");
						break;
				}
			}
		});
		timeoutSlider.setValue(1);
		timeoutSlider.setMaximum(3);
		timeoutSlider.setBounds(308, 77, 109, 52);
		panel.add(timeoutSlider);
		
		JLabel lblScanTimeout = new JLabel("Scan timeout:");
		lblScanTimeout.setToolTipText("Each attempt");
		lblScanTimeout.setBounds(308, 53, 109, 16);
		panel.add(lblScanTimeout);
		
		JPanel panel_1 = new JPanel();
		tabbedPane.addTab("Share", null, panel_1, null);
		panel_1.setLayout(null);
		
		btnStartSharing = new JButton("START SHARING");
		btnStartSharing.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				startSharing();
			}
		});
		btnStartSharing.setBounds(8, 242, 409, 53);
		panel_1.add(btnStartSharing);
		
		exitWhenFinishedServer = new JCheckBox("Exit when finished");
		exitWhenFinishedServer.setBounds(12, 20, 148, 25);
		panel_1.add(exitWhenFinishedServer);
		
		JPanel securityPanel = new JPanel();
		securityPanel.setBorder(new TitledBorder(null, "Security", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		securityPanel.setBounds(12, 54, 405, 175);
		panel_1.add(securityPanel);
		securityPanel.setLayout(null);
		
		password = new JPasswordField();
		password.setBounds(166, 23, 227, 28);
		securityPanel.add(password);
		password.setEnabled(false);
		
		maxWrongPasswordAttemptsSlider = new JSlider();
		maxWrongPasswordAttemptsSlider.setBounds(12, 92, 256, 70);
		securityPanel.add(maxWrongPasswordAttemptsSlider);
		maxWrongPasswordAttemptsSlider.setEnabled(false);
		maxWrongPasswordAttemptsSlider.setMinimum(1);
		maxWrongPasswordAttemptsSlider.setValue(10);
		maxWrongPasswordAttemptsSlider.setMaximum(20);
		maxWrongPasswordAttemptsSlider.setMinorTickSpacing(1);
		maxWrongPasswordAttemptsSlider.setMajorTickSpacing(19);
		maxWrongPasswordAttemptsSlider.setPaintLabels(true);
		maxWrongPasswordAttemptsSlider.setPaintTicks(true);
		
		lblMaxWrongPassword = new JLabel("Max. wrong password attempts:");
		lblMaxWrongPassword.setBounds(12, 71, 200, 16);
		securityPanel.add(lblMaxWrongPassword);
		lblMaxWrongPassword.setEnabled(false);
		
		protectWithPassword = new JCheckBox("Protect with password");
		protectWithPassword.setBounds(8, 24, 155, 25);
		securityPanel.add(protectWithPassword);
		protectWithPassword.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				password.setEnabled(protectWithPassword.isSelected());
				lblMaxWrongPassword.setEnabled(protectWithPassword.isSelected());
				maxWrongPasswordAttemptsSlider.setEnabled(protectWithPassword.isSelected());
			}
		});
		
		JLabel lblQuickAccess = new JLabel("Quick access:");
		lblQuickAccess.setBounds(12, 13, 108, 16);
		contentPane.add(lblQuickAccess);
		
		quickRun = new JButton("Run server with default configuration");
		quickRun.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				startSharing();
			}
		});
		quickRun.setBounds(132, 9, 286, 25);
		contentPane.add(quickRun);
		
		update();
	}
	
	private void update() {
		new UpdateListThread(state, list).start();
	}
	
	private void startSharing() {
		try {
			if(protectWithPassword.isSelected() && new String(password.getPassword()).equals("")) {
				JOptionPane.showMessageDialog(null, "The password field is empty!");
				return;
			}
			
			ServerSocket ss = new ServerSocket(PORT);

			new SocketThread(ss, protectWithPassword.isSelected(), new String(password.getPassword()), exitWhenFinishedServer.isSelected(), maxWrongPasswordAttemptsSlider.getValue()).start();
			
			quickRun.setEnabled(false);
			btnStartSharing.setEnabled(false);
			btnStartSharing.setText("SHARING");
			exitWhenFinishedServer.setEnabled(false);
			protectWithPassword.setEnabled(false);
			password.setEnabled(false);
			lblMaxWrongPassword.setEnabled(false);
			maxWrongPasswordAttemptsSlider.setEnabled(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
