package net.jlxip.lanclipboard;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

public class SocketThread extends Thread {
	ServerSocket ss = null;
	Boolean usePassword = null;
	String Hpassword = null;
	Boolean exitWhenFinished = null;
	private static final String SALT = CryptoFunctions.generateRandomSalt();	// Random salt!
	
	// BRUTE FORCE PROTECTION
	private static final int LimitFailedPasswords = 10;		// TODO: Let user choose the limit.
	private static int FailedPasswords = 0;
	
	public SocketThread(ServerSocket ss, Boolean usePassword, String password, Boolean exitWhenFinished) {
		this.ss = ss;
		this.usePassword = usePassword;
		this.exitWhenFinished = exitWhenFinished;
		
		this.Hpassword = CryptoFunctions.hash(password, SALT);
	}
	
	public void run() {
		Boolean exit = false;
		
		while(!exit) {
			if(FailedPasswords >= LimitFailedPasswords) {
				try {
					ss.close();
				} catch (IOException e) {
					System.out.println("The server could not be stopped.");
					e.printStackTrace();
				}
				
				JOptionPane.showMessageDialog(null, "There were " + LimitFailedPasswords + " failed attempts of authentication.\nFor security reasons, the server has been stopped. Please, restart the program and choose a diferent password.");
				System.exit(0);
			}
			
			try {
				Socket s = ss.accept();
				byte[] recv = new byte[1];
				OutputStream os = s.getOutputStream();
				InputStream is = s.getInputStream();
				s.getInputStream().read(recv);
				if(recv[0] == 0x01) {	// Up?
					if(usePassword) s.getOutputStream().write(0x01);	// Sure! PWD!
					else s.getOutputStream().write(0x00);	// Sure! FREE!
				} else {
					if(exitWhenFinished) exit = true;
					
					if(usePassword) {
						os.write(new byte[]{ 0x01 });
						
						os.write(SALT.getBytes()); // SEND SALT
						
						byte[] firstByte = new byte[1];
						is.read(firstByte);
						byte[] restBytes = new byte[is.available()];
						is.read(restBytes);
						
						String enteredPassword = new String(firstByte) + new String(restBytes);
						
						if(enteredPassword.equals(Hpassword)) {
							os.write(new byte[]{ 0x00 });
						} else {	// Wrong password! :(
							FailedPasswords++;
							os.write(new byte[]{ 0x01 });
							s.close();
							continue;
						}
					} else {
						os.write(new byte[]{ 0x00 });
					}
					
					ArrayList<Byte> arrayToSend = new ArrayList<Byte>();
					
					byte end = (byte)0x90;
					
					Object[] data = Clipboard.readClipboard();
					if((int)data[0] == 0) {	// TEXT
						byte type = 0x00;
						arrayToSend.add(type);
						String str = (String)data[1];
						byte[] Bstr = Base64.getEncoder().encode(str.getBytes());
						for(int i=0;i<Bstr.length;i++) {
							arrayToSend.add(Bstr[i]);
						}
					} else if((int)data[0] == 1) {	// FILE(S)
						byte type = 0x01;
						arrayToSend.add(type);
						@SuppressWarnings("unchecked")
						List<File> files = (List<File>)data[1];
						Iterator<File> it = files.iterator();
						while(it.hasNext()) {
							File file = it.next();
							
							byte[] filename = Base64.getEncoder().encode(file.getName().getBytes());
							for(int i=0;i<filename.length;i++) {
								arrayToSend.add(filename[i]);
							}
							arrayToSend.add("$".getBytes()[0]);
							
							FileInputStream fis = new FileInputStream(file);
				            BufferedInputStream bis = new BufferedInputStream(fis);
				            byte[] array = new byte[bis.available()];
				            bis.read(array);
				            bis.close();
				            fis.close();
				            
				            byte[] B64file = Base64.getEncoder().encode(array);
				            for(int j=0;j<B64file.length;j++) {
				            	arrayToSend.add(B64file[j]);
				            }
				            
				            if(it.hasNext()) {
				            	arrayToSend.add("|".getBytes()[0]);
				            }
						}
					}
					arrayToSend.add(end);
					
					byte[] toSend = new byte[arrayToSend.size()];
					for(int i=0;i<arrayToSend.size();i++) {
						toSend[i] = arrayToSend.get(i);
					}
					
					s.getOutputStream().write(toSend);
				}
				
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		System.exit(0);
	}
}
