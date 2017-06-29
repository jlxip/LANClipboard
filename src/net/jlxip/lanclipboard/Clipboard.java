package net.jlxip.lanclipboard;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Clipboard {
	@SuppressWarnings("unchecked")
	public static Object[] readClipboard() {
		/*
		 * TYPES
		 * 0 = STRING
		 * 1 = FILE
		 * */
		int type = -1;
		String type0 = null;
		List<File> type1 = null;
		
		try {
			DataFlavor[] b = Toolkit.getDefaultToolkit().getSystemClipboard().getAvailableDataFlavors();
			if(b[0].equals(DataFlavor.stringFlavor) || b[0].toString().toLowerCase().contains("text")) {	// STRING
				type = 0;
				type0 = (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
				if(type0 == null) type0 = "";	// Is this necessary?
				
				if(System.getProperty("os.name").equals("Linux")) {	// Clipboard on linux works like this
					if(b[0].toString().contains("uri-list") || type0.substring(0, 8).equals("file:///")) {
						type = 1;
						Pattern Pendline = Pattern.compile(Pattern.quote("\r\n"));	// This is it
						String[] files = Pendline.split(type0);
						type1 = new ArrayList<File>();
						for(int i=0;i<files.length;i++) {
							if(type0.substring(0, 8).equals("file:///")) {
								type1.add(new File(files[i].substring(7)));
							} else {
								type1.add(new File(files[i]));
							}
						}
					}
				}
			} else if(b[0].equals(DataFlavor.javaFileListFlavor)) {	// FILE
				type = 1;
				type1 = (List<File>)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.javaFileListFlavor);
			} else {
				System.out.println("BUG BUG BUG BUG BUG");
				System.out.println("Please, submit this on the project issue board.");
				System.out.println("TYPE: "+b[0]);
			}
		} catch (HeadlessException e) {
			e.printStackTrace();
		} catch (UnsupportedFlavorException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Object[] toSend = new Object[2];
		toSend[0] = type;
		
		if(type == 0) {
			toSend[1] = type0;
		} else {
			toSend[1] = type1;
		}
		
		return toSend;
	}
	
	public static void setClipboard(String str) {
		StringSelection selection = new StringSelection(str);
	    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);		
	}
	
	public static void setClipboard(List<File> files) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
				@Override
				public DataFlavor[] getTransferDataFlavors() {
					return new DataFlavor[] { DataFlavor.javaFileListFlavor };
				}

				@Override
				public boolean isDataFlavorSupported(DataFlavor flavor) {
					return DataFlavor.javaFileListFlavor.equals(flavor);
				}

				@Override
				public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
					return files;
				}
			}, null);
	}
}