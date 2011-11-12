package epyks;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import comm.Connection;
import comm.Contact;

public class Peer extends Contact {

	public final PeerPanel panel;
	
	private String name;
	private ImageIcon pic;
	
	public Peer(String n, Connection c) {
		super(c);
		panel = new PeerPanel();
		panel.makeLostPanel(n, null);
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
	public void activate() {
		super.activate();
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						panel.makeActivePanel(pic,name,connection);
					}
				}
			);
	}

	@Override
	public void lose() {
		super.lose();
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						panel.makeLostPanel(name,connection);
					}
				}
			);
	}
}
