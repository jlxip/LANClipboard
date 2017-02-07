package net.jlxip.lanclipboard;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class Main extends JFrame {
	static final int PORT = 24812;
	
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JPasswordField password;
	private JList<String> list;
	private JLabel state;
	private JCheckBox protectWithPassword;
	private JCheckBox exitWhenFinishedClient;
	private JCheckBox exitWhenFinishedServer;
	private JSlider timeoutSlider;
	
	private static final Pattern Pfile = Pattern.compile(Pattern.quote("|"));
	private static final Pattern Pfilename = Pattern.compile(Pattern.quote("$"));
	private static final Pattern Pspace = Pattern.compile(Pattern.quote(" "));

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
		setBounds(100, 100, 450, 327);
		setLocationRelativeTo(null);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setLocation(135, 5);
		contentPane.add(tabbedPane, BorderLayout.CENTER);
		
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
						socket.getOutputStream().write(0x00);
						
						InputStream is = socket.getInputStream();
						
						byte[] usePassword = new byte[1];
						is.read(usePassword);
						if(usePassword[0] == 0x01) {
							state.setText("State: asking password.");
							String password = EnterPassword.run();
							socket.getOutputStream().write(password.getBytes());
							byte[] correct = new byte[1];
							is.read(correct);
							if(correct[0] == 0x01) {
								state.setText("State: disconnected.");
								JOptionPane.showMessageDialog(null, "Incorrect password.");
								socket.close();
								return;
							}
						}
						
						byte[] type = new byte[1];
						is.read(type);
						
						ArrayList<Byte> allContent = new ArrayList<Byte>();
						
						while(is.available() > 0) {	// TODO: This is a non-orthodox solution.
							byte[] onebyte = new byte[1];
							is.read(onebyte);
							allContent.add(onebyte[0]);
						}
						
						byte[] content = new byte[allContent.size()];
						for(int i=0;i<allContent.size();i++) {
							content[i] = allContent.get(i);
						}
						
						
						if(type[0] == 0x00) {	// STRING
							Clipboard.setClipboard(new String(content));
						} else {	// FILE
							String Scontent = new String(content);
							String[] files = Pfile.split(Scontent);
							List<File> Lfiles = new ArrayList<File>();
							for(int i=0;i<files.length;i++) {
								String filename = new String(Base64.getDecoder().decode(Pfilename.split(files[i])[0].getBytes()));
								byte[] Bfile = Base64.getDecoder().decode(Pfilename.split(files[i])[1].getBytes());
								
								File file = new File(System.getenv("TEMP") + File.separator + filename);
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
		timeoutSlider.setBounds(256, 12, 161, 26);
		panel.add(timeoutSlider);
		
		JPanel panel_1 = new JPanel();
		tabbedPane.addTab("Send", null, panel_1, null);
		panel_1.setLayout(null);
		
		JButton btnStartSharing = new JButton("START SHARING");
		btnStartSharing.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					ServerSocket ss = new ServerSocket(PORT);

					new SocketThread(ss, protectWithPassword.isSelected(), new String(password.getPassword()), exitWhenFinishedServer.isSelected()).start();
					
					btnStartSharing.setEnabled(false);
					btnStartSharing.setText("SHARING");
					exitWhenFinishedServer.setEnabled(false);
					protectWithPassword.setEnabled(false);
					password.setEnabled(false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		btnStartSharing.setBounds(12, 159, 405, 53);
		panel_1.add(btnStartSharing);
		
		protectWithPassword = new JCheckBox("Protect with password");
		protectWithPassword.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				password.setEnabled(protectWithPassword.isSelected());
			}
		});
		protectWithPassword.setBounds(12, 65, 155, 25);
		panel_1.add(protectWithPassword);
		
		password = new JPasswordField();
		password.setEnabled(false);
		password.setBounds(175, 63, 242, 28);
		panel_1.add(password);
		
		exitWhenFinishedServer = new JCheckBox("Exit when finished");
		exitWhenFinishedServer.setBounds(12, 20, 148, 25);
		panel_1.add(exitWhenFinishedServer);
		
		update();
	}
	
	private void update() {
		new UpdateListThread(state, list).start();
	}
}
