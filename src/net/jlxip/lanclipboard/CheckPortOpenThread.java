package net.jlxip.lanclipboard;

import java.io.IOException;
import java.net.ConnectException;
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
	
	public void run() {
		String host=subnet + "." + i;
		
		try {
			Socket socket = new Socket(host, Main.PORT);
			socket.getOutputStream().write(0x01);
			byte[] recv = new byte[1];
			socket.getInputStream().read(recv);
			if(recv[0] == 0x00) {
				reachableHosts.add(host);
			}
			socket.close();
		} catch(ConnectException e) {
			// It's ok.
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
