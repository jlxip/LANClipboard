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
	String encryptionPassword = null;
	Boolean exitWhenFinished = null;
	private static final String SALT = CryptoFunctions.generateRandomSalt();	// Random salt!
	
	// BRUTE FORCE PROTECTION
	private static int LimitFailedPasswords = 10;	// Just in case something goes wrong (it shouldn't), I set it to 10.
	private static int FailedPasswords = 0;
	
	public SocketThread(ServerSocket ss, Boolean usePassword, String password, Boolean exitWhenFinished, int LimitFailedPasswords) {
		this.ss = ss;
		this.usePassword = usePassword;
		this.exitWhenFinished = exitWhenFinished;
		SocketThread.LimitFailedPasswords = LimitFailedPasswords;
		
		this.encryptionPassword = CryptoFunctions.hash(password, SALT);	// An encryption password of 128 bits (16 bytes) is required.
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
				
				JOptionPane.showMessageDialog(null, "There were " + LimitFailedPasswords + " failed attempts of authentication.\nFor security reasons, the server has been stopped.\nPlease, restart the program and choose a diferent password.");
				System.exit(0);
			}
			
			try {
				Socket s = ss.accept();
				byte[] recv = new byte[1];
				OutputStream os = s.getOutputStream();
				InputStream is = s.getInputStream();
				s.getInputStream().read(recv);
				if(recv[0] == 0x01) {	// Up?
					if(usePassword) os.write(0x01);	// Sure! PWD!
					else os.write(0x00);	// Sure! FREE!
				} else {
					if(exitWhenFinished) exit = true;
					
					if(usePassword) {
						os.write(new byte[]{ 0x01 });
						
						os.write(SALT.getBytes()); // SEND SALT
						
						byte[] firstByte = new byte[1];
						is.read(firstByte);
						ArrayList<Byte> restBytesArray = new ArrayList<Byte>();
						while(true) {
							byte[] onebyte = new byte[1];
							is.read(onebyte);
							if(onebyte[0] != (byte)0x90) restBytesArray.add(onebyte[0]);
							else break;
						}
						
						byte[] enteredPasswordConfirmation = new byte[1+restBytesArray.size()];
						enteredPasswordConfirmation[0] = firstByte[0];
						for(int i=0;i<restBytesArray.size();i++) enteredPasswordConfirmation[i+1] = restBytesArray.get(i);
						byte[] decodedEnteredPasswordConfirmation = Base64.getDecoder().decode(enteredPasswordConfirmation);
						
						byte[] correctPasswordConfirmation = CryptoFunctions.AESencrypt(encryptionPassword, SALT, CryptoFunctions.PASSWORD_CONFIRMATION_STRING.getBytes());
						
						// It's easier to check this way hahaha
						StringBuilder sbEPC = new StringBuilder();
						for (byte b : decodedEnteredPasswordConfirmation) sbEPC.append(String.format("%02X", b));
						StringBuilder sbCPC = new StringBuilder();
						for (byte b : correctPasswordConfirmation) sbCPC.append(String.format("%02X", b));
						
						if(sbEPC.toString().equals(sbCPC.toString())) {
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
					
					byte[] toSend = new byte[arrayToSend.size()];
					for(int i=0;i<arrayToSend.size();i++) {
						toSend[i] = arrayToSend.get(i);
					}
					
					if(usePassword) {
						toSend = CryptoFunctions.AESencrypt(encryptionPassword, SALT, toSend);	// Encrypt the data
						toSend = Base64.getEncoder().encode(toSend);	// Now in BASE64 to allow the final 0x90 byte
					}
					
					byte[] endedToSend = new byte[toSend.length + 1];
					for(int i=0;i<toSend.length;i++) endedToSend[i] = toSend[i];
					endedToSend[endedToSend.length-1] = end;
					
					os.write(endedToSend);
				}
				
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		System.exit(0);
	}
}
