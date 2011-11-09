package epyks;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import comm.Connection;
import comm.Event;

public class User extends Plugin {

	public final PeerPanel peer;
	
	public User() {
		setName("Settings");
		
		try {
			Scanner sc = new Scanner(new File("data/settings"));
			sc.next();
			name = sc.next();
			sc.next();
			int p = sc.nextInt();
			pic = PeerPanel.loadImage(null, p);
			sc.close();
		} catch (Exception e) {
			File loc = new File("data/settings");
			System.err.println("faulty settings file: rewriting at " + loc.getAbsolutePath());
			
			try {
				FileOutputStream fo = new FileOutputStream(loc);
				fo.write("name: User\npic: 0".getBytes());
				fo.flush();
				fo.close();
			} catch (Exception e2) {
				System.err.println("Could not write file!");
				e2.printStackTrace();
			}
			
			name = "User";
			pic = PeerPanel.DEFAULT_PIC;
		}
		
		peer = PeerPanel.createUserPeer();
	}
	
	public void setUserName(final String n) {
		synchronized (this) {
			name = n;
		}
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					peer.jname.setText(n);
				}
			}
		);
	}
	
	public String getUserName() {
		synchronized (this) {
			return name;
		}
	}
	
	public void status(final String m,final Color c) {
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						peer.jaddress.setText(m!=null?m:connection.toString());
						peer.jaddress.setForeground(c!=null?c:Color.BLACK);
					}
				}
			);
	}
	
	public void setConnection(Connection c) {
		connection = c;
		status(c.toString(),null);
	}
	
	public byte getByte() {
		return 0xf;
	}
	public void pollData(ByteBuffer b) {}
	public void doData(PeerPanel p, ByteBuffer b) {}
	public void doEvent(Event e) {}
	
}
