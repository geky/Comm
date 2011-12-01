package epyks;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import comm.Connection;
import comm.Contact;

public class Peer extends Contact {

	public final PeerPanel panel;
	
	private String name;
	private ImageIcon pic;
	
	public Peer(Connection c, Epyks e) {
		super(c);
		panel = new PeerPanel(e);
		panel.makeLostPanel(null, c);
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
	public void connect() {
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

	@Override
	public void setData(ByteBuffer b) {
		byte[] temp = new byte[b.get()];
		b.get(temp);
		
		synchronized (this) {
			name = new String(temp);
		}
	}
}
