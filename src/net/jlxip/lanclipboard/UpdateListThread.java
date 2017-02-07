package net.jlxip.lanclipboard;

import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;

public class UpdateListThread extends Thread {
	JLabel state = null;
	JList<String> list = null;
	private static final Pattern Pdot = Pattern.compile(Pattern.quote("."));
	private static final Pattern Pspace = Pattern.compile(Pattern.quote(" "));
	
	public UpdateListThread(JLabel state, JList<String> list) {
		this.state = state;
		this.list = list;
	}
	
	public void run() {
		state.setText("State: searching...");
		
		ArrayList<String> hosts = new ArrayList<String>();
		
		ArrayList<String> IPv4s = LAN.getIPv4s();
		
		for(int i=0;i<IPv4s.size();i++) {
			String subnet = "";
			String[] dots = Pdot.split(IPv4s.get(i));
			for(int j=0;j<dots.length-1;j++) {	// All but the last one
				subnet += dots[j];
				if(j != dots.length-2) {
					subnet += ".";
				}
			}
			
			ArrayList<String> reachableHosts = LAN.getHosts(subnet);
			
			for(int j=0;j<reachableHosts.size();j++) {
				String toAdd = "";
				String realHost = Pspace.split(reachableHosts.get(j))[Pspace.split(reachableHosts.get(j)).length-1];
				
				if(realHost.equals(IPv4s.get(i))) {
					toAdd += "[ME] ";
				}
				
				toAdd += reachableHosts.get(j);
				hosts.add(toAdd);
			}
		}
		
		DefaultListModel<String> model = new DefaultListModel<String>();
		for(int i=0;i<hosts.size();i++) {
			model.addElement(hosts.get(i));
		}
		list.setModel(model);
		
		state.setText("State: finished.");
		
		try {
			this.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
