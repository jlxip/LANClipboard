package net.jlxip.lanclipboard;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Pattern;

public class LAN {
	static int finishedThreads = 0;
	static ArrayList<String> reachableHosts = null;
	
	public static synchronized int incrementFinishedThreads() {
        return finishedThreads++;
    }
	
	public static synchronized void addReachableHost(String HOST) {
		reachableHosts.add(HOST);
	}
	
	public static ArrayList<String> getHosts(String subnet) {	// Very root source: https://goo.gl/9X75Ft
		finishedThreads = 0;
		reachableHosts = new ArrayList<String>();
		
		for(int i=1;i<255;i++) {	// 255 THREADS! xD
			new CheckPortOpenThread(reachableHosts, subnet, i).start();
		}
		
		while(finishedThreads < 254) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return reachableHosts;
	}
	
	private static final Pattern Pdot = Pattern.compile(Pattern.quote("."));
	public static ArrayList<String> getIPv4s() {
		ArrayList<String> IPs = new ArrayList<String>();
		
		Enumeration<NetworkInterface> interfaces = null;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		
		while(interfaces.hasMoreElements()) {
			NetworkInterface thisinterface = interfaces.nextElement();
			Enumeration<InetAddress> addresses = thisinterface.getInetAddresses();
			while(addresses.hasMoreElements()) {
				String IP = addresses.nextElement().getHostAddress();
				if(Pdot.split(IP).length == 4 && !IP.equals("127.0.0.1")) {	// 255.255.255.255 = 4 dots
					IPs.add(IP);
				}
			}
		}
		
		return IPs;
	}
}
