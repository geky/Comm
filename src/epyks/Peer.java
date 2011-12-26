package epyks;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import comm.Connection;
import comm.Contact;
import comm.StatusListener;

public class Peer extends Contact implements StatusListener {

	public final PeerPanel panel;
	
	private String name;
	private ImageIcon pic;
	
	private Set<Byte> plugins;
	
	public Peer(Connection c, Epyks e, PeerPanel p) {
		super(c,p);
		name = c.toString();
		panel = p;
		panel.makeLostPanel(null, c);
	}
	
	public void setInit(String n, HashSet<Byte> s) {
		name = n;
		plugins = s;
	}
	
	public String getName() {
		return name;
	}

	public ImageIcon getIcon() {
		return pic;
	}

	@Override
	public void status(String s) {
		panel.message(s, Color.BLACK);
	}

	@Override
	public void error(String s) {
		panel.message(s, Color.RED);
	}

	@Override
	public void join() {
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						synchronized (this) {
							panel.makeActivePanel(pic,name,connection);
						}
					}
				}
			);
	}

	@Override
	public void lose() {
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						synchronized (this) {
							panel.makeLostPanel(name,connection);
						}
					}
				}
			);
	}
}
