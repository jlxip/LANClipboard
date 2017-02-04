package net.jlxip.lanclipboard;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

public class LAN {
	public static ArrayList<String> getHosts(String subnet) {	// Very root source: https://goo.gl/9X75Ft
		ArrayList<String> reachableHosts = new ArrayList<String>();
		
		for(int i=1;i<255;i++) {	// 255 THREADS? WOW, THAT SOUNDS LIKE A LOT
			new CheckPortOpenThread(reachableHosts, subnet, i).start();	// But it works
		}
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return reachableHosts;
	}
	
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
			if(addresses.hasMoreElements()) {
				String IP = addresses.nextElement().getHostAddress();
				if(IP.length() < 16) {	// LENGTH(255.255.255.255) = 15
					IPs.add(IP);	// We just need the first one, and IPv4.
				}
			}
		}
		
		return IPs;
	}
}
