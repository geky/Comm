package epyks;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import comm.Connection;
import comm.Contact;

public class PeerPanel extends JPanel {
	
	public static final ImageIcon DEFAULT_PIC;
	
	static {
		ImageIcon temp;
		try {
			temp = new ImageIcon(ImageIO.read(new File("data"+File.separator+"defaultpic.png")));
		} catch (IOException e) {
			temp = null;
			e.printStackTrace();
		}
		DEFAULT_PIC = temp;
	}
	
	public static ImageIcon loadImage(Connection c) throws IOException {
		return new ImageIcon(ImageIO.read(new File("data/" + c==null?"user":c.toString() + "pic.png")));
	}
	
	private JLabel jpic;
	private JLabel jname;
	private JLabel jaddress;
	private JLabel jping;
	
	private final Epyks source;
	
	public PeerPanel(Epyks e) {
		super(new GridBagLayout());
		source = e;
	}
	
	public void makeUserPanel(ImageIcon pic,String name) {		
		removeAll();
		
		GridBagConstraints gbc = new GridBagConstraints();
		
		jpic = new JLabel(pic!=null?pic:DEFAULT_PIC);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		add(jpic,gbc);

		jping = null;
        
		jname = new JLabel(name!=null?name:"Username");
		jname.setFont(jname.getFont().deriveFont(Font.BOLD,12));
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		add(jname,gbc);
		
		jaddress = new JLabel("initializing...");
		jaddress.setFont(jaddress.getFont().deriveFont(10f));
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		add(jaddress,gbc);
		
		setMaximumSize(this.getPreferredSize());
		revalidate();
		repaint();
	}
	
	public void makeActivePanel(ImageIcon pic, String name, Connection conn) {
		removeAll();
		
		GridBagConstraints gbc = new GridBagConstraints();
		
		jpic = new JLabel(pic!=null?pic:DEFAULT_PIC);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 2;
		add(jpic,gbc);
		
		JButton jexit = new JButton("x");
        jexit.setMargin(new Insets(0,4,0,3));
        jexit.addActionListener(source.new Remover(conn));
        gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
        add(jexit,gbc);
        
        jping = new JLabel();
        gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		add(jping,gbc);
        
		jname = new JLabel(name!=null?name:conn.toString());
		jname.setFont(jname.getFont().deriveFont(Font.BOLD,12));
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		add(jname,gbc);
		
		jaddress = new JLabel(conn.toString());
		jaddress.setFont(jaddress.getFont().deriveFont(10f));
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		add(jaddress,gbc);
		
		setMaximumSize(this.getPreferredSize());
		revalidate();
		repaint();
	}
	
	public void makeLostPanel(String name, Connection conn) {
		removeAll();
		
		GridBagConstraints gbc = new GridBagConstraints();
		
		jpic = new JLabel("Not Connected");
		jpic.setFont(jpic.getFont().deriveFont(Font.BOLD,12));
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		add(jpic,gbc);
		
		JButton jretry = new JButton("\u21BB");
		jretry.setMargin(new Insets(0,4,0,3));
		jretry.addActionListener(source.new Retry(conn));
        gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
        add(jretry,gbc);
		
		JButton jexit = new JButton("x");
        jexit.setMargin(new Insets(0,4,0,3));
        jexit.addActionListener(source.new Remover(conn));
        gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
        add(jexit,gbc);
        
        jping = null;
        
		jname = new JLabel(name!=null?name:conn.toString());
		jname.setFont(jname.getFont().deriveFont(Font.BOLD,12));
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		gbc.gridheight = 1;
		add(jname,gbc);
		
		jaddress = new JLabel(conn.toString());
		jaddress.setFont(jaddress.getFont().deriveFont(10f));
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 3;
		gbc.gridheight = 1;
		add(jaddress,gbc);
		
		setMaximumSize(this.getPreferredSize());
		revalidate();
		repaint();
	}
	
	public void makePendingPanel(Connection conn) {
		removeAll();
		
		GridBagConstraints gbc = new GridBagConstraints();
		
		jpic = new JLabel("Pending");
		jpic.setFont(jpic.getFont().deriveFont(Font.BOLD,12));
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		add(jpic,gbc);
		
		JButton jaccept = new JButton("\u2713");
        jaccept.setMargin(new Insets(0,4,0,3));
        jaccept.addActionListener(source.new Accept(conn));
        gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
        add(jaccept,gbc);
		
        JButton jexit = new JButton("x");
        jexit.setMargin(new Insets(0,4,0,3));
        jexit.addActionListener(source.new Remover(conn));
        gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
        add(jexit,gbc);
        
        jping = null;
        
		jname = new JLabel("status");
		jname.setFont(jname.getFont().deriveFont(Font.BOLD,12));
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		gbc.gridheight = 1;
		add(jname,gbc);
		
		jaddress = new JLabel(conn.toString());
		jaddress.setFont(jaddress.getFont().deriveFont(10f));
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 3;
		gbc.gridheight = 1;
		add(jaddress,gbc);
		
		setMaximumSize(this.getPreferredSize());
		revalidate();
		repaint();
	}
	
	public void setName(final String s) {
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						jname.setText(s);
						repaint();
					}
				}
			);
	}
	
	public void setPic(final ImageIcon i) {
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						jpic.setIcon(i);
						repaint();
					}
				}
			);
	}
	
	public void message(final String m, final Color c) {		
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					jaddress.setText(m);
					jaddress.setForeground(c);
					repaint();
				}
			}
		);
	}
	
//	private class Exit implements ActionListener {
//		private Contact owner;
//		
//		public Exit(Contact o) {
//			owner = o;
//		}
//		
//		@Override
//		public void actionPerformed(ActionEvent arg0) {
//			owner.active = false;
//			Epyks.remove(owner);
//		}
//	}
	
//	private class Exit implements ActionListener {
//		@Override
//		public void actionPerformed(ActionEvent arg0) {
//			Peer.this.active = false;
//			Epyks.remove(Peer.this);
//		}
//	}
}
