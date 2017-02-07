package net.jlxip.lanclipboard;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class CheckPortOpenThread extends Thread {
	ArrayList<String> reachableHosts;
	String subnet;
	int i;
	
	public CheckPortOpenThread(ArrayList<String> reachableHosts, String subnet, int i) {
		this.reachableHosts = reachableHosts;
		this.subnet = subnet;
		this.i = i;
	}
	
	public static int timeout = 1000;	// The value is changed when double-clicking from the JSlider ´timeoutSlider´
	public void run() {
		String host=subnet + "." + i;
		
		try {
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress(host, Main.PORT), timeout);
			System.out.println("Found server: " + host);
			socket.getOutputStream().write(0x01);
			byte[] recv = new byte[1];
			socket.getInputStream().read(recv);
			if(recv[0] == 0x00) {
				LAN.addReachableHost(host);	// FREE
			} else if(recv[0] == 0x01) {
				LAN.addReachableHost("[PWD] "+host);	// PWD
			}
			socket.close();
		} catch(ConnectException e) {
			// It's ok.
		} catch (UnknownHostException e) {
			System.out.println("This is a bug of the author (E1)!");
		} catch (IOException e) {
			// It's ok.
		} catch(Exception e) {	// Other type of exception (?)
			System.out.println("This is a bug of the author (E2)!");
			e.printStackTrace();
		}
		
		LAN.incrementFinishedThreads();
		
		try {
			this.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}