package epyks;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

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
	public synchronized void activate() {
		super.activate();
		panel.makeActivePanel(pic,name,connection);
	}

	@Override
	public synchronized void lose() {
		super.lose();
		panel.makeLostPanel(name,connection);
	}
}
