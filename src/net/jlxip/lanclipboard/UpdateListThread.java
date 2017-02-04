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
	
	public UpdateListThread(JLabel state, JList<String> list) {
		this.state = state;
		this.list = list;
	}
	
	public void run() {
		state.setText("State: searching...");
		
		ArrayList<String> hosts = null;
		
		ArrayList<String> IPv4s = LAN.getIPv4s();
		
		/*
		 * WARNING
		 * MAYBE THE FIRST FIELD IN THE ARRAYLIST IPv4s IS NOT ALWAYS 127.0.0.1
		 * IN THAT CASE, CHANGE THE INITIAL VALUE OF THE FOR LOOP TO ZERO
		 * */
		
		for(int i=1;i<IPv4s.size();i++) {	// Skip the first one (127.0.0.1)
			String subnet = "";
			String[] dots = Pdot.split(IPv4s.get(i));
			for(int j=0;j<dots.length-1;j++) {	// All but the last one
				subnet += dots[j];
				if(j != dots.length-2) {
					subnet += ".";
				}
			}
			
			ArrayList<String> reachableHosts = LAN.getHosts(subnet);
			hosts = reachableHosts;
		}
		
		for(int i=0;i<hosts.size();i++) {
			DefaultListModel<String> model = new DefaultListModel<String>();
			model.addElement(hosts.get(i));
			list.setModel(model);
		}
		
		state.setText("State: finished.");
	}
}
